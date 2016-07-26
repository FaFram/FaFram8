package org.jboss.fuse.qa.fafram8.manager;

import org.apache.commons.io.FileUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.downloader.Downloader;
import org.jboss.fuse.qa.fafram8.exception.ContainerException;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.modifier.ModifierExecutor;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;

/**
 * Local node manager class. This class gets, prepares and starts the container on local machine.
 * Created by avano on 19.8.15.
 */
@Slf4j
public class LocalNodeManager implements NodeManager {
	// File separator
	private static final String SEP = File.separator;
	@Getter
	private Executor executor;
	// Is windows?
	private boolean windows = System.getProperty("os.name").startsWith("Windows");

	// Flag if this instance was already stopped
	private boolean stopped = true;

	// Product zip path
	private String productZipPath;

	// Target dir path
	private String targetPath;

	// Full path to unzipped product
	private String productPath;

	// Container process
	private Process productProcess;

	// Is jenkins?
	private boolean jenkins = System.getenv("WORKSPACE") != null;

	// Restart flag - used to successfully shutdown the container in case of restart fail
	private boolean restart = false;

	/**
	 * Constructor.
	 *
	 * @param executor node executor
	 */
	public LocalNodeManager(Executor executor) {
		this.executor = executor;
	}

	@Override
	public void checkRunningContainer() {
		final int port = 8101;
		try (Socket s = new Socket("localhost", port)) {
			log.error("Port 8101 is not free! Other karaf instance may be running. Shutting down...");
			throw new FaframException("Port 8101 is not free! Other karaf instance may be running.");
		} catch (IOException ex) {
			// Do nothing, the port is free
		}
	}

	@Override
	public void prepareZip() {
		productZipPath = Downloader.getProduct();
	}

	@Override
	public void unzipArtifact(RootContainer container) {
		// Fix for long jenkins paths
		if (jenkins) {
			targetPath = new File(System.getenv("WORKSPACE") + SEP + new Date().getTime()).getAbsolutePath();
		} else {
			targetPath = new File("target" + SEP + "container" + SEP + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SS")
					.format(new Date())).getAbsolutePath();
		}

		log.debug("Unzipping to " + targetPath);

		try {
			final ZipFile zipFile = new ZipFile(new File(productZipPath).getAbsolutePath());
			zipFile.extractAll(targetPath);
		} catch (Exception ex) {
			log.error("Exception caught during unzipping!");
			throw new FaframException(ex);
		}

		// Construct the full path to product root - get the subdir name in targetPath
		final String folderName = new File(targetPath).list()[0];

		// Use the subdir name to construct the product path
		productPath = targetPath + SEP + folderName;
		log.trace("Product path is " + productPath);
		SystemProperty.set(FaframConstant.BASE_DIR, new File(jenkins ? System.getenv("WORKSPACE") : "").getAbsolutePath());
		container.setFusePath(productPath);
	}

	@Override
	public void prepareFuse(Container container) {
		ModifierExecutor.executeModifiers(container);
		ModifierExecutor.executeCustomModifiers(container);
	}

	@Override
	public void startFuse() {
		final String executable = "start";
		final String extension = windows ? ".bat" : "";

		log.debug("Executable file is \"" + executable + extension + "\"");

		// Construct the path to the executable file
		final String executablePath = productPath + SEP + "bin" + SEP + executable + extension;
		log.debug("Executing " + executablePath);

		try {
			if (SystemProperty.getFuseZip() != null) {
				// If we run custom zip
				log.info("Starting container");
			} else {
				// If we run artifact from mvn
				log.info("Starting Fuse " + SystemProperty.getFuseVersion());
			}
			productProcess = Runtime.getRuntime().exec(executablePath);
			stopped = false;
			log.info("Waiting for the container to be online");
			executor.waitForBoot();
			if (!SystemProperty.isFabric() && !SystemProperty.skipBrokerWait()) {
				executor.waitForBroker();
			}
		} catch (Exception e) {
			throw new ContainerException("Could not start root container: " + e);
		}
	}

	@Override
	public void kill() {
	}

	@Override
	public void clean() {
		// We do not clean on local
	}

	@Override
	public void stopAndClean(boolean ignoreExceptions) {
		final boolean suppressStart = SystemProperty.suppressStart();

		// If the instance is running or we fail restarting
		if (!stopped || restart) {
			ModifierExecutor.clearAllModifiers();
			cleanChildContainers();
			stop(ignoreExceptions);
			deleteTargetDir(ignoreExceptions);
		} else {
			// If the instance is not running - if the 8181 port is occupied or we suppress start
			if (SystemProperty.suppressStart()) { // If there are some files
				ModifierExecutor.clearAllModifiers();
				// This should be called after all modifiers but before stop/delete because they can throw exceptions
				deleteTargetDir(ignoreExceptions);
			}
		}

		SystemProperty.clearAllProperties();
	}

	@Override
	public void stop() {
		stop(false);
	}

	/**
	 * Stops the container.
	 *
	 * @param ignoreExceptions ignore exceptions flag
	 */
	private void stop(boolean ignoreExceptions) {
		final String executable = "stop";
		final String extension = windows ? ".bat" : "";

		log.debug("Executable file is \"" + executable + extension + "\"");

		final String executablePath = productPath + SEP + "bin" + SEP + executable + extension;
		log.debug("Executing " + executablePath);

		try {
			if (SystemProperty.getFuseZip() != null) {
				// If we run custom zip
				log.info("Stopping container");
			} else {
				// If we run artifact from mvn
				log.info("Stopping Fuse " + SystemProperty.getFuseVersion());
			}
			Runtime.getRuntime().exec(executablePath).waitFor();
			executor.waitForShutdown();
		} catch (Exception e) {
			// If we get an exception during shutdown, kill the process at OS level
			if (productProcess != null) {
				productProcess.destroy();
				try {
					productProcess.waitFor();
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
			if (!ignoreExceptions) {
				// Throw the exception because something was wrong
				throw new FaframException("Could not stop container: " + e);
			}
		}

		stopped = true;
	}

	/**
	 * Force-Delete target dir.
	 *
	 * @param ignoreExceptions ignore exceptions flag
	 */
	private void deleteTargetDir(boolean ignoreExceptions) {
		if (!SystemProperty.isKeepFolder()) {
			try {
				log.debug("Deleting " + targetPath);
				FileUtils.forceDelete(new File(targetPath));
			} catch (Exception e) {
				if (!ignoreExceptions) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void restart() {
		restart = true;
		stop(false);
		// If start fails, the flag will remain set so the shutdown will be called (see stop())
		// Root restarts are done using the system property, so no need to call .start() here
		restart = false;
		stopped = false;
	}

	/**
	 * Cleans all the child containers.
	 */
	private void cleanChildContainers() {
		for (String name : executor.listChildContainers()) {
			log.info("Deleting child container " + name);
			executor.executeCommand("container-delete " + name);
		}
	}
}
