package org.jboss.fuse.qa.fafram8.cluster.container;

import org.jboss.fuse.qa.fafram8.cluster.node.Node;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.ssh.FuseSSHClient;
import org.jboss.fuse.qa.fafram8.ssh.SSHClient;
import org.jboss.fuse.qa.fafram8.util.Option;
import org.jboss.fuse.qa.fafram8.util.OptionUtils;

import com.google.common.collect.Lists;

import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract container class - contains all the shared attributes and methods among the containers.
 * Created by avano on 1.2.16.
 */
@Slf4j
@ToString
public abstract class Container implements Comparable<Container> {
	public static final int DEFAULT_FUSE_PORT = 8101;

	@Getter
	@Setter
	private String name;

	@Getter
	@Setter
	private Executor executor;

	@Getter
	@Setter
	private Node node;

	@Getter
	@Setter
	private Container parent;

	@Getter
	@Setter
	// Because when using .containers(Root, Child), the child is spawned before the root is in the list,
	// so we just save it's name and assign the actual container object later
	private String parentName;

	@Getter
	@Setter
	private boolean root;

	@Getter
	@Setter
	private boolean online;

	@Getter
	@Setter
	// Flag if the container was really created - for example suppressStart does not create the containers and then the destroy methods
	// were causing problems
	private boolean created;

	@Getter
	@Setter
	private boolean fabric;

	@Getter
	@Setter
	private Map<Option, List<String>> options = getInitialOptionsMap();

	// Full path to unzipped product directory for root container
	@Getter
	@Setter
	private String fusePath;

	// Property for deciding if we should clean environment or only try to connect to running instance
	// Default value is set to false
	@Getter
	@Setter
	private boolean onlyConnect = false;

	// SSH port of Fuse instance. This parameter is used only when using onlyConnect feature for connecting to running Fuse instance
	// This port is used when creating executor for Fuse
	@Getter
	@Setter
	private int fuseSshPort = DEFAULT_FUSE_PORT;

	/**
	 * Creates a container.
	 */
	public abstract void create();

	/**
	 * Destoys a container.
	 */
	public abstract void destroy();

	/**
	 * Restarts a container.
	 *
	 * @param force force flag
	 */
	public abstract void restart(boolean force);

	/**
	 * Starts a container.
	 *
	 * @param force force flag
	 */
	public abstract void start(boolean force);

	/**
	 * Stops a container.
	 *
	 * @param force force flag
	 */
	public abstract void stop(boolean force);

	/**
	 * Kills a container.
	 */
	public abstract void kill();

	/**
	 * Waits for success provision state.
	 */
	public abstract void waitForProvisioning();

	/**
	 * Waits for the successful provisioning for a given amount of time.
	 *
	 * @param time time in seconds
	 */
	public abstract void waitForProvisioning(int time);

	/**
	 * Waits for defined provision status.
	 *
	 * @param status status
	 */
	public abstract void waitForProvisionStatus(String status);

	/**
	 * Waits for the defined provision status for a given amount of time.
	 *
	 * @param status provision status
	 * @param time time in seconds
	 */
	public abstract void waitForProvisionStatus(String status, int time);

	/**
	 * Executes multiple commands in container.
	 *
	 * @param commands commands array to execute
	 * @return list of commands responses
	 */
	public abstract List<String> executeCommands(String... commands);

	/**
	 * Executes multiple commands in node shell.
	 *
	 * @param commands commands array to execute
	 * @return list of commands responses
	 */
	public abstract List<String> executeNodeCommands(String... commands);

	/**
	 * Executes a command in container.
	 *
	 * @param command command to execute
	 * @return command response
	 */
	public String executeCommand(String command) {
		return executeCommands(command).get(0);
	}

	/**
	 * Executes a command in node shell.
	 *
	 * @param command command to execute
	 * @return command response
	 */
	public String executeNodeCommand(String command) {
		return executeNodeCommands(command).get(0);
	}

	/**
	 * Restarts a container.
	 */
	public void restart() {
		restart(false);
	}

	/**
	 * Starts a container.
	 */
	public void start() {
		start(false);
	}

	/**
	 * Stops a container.
	 */
	public void stop() {
		stop(false);
	}

	/**
	 * Getter.
	 *
	 * @return user
	 */
	public String getUser() {
		return OptionUtils.getString(this.getOptions(), Option.USER);
	}

	/**
	 * Getter.
	 *
	 * @return password
	 */
	public String getPassword() {
		return OptionUtils.getString(this.getOptions(), Option.PASSWORD);
	}

