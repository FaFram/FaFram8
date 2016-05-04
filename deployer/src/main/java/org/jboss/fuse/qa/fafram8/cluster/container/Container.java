package org.jboss.fuse.qa.fafram8.cluster.container;

import org.jboss.fuse.qa.fafram8.cluster.node.Node;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.ssh.FuseSSHClient;
import org.jboss.fuse.qa.fafram8.ssh.SSHClient;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

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
	@Getter
	@Setter
	private String name;

	@Getter
	@Setter
	private String user = SystemProperty.getFuseUser();

	@Getter
	@Setter
	private String password = SystemProperty.getFusePassword();

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
	private List<String> commands = new ArrayList<>();

	@Getter
	@Setter
	private List<String> bundles = new ArrayList<>();

	@Getter
	@Setter
	private List<String> profiles = new ArrayList<>();

	@Getter
	@Setter
	private boolean fabric;

	@Getter
	@Setter
	private String fabricCreateArguments = "";

	@Getter
	@Setter
	private String version;

	@Getter
	@Setter
	private List<String> envs = new ArrayList<>();

	@Getter
	@Setter
	private List<String> jvmOpts = new ArrayList<>();

	@Getter
	@Setter
	private List<String> jvmMemOpts = new ArrayList<>();

	@Getter
	@Setter
	private String workingDirectory = "";

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
	private int fuseSshPort = 8101;

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
	 * @param force force flag
	 */
	public abstract void restart(boolean force);

	/**
	 * Starts a container.
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
		return new Executor(fuseClient);
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
		this.user = user;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param password password
	 * @return this
	 */
	public Container password(String password) {
		this.password = password;
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
		this.commands = commands;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param bundles bundles array
	 * @return this
	 */
	public Container bundles(List<String> bundles) {
		this.bundles = bundles;
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
		this.profiles = profiles;
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
		this.fabricCreateArguments = fabricCreateArguments;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param version version
	 * @return this
	 */
	public Container version(String version) {
		this.version = version;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param envs environment variables
	 * @return this
	 */
	public Container env(List<String> envs) {
		this.envs = envs;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param jvmOpts JVM options
	 * @return this
	 */
	public Container jvmOpts(List<String> jvmOpts) {
		this.jvmOpts = jvmOpts;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param jvmMemOpts JVM memory options
	 * @return this
	 */
	public Container jvmMemOpts(List<String> jvmMemOpts) {
		this.jvmMemOpts = jvmMemOpts;
		return this;
	}

	/**
	 * Setter.
	 *
	 * @param workingDirectory file path to working directory for container
	 * @return this
	 */
	public Container directory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
		return this;
	}

	@Override
	public int compareTo(@Nonnull Container other) {
		return this.getParentCount() - other.getParentCount();
	}

	/**
	 * Gets the parent count of container.
	 * @return parent count
	 */
	public int getParentCount() {
		Container c = this;
		int count = 0;
		do {
			if (c.getParent() == null) {
				break;
			}
			count++;
			c = c.getParent();
		} while (c != null);
		return count;
	}
}
