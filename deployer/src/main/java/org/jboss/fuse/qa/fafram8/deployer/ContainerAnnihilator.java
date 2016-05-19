package org.jboss.fuse.qa.fafram8.deployer;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.ThreadContainer;
import org.jboss.fuse.qa.fafram8.exception.ContainerThreadException;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.ssh.FuseSSHClient;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread worker class for destroying(annihilating) container.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class ContainerAnnihilator implements Runnable {
	private Container container;
	private boolean force;

	public ContainerAnnihilator(Container container, boolean force) {
		this.container = container;
		this.force = force;
	}

	@Override
	public void run() {
		if (container instanceof RootContainer || container instanceof SshContainer) {
			for (Thread child : getChildrenThreads(container)) {
				try {
					log.trace("Container: " + container.getName() + " waiting for thread: " + child.getName() + " to finish.");
					child.join();
				} catch (InterruptedException e) {
					throw new ContainerThreadException("Exception when waiting for thread destroying container's child :" + child.getName()
							+ " when destroying container:" + container);
				}
			}

			// If SSH container we need to do this
			if (container instanceof SshContainer) {
				final Executor executor = new Executor(new FuseSSHClient(container.getParent().getExecutor().getClient()));
				executor.connect();
				((ThreadContainer) container).destroy(executor);
			} else {
				container.destroy();
			}
		} else {
			// Container is instance of ChildContainer
			try {
				final Executor executor = new Executor(new FuseSSHClient(container.getParent().getExecutor().getClient()));
				executor.connect();
				((ThreadContainer) container).destroy(executor);
			} catch (Exception ex) {
				log.warn("Exception when deleting container!", ex);
				if (!force) {
					throw new FaframException("Error while destroying root container! " + ex);
				}
			}
		}
	}

	/**
	 * Helping method for finding all threads of children containers of given container.
	 *
	 * @param container container for finding its children
	 * @return set of threads responsible for destroying child containers of given container
	 */
	public Set<Thread> getChildrenThreads(Container container) {
		final Set<Thread> threads = new HashSet<>();
		for (Container c : ContainerManager.getContainerList()) {
			if (!(c instanceof RootContainer)) {
				if (c.getParent().getName().equals(container.getName())) {
					threads.add(Deployer.getThreads().get(c.getName()));
				}
			}
		}

		return threads;
	}
}
