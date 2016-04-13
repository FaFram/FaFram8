package org.jboss.fuse.qa.fafram8.provision.openstack;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.exception.InvokerPoolInterruptedException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * ServerInvoker thread pool with thread executor. One thread per container will be spawn.
 * <p/>
 * Created by ecervena on 28.9.15.
 */
@Slf4j
public class ServerInvokerPool {

	// Number of threads
	private static final int POOL_SIZE = 5;

	// Log wait time
	private static final int LOG_WAIT_TIME = 5;

	/**
	 * Calling this method will spawn thread workers to create OpenStack nodes in parallel.
	 *
	 * @param containers list of containers provided by ConfigurationParser
	 */
	public void spawnServers(List<Container> containers) {
		log.debug("Initializing ServerInvokerPool.");
		//TODO(ecervena): 5 threads in pool is only for proof of concept purposes. Figure out something smarter.
		final ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
		for (Container container : containers) {
			log.trace("Spawning invoker thread for container: " + container.getName());
			final Runnable serverInvoker = new ServerInvoker(container.getName());
			executor.execute(serverInvoker);
		}
		executor.shutdown();
		log.trace("Waiting for ServerInvoker threads to finish a job.");
		try {
			while (!executor.awaitTermination(LOG_WAIT_TIME, TimeUnit.SECONDS)) {
				log.trace("Waiting for ServerInvoker threads to finish a job.");
			}
		} catch (InterruptedException ie) {
			throw new InvokerPoolInterruptedException(ie.getMessage());
		}
		log.debug("ServerInvokerPool done.");
	}

	/**
	 * Calling this method will spawn thread workers to create OpenStack nodes in parallel.
	 *
	 * @param machineNames list of machines names that should be spawned
	 */
	public void spawnServersByNames(List<String> machineNames) {
		log.debug("Initializing ServerInvokerPool.");
		final ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
		for (String name : machineNames) {
			log.trace("Spawning invoker thread for container: " + name);
			final Runnable serverInvoker = new ServerInvoker(name);
			executor.execute(serverInvoker);
		}
		executor.shutdown();
		log.trace("Waiting for ServerInvoker threads to finish a job.");
		try {
			while (!executor.awaitTermination(LOG_WAIT_TIME, TimeUnit.SECONDS)) {
				log.trace("Waiting for ServerInvoker threads to finish a job.");
			}
		} catch (InterruptedException ie) {
			throw new InvokerPoolInterruptedException(ie.getMessage());
		}
		log.debug("ServerInvokerPool done.");
	}
}
