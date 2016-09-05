package org.jboss.fuse.qa.fafram8.executor;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.exception.ConnectionException;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.exception.PatchException;
import org.jboss.fuse.qa.fafram8.exception.ProvisionException;
import org.jboss.fuse.qa.fafram8.exceptions.CopyFileException;
import org.jboss.fuse.qa.fafram8.exceptions.KarafSessionDownException;
import org.jboss.fuse.qa.fafram8.exceptions.SSHClientException;
import org.jboss.fuse.qa.fafram8.exceptions.VerifyFalseException;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.ssh.NodeSSHClient;
import org.jboss.fuse.qa.fafram8.ssh.SSHClient;
import org.jboss.fuse.qa.fafram8.timer.TimerUtils;
import org.jboss.fuse.qa.fafram8.util.ExecutorCommandHistory;
import org.jboss.fuse.qa.fafram8.util.callables.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Executor class. This class servers as a wrapper around ssh client and offers methods for waiting for karaf
 * startup, waiting for successful provision, etc.
 * Created by avano on 19.8.15.
 */
@Slf4j
@ToString(of = {"client"})
public class Executor {
	// waitfor - wait before next attempt (in milliseconds)
	public static final long DEFAULT_TIMEOUT_PERIOD = TimeUnit.SECONDS.toMillis(3);

	@Getter
	protected SSHClient client;
	private int provisionRetries = 0;

	@Getter
	@Setter
	protected ExecutorCommandHistory history;

	@Getter
	@Setter
	protected String name;

	private Timer timer;
	private static final long TIMER_START_DELAY = 300000L;
	private static final long TIMER_DELAY = 600000L;

	public Executor() {
	}

	/**
	 * Constructor.
	 *
	 * @param client ssh client instance
	 * @param name executor name
	 */
	public Executor(SSHClient client, String name) {
		this.client = client;
		this.name = name;
		history = new ExecutorCommandHistory(name);
	}

	/**
	 * Executes a command.
	 *
	 * @param cmd command
	 * @param silent do not log if true
	 * @param ignoreExceptions do not log if true
	 * @return command response
	 */
	@SuppressWarnings("TryWithIdenticalCatches")
	private String executeCommand(String cmd, boolean silent, boolean ignoreExceptions) {
		try {
			final String response = client.executeCommand(cmd, silent, ignoreExceptions);
			if (!silent) {
				log.debug("Response: " + response);
			}
			history.log(cmd, response);
			return response;
		} catch (KarafSessionDownException e) {
			if (!ignoreExceptions) {
				log.error("Karaf session is down!");
			}
		} catch (SSHClientException e) {
			if (!ignoreExceptions) {
				log.error("SSHClient exception thrown: " + e);
			}
		}

		return null;
	}

	/**
	 * Executes a command.
	 * @param cmd command
	 * @param silent do not log if true
	 * @return command response
	 */
	private String executeCommand(String cmd, boolean silent) {
		return executeCommand(cmd, silent, true);
	}

	/**
	 * Executes a command silently.
	 *
	 * @param cmd command
	 * @return command response
	 */
	public String executeCommandSilently(String cmd) {
		return executeCommandSilently(cmd, true);
	}

	/**
	 * Executes a command silently.
	 * @param cmd command
	 * @param ignoreExceptions true to not log any exceptions
	 * @return response
	 */
	public String executeCommandSilently(String cmd, boolean ignoreExceptions) {
		return executeCommand(cmd, true, ignoreExceptions);
	}

	/**
	 * Executes a command.
	 *
	 * @param cmd command
	 * @return command response
	 */
	public String executeCommand(String cmd) {
		return executeCommand(cmd, false);
	}

	/**
	 * Executes multiple commands.
	 *
	 * @param commands commands array
	 * @return list of command responses
	 */
	public List<String> executeCommands(String... commands) {
		final List<String> responses = new ArrayList<>();
		for (String command : commands) {
			responses.add(executeCommand(command));
		}
		return responses;
	}

