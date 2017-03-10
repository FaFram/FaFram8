package org.jboss.fuse.qa.fafram8.ssh;

import org.apache.commons.io.IOUtils;

import org.jboss.fuse.qa.fafram8.exceptions.AuthFailException;
import org.jboss.fuse.qa.fafram8.exceptions.ConnectionRefusedException;
import org.jboss.fuse.qa.fafram8.exceptions.KarafSessionDownException;
import org.jboss.fuse.qa.fafram8.exceptions.NoRouteToHostException;
import org.jboss.fuse.qa.fafram8.exceptions.SSHClientException;
import org.jboss.fuse.qa.fafram8.exceptions.SessionTimeoutException;
import org.jboss.fuse.qa.fafram8.exceptions.VerifyFalseException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract class for SSHClients.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
@ToString
@EqualsAndHashCode
public abstract class SSHClient {
	static {
		// Set static logger for JSCH
		JSch.setLogger(new JSCHLogger());
	}

	@Getter
	@Setter
	protected String host = "localhost";

	@Getter
	@Setter
	protected int port = -1;

	@Getter
	@Setter
	protected String username;

	@Getter
	@Setter
	protected String password;

	@Getter
	@Setter
	protected String privateKey;

	@Getter
	@Setter
	protected String passphrase;

	@Getter
	@Setter
	protected Session session;

	@Getter
	@Setter
	protected Channel channel;

	protected JSch ssh = new JSch();

	private static final int DEFAULT_NODE_PORT = 22;
	private static final int DEFAULT_FUSE_PORT = 8101;

	/**
	 * Method for executing command on connected SSH server. Each implementation has some specific small hacks.
	 *
	 * @param command command to be executed
	 * @param suppressLog supress exception/command logging
	 * @return String containing response from command
	 * @throws KarafSessionDownException throws this exception if Karaf is down(specific for FuseSSHClient)
	 * @throws SSHClientException common exception for sshclient when there is some problem in connecting
	 * (auth fail, timeout, wrong host/port)
	 */
	public abstract String executeCommand(String command, boolean suppressLog) throws KarafSessionDownException,
			SSHClientException;

	/**
	 * Same as executeCommand(String command, boolean suppressLog), but with option to ignore exception logging.
	 *
	 * @param command command to be executed
	 * @param suppressLog supress exception/command logging
	 * @param ignoreExceptions ignore exceptions if true
	 * @return String containing response from command
	 * @throws KarafSessionDownException throws this exception if Karaf is down(specific for FuseSSHClient)
	 * @throws SSHClientException common exception for sshclient when there is some problem in connecting
	 * (auth fail, timeout, wrong host/port)
	 */
	public abstract String executeCommand(String command, boolean suppressLog, boolean ignoreExceptions) throws KarafSessionDownException,
			SSHClientException;

	/**
	 * Method for creating connection and session, that is is used in executeCommand() method.
	 *
	 * @param suppressLog supress exception logging
	 * @throws VerifyFalseException throw this exception when JschClient drop connection
	 * @throws SSHClientException common exception for sshclient when there is some problem in executing command
	 */
	public void connect(boolean suppressLog) throws VerifyFalseException, SSHClientException {
		final int sessionTimeout = 20000;
		try {
			if (privateKey != null) {
				if (passphrase != null) {
					ssh.addIdentity(privateKey, passphrase);
				} else {
					ssh.addIdentity(privateKey);
				}
			}

			session = ssh.getSession(username, host, port);

			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(password);

			session.connect(sessionTimeout);

			if (!suppressLog) {
				log.info("Connection established.");
			}
		} catch (JSchException ex) {
			if (ex.getMessage().contains("verify false")) {
				if (!suppressLog) {
					log.error("JschException caught - Verify false");
				}
				throw new VerifyFalseException(ex);
			}

			if (ex.getMessage().contains("timeout: socket is not established")) {
				if (!suppressLog) {
					log.error("Unable to connect to specified host: " + session.getHost() + ":" + session.getPort()
							+ " after " + sessionTimeout + " miliseconds");
				}
				throw new SessionTimeoutException("Unable to connect to specified host: " + session.getHost() + ":"
						+ session.getPort() + " after " + sessionTimeout + " miliseconds");
			}

			// This is common exception when host is still unreachable
			if (ex.getMessage().contains("Connection refused")) {
				if (!suppressLog) {
					log.error(ex.getLocalizedMessage());
				}
				throw new ConnectionRefusedException("Connection refused", ex);
			}

			if (ex.getMessage().contains("No route to host")) {
				// This happens if few first seconds when spawning machines but it can be also serious issue when wrong IP so log it in TRACE.
				log.trace(ex.getLocalizedMessage());
				throw new NoRouteToHostException("No route to host", ex);
			}

			if (ex.getMessage().contains("Auth fail")) {
				// This happens if few first seconds when spawning machines but it can be also serious issue so at least log it in TRACE
				log.trace(ex.getLocalizedMessage());
				throw new AuthFailException("Auth fail", ex);
			}

			if (!suppressLog) {
				log.error(ex.getLocalizedMessage());
			}
			throw new SSHClientException(ex);
		}
	}

	/**
	 * Disconnects channel and session.
	 */
	public void disconnect() {

		if (channel != null) {
			channel.disconnect();
		}
		if (session != null) {
			session.disconnect();
		}
	}

	/**
	 * Helper method for checking if session is connected.
	 *
	 * @return true if connected
	 */
	public Boolean isConnected() {
		return session != null && session.isConnected();
	}

	/**
	 * Helper method for converting Stream to String.
	 *
	 * @param is InputStream to be converted to String
	 * @return crated String from InputStream
	 * @throws IOException if there is some problem with conversion
	 */
	protected String convertStreamToString(java.io.InputStream is) throws IOException {
		return IOUtils.toString(is, "UTF-8");
	}

	/**
	 * Sets the host.
	 *
	 * @param host host
	 * @return this
	 */
	public SSHClient host(String host) {
		this.host = host;
		return this;
	}

	/**
	 * Sets the port.
	 *
	 * @param port port
	 * @return this
	 */
	public SSHClient port(int port) {
		this.port = port;
		return this;
	}

	/**
	 * Sets the username.
	 *
	 * @param username username
	 * @return this
	 */
	public SSHClient username(String username) {
		this.username = username;
		return this;
	}

	/**
	 * Sets the password.
	 *
	 * @param password password
	 * @return this
	 */
	public SSHClient password(String password) {
		this.password = password;
		return this;
	}

	/**
	 * Sets the private key.
	 *
	 * @param privateKey private key
	 * @return this
	 */
	public SSHClient privateKey(String privateKey) {
		this.privateKey = privateKey;
		return this;
	}

	/**
	 * Sets the passphrase.
	 *
	 * @param passphrase passphrase
	 * @return this
	 */
	public SSHClient passphrase(String passphrase) {
		this.passphrase = passphrase;
		return this;
	}

	/**
	 * Sets the default ssh port.
	 *
	 * @return this
	 */
	public SSHClient defaultSSHPort() {
		this.port = DEFAULT_NODE_PORT;
		return this;
	}

	/**
	 * Sets the default fuse port.
	 *
	 * @return this
	 */
	public SSHClient fuseSSHPort() {
		this.port = DEFAULT_FUSE_PORT;
		return this;
	}
}
