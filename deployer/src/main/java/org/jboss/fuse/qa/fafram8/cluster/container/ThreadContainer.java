package org.jboss.fuse.qa.fafram8.cluster.container;

import org.jboss.fuse.qa.fafram8.executor.Executor;

/**
 * Interface for Child and SSH containers used for theirs creation and destruction.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public interface ThreadContainer {

	/**
	 * Creates a container using given executor.
	 *
	 * @param executor executor used for execution of commands
	 */
	void create(Executor executor);

	/**
	 * Destroys a container using given executor.
	 *
	 * @param executor executor used for execution of commands
	 */
	void destroy(Executor executor);
}
