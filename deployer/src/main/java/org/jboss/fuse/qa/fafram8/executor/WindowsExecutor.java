package org.jboss.fuse.qa.fafram8.executor;

import org.jboss.fuse.qa.fafram8.exception.ConnectionException;
import org.jboss.fuse.qa.fafram8.exceptions.SSHClientException;
import org.jboss.fuse.qa.fafram8.exceptions.VerifyFalseException;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Special executor for Windows machines that reconnects SSH client for each command. Reconnect is required for successful deployment on Windows.
 * It is used only internally in Fafram and automatically created when Fafram recognizes that it is deploying Fuse to Windows machine.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class WindowsExecutor extends Executor {

	/**
	 * Constructor.
	 *
	 * @param executor executor from which attributes will be copied
	 */
	public WindowsExecutor(Executor executor) {
		super.client = executor.getClient();
		this.name = executor.getName();
		super.history = executor.getHistory();
	}

	/**
	 * Silent reconnect that is needed for each command executed on Windows node.
	 */
	private void connectSilent() {
		log.trace("Connecting: " + this.toString());
		Boolean connected = false;
		final int step = 5;
		int elapsed = 0;
		final long timeout = step * 1000L;

		log.trace("Waiting for SSH connection ...");
		while (!connected) {
			// Check if the time is up
			if (elapsed > SystemProperty.getStartWaitTime()) {
				log.error("Connection couldn't be established after " + SystemProperty.getStartWaitTime()
						+ " seconds");
				throw new ConnectionException("Connection couldn't be established after "
						+ SystemProperty.getStartWaitTime() + " seconds");
			}
			try {
				client.connect(true);
				connected = true;
				log.trace("Connected to SSH server");
			} catch (VerifyFalseException ex) {
				log.trace("Remaining time: " + (SystemProperty.getStartWaitTime() - elapsed) + " seconds. ");
				elapsed += step;
			} catch (SSHClientException e) {
				elapsed += step;
			}
			Executor.sleep(timeout);
		}

		// When connected, schedule a new keep alive thread for this executor
		// First shutdown all other tasks from previous runs, because you can use .connect() without previous .disconnect()
		super.stopKeepAliveTimer();
		super.startKeepAliveTimer();
	}

	/**
	 * Silent reconnect that is executed before every executeCommand.
	 */
	private void reconnectSilently() {
		super.disconnect();
		connectSilent();
	}

	@Override
	public String executeCommandSilently(String cmd, boolean ignoreExceptions) {
		log.trace("Reconnecting executor on Windows before executing commands silently");
		reconnectSilently();
		return super.executeCommandSilently(cmd, ignoreExceptions);
	}

	@Override
	public String executeCommand(String cmd) {
		log.trace("Reconnecting executor on Windows before executing command");
		reconnectSilently();
		return super.executeCommand(cmd);
	}

	@Override
	public List<String> executeCommands(String... commands) {
		log.trace("Reconnecting executor on Windows before executing commands");
		reconnectSilently();
		return super.executeCommands(commands);
	}

	@Override
	public String executeCommandSilently(String cmd) {
		log.trace("Reconnecting executor on Windows before executing command silently");
		reconnectSilently();
		return super.executeCommandSilently(cmd);
	}
}
