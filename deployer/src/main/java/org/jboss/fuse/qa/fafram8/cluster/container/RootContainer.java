package org.jboss.fuse.qa.fafram8.cluster.container;

import static org.jboss.fuse.qa.fafram8.modifier.impl.AccessRightsModifier.setExecutable;
import static org.jboss.fuse.qa.fafram8.modifier.impl.JavaHomeModifier.setJavaHome;
import static org.jboss.fuse.qa.fafram8.modifier.impl.JvmOptsModifier.addJvmOpts;
import static org.jboss.fuse.qa.fafram8.modifier.impl.PropertyModifier.putProperty;
import static org.jboss.fuse.qa.fafram8.modifier.impl.RootNameModifier.setRootName;

import org.jboss.fuse.qa.fafram8.cluster.node.Node;
import org.jboss.fuse.qa.fafram8.deployer.ContainerSummoner;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.executor.WindowsExecutor;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.manager.LocalNodeManager;
import org.jboss.fuse.qa.fafram8.manager.NodeManager;
import org.jboss.fuse.qa.fafram8.manager.RemoteLinuxNodeManager;
import org.jboss.fuse.qa.fafram8.manager.RemoteNodeManager;
import org.jboss.fuse.qa.fafram8.modifier.Modifier;
import org.jboss.fuse.qa.fafram8.modifier.ModifierExecutor;
import org.jboss.fuse.qa.fafram8.modifier.impl.JvmMemoryModifier;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.util.Option;
import org.jboss.fuse.qa.fafram8.util.OptionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Class representing root container. Instances of this class should and can be created just with RootBuilder class, because it sets
 * necessary modifiers, etc.
 * Created by avano on 1.2.16.
 */
@Slf4j
public class RootContainer extends Container {
	// Node manager instance - sets up the container on the host
	@Getter
	protected NodeManager nodeManager;
	@Setter
	@Getter
	private Modifier usersMod = null;

	/**
	 * Constructor.
	 */
	protected RootContainer() {
		super();
		super.setRoot(true);
	}

	/**
	 * Constructor.
	 *
	 * @param name container name
	 */
	protected RootContainer(String name) {
		super();
		super.setName(name);
	}

	/**
	 * Builder getter.
	 *
	 * @return builder instance
	 */
	public static RootBuilder builder() {
		return new RootBuilder(null);
	}

	/**
	 * Builder getter.
	 *
	 * @param c container that will be copied
	 * @return builder instance
	 */
	public static RootBuilder builder(Container c) {
		return new RootBuilder(c);
	}

	@Override
	public void create() {
		// Create fuse executor
		super.setExecutor(super.createExecutor());
		final String logMsg = (super.isOnlyConnect()) ? "Connecting to " : "Creating ";
		log.info(logMsg + this);

		if (!super.isOnlyConnect() || SystemProperty.isClean()) {
			modifyContainer();

			nodeManager.clean();
			nodeManager.checkRunningContainer();
			super.getNode().getExecutor().executeCommands(OptionUtils.get(this.getOptions(), Option.STARTUP_NODE_COMMANDS)
					.toArray(new String[OptionUtils.get(this.getOptions(), Option.STARTUP_NODE_COMMANDS).size()]));
			try {
				nodeManager.prepareZip();
				nodeManager.unzipArtifact(this);
				super.setCreated(true);
				nodeManager.prepareFuse(this);
				if (!SystemProperty.suppressStart()) {
					nodeManager.startFuse();
					ContainerManager.patchStandaloneBeforeFabric(this);

					ContainerManager.setupFabric(this);
					ContainerManager.patchFuse(this);
					ContainerManager.executeStartupCommands(this);
					super.setOnline(true);
				}
			} catch (FaframException ex) {
				ex.printStackTrace();
				ContainerSummoner.setStopWork(true);
				nodeManager.stopAndClean(true);
				throw new FaframException(ex);
			}
		} else {
			log.trace("Connecting both executors when using onlyConnect");
			super.getExecutor().connect();
			super.getNode().getExecutor().connect();
		}
	}

