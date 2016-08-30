package org.jboss.fuse.qa.fafram8.manager;

/**
 * Interface for remote node managers.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public interface RemoteNodeManager extends NodeManager{

	/**
	 * Creates folder path on remote machines.
	 * Checking if property fafram.working.directory is set or if specific working directory was set for container.
	 *
	 * @return path where fafram8 folder should be created
	 */
	String getFolder();

	/**
	 * Setter.
	 *
	 * @param workingDirectory working directory
	 */
	void  setWorkingDirectory(String workingDirectory);

	/**
	 * Stops specific karaf instance that is define by provided container path.
	 *
	 * @param containerPath path to container home dir
	 */
	void clean(String containerPath);

	/**
	 * Checks if container is running using bin/status script.
	 *
	 * @return true if container is running otherwise false
	 */
	boolean isRunning();
}
