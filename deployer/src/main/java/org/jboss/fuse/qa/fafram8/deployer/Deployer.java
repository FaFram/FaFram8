package org.jboss.fuse.qa.fafram8.deployer;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.openstack.exception.InvokerPoolInterruptedException;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Deployer class. Iterates through the container list and creates all the containers. If there is no container in the container list,
 * it builds a default one from system properties.
 * Created by avano on 19.8.15.
 */
@Slf4j
public final class Deployer {

	@Getter
	@Setter
	private static volatile boolean fail = false;

	/**
	 * Private constructor.
	 */
	private Deployer() {
	}

	/**
	 * Creates all containers from the container list.
	 */
	public static void deploy() {
		// Convert all parentName attributes to parent container object on all containers
		for (Container container : ContainerManager.getContainerList()) {
			if (!(container instanceof RootContainer)) {
				if (container.getParent() == null) {
					// Search the parent by its name
					final Container parent = ContainerManager.getContainer(container.getParentName());
					if (parent == null) {
						throw new FaframException(String.format("Specified parent (%s) of container %s does not exist in container list!",
								container.getParentName(), container.getName()));
					}
					container.setParent(parent);
				}
			}
		}

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
		final ExecutorService executorService = Executors.newFixedThreadPool(10);
		final Set<Future> futureSet = new HashSet<>();

		final ConcurrentHashMap<String, ContainerSummoner> joiningThreads = new ConcurrentHashMap<>();

		for (Container c : ContainerManager.getContainerList()) {
			final ContainerSummoner containerSummoner;
			if (!c.isCreated()) {
				if (c instanceof RootContainer) {
					containerSummoner = new ContainerSummoner(c, null);
				} else {
					final ContainerSummoner parentSummoner = joiningThreads.get(c.getParent().getName());
					containerSummoner = new ContainerSummoner(c, parentSummoner);
				}
				joiningThreads.putIfAbsent(c.getName(), containerSummoner);
				log.debug("Creating thread for spawning container: " + c.getName());
				futureSet.add(executorService.submit(containerSummoner));
			}
		}

		boolean flag = false;
		for (Future future : futureSet) {
			try {
				if (future.get() == null) {
					ContainerSummoner.setStopWork(true);
					flag = true;
					break;
				}
			} catch (Exception e) {
				// TODO(rjakubco): I don't think this will ever happen
				ContainerSummoner.setStopWork(true);
				log.error("Exception thrown from the thread ", e);
				flag = true;
				break;
			}
		}

		if (flag || ContainerSummoner.isStopWork()) {
			log.error("Shutting down spawning threads");
			ContainerSummoner.setStopWork(true);
			for (Future future : futureSet) {
				future.cancel(true);
			}
			executorService.shutdownNow();

			// TODO(rjakubco): Find a way to make more specific exception
			throw new FaframException("Deployment failed");
		}

		executorService.shutdown();
		log.trace("Waiting for ContainerSummoner threads to finish a job.");
		try {
			while (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
				log.trace("Waiting for ContainerSummoner threads to finish a job.");
			}
		} catch (InterruptedException ie) {
			throw new InvokerPoolInterruptedException(ie.getMessage());
		}
	}

	/**
	 * Destroys containers using threads.
	 *
	 * @param force flag if the exceptions should be ignored
	 */
	private static void destroyWithThreads(boolean force) {
		final ExecutorService executorService = Executors.newFixedThreadPool(10);
		final Set<Future> futureSet = new HashSet<>();

		// Map containing all created annihilators with the name of theirs container
		final ConcurrentHashMap<Container, ContainerAnnihilator> joiningThreads = new ConcurrentHashMap<>();
		final List<Container> list = ContainerManager.getContainerList();
		for (int i = list.size() - 1; i >= 0; i--) {
			final Container c = list.get(i);
			final ContainerAnnihilator containerAnnihilator;
			if (c.isCreated()) {
				if (c instanceof RootContainer || c instanceof SshContainer) {
					// children
					final Set<ContainerAnnihilator> children = new HashSet<>();
					// Find threads of child containers
					for (Container child : getChildContainers(c)) {
						children.add(joiningThreads.get(child));
					}

					containerAnnihilator = new ContainerAnnihilator(c, children, force);
				} else {
					// Annihilator for child container
					containerAnnihilator = new ContainerAnnihilator(c, null, force);
				}
				joiningThreads.putIfAbsent(c, containerAnnihilator);
				log.debug("Creating thread for deleting container: " + c.getName());
				futureSet.add(executorService.submit(containerAnnihilator));
			}
		}

		executorService.shutdown();
		log.trace("Waiting for ContainerAnnihilator threads to finish a job.");
		try {
			while (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
				log.trace("Waiting for ContainerAnnihilator threads to finish a job.");
			}
		} catch (InterruptedException ie) {
			ContainerAnnihilator.setStopWork(true);
			throw new InvokerPoolInterruptedException(ie.getMessage());
		}
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

	/**
	 * Helping method for finding all child containers of given container.
	 *
	 * @param container container for finding its children
	 * @return set of child containers for given container
	 */
	private static Set<Container> getChildContainers(Container container) {
		final Set<Container> containers = new HashSet<>();
		for (Container c : ContainerManager.getContainerList()) {
			if (!(c instanceof RootContainer)) {
				if (c.getParent().getName().equals(container.getName())) {
					containers.add(c);
				}
			}
		}

		return containers;
	}
}