	/**
	 * Checks if the client can connect.
	 *
	 * @return true/false if can/couldn't connect
	 */
	public boolean canConnect() {
		try {
			// We just check if its possible to connect - suppress exception
			client.connect(true);
			client.disconnect();
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	/**
	 * Connects client to specified remote server.
	 */
	public void connect() {
		log.debug("Connecting: " + this.toString());
		Boolean connected = false;
		final int step = 5;
		int elapsed = 0;
		final long timeout = step * 1000L;

		log.info("Waiting for SSH connection ...");
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
				log.info("Connected to SSH server");
			} catch (VerifyFalseException ex) {
				log.debug("Remaining time: " + (SystemProperty.getStartWaitTime() - elapsed) + " seconds. ");
				elapsed += step;
			} catch (SSHClientException e) {
				elapsed += step;
			}
			sleep(timeout);
		}

		// When connected, schedule a new keep alive thread for this executor
		// First shutdown all other tasks from previous runs, because you can use .connect() without previous .disconnect()
		this.stopKeepAliveTimer();
		this.startKeepAliveTimer();
	}

	/**
	 * Checks if the executor (client) is connected.
	 *
	 * @return connected
	 */
	public boolean isConnected() {
		return client.isConnected();
	}

	/**
	 * Disconnects the client.
	 */
	public void disconnect() {
		this.stopKeepAliveTimer();
		client.disconnect();
	}

	/**
	 * Reconnects the executor.
	 */
	public void reconnect() {
		disconnect();
		connect();
	}

	/**
	 * Waits for the container to boot.
	 */
	public void waitForBoot() {
		final int step = 3;
		final long timeout = step * 1000L;
		boolean online = false;

		int elapsed = 0;

		while (!online) {
			// Check if the time is up
			if (elapsed > SystemProperty.getStartWaitTime()) {
				log.error("Connection couldn't be established after " + SystemProperty.getStartWaitTime()
						+ " seconds");
				throw new ConnectionException("Connection couldn't be established after "
						+ SystemProperty.getStartWaitTime() + " seconds");
			}

			try {
				// Try to execute the command - if it throws an exception, it is not ready yet
				// Suppress the exception here to reduce spam
				client.connect(true);
				online = true;
				log.info("Container online");
			} catch (Exception ex) {
				log.debug("Remaining time: " + (SystemProperty.getStartWaitTime() - elapsed) + " seconds. ");
				elapsed += step;
			}
			sleep(timeout);
		}
		// There should be nothing scheduled, but just to be sure
		this.stopKeepAliveTimer();
		this.startKeepAliveTimer();
	}

	/**
	 * Waits for the broker start.
	 */
	public void waitForBroker() {
		final int step = 3;
		final long timeout = step * 1000L;
		boolean online = false;

		int elapsed = 0;

		log.info("Waiting for the broker to be online");

		while (!online) {
			// Check if the time is up
			if (elapsed > SystemProperty.getBrokerStartWaitTime()) {
				log.error("Broker wasn't started after " + SystemProperty.getBrokerStartWaitTime() + " seconds");
				throw new FaframException(
						"Broker wasn't started after " + SystemProperty.getBrokerStartWaitTime() + " seconds");
			}

			String response = null;
			try {
				response = client.executeCommand("activemq:bstat", true);
			} catch (Exception ignored) {
				// Do nothing
			}

			if (StringUtils.contains("BrokerName", response)) {
				log.debug("Remaining time: " + (SystemProperty.getBrokerStartWaitTime() - elapsed) + " seconds. ");
				elapsed += step;
			} else {
				online = true;
				log.info("Broker online");
			}

			sleep(timeout);
		}
	}

	/**
	 * Waits for the container to shut down.
	 */
	public void waitForShutdown() {
		this.stopKeepAliveTimer();

		final int step = 5;
		final long timeout = (step * 1000L);
		boolean online = true;

		int elapsed = 0;

		log.info("Waiting for shutdown");

		while (online) {
			// Check if the time is up
			if (elapsed > SystemProperty.getStopWaitTime()) {
				log.error("Connection could be established after " + SystemProperty.getStopWaitTime() + " seconds");
				throw new ConnectionException(
						"Connection could be established after " + SystemProperty.getStopWaitTime() + " seconds");
			}

			try {
				// Check if we are still connected
				online = client.isConnected();
				log.debug("Remaining time: " + (SystemProperty.getStopWaitTime() - elapsed) + " seconds. ");
				elapsed += step;
			} catch (Exception ex) {
				online = false;
			}

			sleep(timeout);
		}
	}

