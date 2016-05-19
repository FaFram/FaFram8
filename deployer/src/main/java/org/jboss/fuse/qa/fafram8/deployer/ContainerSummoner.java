package org.jboss.fuse.qa.fafram8.deployer;

import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.ThreadContainer;
import org.jboss.fuse.qa.fafram8.exception.ContainerThreadException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.ssh.FuseSSHClient;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread worker class for creating(summoning) container.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class ContainerSummoner implements Runnable {
	@Getter
	private Container container;

	public ContainerSummoner(Container container) {
		this.container = container;
	}

	@Override
	public void run() {
		if (container instanceof ChildContainer || container instanceof SshContainer) {
			final Thread t = Deployer.getThreads().get(container.getParent().getName());
			if (t == null) {
				throw new ContainerThreadException("Container :" + container + " have without created thread!");
			} else {
				try {
					log.trace("Container: " + container.getName() + " waiting for thread: " + t.getName() + " to finish.");
					t.join();
				} catch (InterruptedException e) {
					throw new ContainerThreadException("Exception when waiting for thread creating container's parent :" + container.getParent()
							+ " when creating container:" + container);
				}
				final Executor executor = new Executor(new FuseSSHClient(container.getParent().getExecutor().getClient()));
				executor.connect();
				((ThreadContainer) container).create(executor);
			}
		} else {
			container.create();
		}
	}
}
