package org.jboss.fuse.qa.fafram8.cluster.container;

import static org.jboss.fuse.qa.fafram8.modifier.impl.PropertyModifier.putProperty;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.cluster.node.Node;
import org.jboss.fuse.qa.fafram8.deployer.ContainerSummoner;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.manager.RemoteNodeManager;
import org.jboss.fuse.qa.fafram8.modifier.ModifierExecutor;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.util.Option;
import org.jboss.fuse.qa.fafram8.util.OptionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal class for representing SSH container on Windows machine.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class JoinContainer extends RootContainer implements ThreadContainer {
	@Getter
	@Setter
	private AtomicInteger counter = new AtomicInteger(0);
	private static final int ACTIVEMQ_PORT = 61616;
	private static final int ORG_OSGI_SERVICE_HTTP_PORT = 8181;
	private static final int SSH_PORT = 8101;
	private static final int RMI_REGISTRY_PORT = 1099;
	private static final int RMI_SERVER_PORT = 44444;

	/**
	 * Builder getter.
	 *
	 * @return builder instance
	 */
	public static JoinBuilder joinBuilder() {
		return new JoinBuilder(null);
	}

	/**
	 * Builder getter.
	 *
	 * @param c container that will be copied
	 * @return builder instance
	 */
	public static JoinBuilder joinBuilder(Container c) {
		return new JoinBuilder(c);
	}

	@Override
	public void create() {
		create(null);
	}

	// for thread support
	@Override
	public void create(Executor executor) {
		if (!OptionUtils.getString(this.getOptions(), Option.SAME_NODE_AS).isEmpty()) {
			final JoinContainer container = (JoinContainer) ContainerManager.getContainer(OptionUtils.getString(this.getOptions(), Option.SAME_NODE_AS));
			super.setFuseSshPort(SSH_PORT + container.getCounter().incrementAndGet());

			// Modify ports for running multiple join containers on the same node
			ModifierExecutor.addModifiers(putProperty("etc/system.properties", "activemq.port",
					String.valueOf(ACTIVEMQ_PORT + container.getCounter().get())));
			ModifierExecutor.addModifiers(putProperty("etc/system.properties", "org.osgi.service.http.port",
					String.valueOf(ORG_OSGI_SERVICE_HTTP_PORT + container.getCounter().get())));
			ModifierExecutor.addModifiers(
					putProperty("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port",
							String.valueOf(ORG_OSGI_SERVICE_HTTP_PORT + container.getCounter().get())));
			ModifierExecutor.addModifiers(putProperty("etc/org.apache.karaf.shell.cfg", "sshPort",
					String.valueOf(SSH_PORT + container.getCounter().get())));
			ModifierExecutor.addModifiers(putProperty("etc/org.apache.karaf.management.cfg", "rmiRegistryPort",
					String.valueOf(RMI_REGISTRY_PORT + container.getCounter().get())));
			ModifierExecutor.addModifiers(putProperty("etc/org.apache.karaf.management.cfg", "rmiServerPort",
					String.valueOf(RMI_SERVER_PORT + container.getCounter().get())));
			// Add 1 to counter of this container. This is needed when somebody use this container to run other container on the same node
		}

		log.trace("Connecting in JoinContainer");
		super.setExecutor(super.createExecutor());
		log.info("Creating JoinContainer: " + this);

		super.modifyContainer();

		((RemoteNodeManager) nodeManager).clean(OptionUtils.getString(this.getOptions(), Option.WORKING_DIRECTORY));
		nodeManager.checkRunningContainer();
		try {
			nodeManager.prepareZip();
			nodeManager.unzipArtifact(this);
			super.setCreated(true);
			nodeManager.prepareFuse(this);
			nodeManager.startFuse();

			// Parent info
			final String uri = super.getParent().getExecutor().executeCommand("fabric:info");
			final String zookeeperUri = StringUtils.substringBetween(uri, "ZooKeeper URI:", "\n").trim();
			final String options = parseOptions();

			// Name of the container should be changed in property files -> only join with correct password for root and zookeeperUri from root
			log.trace("First time connecting join executor");
			super.getExecutor().connect();
			super.getExecutor().executeCommands("fabric:join " + options + " " + zookeeperUri);
			super.getExecutor().waitForProvisioning(this);
			ModifierExecutor.clearAllModifiers();
		} catch (FaframException ex) {
			ex.printStackTrace();
			ContainerSummoner.setStopWork(true);
			nodeManager.stopAndClean(true);
			throw new FaframException(ex);
		}
	}

	/**
	 * Parse Options related to fabric:join command and creates string containing all option that can be used in fabric:join command.
	 *
	 * @return string containing options
	 */
	private String parseOptions() {
		final StringBuilder builder = new StringBuilder();

		builder.append(" --zookeeper-password ");

		final String zookeeperPassword = OptionUtils.getString(this.getOptions(), Option.ZOOKEEPER_PASSWORD).isEmpty()
				? OptionUtils.getString(this.getOptions(), Option.ZOOKEEPER_PASSWORD)
				: OptionUtils.getString(this.getOptions(), Option.PASSWORD);

		log.trace("Zookeeper password: " + zookeeperPassword);
		if (zookeeperPassword == null || zookeeperPassword.isEmpty()) {
			builder.append(SystemProperty.getFusePassword());
		}

		if (!OptionUtils.get(this.getOptions(), Option.PROFILE).isEmpty()) {
			for (String profile : this.getOptions().get(Option.PROFILE)) {
				builder.append(" --profile ").append(profile);
			}
		}

		if (!OptionUtils.getString(this.getOptions(), Option.MAX_PORT).isEmpty()) {
			builder.append(" --max-port ").append(OptionUtils.getString(this.getOptions(), Option.MAX_PORT));
		}
		if (!OptionUtils.getString(this.getOptions(), Option.MIN_PORT).isEmpty()) {
			builder.append(" --min-port ").append(OptionUtils.getString(this.getOptions(), Option.MIN_PORT));
		}
		if (!OptionUtils.getString(this.getOptions(), Option.RESOLVER).isEmpty()) {
			builder.append(" --resolver ").append(OptionUtils.getString(this.getOptions(), Option.RESOLVER));
		}
		if (!OptionUtils.getString(this.getOptions(), Option.MANUAL_IP).isEmpty()) {
			builder.append(" --manual-ip ").append(OptionUtils.getString(this.getOptions(), Option.MANUAL_IP));
		}
		return builder.toString();
	}

	@Override
	public void destroy(Executor executor) {
		super.destroy();
	}

	/**
	 * Root builder class - this class returns the JoinContainer object and it is the only way the join container should be built.
	 * For internal use only.
	 */
	public static class JoinBuilder {
		// Container instance
		private Container container;

		/**
		 * Constructor.
		 *
		 * @param join container that will be copied
		 */
		public JoinBuilder(Container join) {
			if (join == null) {
				container = new JoinContainer();
			} else {
				Node node = null;
				if (join.getNode() != null) {
					node = Node.builder()
							.host(join.getNode().getHost())
							.port(join.getNode().getPort())
							.username(join.getNode().getUsername())
							.password(join.getNode().getPassword())
							.build();
				}

				final Map<Option, List<String>> opts = new HashMap<>();
				for (Map.Entry<Option, List<String>> optionListEntry : join.getOptions().entrySet()) {
					// We need to copy the lists aswell
					final List<String> copy = new ArrayList<>();
					copy.addAll(optionListEntry.getValue());
					opts.put(optionListEntry.getKey(), copy);
				}

				// fuse executor is set when the container is being created
				this.container = new JoinContainer()
						.name(join.getName())
						.user(join.getUser())
						.password(join.getPassword())
						// TODO(rjakubco): Not sure if join container can be recognized as root but probably it shouldn't
						.root(false)

						// We need to create a new instance of the node for the cloning case, otherwise all clones
						// would share the same object instance
						.node(node)
						.parent(join.getParent())
						.parentName(join.getParentName())
						// The same as node
						.options(opts)
						// Set directory to containers because otherwise it will be created in fafram/
						.directory(OptionUtils.getString(opts, Option.WORKING_DIRECTORY) + "containers/" + join.getName());
			}
		}

		/**
		 * Builds the container.
		 *
		 * @return rootcontainer instance
		 */
		public Container build() {
			return container;
		}
	}
}
