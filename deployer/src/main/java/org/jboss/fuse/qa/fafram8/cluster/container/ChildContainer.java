package org.jboss.fuse.qa.fafram8.cluster.container;

import org.jboss.fuse.qa.fafram8.cluster.resolver.Resolver;
import org.jboss.fuse.qa.fafram8.deployer.ContainerSummoner;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.util.Option;
import org.jboss.fuse.qa.fafram8.util.OptionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Class representing child container. Instances of this class should and can be created just with ChildBuilder class.
 * Created by avano on 1.2.16.
 */
@Slf4j
public class ChildContainer extends Container implements ThreadContainer {
	/**
	 * Constructor.
	 */
	protected ChildContainer() {
	}

	/**
	 * Constructor.
	 *
	 * @param name container name
	 */
	protected ChildContainer(String name) {
		super();
		super.setName(name);
		super.setRoot(false);
	}

	/**
	 * Builder getter.
	 *
	 * @return builder instance
	 */
	public static ChildBuilder builder() {
		return new ChildBuilder(null);
	}

	/**
	 * Builder getter.
	 *
	 * @param c container that will be copied
	 * @return builder instance
	 */
	public static ChildBuilder builder(Container c) {
		return new ChildBuilder(c);
	}

	@Override
	public void create() {
		create(super.getParent().getExecutor());
	}

	@Override
	public void create(Executor executor) {
		if (SystemProperty.suppressStart()) {
			return;
		}

		log.info("Creating container " + this);
		executor.connect();
		String jmxUser = super.getUser();
		String jmxPass = super.getPassword();
		if (super.getOptions().containsKey(Option.JMX_USER)) {
			jmxUser = super.getOptions().get(Option.JMX_USER).get(0);
		}
		if (super.getOptions().containsKey(Option.JMX_PASSWORD)) {
			jmxPass = super.getOptions().get(Option.JMX_PASSWORD).get(0);
		}

		executor.executeCommand(String.format("container-create-child %s --jmx-user %s --jmx-password %s %s %s",
				OptionUtils.getCommand(super.getOptions()), jmxUser, jmxPass, super.getParent().getName(), super.getName()));
		super.setCreated(true);
		try {
			executor.waitForProvisioning(this);
		} catch (FaframException e) {
			ContainerSummoner.setStopWork(true);
			throw e;
		}

		super.setOnline(true);
		// Set node object
		super.setNode(super.getParent().getNode());
		// Create a new executor
		try {
			final Executor childExecutor = super.createExecutor();
			final String port = super.getParent().getExecutor().executeCommandSilently("zk:get /fabric/registry/ports/containers/"
					+ super.getName() + "/org.apache.karaf.shell/sshPort").trim();
			childExecutor.getClient().setPort(Integer.parseInt(port));
			childExecutor.connect();
			super.setExecutor(childExecutor);
		} catch (Exception ex) {
			log.warn("Couldn't create executor / couldn't parse ssh port, child.executeCommand() won't work!");
		}
		// Set the fuse path
		try {
			super.setFusePath(super.getExecutor().executeCommandSilently("shell:info | grep \"Karaf base\"").trim().replaceAll(" +", " ").split(" ")[1]);
		} catch (Exception ex) {
			log.warn("Setting fuse path failed, it won't be available");
		}
	}

	@Override
	public void destroy() {
		destroy(super.getParent().getExecutor());
	}

	@Override
	public void destroy(Executor executor) {
		if (SystemProperty.suppressStart() || !super.isCreated()) {
			return;
		}

		log.info("Destroying container " + super.getName());
		executor.connect();
		executor.executeCommand("container-delete --force " + super.getName());
		super.setCreated(false);
		ContainerManager.getContainerList().remove(this);
	}

	@Override
	public void restart(boolean force) {
		stop(force);
		start(force);
	}

	@Override
	public void start(boolean force) {
		super.getParent().getExecutor().executeCommand("container-start " + (force ? "--force " : "") + super.getName());
		super.getParent().getExecutor().waitForProvisioning(this);
	}

	@Override
	public void stop(boolean force) {
		super.getParent().getExecutor().executeCommand("container-stop " + (force ? "--force " : "") + super.getName());
		super.getParent().getExecutor().waitForContainerStop(this);
		super.setOnline(false);
	}

	@Override
	public void kill() {
		super.getExecutor().executeCommand("exec pkill -9 -f " + super.getName());
		super.setOnline(false);
	}

	@Override
	public List<String> executeCommands(String... commands) {
		return super.getExecutor().executeCommands(commands);
	}

	@Override
	public List<String> executeNodeCommands(String... commands) {
		return super.getParent().executeNodeCommands(commands);
	}