	/**
	 * Waits for the (remote) container stop.
	 *
	 * @param c container
	 */
	public void waitForContainerStop(Container c) {
		log.trace("Shutting down scheduled executor for container " + c.getName());
		// Because we can stop ssh container through root's executor and in that case the "c" here would be ssh container, but the executor is from root
		if (this.getName().equals(c.getName())) {
			c.getExecutor().stopKeepAliveTimer();
		}
		final int step = 5;
		final long timeout = (step * 1000L);
		boolean online = true;

		int elapsed = 0;

		log.info("Waiting for container " + c.getName() + " to shutdown");

		while (online) {
			// Check if the time is up
			if (elapsed > SystemProperty.getStopWaitTime()) {
				log.error("PID was still found after " + SystemProperty.getStopWaitTime() + " seconds");
				throw new FaframException(
						"PID was still found after after " + SystemProperty.getStopWaitTime() + " seconds");
			}

			try {
				// If the response contains NoNodeException, then the container is stopped definitely and the node disappears
				online = !client.executeCommand("zk:get /fabric/registry/containers/status/" + c.getName() + "/pid", true).contains("NoNode");
				log.debug("Remaining time: " + (SystemProperty.getStopWaitTime() - elapsed) + " seconds. ");
				elapsed += step;
			} catch (Exception ex) {
				online = false;
			}

			sleep(timeout);
		}
		// Because we can stop ssh container through root's executor and in that case the "c" here would be ssh container, but the executor is from root
		if (this.getName().equals(c.getName())) {
			c.getExecutor().disconnect();
		}
	}

	/**
	 * Waits for the defined status of container.
	 *
	 * @param c container
	 * @param status status
	 */
	public void waitForProvisionStatus(Container c, String status) {
		waitForProvisioning(null, c, status, SystemProperty.getProvisionWaitTime());
	}

	/**
	 * Waits for the provisioning of a container.
	 *
	 * @param containerName container name
	 */
	public void waitForProvisioning(String containerName) {
		waitForProvisioning(containerName, null, "success", SystemProperty.getProvisionWaitTime());
	}

	/**
	 * Waits for container provisioning. It may restart the container if the provision status is "requires full restart"
	 * or if the provision status contains "NoNodeException"
	 *
	 * @param c Container instance
	 */
	public void waitForProvisioning(Container c) {
		waitForProvisioning(null, c, "success", SystemProperty.getProvisionWaitTime());
	}

	/**
	 * Waits for the successful provisioning of container for defined period of time. It may restart the container if the provision status is
	 * "requires full restart or if the provision status contains "NoNodeException".
	 *
	 * @param c container
	 * @param time time in seconds
	 */
	public void waitForProvisioning(Container c, int time) {
		waitForProvisioning(null, c, "success", time);
	}

	/**
	 * Waits for the defined provision status of the container for defined period of time. It may restart the container if the provision status is
	 * "requires full restart or if the provision status contains "NoNodeException".
	 *
	 * @param c container
	 * @param status provision status
	 * @param time time in seconds
	 */
	public void waitForProvisionStatus(Container c, String status, int time) {
		waitForProvisioning(null, c, status, time);
	}