	/**
	 * Sets and modifies all necessary options and things for root container.
	 */
	protected void modifyContainer() {
		// Instantiate the node manager based on node.getHost()
		if ("localhost".equals(super.getNode().getHost())) {
			nodeManager = new LocalNodeManager(super.getExecutor());
		} else {
			// Re-create the executor
			super.getNode().setExecutor(super.getNode().createExecutor());
			// Connect the node executor
			if (!super.getNode().getExecutor().isConnected()) {
				log.trace("First time connecting node executor");
				super.getNode().getExecutor().connect();
			}
			if (super.getNode().getExecutor().isCygwin()) {
				log.trace("Using RemoteWindowsNodeManager!");
				nodeManager = new RemoteLinuxNodeManager(new WindowsExecutor(super.getNode().getExecutor()), super.getExecutor());
			} else {
				nodeManager = new RemoteLinuxNodeManager(super.getNode().getExecutor(), super.getExecutor());
			}
			// Set working directory for root container if it was set on root container object
			// It will be either empty string of file system path
			if (!OptionUtils.getString(super.getOptions(), Option.WORKING_DIRECTORY).isEmpty()) {
				((RemoteNodeManager) nodeManager).setWorkingDirectory(OptionUtils.getString(super.getOptions(), Option.WORKING_DIRECTORY));
			}
		}

		// If we shouldn't skip default user and the usersMod is null == we dont add specific user to the container, so add fafram/fafram
		if (usersMod == null && !SystemProperty.skipDefaultUser()) {
			// Add default user which is now fafram/fafram with only role Administrator for more transparent tests
			ModifierExecutor.addModifiers(
					putProperty(super.getNode().getHost(), "etc/users.properties", super.getUser(), super.getPassword() + ",Administrator"));
			// If we should skip default user and the usersMod is set, use usersMod, otherwise the modifier from Fafram.addUser() will be used
		} else if (usersMod != null) {
			usersMod.setHost(super.getNode().getHost());
			ModifierExecutor.addModifiers(usersMod);
			usersMod = null;
		}

		if (!SystemProperty.getJavaHome().isEmpty()) {
			ModifierExecutor.addModifiers(setJavaHome(SystemProperty.getJavaHome()));
		}

		if (!OptionUtils.get(getOptions(), Option.JVM_MEM_OPTS).isEmpty()) {
			ModifierExecutor.addModifiers(JvmMemoryModifier.setJvmMemOpts(OptionUtils.get(getOptions(), Option.JVM_MEM_OPTS)));
		}

		ModifierExecutor.addModifiers(setExecutable("bin/karaf", "bin/start", "bin/stop", "bin/client", "bin/fuse"),
				setRootName(this, super.getNode().getHost()), addJvmOpts(OptionUtils.get(getOptions(), Option.JVM_OPTS)));
	}

	@Override
	public void destroy() {
		if (!super.isCreated()) {
			return;
		}
		super.getNode().getExecutor().stopKeepAliveTimer();
		super.getExecutor().stopKeepAliveTimer();

		if ("localhost".equals(super.getNode().getHost())) {
			ModifierExecutor.executePostModifiers(this);
		} else {
			ModifierExecutor.executePostModifiers(this, super.getNode().getExecutor());
		}

		log.info("Destroying container " + super.getName());

		if (super.isCreated()) {
			nodeManager.stopAndClean(false);
			log.trace("Disconnecting executor after destroying root container");
			super.getExecutor().disconnect();
			if (super.getNode().getExecutor() != null && super.getNode().getExecutor().isConnected()) {
				log.trace("Disconnecting node executor after destroying root container");
				super.getNode().getExecutor().disconnect();
			}
		}
		super.setCreated(false);
		ContainerManager.getContainerList().remove(this);
	}

	@Override
	public void restart(boolean force) {
		// Force not used with root container
		executeCommand("system-property karaf.restart.jvm true");
		nodeManager.restart();
		log.trace("Connecting the executor after restarting the container");
		super.getExecutor().connect();
		if (super.isFabric()) {
			waitForProvisioning();
		}
	}

	@Override
	public void start(boolean force) {
		// Force not used with root container
		nodeManager.startFuse();
		super.setOnline(true);
		log.trace("Connecting the executor after starting the container");
		super.getExecutor().connect();
		if (super.isFabric()) {
			waitForProvisioning();
		}
	}

	@Override
	public void stop(boolean force) {
		// Force not used with root container
		nodeManager.stop();
		super.setOnline(false);
		log.trace("Disconnecting executor in root's stop()");
		super.getExecutor().disconnect();
	}

	@Override
	public void kill() {
		super.getNode().getExecutor().executeCommand("pkill -9 -f " + super.getName());
		super.setOnline(false);
		log.trace("Disconnecting executor in root's kill()");
		super.getExecutor().disconnect();
	}

	@Override
	public void waitForProvisioning() {
		waitForProvisionStatus("success");
	}

	@Override
	public void waitForProvisioning(int time) {
		super.getExecutor().waitForProvisioning(this, time);
	}

	@Override
	public void waitForProvisionStatus(String status) {
		super.getExecutor().waitForProvisionStatus(this, status);
	}

	@Override
	public void waitForProvisionStatus(String status, int time) {
		super.getExecutor().waitForProvisionStatus(this, status, time);
	}

