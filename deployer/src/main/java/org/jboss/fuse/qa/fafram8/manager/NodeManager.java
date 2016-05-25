package org.jboss.fuse.qa.fafram8.manager;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.executor.Executor;

/**
 * Node manager interface.
 * Created by avano on 19.8.15.
 */
public interface NodeManager {
	/**
	 * Prepares the zip file.
	 */
	void prepareZip();

	/**
	 *  Unzips the zip file.
	 *
	 * @param container root container that is being created
	 */
	void unzipArtifact(RootContainer container);

	/**
	 * Configures fuse on specified host.
	 *
	 * @param host host
	 */
	void prepareFuse(Container host);

	/**
	 * Starts fuse.
	 */
	void startFuse();

	/**
	 * Gets the executor.
	 *
	 * @return executor
	 */
	Executor getExecutor();

	/**
	 * Stops the container and cleans the workspace if necessary.
	 *
	 * @param ignoreExceptions ignore exceptions true/false
	 */
	void stopAndClean(boolean ignoreExceptions);

	/**
	 * Stops the container.
	 */
	void stop();

	/**
	 * Restarts the container.
	 */
	void restart();

	/**
	 * Checks if the container is already running on the host.
	 */
	void checkRunningContainer();

	/**
	 * Kills the container.
	 */
	void kill();

	/**
	 * Kills the karaf instances.
	 */
	void clean();
}