	/**
	 * General wait for provisioning method, called by the others. Waits for provisioning of container/container name.
	 *
	 * @param containerName container name
	 * @param c container
	 * @param status status to wait on
	 * @param time time in seconds
	 */
	public void waitForProvisioning(String containerName, Container c, String status, int time) {
		final String waitFor = c == null ? containerName : c.getName();

		final int step = 3;
		final long timeout = step * 1000L;
		final long startTimeout = 10000L;

		// Wait before executing - sometimes the provision is triggered a bit later
		sleep(startTimeout);

		// Used to control the cycle
		int elapsed = 0;
		String provisionStatus = "";
		boolean isSuccessful = false;
		boolean restarted = false;

		while (!isSuccessful) {
			handleProvisionWaitTime(elapsed, waitFor, status, provisionStatus, time);
			provisionStatus = "";
			String reason = "";

			try {
				provisionStatus = StringUtils.substringAfter(client.executeCommand("container-info " + waitFor, true), "Provision Status:").trim();
				isSuccessful = provisionStatus.contains(status);
			} catch (Exception e) {
				// Get the reason
				reason = e.getMessage();

				// Re-init the ssh connection if it's not successful
				try {
					client.connect(true);
				} catch (Exception ignored) {
				}
			}
			if (("requires full restart".equals(provisionStatus) || provisionStatus.contains("NoNodeException") || provisionStatus.contains(
					"Client is not started") || (SystemProperty.isRetryProvisioning() && "failed".equals(provisionStatus))) && c != null) {
				handleProvisionRetries(waitFor, status);
				restarted = true;
				log.warn("Container requires restart (provision status: " + provisionStatus + ")! Restarting...");
				break;
			}

			// If we are waiting for certain provision status and status is either error/success(opposite to wanted status) then terminate waitForProvision with exception
			log.trace("Waiting for: {} , current provision status: {}", status, provisionStatus);
			if (!status.equals(provisionStatus) && (provisionStatus.contains("error") || provisionStatus.contains("success"))) {
				log.error("Container {} did not provision to state \"{}\" but ended in state: \"{}\"", waitFor, status, provisionStatus);
				throw new ProvisionException("Container " + waitFor + " failed to provision to state \"" + status + "\"  and ended in provision status \"" + provisionStatus + "\"");
			}

			if (!isSuccessful) {
				log.debug("Remaining time: " + (time - elapsed) + " seconds. " + (""
						.equals(reason) ? "" : "(" + reason + ")") + ("".equals(provisionStatus) ? "" : "("
						+ provisionStatus + ")"));
				elapsed += step;
				sleep(timeout);
			}
		}

		// If the container was restarted during the provisioning, trigger the provisioning again
		handleRestart(restarted, c);

		provisionRetries = 0;
	}

	/**
	 * Handles the maximal provision retries count. If the retries are > 2, fail because probably the container won't provision.
	 *
	 * @param container container
	 * @param status status to wait for
	 */
	private void handleProvisionRetries(String container, String status) {
		if (provisionRetries > 1) {
			log.error("Container " + container + " did not provision to state \"" + status + "\" after 3 retries");
			throw new ProvisionException("Container " + container + " did not provision to state \"" + status + "\" after 3 retries");
		}
	}

	/**
	 * Handles the maximal provision time. If the time is up, fail because probably the container won't provision.
	 *
	 * @param elapsed elapsed time
	 * @param container container
	 * @param status status to wait for
	 */
	private void handleProvisionWaitTime(int elapsed, String container, String status, String containerActualStatus, int time) {
		if (elapsed > time) {
			log.error("Container " + container + " failed to provision to state \"" + status + "\" in time and ended in status:" + containerActualStatus);
			throw new ProvisionException("Container " + container + " failed to provision to state \"" + status + "\" in time and ended in status:"
					+ containerActualStatus);
		}
	}

	/**
	 * Handles the restart. If the flag is set, it will restart the container and trigger the provisioning again.
	 *
	 * @param restart restart flag
	 * @param c container
	 */
	private void handleRestart(boolean restart, Container c) {
		if (restart) {
			// Force restart in case of ensemble member for example
			c.restart(true);
			provisionRetries++;
			waitForProvisioning(c);
		}
	}

	/**
	 * Copies local file to specified location in Fuse folder on remote host.
	 *
	 * @param localPath absolute path to the file on local machine that should be copied
	 * @param remotePath path to destination inside Fuse folder where the file should be copied
	 * @throws CopyFileException if there was error in copying file
	 */
	public void copyFileToRemote(final String localPath, final String remotePath) throws CopyFileException {
		if (client instanceof NodeSSHClient) {
			((NodeSSHClient) client).copyFileToRemote(localPath, remotePath);
		} else {
			throw new CopyFileException("SSH client assigned to Executor is not instance of NodeSSHClient!");
		}
	}

	/**
	 * Waits for the patch defined status.
	 *
	 * @param patchName patch name
	 * @param status status of patch to wait for
	 */
	public void waitForPatchStatus(String patchName, boolean status) {
		final int step = 3;
		final long timeout = step * 1000L;
		int retries = 0;
		boolean isSuccessful = false;

		log.info("Waiting for patch to be installed");

		while (!isSuccessful) {
			sleep(timeout);
			retries += step;
			boolean shouldReconnect = false;
			if (retries > SystemProperty.getPatchWaitTime()) {
				log.error("Container failed to install patch after " + SystemProperty.getPatchWaitTime() + " seconds.");

				final String action = "true".equals(String.valueOf(status)) ? "install" : "rollback";
				log.error("Standalone container failed to " + action + " patch after "
						+ SystemProperty.getPatchWaitTime() + " seconds.");
				throw new PatchException(
						"Container failed to " + action + " patch after " + SystemProperty.getPatchWaitTime() + " seconds.");
			}

			String reason = "";

			try {
				isSuccessful =
						client.executeCommand("patch:list | grep " + patchName, true).contains(String.valueOf(status));
			} catch (Exception e) {
				reason = e.getMessage();
				shouldReconnect = true;
			}

			if (!isSuccessful) {
				log.debug("Remaining time: " + (SystemProperty.getPatchWaitTime() - retries) + " seconds. " + (""
						.equals(reason) ? "" : "(" + reason + ")"));
			}

			if (shouldReconnect) {
				try {
					client.connect(true);
				} catch (Exception ignored) {
				}
			}
		}
	}