	@Override
	public List<String> executeCommands(String... commands) {
		return super.getExecutor().executeCommands(commands);
	}

	@Override
	public List<String> executeNodeCommands(String... commands) {
		if ("localhost".equals(super.getNode().getHost())) {
			log.error("Execute node command is not supported on localhost");
			return null;
		}

		return super.getNode().getExecutor().executeCommands(commands);
	}

	/**
	 * Uploads bundles to fabric maven proxy on root container (remote).
	 *
	 * @param projectPaths list of paths to pom.xml files of different projects for upload
	 */
	public void uploadBundles(String... projectPaths) {
		for (String projectPath : projectPaths) {
			ContainerManager.uploadBundle(this, projectPath);
		}
	}

	/**
	 * Root builder class - this class returns the RootContainer object and it is the only way the root container should be built.
	 */
	public static class RootBuilder {
		// Container instance
		private Container container;

		/**
		 * Constructor.
		 *
		 * @param root container that will be copied
		 */
		public RootBuilder(Container root) {
			if (root == null) {
				container = new RootContainer();
			} else {
				Node node = null;
				if (root.getNode() != null) {
					node = Node.builder()
							.host(root.getNode().getHost())
							.port(root.getNode().getPort())
							.username(root.getNode().getUsername())
							.password(root.getNode().getPassword())
							.build();
				}

				final Map<Option, List<String>> opts = new HashMap<>();
				for (Map.Entry<Option, List<String>> optionListEntry : root.getOptions().entrySet()) {
					// We need to copy the lists aswell
					final List<String> copy = new ArrayList<>();
					copy.addAll(optionListEntry.getValue());
					opts.put(optionListEntry.getKey(), copy);
				}

				// fuse executor is set when the container is being created
				this.container = new RootContainer()
						.name(root.getName())
						.user(root.getUser())
						.password(root.getPassword())
						.root(true)
						// We need to create a new instance of the node for the cloning case, otherwise all clones
						// would share the same object instance
						.node(node)
						.parent(null)
						.parentName(null)
						.fabric(root.isFabric())
						// The same as node
						.options(opts);
			}
		}