	@Override
	public void waitForProvisioning() {
		waitForProvisionStatus("success");
	}

	@Override
	public void waitForProvisioning(int time) {
		super.getParent().getExecutor().waitForProvisioning(this, time);
	}

	@Override
	public void waitForProvisionStatus(String status) {
		super.getParent().getExecutor().waitForProvisionStatus(this, status);
	}

	@Override
	public void waitForProvisionStatus(String status, int time) {
		super.getParent().getExecutor().waitForProvisionStatus(this, status, time);
	}

	/**
	 * Child builder class - this class returns the ChildContainer object and it is the only way the child container should be built.
	 */
	public static class ChildBuilder {
		// Container instance
		private Container container;

		/**
		 * Constructor.
		 *
		 * @param copy container that will be copied.
		 */
		public ChildBuilder(Container copy) {
			if (copy == null) {
				this.container = new ChildContainer();
			} else {
				final Map<Option, List<String>> opts = new HashMap<>();
				for (Map.Entry<Option, List<String>> optionListEntry : copy.getOptions().entrySet()) {
					// We need to copy the lists aswell
					final List<String> listCopy = new ArrayList<>();
					listCopy.addAll(optionListEntry.getValue());
					opts.put(optionListEntry.getKey(), listCopy);
				}
				this.container = new ChildContainer()
						.name(copy.getName())
						.user(copy.getUser())
						.password(copy.getPassword())
						.parent(copy.getParent())
						.parentName(copy.getParentName())
						.options(opts)
						.node(null);
			}
		}

		/**
		 * Setter.
		 *
		 * @param name name
		 * @return this
		 */
		public ChildBuilder name(String name) {
			container.setName(name);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param parent parent
		 * @return this
		 */
		public ChildBuilder parent(Container parent) {
			container.setParent(parent);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param parent parent container name
		 * @return this
		 */
		public ChildBuilder parentName(String parent) {
			container.setParentName(parent);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param profiles profiles array
		 * @return this
		 */
		public ChildBuilder profiles(String... profiles) {
			OptionUtils.set(container.getOptions(), Option.PROFILE, profiles);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param commands commands array
		 * @return this
		 */
		public ChildBuilder commands(String... commands) {
			OptionUtils.get(container.getOptions(), Option.COMMANDS).addAll(Arrays.asList(commands));
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param version version
		 * @return this
		 */
		public ChildBuilder version(String version) {
			OptionUtils.set(container.getOptions(), Option.VERSION, version);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param jvmOpts JVM options
		 * @return this
		 */
		public ChildBuilder jvmOpts(String... jvmOpts) {
			OptionUtils.set(container.getOptions(), Option.JVM_OPTS, jvmOpts);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param resolver one of resolver enum
		 * @return this
		 */
		public ChildBuilder resolver(Resolver resolver) {
			OptionUtils.set(container.getOptions(), Option.RESOLVER, resolver.toString());
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param jmxUser jmx user
		 * @return this
		 */
		public ChildBuilder jmxUser(String jmxUser) {
			OptionUtils.set(container.getOptions(), Option.JMX_USER, jmxUser);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param jmxPassword jmx password
		 * @return this
		 */
		public ChildBuilder jmxPassword(String jmxPassword) {
			OptionUtils.set(container.getOptions(), Option.JMX_PASSWORD, jmxPassword);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param zkPass zookeeper password
		 * @return this
		 */
		public ChildBuilder zookeeperPassword(String zkPass) {
			OptionUtils.set(container.getOptions(), Option.ZOOKEEPER_PASSWORD, zkPass);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param manualIp manual ip
		 * @return this
		 */
		public ChildBuilder manualIp(String manualIp) {
			OptionUtils.set(container.getOptions(), Option.MANUAL_IP, manualIp);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param addr bind address
		 * @return this
		 */
		public ChildBuilder bindAddress(String addr) {
			OptionUtils.set(container.getOptions(), Option.BIND_ADDRESS, addr);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param datastore datastore option
		 * @return this
		 */
		public ChildBuilder datastore(String... datastore) {
			OptionUtils.set(container.getOptions(), Option.DATASTORE_OPTION, datastore);
			return this;
		}

		/**
		 * Setter for additional create options that does not have special method.
		 *
		 * @param options options string
		 * @return this
		 * @deprecated Use other setters, they should be complete.
		 */
		@Deprecated
		public ChildBuilder options(String options) {
			final List<String> old = OptionUtils.get(container.getOptions(), Option.OTHER);
			old.add(options);
			container.getOptions().put(Option.OTHER, old);
			return this;
		}

		/**
		 * Builds the container.
		 *
		 * @return childcontainer instance
		 */
		public Container build() {
			return this.container;
		}
	}
}
