package org.jboss.fuse.qa.fafram8.manager;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.downloader.Downloader;
import org.jboss.fuse.qa.fafram8.exception.ContainerException;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.exception.ZipNotFoundException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.modifier.ModifierExecutor;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import java.io.File;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Specific remote node manager for Linux deployments.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class RemoteNodeManager implements NodeManager {

	// File separator
	private static final String SEP = File.separator;
	// executor to node(remote host)
	@Getter
	private Executor executor;

	// executor for Fuse on remote host
	@Getter
	private Executor fuseExecutor;
	// Product zip path
	private String productZipPath;

	// Full path to unzipped product
	private String productPath;

	// Working directory for root container for overriding system property fafram.working.dir
	private String workingDirectory = "";

	/**
	 * Constructor.
	 *
	 * @param nodeExecutor node executor
	 * @param fuseExecutor fuse executor
	 */
	public RemoteNodeManager(Executor nodeExecutor, Executor fuseExecutor) {
		this.executor = nodeExecutor;
		this.fuseExecutor = fuseExecutor;
	}

	@Override
	public void prepareZip() {
		log.info("Preparing zip...");
		executor.executeCommand("mkdir -p " + getFolder());
		productZipPath = Downloader.getProduct(executor, this);
		log.trace("Zip path is " + productZipPath);
	}

	@Override
	public void unzipArtifact(RootContainer container) {
		log.info("Unzipping fuse from " + productZipPath);
		// Jar can't unzip to specified directory, so we need to change the dir first
		if (executor.isCygwin()) {
			productZipPath = "$(cygpath -w " + productZipPath + ")";
		}

		if (Integer.parseInt(executor.executeCommandSilently("stat -t " + productZipPath + " >/dev/null 2>&1; echo $?").trim()) != 0) {
			throw new ZipNotFoundException("Zip file " + productZipPath + " does not exist!");
		}

		if (productZipPath.contains(getFolder())) {
			executor.executeCommand("cd " + getFolder() + "; jar xf $(basename " + productZipPath + ")");
		} else {
			executor.executeCommand("cd " + getFolder() + "; jar xf " + productZipPath);
		}

		// Problem if WORKING_DIRECTORY is set because then the first command doesn't work

		productPath = "".equals(SystemProperty.getWorkingDirectory()) && "".equals(workingDirectory)
				? executor.executeCommand("ls -d $PWD" + SEP + getFolder() + SEP + "*" + SEP).trim()
				: executor.executeCommand("ls -d " + getFolder() + SEP + "*" + SEP).trim();

		log.trace("Product path is " + productPath);

		container.setFusePath(productPath);
	}

	@Override
	public void prepareFuse(Container host) {
		ModifierExecutor.executeModifiers(host, executor);
		ModifierExecutor.executeCustomModifiers(host, executor);
	}

	@Override
	public void startFuse() {
		try {
			log.info("Starting container");
			executor.executeCommand(productPath + "bin" + SEP + "start");

			fuseExecutor.waitForBoot();
			// TODO(avano): special usecase for remote standalone starting? maybe not necessary
			if (!SystemProperty.isFabric() && !SystemProperty.skipBrokerWait()) {
				fuseExecutor.waitForBroker();
			}
		} catch (Exception e) {
			throw new ContainerException("Could not start root container: ", e);
		}
	}

	@Override
	public void stopAndClean(boolean ignoreExceptions) {
		// For remote deployment just clean modifiers and System properties - those will be cleaned in Fafram.tearDown()
	}

	@Override
	public void stop() {
		log.info("Stopping container");
		executor.executeCommand(productPath + "bin" + SEP + "stop");
		fuseExecutor.waitForShutdown();
	}

	/**
	 * Stops all karaf instances and removes them.
	 */
	@Override
	public void clean() {
		// todo(rjakubco): create better cleaning mechanism for Fabric on Windows machines

		log.debug("Killing container");
		executor.executeCommand("pkill -9 -f karaf.base");

		log.debug("Deleting Fuse folder on " + executor.getClient().getHost());

		executor.executeCommand("rm -rf " + getFolder());
	}

	/**
	 * Stops specific karaf instance that is define by provided container path.
	 *
	 * @param containerPath path to container home dir
	 */
	public void clean(String containerPath) {
		log.debug("Killing container");
		executor.executeCommand("pkill -9 -f " + containerPath);

		log.debug("Deleting container folder on " + executor.getClient().getHost());

		executor.executeCommand("rm -rf " + getFolder());
	}

	/**
	 * Creates folder path on remote machines.
	 * Checking if property fafram.working.directory is set or if specific working directory was set for container.
	 *
	 * @return path where fafram8 folder should be created
	 */
	public String getFolder() {
		// Check if specific working folder was set for container
		final String prefix = "".equals(workingDirectory) ? SystemProperty.getWorkingDirectory() : workingDirectory;

		final String folder;
		if ("".equals(prefix)) {
			folder = SystemProperty.getFaframFolder();
		} else {
			folder = prefix + SEP + SystemProperty.getFaframFolder();
		}
		return folder;
	}

	@Override
	public void restart() {
		executor.executeCommand(productPath + SEP + "bin" + SEP + "stop");
		fuseExecutor.waitForShutdown();
		startFuse();
	}

	@Override
	public void checkRunningContainer() {
		if (!executor.executeCommandSilently("ps aux | grep karaf.base | grep -v grep").isEmpty()) {
			log.error("Port 8101 is not free! Other karaf instance may be running. Shutting down...");
			throw new FaframException("Port 8101 is not free! Other karaf instance may be running.");
		}
	}

	@Override
	public void kill() {
		executor.executeCommand("pkill -9 -f karaf.base");
	}

	/**
	 * Setter.
	 *
	 * @param workingDirectory working directory
	 */
	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	/**
	 * Checks if container is running using bin/status script.
	 *
	 * @return true if container is running otherwise false
	 */
	public boolean isRunning() {
		log.info("Checking container");
		return !StringUtils.containsIgnoreCase(executor.executeCommandSilently(productPath + "bin" + SEP + "status"), "not");
	}
}
