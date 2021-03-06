package org.jboss.fuse.qa.fafram8.ssh;

import org.jboss.fuse.qa.fafram8.exceptions.KarafSessionDownException;
import org.jboss.fuse.qa.fafram8.exceptions.SSHClientException;
import org.jboss.fuse.qa.fafram8.util.PasswordUtils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;

import java.io.IOException;
import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * Specific SSHClient for connecting to Fuse.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class FuseSSHClient extends SSHClient {

	/**
	 * Constructor.
	 */
	public FuseSSHClient() {
	}

	/**
	 * Copy constructor.
	 *
	 * @param client client to be copied
	 */
	public FuseSSHClient(SSHClient client) {
		log.trace("Creating copy of FuseSSHClient: " + client);
		this.host = client.getHost();
		this.port = client.getPort();
		this.username = client.getUsername();
		this.password = client.getPassword();
		this.privateKey = client.getPrivateKey();
		this.passphrase = client.getPassphrase();
	}

	@Override
	public String executeCommand(String command, boolean suppressLog, boolean ignoreExceptions) throws KarafSessionDownException, SSHClientException {
		if (!suppressLog) {
			log.info("Executing command: " + PasswordUtils.maskPassword(command));
		}
		final int retriesCount = 2;
		final long commandRetryTimeout = 1000 * Long.parseLong(System.getProperty("command.retry.timeout", "5"));
		try {
			// If we should retry the command
			boolean retry;

			int retries = 0;

			String returnString = "";
			do {
				if (retries == retriesCount) {
					// If we retried it 2 times already, break
					break;
				}

				channel = session.openChannel("exec");
				((ChannelExec) channel).setCommand(command);

				channel.setInputStream(null);
				((ChannelExec) channel).setErrStream(System.err);

				final InputStream in = channel.getInputStream();

				channel.connect();

				returnString = convertStreamToString(in);
				if (returnString.replaceAll("\u001B\\[[;\\d]*m", "").trim().startsWith("Command not found")) {
					if (!suppressLog) {
						log.debug("Retrying command in " + (commandRetryTimeout / 1000) + " seconds");
					}
					retry = true;
					retries++;
					// Wait for 5 sec before executing command
					try {
						Thread.sleep(commandRetryTimeout);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					retry = false;
				}
			} while (retry);

			returnString = returnString.replaceAll("\u001B\\[[;\\d]*m", "").trim();

			return returnString;
		} catch (JSchException ex) {
			if (ex.getMessage().contains("session is down")) {
				if (!suppressLog && !ignoreExceptions) {
					log.error("JschException caught - Session is down");
				}
				throw new KarafSessionDownException(ex);
			}
			if (!ignoreExceptions) {
				log.error("Cannot execute Fuse ssh command: \"" + command + "\"", ex);
			}
			throw new SSHClientException(ex);
		} catch (IOException ex) {
			log.error(ex.getLocalizedMessage());
			throw new SSHClientException(ex);
		}
	}

	@Override
	public String executeCommand(String command, boolean suppressLog) throws KarafSessionDownException, SSHClientException {
		return executeCommand(command, suppressLog, false);
	}
}
