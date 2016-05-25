package org.jboss.fuse.qa.fafram8.manager;

import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.downloader.Downloader;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.modifier.ModifierExecutor;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import java.io.File;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Remote node manager class.
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
	@Setter
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
		executor.executeCommand("mkdir " + getFolder());
		productZipPath = Downloader.getProduct(executor, this);
		log.trace("Zip path is " + productZipPath);
	}

	@Override
	public void unzipArtifact(RootContainer container) {
		log.info("Unzipping fuse from " + productZipPath);

		// Jar can't unzip to specified directory, so we need to change the dir first
		if (productZipPath.contains(getFolder())) {
			executor.executeCommand("cd " + getFolder() + "; jar xf $(basename " + productZipPath + ")");
		} else {
			executor.executeCommand("cd " + getFolder() + "; jar xf " + productZipPath);
		}

		// Problem if WORKING_DIRECTORY is set because then the first command doesn't work

		productPath = "".equals(SystemProperty.getWorkingDirectory())
				? executor.executeCommandSilently("ls -d $PWD" + SEP + getFolder() + SEP + "*" + SEP).trim()
				: executor.executeCommandSilently("ls -d " + getFolder() + SEP + "*" + SEP).trim();

		log.trace("Product path is " + productPath);

		container.setFusePath(productPath);
	}

	@Override
	public void prepareFuse(String host) {
		ModifierExecutor.executeModifiers(host, executor);
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
			throw new FaframException("Could not start container: ", e);
		}
	}

	@Override
	public void stopAndClean(boolean ignoreExceptions) {
		// For remote deployment just clean modifiers and System properties
		SystemProperty.clearAllProperties();
		ModifierExecutor.clearAllModifiers();
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
	public void clean() {
		// todo(rjakubco): create better cleaning mechanism for Fabric on Windows machines
		log.debug("Killing container");
		executor.executeCommand("pkill -9 -f karaf.base");

		log.debug("Deleting Fafram folder on " + executor.getClient().getHost());
		final String directory = SystemProperty.getWorkingDirectory().isEmpty()
				? SystemProperty.getFaframFolder() : SystemProperty.getWorkingDirectory() + SEP + SystemProperty.getFaframFolder();
		executor.executeCommand("rm -rf " + directory);
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
}