		/**
		 * Setter.
		 *
		 * @param name name
		 * @return this
		 */
		public RootBuilder name(String name) {
			container.setName(name);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param user username
		 * @return this
		 * @deprecated if you want to change this, use addUser in RootContainer.builder()
		 */
		@Deprecated
		public RootBuilder user(String user) {
			OptionUtils.set(container.getOptions(), Option.USER, user);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param password password
		 * @return this
		 * @deprecated if you want to change this, use addUser in RootContainer.builder()
		 */
		@Deprecated
		public RootBuilder password(String password) {
			OptionUtils.set(container.getOptions(), Option.PASSWORD, password);
			return this;
		}

		/**
		 * Adds the user to the container and uses his credentials as login ssh credentials.
		 *
		 * @param user username
		 * @param pass password
		 * @param roles comma-separated roles
		 * @return this
		 */
		public RootBuilder addUser(String user, String pass, String roles) {
			OptionUtils.set(container.getOptions(), Option.USER, user);
			OptionUtils.set(container.getOptions(), Option.PASSWORD, pass);
			((RootContainer) container).setUsersMod(putProperty("etc/users.properties", user, pass + "," + roles));
			// We need to pass it to ModifierExecutor later when we will have the IP
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param node node instance
		 * @return this
		 */
		public RootBuilder node(Node node) {
			container.setNode(node);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param host host
		 * @return this
		 */
		public RootBuilder node(String host) {
			container.setNode(Node.builder().host(host).build());

			return this;
		}

		/**
		 * Setter.
		 *
		 * @param host host
		 * @param user user
		 * @param password password
		 * @return this
		 */
		public RootBuilder node(String host, String user, String password) {
			container.setNode(
					Node.builder()
							.host(host)
							.username(user)
							.password(password)
							.build()
			);

			return this;
		}

		/**
		 * Setter.
		 *
		 * @param host host
		 * @param port port
		 * @param user user
		 * @param password password
		 * @return this
		 */
		public RootBuilder node(String host, int port, String user, String password) {
			container.setNode(
					Node.builder()
							.host(host)
							.port(port)
							.username(user)
							.password(password)
							.build()
			);

			return this;
		}

		/**
		 * Setter.
		 *
		 * @return this
		 */
		public RootBuilder withFabric() {
			container.setFabric(true);

			return this;
		}

		/**
		 * Setter.
		 *
		 * @param args fabric create arguments
		 * @return this
		 */
		public RootBuilder withFabric(String args) {
			container.setFabric(true);
			OptionUtils.set(container.getOptions(), Option.FABRIC_CREATE, args);

			return this;
		}

		/**
		 * Setter.
		 *
		 * @param commands commands array
		 * @return this
		 */
		public RootBuilder commands(String... commands) {
			OptionUtils.set(container.getOptions(), Option.COMMANDS, commands);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param bundles bundles array
		 * @return this
		 */
		public RootBuilder bundles(String... bundles) {
			OptionUtils.set(container.getOptions(), Option.BUNDLES, bundles);
			return this;
		}

		/**
		 * Sets the JVM options.
		 *
		 * @param xms xms
		 * @param xmx xmx
		 * @param permMem perm mem
		 * @param maxPermMem max perm mem
		 * @return this
		 */
		public RootBuilder jvmMemoryOpts(String xms, String xmx, String permMem, String maxPermMem) {
			OptionUtils.set(container.getOptions(), Option.JVM_MEM_OPTS, "JAVA_MIN_MEM=" + xms, "JAVA_MAX_MEM=" + xmx,
					"JAVA_PERM_MEM=" + permMem, "JAVA_MAX_PERM_MEM=" + maxPermMem);
			return this;
		}

		/**
		 * Sets JVM opts.
		 *
		 * @param jvmOpts JVM options for setting
		 * @return this
		 */
		public RootBuilder jvmOpts(String... jvmOpts) {
			OptionUtils.set(container.getOptions(), Option.JVM_OPTS, jvmOpts);
			return this;
		}

		/**
		 * Builds the default root container.
		 *
		 * @return this
		 */
		public RootBuilder defaultRoot() {
			// Check if we should add the fabric attributes
			if (SystemProperty.isFabric() && !container.isFabric()) {
				container.setFabric(true);
				OptionUtils.set(container.getOptions(), Option.FABRIC_CREATE, SystemProperty.getFabric());
			}
			// Create node with default properties but without the host - it will be set later in ContainerManager configure roots
			// because in this moment we don't know if we use local or openstack or anything else
			// Port, user and pass are automatically set in Node object from system properties
			container.setNode(Node.builder().build());
			container.setName(SystemProperty.getDefaultRootName());
			OptionUtils.set(container.getOptions(), Option.USER, SystemProperty.getFuseUser());
			OptionUtils.set(container.getOptions(), Option.PASSWORD, SystemProperty.getFusePassword());
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param workingDirectory file path to working directory for SSH container
		 * @return this
		 */
		public RootBuilder directory(String workingDirectory) {
			OptionUtils.set(container.getOptions(), Option.WORKING_DIRECTORY, workingDirectory);
			return this;
		}

		/**
		 * Defines that remote Fuse shouldn't be deleted and Fafram should only connect to running instance.
		 * You can specify SSH port for connecting to running instance.
		 *
		 * @param port port number for running Fuse instance
		 * @return this
		 */
		public RootBuilder onlyConnect(int port) {
			SystemProperty.set(FaframConstant.CLEAN, "false");
			container.setOnlyConnect(true);
			container.setFuseSshPort(port);
			return this;
		}

		/**
		 * Defines that remote Fuse shouldn't be deleted and Fafram should only connect to running instance.
		 * Default option when you want to use default SSH port for Fuse.
		 *
		 * @return this
		 */
		public RootBuilder onlyConnect() {
			onlyConnect(container.getFuseSshPort());
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param profiles profiles array
		 * @return this
		 */
		public RootBuilder profiles(String... profiles) {
			OptionUtils.set(container.getOptions(), Option.PROFILE, profiles);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param commands startup node commands
		 * @return this
		 */
		public RootBuilder startupNodeCommands(String... commands) {
			OptionUtils.set(container.getOptions(), Option.STARTUP_NODE_COMMANDS, commands);
			return this;
		}

		/**
		 * Builds the container.
		 *
		 * @return rootcontainer instance
		 */
		public Container build() {
			// Add zookeeper commands because child/ssh container.stop() need them
			if (container.isFabric()) {
				final String zkCommand = "fabric:profile-edit --feature fabric-zookeeper-commands/0.0.0 default";
				if (!OptionUtils.get(container.getOptions(), Option.COMMANDS).contains(zkCommand)) {
					container.getOptions().get(Option.COMMANDS).add(zkCommand);
				}
				if (SystemProperty.clearDefaultRepositories()) {
					container.getOptions().get(Option.COMMANDS).add("fabric:profile-edit --pid io.fabric8.agent/org.ops4j.pax.url.mvn.repositories='file:${runtime.home}/"
							+ "${karaf.default.repository}@snapshots@id=karaf-default, file:${runtime.data}/maven/upload@snapshots@id=fabric-upload' "
							+ "default");
				}
			}
			return container;
		}
	}
}
