package org.jboss.fuse.qa.fafram8.deployer;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.ThreadContainer;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.ssh.FuseSSHClient;

import java.util.Set;
import java.util.concurrent.Callable;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread worker class for destroying(annihilating) container.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class ContainerAnnihilator implements Callable {
	@Getter
	private Container container;
	private boolean force;

	@Getter
	@Setter
	private volatile boolean ready = false;

	private Set<ContainerAnnihilator> containerAnnihilators;

	@Setter
	@Getter
	private static volatile boolean stopWork = false;

	/**
	 * Constructor.
	 * @param container container
	 * @param containerAnnihilators set of container annihilators
	 * @param force force flag
	 */
	public ContainerAnnihilator(Container container, Set<ContainerAnnihilator> containerAnnihilators, boolean force) {
		this.container = container;
		this.force = force;
		this.containerAnnihilators = containerAnnihilators;
	}

	@Override
	public Container call() {
		Thread.currentThread().setName(container.getName());
		if (container instanceof RootContainer || container instanceof SshContainer) {
			for (ContainerAnnihilator child : this.containerAnnihilators) {
				log.trace("Container " + container.getName() + " starting waiting for container thread: " + child.getContainer().getName());
				while (!child.isReady()) {
					if (ContainerAnnihilator.stopWork) {
						Thread.currentThread().interrupt();
						return null;
					}
				}
			}

			// If SSH container we need to do this
			if (container instanceof SshContainer) {
				final Executor executor = new Executor(new FuseSSHClient(container.getParent().getExecutor().getClient()), container.getName());
				((ThreadContainer) container).destroy(executor);
			} else {
				container.destroy();
			}
		} else {
			// Container is instance of ChildContainer
			try {
				final Executor executor = new Executor(new FuseSSHClient(container.getParent().getExecutor().getClient()), container.getName());
				((ThreadContainer) container).destroy(executor);
			} catch (Exception ex) {
				log.warn("Exception when deleting container!", ex);
				if (!force) {
					throw new FaframException("Error while destroying root container! " + ex);
				}
			}
		}
		this.ready = true;
		return container;
	}
}
