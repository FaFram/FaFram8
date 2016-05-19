package org.jboss.fuse.qa.fafram8.deployer;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Deployer class. Iterates through the container list and creates all the containers. If there is no container in the container list,
 * it builds a default one from system properties.
 * Created by avano on 19.8.15.
 */
@Slf4j
public final class Deployer {

	@Getter
	private static HashMap<String, Thread> threads = new HashMap<>();

	/**
	 * Private constructor.
	 */
	private Deployer() {
	}

	/**
	 * Creates all containers from the container list.
	 */
	public static void deploy() {
		if (SystemProperty.isNoThreads()) {
			for (Container c : ContainerManager.getContainerList()) {
				if (!c.isCreated()) {
					c.create();
				}
			}
		} else {
			deployWithThreads();
		}
	}

	/**
	 * Destroys all containers from container list.
	 *
	 * @param force flag if the exceptions should be ignored
	 */
	public static void destroy(boolean force) {
		// Do nothing
		if (SystemProperty.isKeepContainers()) {
			return;
		}

		if (SystemProperty.isNoThreads()) {
			destroyWithoutThreads(force);
		} else {
			destroyWithThreads(force);
		}
	}

	/**
	 * Creates containers using threads.
	 */
	private static void deployWithThreads() {
		final ArrayList<Thread> joiningThreads = new ArrayList<>();
		for (Container c : ContainerManager.getContainerList()) {
			if (!c.isCreated()) {
				log.trace("Spawning thread for creating container: " + c.getName());
				final Runnable containerCreator = new ContainerSummoner(c);
				final Thread t = new Thread(containerCreator);
				t.setName(c.getName());
				threads.put(c.getName(), t);
				joiningThreads.add(t);
				t.start();
			}
		}

		for (Thread t : joiningThreads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				throw new FaframException("Exception when creating container", e);
			}
		}
		// Clear threads! This hash set is used also for destroying
		threads.clear();
	}

	/**
	 * Destroys containers using threads.
	 *
	 * @param force flag if the exceptions should be ignored
	 */
	private static void destroyWithThreads(boolean force) {
		final ArrayList<Thread> joiningThreads = new ArrayList<>();
		for (Container c : ContainerManager.getContainerList()) {
			log.trace("Spawning thread for destroying container: " + c.getName());
			final Runnable containerDestroyer = new ContainerAnnihilator(c, force);
			final Thread t = new Thread(containerDestroyer);
			t.setName(c.getName());
			threads.put(c.getName(), t);
			joiningThreads.add(t);
			t.start();
		}

		for (Thread t : joiningThreads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				throw new FaframException("Exception when destroying containers!", e);
			}
		}
		// Just to be sure
		threads.clear();
	}

	/**
	 * Destroys using only one thread.
	 *
	 * @param force flag if the exceptions should be ignored
	 */
	private static void destroyWithoutThreads(boolean force) {
		// Temp set which holds all root containers after the first cycle, so that we don't need to mess with concurent mods
		final Set<Container> set = new HashSet<>();
		set.addAll(ContainerManager.getContainerList());

		for (int i = 0; i < ContainerManager.getContainerList().size(); i++) {
			final Container c = ContainerManager.getContainerList().get(i);
			try {
				if (!(c instanceof RootContainer)) {
					c.destroy();
					set.remove(c);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				if (!force) {
					throw new FaframException("Error while destroying child/ssh container! " + ex);
				}
			}
		}

		for (Container c : set) {
			try {
				c.destroy();
			} catch (Exception ex) {
				ex.printStackTrace();
				if (!force) {
					throw new FaframException("Error while destroying root container! " + ex);
				}
			}
		}
	}
}
