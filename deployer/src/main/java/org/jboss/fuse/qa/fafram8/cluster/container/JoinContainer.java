package org.jboss.fuse.qa.fafram8.cluster.container;

import static org.jboss.fuse.qa.fafram8.modifier.impl.PropertyModifier.putProperty;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.cluster.node.Node;
import org.jboss.fuse.qa.fafram8.deployer.ContainerSummoner;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.manager.RemoteNodeManager;
import org.jboss.fuse.qa.fafram8.modifier.ModifierExecutor;
import org.jboss.fuse.qa.fafram8.util.Option;
import org.jboss.fuse.qa.fafram8.util.OptionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

/**
 * Internal class for representing SSH container on Windows machine.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class JoinContainer extends RootContainer implements ThreadContainer {

	private static AtomicInteger counter = new AtomicInteger(0);
	private final static int ACTIVEMQ_PORT = 61616;
	private final static int ORG_OSGI_SERVICE_HTTP_PORT = 8181;
	private final static int SSH_PORT = 8101;
	private final static int RMI_REGISTRY_PORT = 1099;
	private final static int RMI_SERVER_PORT = 44444;

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
		create(super.getParent().getExecutor());
	}

	// for thread support
	@Override
	public void create(Executor executor) {
		//TODO set working dir for join container -> because by default root container will create fafram -> this is problem if creating more than 1 container on the same node
		super.setExecutor(super.createExecutor());
		log.info("Creating JoinContainer: " + this);

		super.modifyContainer();

		// Modify ports for running multiple join containers on the same node
		ModifierExecutor.addModifiers(putProperty("etc/system.properties", "activemq.port", String.valueOf(ACTIVEMQ_PORT + counter.get())));
		ModifierExecutor.addModifiers(putProperty("etc/system.properties", "org.osgi.service.http.port", String.valueOf(ORG_OSGI_SERVICE_HTTP_PORT + counter.get())));
		ModifierExecutor.addModifiers(
				putProperty("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", String.valueOf(ORG_OSGI_SERVICE_HTTP_PORT + counter.get())));
		ModifierExecutor.addModifiers(putProperty("etc/org.apache.karaf.shell.cfg", "sshPort", String.valueOf(SSH_PORT + counter.get())));
		ModifierExecutor.addModifiers(putProperty("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", String.valueOf(RMI_REGISTRY_PORT + counter.get())));
		ModifierExecutor.addModifiers(putProperty("etc/org.apache.karaf.management.cfg", "rmiServerPort", String.valueOf(RMI_SERVER_PORT + counter.get())));
		counter.addAndGet(1);

		((RemoteNodeManager) nodeManager).clean(OptionUtils.getString(this.getOptions(), Option.WORKING_DIRECTORY));
		nodeManager.checkRunningContainer();
		try {
			nodeManager.prepareZip();
			nodeManager.unzipArtifact(this);
			super.setCreated(true);
			nodeManager.prepareFuse(this);
			nodeManager.startFuse();

			final String zookeeperUri = StringUtils.substringBetween(getParent().getExecutor().executeCommandSilently("fabric:info"), "ZooKeeper URI:", "\n").trim();
			String options = parseOptions();

			// Name of the container should be changed in property files -> only join with correct password for root and zookeeperUri from root
			super.executeCommands("fabric:join " + options + " " + zookeeperUri);
			executor.waitForProvisioning(this);
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
		StringBuilder builder = new StringBuilder();

		builder.append(" --zookeeper-password ");

		String zookeeperPassword = OptionUtils.getString(this.getOptions(), Option.ZOOKEEPER_PASSWORD).isEmpty()
				? OptionUtils.getString(this.getOptions(), Option.ZOOKEEPER_PASSWORD)
				: OptionUtils.getString(this.getOptions(), Option.PASSWORD);
		
		log.error(zookeeperPassword);
		builder.append(zookeeperPassword);

		// TODO: refactor to correct getter
		if (!OptionUtils.get(this.getOptions(), Option.PROFILE).isEmpty()) {
			for (String profile : this.getOptions().get(Option.PROFILE)) {
				builder.append(" --profile ").append(profile);
			}
		}

		if (!OptionUtils.get(this.getOptions(), Option.MAX_PORT).isEmpty()) {
			builder.append(" --max-port ").append(OptionUtils.getString(this.getOptions(), Option.MAX_PORT));
		}
		if (!OptionUtils.get(this.getOptions(), Option.MIN_PORT).isEmpty()) {
			builder.append(" --min-port ").append(OptionUtils.getString(this.getOptions(), Option.MIN_PORT));
		}
		if (!OptionUtils.get(this.getOptions(), Option.RESOLVER).isEmpty()) {
			builder.append(" --resolver ").append(OptionUtils.getString(this.getOptions(), Option.RESOLVER));
		}
		if (!OptionUtils.get(this.getOptions(), Option.MANUAL_IP).isEmpty()) {
			builder.append(" --manual-ip ").append(OptionUtils.getString(this.getOptions(), Option.MANUAL_IP));
		}
		return builder.toString();
	}

	@Override
	public void destroy(Executor executor) {
		super.destroy();
	}

	/**
	 * Root builder class - this class returns the RootContainer object and it is the only way the root container should be built.
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