	/**
	 * Creates the executor from the specified attributes. It is used in builder and in OpenstackProvisionProvider.
	 *
	 * @return executor instance
	 */
	public Executor createExecutor() {
		final SSHClient fuseClient = new FuseSSHClient()
				.host(this.getNode().getHost())
				.port(this.getFuseSshPort())
				.username(this.getUser())
				.password(this.getPassword());
		return new Executor(fuseClient, this.getName());
	}

	/**
	 * Getter.
	 *
	 * @return profiles list
	 */
	public List<String> getProfiles() {
		return OptionUtils.get(this.getOptions(), Option.PROFILE);
	}

	/**
	 * Setter.
	 *
	 * @param name name
	 * @return this
	 */
	public Container name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param user user
	 * @return this
	 */
	public Container user(String user) {
		OptionUtils.set(this.getOptions(), Option.USER, user);
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param password password
	 * @return this
	 */
	public Container password(String password) {
		OptionUtils.set(this.getOptions(), Option.PASSWORD, password);
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param node node
	 * @return this
	 */
	public Container node(Node node) {
		this.node = node;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param parent parent container
	 * @return this
	 */
	public Container parent(Container parent) {
		this.parent = parent;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param parentName parent container name
	 * @return this
	 */
	public Container parentName(String parentName) {
		this.parentName = parentName;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param commands commands array
	 * @return this
	 */
	public Container commands(List<String> commands) {
		OptionUtils.set(getOptions(), Option.COMMANDS, commands);
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param bundles bundles array
	 * @return this
	 */
	public Container bundles(List<String> bundles) {
		OptionUtils.set(getOptions(), Option.BUNDLES, bundles);
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param root root flag
	 * @return this
	 */
	public Container root(boolean root) {
		this.root = root;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param profiles profiles list
	 * @return this
	 */
	public Container profiles(List<String> profiles) {
		OptionUtils.set(getOptions(), Option.PROFILE, profiles);
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param fabric fabric flag
	 * @return this
	 */
	public Container fabric(boolean fabric) {
		this.fabric = fabric;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param fabricCreateArguments fabric create arguments
	 * @return this
	 */
	public Container fabricCreateArguments(String fabricCreateArguments) {
		OptionUtils.set(getOptions(), Option.FABRIC_CREATE, fabricCreateArguments);
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param map options map
	 * @return this
	 */
	public Container options(Map<Option, List<String>> map) {
		this.options = map;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param version version
	 * @return this
	 */
	public Container version(String version) {
		OptionUtils.set(getOptions(), Option.VERSION, version);
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param envs environment variables
	 * @return this
	 */
	public Container env(List<String> envs) {
		OptionUtils.set(getOptions(), Option.ENV, envs);
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param jvmOpts JVM options
	 * @return this
	 */
	public Container jvmOpts(List<String> jvmOpts) {
		OptionUtils.set(getOptions(), Option.JVM_OPTS, jvmOpts);
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param jvmMemOpts JVM memory options
	 * @return this
	 */
	public Container jvmMemOpts(List<String> jvmMemOpts) {
		OptionUtils.set(getOptions(), Option.JVM_MEM_OPTS, jvmMemOpts);
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param workingDirectory file path to working directory for container
	 * @return this
	 */
	public Container directory(String workingDirectory) {
		OptionUtils.set(getOptions(), Option.WORKING_DIRECTORY, workingDirectory);
		return this;
	}

	/**
	 * Gets the initialized options map.
	 *
	 * @return initialized options map
	 */
	private Map<Option, List<String>> getInitialOptionsMap() {
		final Map<Option, List<String>> map = new HashMap<>();
		map.put(Option.USER, Lists.newArrayList(SystemProperty.getFuseUser()));
		map.put(Option.PASSWORD, Lists.newArrayList(SystemProperty.getFusePassword()));

		if (!System.getProperty("os.name").startsWith("Windows")) {
			map.put(Option.JVM_OPTS, Lists.newArrayList("-Djava.security.egd=file:/dev/./urandom"));
		}

		return map;
	}

	@Override
	public int compareTo(@Nonnull Container other) {
		return this.getParentCount() - other.getParentCount();
	}

	/**
	 * Gets the parent count of container.
	 *
	 * @return parent count
	 */
	public int getParentCount() {
		Container c = this;
		int count = 0;
		do {
			final Container currentParent = c.getParent() == null ? (c.getParentName() == null ? null : ContainerManager.getContainer(c.getParentName())) : c.getParent();
			if (currentParent == null) {
				break;
			}
			count++;
			c = currentParent;
		} while (c != null);
		return count;
	}
}
