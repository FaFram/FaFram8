package org.jboss.fuse.qa.fafram8.deployer;

import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.ThreadContainer;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.ssh.FuseSSHClient;

import java.util.concurrent.Callable;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread worker class for creating(summoning) container.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class ContainerSummoner implements Callable {
	@Getter
	private Container container;

	@Getter
	private volatile boolean ready = false;

	private ContainerSummoner containerSummoner;

	@Getter
	private static volatile boolean stopWork = false;

	@Getter
	private String name;

	public ContainerSummoner(Container container, ContainerSummoner containerSummoner) {
		this.container = container;
		this.containerSummoner = containerSummoner;
		this.name = container.getName();
	}

	public static void setStopWork(boolean stopWork) {
		ContainerSummoner.stopWork = stopWork;
	}

	@Override
	public Container call() throws InterruptedException {
		Thread.currentThread().setName(container.getName());
		if (container instanceof ChildContainer || container instanceof SshContainer) {
			log.trace("Container " + container.getName() + " starting waiting for container thread: " + containerSummoner.getName());
			while (!containerSummoner.isReady()) {
				if (ContainerSummoner.stopWork) {
					Thread.currentThread().interrupt();
					return null;
				}
			}
			log.trace("Container " + container.getName() + " finished waiting!");
			final Executor executor = new Executor(new FuseSSHClient(container.getParent().getExecutor().getClient()), container.getName());

			((ThreadContainer) container).create(executor);
		} else {
			container.create();
		}

		this.ready = true;
		return container;
	}

	public void setReady(boolean ready) {
		log.error("Setting ready in summoner to " + ready);
		this.ready = ready;
	}
}