	/**
	 * Sleeps for given amount of time.
	 *
	 * @param time time in millis
	 */
	protected static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Gets all the child containers and returns all their names.
	 * <p/>
	 * TODO(avano): this could be reworked in the future after the changes to deploying
	 *
	 * @return list of child container names
	 */
	public List<String> listChildContainers() {
		// I don't want this to be spammed in the log / added to history, therefore I'm using client instead of the executeCommand method
		final List<String> childs = new ArrayList<>();

		// Do nothing if we don't use .withFabric()
		if (!SystemProperty.isFabric()) {
			return childs;
		}

		try {
			final String containerListResponse = client.executeCommand("container-list | grep -v root | grep karaf", true);

			if (containerListResponse == null) {
				return childs;
			}

			final String[] childContainerList = containerListResponse.split("\n");

			for (String line : childContainerList) {
				final String containerName = line.trim().split(" ")[0];
				// Ssh container is also listed as root
				final boolean isChild = client.executeCommand("container-info " + containerName, true).replaceAll(" +", " ")
						.contains("Root: false");
				if (isChild) {
					childs.add(containerName);
				}
			}
		} catch (Exception ex) {
			log.error("Error while getting child container list! " + ex);
		}

		return childs;
	}

	/**
	 * Utility method for waiting on custom condition.
	 * Check {@link org.jboss.fuse.qa.fafram8.util.callables.Response} and other classes
	 * in this package.
	 *
	 * @param methodBlock callable which is executed every 3 seconds until it returns success
	 * @param secondsTimeout repeat callable until it's success or timeout (in seconds)
	 * @param <T> type of expected data response
	 * @return {@link Response} wrapper with boolean success/fail and nullable data response
	 */
	public static <T> Response<T> waitFor(Callable<Response<T>> methodBlock, long secondsTimeout) {
		final long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsTimeout);
		log.info("Waiting {} seconds for operation {} to complete", secondsTimeout, methodBlock);

		Response<T> response = Response.timeOut();
		while (System.currentTimeMillis() <= deadline) {
			try {
				response = methodBlock.call();
				if (response.getSuccess()) {
					log.info("Patience rewarded, success of {}", methodBlock);
					return response;
				}
				log.debug("Remaining time: {} seconds", TimeUnit.MILLISECONDS.toSeconds(
						deadline - System.currentTimeMillis()
				));
			} catch (Exception e) {
				log.debug("Remaining time: {} - op {} raised exception {}",
						TimeUnit.MILLISECONDS.toSeconds(deadline - System.currentTimeMillis()),
						methodBlock, e.getMessage(), e);
			}
			sleep(DEFAULT_TIMEOUT_PERIOD);
		}
		log.warn("Time is up, fail of {} in {} seconds.", methodBlock, secondsTimeout);
		return response;
	}

	/**
	 * Starts the keep alive timer for this executor.
	 */
	public void startKeepAliveTimer() {
		log.trace("Creating timer for " + this.getName());
		timer = TimerUtils.getNewTimer(this.getName());
		timer.schedule(new KeepAlive(this), TIMER_START_DELAY, TIMER_DELAY);
	}

	/**
	 * Stops the keep alive timer for this executor.
	 */
	public void stopKeepAliveTimer() {
		if (timer == null) {
			return;
		}
		log.trace("Stopping timer for " + this.getName());
		timer.cancel();
	}

	/**
	 * Checks if remote node is a windows server.
	 *
	 * @return true if remote node is a windows server
	 */
	public boolean isCygwin() {
		return StringUtils.containsIgnoreCase(executeCommandSilently("uname"), "cyg");
	}
}
