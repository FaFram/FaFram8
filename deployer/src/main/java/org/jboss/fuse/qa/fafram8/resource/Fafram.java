package org.jboss.fuse.qa.fafram8.resource;

import static org.jboss.fuse.qa.fafram8.modifier.impl.ArchiveModifier.registerArchiver;
import static org.jboss.fuse.qa.fafram8.modifier.impl.FileModifier.moveFile;
import static org.jboss.fuse.qa.fafram8.modifier.impl.JvmMemoryModifier.setDefaultJvmMemOpts;
import static org.jboss.fuse.qa.fafram8.modifier.impl.JvmMemoryModifier.setJvmMemOpts;
import static org.jboss.fuse.qa.fafram8.modifier.impl.PropertyModifier.extendProperty;
import static org.jboss.fuse.qa.fafram8.modifier.impl.PropertyModifier.putProperty;

import org.apache.maven.shared.invoker.MavenInvocationException;

import org.jboss.fuse.qa.fafram8.cluster.broker.Broker;
import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.configuration.ConfigurationParser;
import org.jboss.fuse.qa.fafram8.deployer.ContainerSummoner;
import org.jboss.fuse.qa.fafram8.deployer.Deployer;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.exception.ValidatorException;
import org.jboss.fuse.qa.fafram8.invoker.MavenPomInvoker;
import org.jboss.fuse.qa.fafram8.invoker.MavenProject;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.modifier.Modifier;
import org.jboss.fuse.qa.fafram8.modifier.ModifierExecutor;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.property.Openstack;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.provision.provider.OpenStackProvisionProvider;
import org.jboss.fuse.qa.fafram8.provision.provider.ProvisionProvider;
import org.jboss.fuse.qa.fafram8.provision.provider.StaticProvider;
import org.jboss.fuse.qa.fafram8.util.CommandHistory;
import org.jboss.fuse.qa.fafram8.util.callables.Response;
import org.jboss.fuse.qa.fafram8.validator.Validator;

import org.junit.rules.ExternalResource;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Fafram resource class.
 * Created by avano on 19.8.15.
 */
@Slf4j
public class Fafram extends ExternalResource {
	//Provision provider instance in case of remote deployment
	@Getter
	private ProvisionProvider provisionProvider = new StaticProvider();

	@SuppressWarnings("FieldCanBeLocal")
	private ConfigurationParser configurationParser;

	// Default root container used for .executeCommand() etc.
	@Setter
	@Getter
	private Container rootContainer;

	// Flag if the fafram has already finished the initialization and it's running
	// Used in .containers method
	private boolean running = false;

	@Getter
	private List<MavenProject> bundlesToBuild = new ArrayList<>();

	/**
	 * Constructor.
	 */
	public Fafram() {
	}

	@Override
	protected void before() {
		setup();
	}

	@Override
	protected void after() {
		tearDown();
	}

	/**
	 * Start method.
	 *
	 * @return this
	 */
	public Fafram setup() {
		try {
			SystemProperty.checkKeepAllProperty();
			printLogo();
			initConfiguration();
			ContainerManager.configureRoots();
			Validator.validate();
			ContainerManager.initBrokers();
			prepareNodes(ContainerManager.getContainerList());
			setDefaultModifiers();
			buildBundles();
			Deployer.deploy();
			ContainerManager.createEnsemble();
		} catch (Exception ex) {
			tearDown(true);
			// Rethrow the exception so that we will know what happened
			if (ex instanceof ValidatorException) {
				throw ex;
			} else {
				ex.printStackTrace();
				throw new FaframException("Exception thrown while initializing! " + ex);
			}
		}

		// Save the first root we find - used in .executeCommand() and probably some more methods
		rootContainer = getRoot();

		running = true;

		return this;
	}

	/**
	 * Stop method.
	 */
	public void tearDown() {
		tearDown(false);
	}

	/**
	 * Stop method.
	 */
	public void tearDown(boolean force) {
		try {
			CommandHistory.writeLogs();
			// There can be a problem with stopping containers
			Deployer.destroy(force);
		} catch (Exception ex) {
			CommandHistory.writeLogs();
			ex.printStackTrace();

			if (!SystemProperty.isKeepOsResources()) {
				provisionProvider.cleanIpTables(ContainerManager.getContainerList());
				provisionProvider.releaseResources();
			}
			SystemProperty.clearAllProperties();
			ModifierExecutor.clearAllModifiers();
			ContainerManager.clearAllLists();
			running = false;
			throw new FaframException(ex);
		}

		if (!SystemProperty.isKeepOsResources()) {
			provisionProvider.cleanIpTables(ContainerManager.getContainerList());
			provisionProvider.releaseResources();
		}

		// Thread related cleaning
		ContainerSummoner.setStopWork(false);
		Deployer.setFail(false);
		Deployer.getAnnihilatingThreads().clear();
		Deployer.getSummoningThreads().clear();

		SystemProperty.clearAllProperties();
		ModifierExecutor.clearAllModifiers();
		ContainerManager.clearAllLists();
		running = false;
	}

	/**
	 * -------------------------------------------- FLUENT METHODS START --------------------------------------------
	 */

	/**
	 * Adds container to the container list.
	 *
	 * @param containers containers array
	 * @return this
	 */
	public Fafram containers(Container... containers) {
		ContainerManager.getContainerList().addAll(new ArrayList<>(Arrays.asList(containers)));
		Collections.sort(ContainerManager.getContainerList());
		if (running) {
			// == if we are adding the containers in the test method, we need to create them
			// Validate the containers
			Validator.validate();
			// Create new nodes using provider
			prepareNodes(Arrays.asList(containers));
			Deployer.deploy();
		}
		return this;
	}

	/**
	 * Add broker(s) to the broker list.
	 *
	 * @param brokers brokers array
	 * @return this
	 */
	public Fafram brokers(Broker... brokers) {
		ContainerManager.getBrokers().addAll(new ArrayList<>(Arrays.asList(brokers)));
		if (running) {
			ContainerManager.initBrokers(brokers);
		}
		return this;
	}

	/**
	 * Adds modifiers to the custom modifiers collection.
	 *
	 * @param modifiers custom modifiers
	 * @return this
	 */
	public Fafram modifiers(Modifier... modifiers) {
		ModifierExecutor.addCustomModifiers(modifiers);
		if (running) {
			// If we are running modifiers in the test, execute them directly
			for (Modifier mod : modifiers) {
				for (Container c : ContainerManager.getContainerList()) {
					if (c instanceof RootContainer) {
						mod.execute(c);
					}
				}
			}
		}
		return this;
	}

	/**
	 * Adds a new user.
	 *
	 * @param user user
	 * @param password password
	 * @param roles comma-separated roles
	 * @return this
	 */
	public Fafram addUser(String user, String password, String roles) {
		ModifierExecutor.addModifiers(putProperty("etc/users.properties", user, password + "," + roles));
		// Do not add the default user if we override it
		if (user.equals(SystemProperty.getFuseUser())) {
			SystemProperty.set(FaframConstant.SKIP_DEFAULT_USER, "");
		}
		return this;
	}

	/**
	 * Modifies (add/extend) a property.
	 *
	 * @param file file path relative to karaf home
	 * @param key key
	 * @param value value
	 * @param extend extend flag
	 * @return this
	 */
	public Fafram modifyProperty(String file, String key, String value, boolean extend) {
		if (extend) {
			ModifierExecutor.addModifiers(extendProperty(file, key, value));
		} else {
			ModifierExecutor.addModifiers(putProperty(file, key, value));
		}

		return this;
	}

	/**
	 * Replaces a file.
	 *
	 * @param fileToReplace file to replace
	 * @param fileToUse file to use
	 * @return this
	 */
	public Fafram replaceFile(String fileToReplace, String fileToUse) {
		ModifierExecutor.addModifiers(moveFile(fileToReplace, fileToUse));
		return this;
	}

	/**
	 * Provides deployment with Fabric environment. If you have a multinode environment, use .withFabric directly on the root container you want to
	 * create fabric on.
	 *
	 * @return this
	 */
	public Fafram withFabric() {
		return withFabric("");
	}

	/**
	 * Provide deployment with Fabric provision. If you have a multinode environment, use .withFabric directly on the root container you want to
	 * create fabric on.
	 *
	 * @param opts fabric create options
	 * @return this
	 */
	public Fafram withFabric(String opts) {
		SystemProperty.set(FaframConstant.FABRIC, opts);
		return this;
	}

	/**
	 * Suppress the default user add.
	 *
	 * @return this
	 */
	public Fafram withoutDefaultUser() {
		SystemProperty.set(FaframConstant.SKIP_DEFAULT_USER, "");
		return this;
	}

	/**
	 * Patches standalone container. Only useful together with withFabric().
	 *
	 * @return this
	 */
	public Fafram patchStandalone() {
		SystemProperty.set(FaframConstant.PATCH_STANDALONE, "");
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
	public Fafram setMemoryJvmOptions(String xms, String xmx, String permMem, String maxPermMem) {
		final List<String> opts = new ArrayList<>();
		opts.add("JAVA_MIN_MEM=" + xms);
		opts.add("JAVA_MAX_MEM=" + xmx);
		opts.add("JAVA_PERM_MEM=" + permMem);
		opts.add("JAVA_MAX_PERM_MEM=" + maxPermMem);
		ModifierExecutor.addModifiers(setJvmMemOpts(opts));
		SystemProperty.set(FaframConstant.SKIP_DEFAULT_JVM_OPTS, "");
		return this;
	}

	/**
	 * Suppresses the start of the fuse - probably testing purposes only.
	 *
	 * @return this
	 */
	public Fafram suppressStart() {
		SystemProperty.set(FaframConstant.SUPPRESS_START, "");
		return this;
	}

	/**
	 * Archive files pattern setter.
	 *
	 * @param pattern pattern
	 * @return this
	 */
	public Fafram archive(String pattern) {
		SystemProperty.set(FaframConstant.ARCHIVE_PATTERN, pattern);
		return this;
	}

	/**
	 * Keep folder flag setter.
	 *
	 * @return this
	 */
	public Fafram keepFolder() {
		SystemProperty.set(FaframConstant.KEEP_FOLDER, "");
		return this;
	}

	/**
	 * Skips waiting for broker.
	 *
	 * @return this
	 */
	public Fafram skipBrokerWait() {
		SystemProperty.set(FaframConstant.SKIP_BROKER_WAIT, "true");
		return this;
	}

	/**
	 * Set ProvisionProvider implementation to Fafram.
	 *
	 * @param provider Implementation of ProvisionProvider interface
	 * @return this
	 */
	public Fafram provider(FaframProvider provider) {
		switch (provider) {
			case STATIC:
				provisionProvider = new StaticProvider();
				SystemProperty.set(FaframConstant.PROVIDER, FaframProvider.STATIC.toString());
				break;
			case OPENSTACK:
				provisionProvider = OpenStackProvisionProvider.getInstance();
				SystemProperty.set(FaframConstant.PROVIDER, FaframProvider.OPENSTACK.toString());
				break;
			default:
				log.warn("Provider not found! Using default static provider!");
				provisionProvider = new StaticProvider();
				SystemProperty.set(FaframConstant.PROVIDER, FaframProvider.STATIC.toString());
				break;
		}
		return this;
	}

	/**
	 * Defines hostname/IP address for remote running. If you have a multinode environment, use appropriate methods directly on the containers.
	 *
	 * @param host hostname or ip to remote host for running Fuse
	 * @return this
	 */
	public Fafram host(String host) {
		SystemProperty.set(FaframConstant.HOST, host);
		return this;
	}

	/**
	 * Defines host username. If you have a multinode environment, use appropriate methods directly on the containers.
	 *
	 * @param username username for remote host machine
	 * @return this
	 */
	public Fafram hostUser(String username) {
		SystemProperty.set(FaframConstant.HOST_USER, username);
		return this;
	}

	/**
	 * Defines host password. If you have a multinode environment, use appropriate methods directly on the containers.
	 *
	 * @param password password for remote machine
	 * @return this
	 */
	public Fafram hostPassword(String password) {
		SystemProperty.set(FaframConstant.HOST_PASSWORD, password);
		return this;
	}

	/**
	 * URL to zip with fuse.
	 *
	 * @param zip url
	 * @return this
	 */
	public Fafram fuseZip(String zip) {
		SystemProperty.set(FaframConstant.FUSE_ZIP, zip);
		return this;
	}

	/**
	 * Sets the default container name. If you have a multinode environment, use appropriate methods directly on the containers.
	 *
	 * @param name name
	 * @return this
	 */
	public Fafram name(String name) {
		SystemProperty.set(FaframConstant.DEFAULT_ROOT_NAME, name);
		return this;
	}

	/**
	 * Defines path to configuration file.
	 *
	 * @param configPath patch to configuration file
	 * @return this
	 */
	public Fafram config(String configPath) {
		SystemProperty.set(FaframConstant.FABRIC_CONFIG_PATH, configPath);
		return this;
	}

	/**
	 * Turns environment to offline mode. For this purpose the "iptables-no-internet" configuration file is used which
	 * should be located in specified user's home folder on all nodes. This file is loaded into the iptables on all nodes
	 * specified in FaFram.
	 * <p/>
	 * This method can be used only with ecervena snapshots on OpenStack or snapshots that contain mentioned folder.
	 * <p/>
	 * By default the snapshot used for spawning containers using the OpenStack provider contains this file.
	 *
	 * @return this
	 */
	public Fafram offline() {
		SystemProperty.set(FaframConstant.OFFLINE, "true");
		return this;
	}

	/**
	 * Loads up a custom iptables configuration file from local host and uploads it to remote hosts and executes them.
	 * This method sets up the environment to environment defined in provided the configuration file.
	 * <p/>
	 * This option should be only used on OpenStack machines with sudo command configured to be used without password
	 * otherwise it won't work. Also the snapshots should have exchanged ssh key to be able to connect between them
	 * without password.
	 * <p/>
	 * At the moment this method is only experimental and should be used with caution. Cleaning up the iptables after
	 * the test is not completed yet.
	 *
	 * @param localFilePath path to iptables configuration file
	 * @return this
	 */
	public Fafram loadIPtablesConfigurationFile(String localFilePath) {
		SystemProperty.set(FaframConstant.IPTABLES_CONF_FILE_PATH, localFilePath);
		return this;
	}

	/**
	 * Adds bundle into list of bundles which should be uploaded into fabric maven proxy on the end of initialization.
	 * This works only with defaultRoot() initialization. If working with different infrastructure of fabric then use
	 * RootContainer.bundles() method for specifying exact container where bundles should be uploaded.
	 *
	 * @param bundles list of bundles
	 * @return this
	 */
	public Fafram bundles(String... bundles) {
		ContainerManager.getBundles().addAll(Arrays.asList(bundles));
		return this;
	}

	/**
	 * Adds bundle into list of bundles which should be built with specified goals.
	 *
	 * @param projectPath absolute or relative (to root project pom) path of target project
	 * @param properties properties for maven execution
	 * @param goals list of goals that should be executed
	 * @return this
	 */
	public Fafram buildBundle(String projectPath, Map<String, String> properties, String... goals) {
		bundlesToBuild.add(new MavenProject(projectPath, properties, Arrays.asList(goals)));
		return this;
	}

	/**
	 * Adds bundle into list of bundles which should be built with specified goals.
	 *
	 * @param projectPath absolute or relative (to root project pom) path of target project
	 * @param goals list of goals that should be executed
	 * @return this
	 */
	public Fafram buildBundle(String projectPath, String... goals) {
		buildBundle(projectPath, null, goals);
		return this;
	}

	/**
	 * Adds bundles into list of bundles which should be built with specified goals.
	 *
	 * @param projects list of projects for execution
	 * @return this
	 */
	public Fafram buildBundles(MavenProject... projects) {
		bundlesToBuild.addAll(Arrays.asList(projects));

		return this;
	}

	/**
	 * Adds command into list of commands which should be executed right after fabric create / at the end of initialization.
	 * If you have a multinode environment, use appropriate methods directly on the containers.
	 *
	 * @param commands list of commands
	 * @return this
	 */
	public Fafram commands(String... commands) {
		ContainerManager.getCommands().addAll(Arrays.asList(commands));
		return this;
	}

	/**
	 * Sets path to java directory with predefined path on Openstack machine. If you have a multinode environment, use appropriate methods directly on the containers.
	 *
	 * @param openstack enum Openstack defining path to different jdks
	 * @return this
	 */
	public Fafram jdk(Openstack openstack) {
		jdk(openstack.getPath());
		return this;
	}

	/**
	 * Sets path to java directory that will be used by all containers. If you have a multinode environment, use appropriate methods directly on the containers.
	 *
	 * @param javaHomePath file path to java home
	 * @return this
	 */
	public Fafram jdk(String javaHomePath) {
		SystemProperty.set(FaframConstant.JAVA_HOME, javaHomePath);

		return this;
	}

	/**
	 * Defines the ensemble members.
	 *
	 * @param containers list of container names
	 * @return this
	 */
	public Fafram ensemble(String... containers) {
		ContainerManager.getEnsembleList().addAll(Arrays.asList(containers));
		if (running) {
			ContainerManager.createEnsemble();
		}
		return this;
	}

	/**
	 * Defines the ensemble members.
	 *
	 * @param containers list of containers.
	 * @return this
	 */
	public Fafram ensemble(Container... containers) {
		for (Container c : containers) {
			ContainerManager.getEnsembleList().add(c.getName());
		}
		if (running) {
			ContainerManager.createEnsemble();
		}
		return this;
	}

	/**
	 * -------------------------------------------- FLUENT METHODS END --------------------------------------------
	 */

	/**
	 * Returns the first root container. This root is later used in Fafram.executeCommand(), .waitForPatch, etc.
	 *
	 * @return root container
	 */
	private Container getRoot() {
		return ContainerManager.getRoot();
	}

	/**
	 * Configuration init.
	 */
	public void initConfiguration() {
		if (SystemProperty.getFabricConfigPath() != null) {
			this.configurationParser = new ConfigurationParser();

			try {
				configurationParser.parseConfigurationFile(SystemProperty.getFabricConfigPath());
			} catch (Exception e) {
				e.printStackTrace();
				throw new FaframException("XML configuration parsing error.", e);
			}

			try {
				configurationParser.buildContainers();
			} catch (Exception e) {
				throw new FaframException("Error while building containers from parsed model.", e);
			}
		}
	}

	/**
	 * Prints the logo.
	 */
	private void printLogo() {
		log.info("\n  ___       ___                  _____  \n"
				+ " / __)     / __)                / ___ \\ \n"
				+ "| |__ ____| |__ ____ ____ ____ ( (   ) )\n"
				+ "|  __) _  |  __) ___) _  |    \\ > > < < \n"
				+ "| | ( ( | | | | |  ( ( | | | | ( (___) )\n"
				+ "|_|  \\_||_|_| |_|   \\_||_|_|_|_|\\_____/ \n\n");
	}

	/**
	 * Sets the default modifiers common for both local and remote deploy.
	 */
	private void setDefaultModifiers() {
		if (!SystemProperty.skipDefaultJvmOpts()) {
			ModifierExecutor.addModifiers(setDefaultJvmMemOpts());
		}
		ModifierExecutor.addPostModifiers(registerArchiver());
	}

	/**
	 * Executes a command in node shell.
	 *
	 * @param command command
	 * @return command response
	 */
	public String executeNodeCommand(String command) {
		return rootContainer.getNode().getExecutor().executeCommand(command);
	}

	/**
	 * Executes multiple commands in node shell.
	 *
	 * @param commands commands array to execute
	 * @return list of commands responses
	 */
	public List<String> executeNodeCommands(String... commands) {
		return rootContainer.getNode().getExecutor().executeCommands(commands);
	}

	/**
	 * Executes a command in root container.
	 *
	 * @param command command to execute on root container
	 * @return command response
	 */
	public String executeCommand(String command) {
		return rootContainer.executeCommand(command);
	}

	/**
	 * Executes multiple commands in root container.
	 *
	 * @param commands commands array to execute
	 * @return list of commands responses
	 */
	public List<String> executeCommands(String... commands) {
		return rootContainer.executeCommands(commands);
	}

	/**
	 * Gets the full product path. Functional only for Fafram with one root container.
	 *
	 * @return product path
	 */
	public String getProductPath() {
		return getRoot().getFusePath();
	}

	/**
	 * Get product version (from {@link Fafram#getProductPath()}).
	 *
	 * @return product version, eg {@code 6.2.1.redhat-084} or empty string if regex doesn't match.
	 */
	public String getProductVersion() {
		final Pattern p = Pattern.compile(
				// jboss-fuse-6.3.0.redhat-045 || jboss-a-mq-6.2.1.redhat-084
				".*jboss-(?<id>.+)-(?<version>\\d\\.\\d\\.\\d\\.redhat-\\d{2,4}).*", Pattern.CASE_INSENSITIVE
		);

		final Matcher m = p.matcher(getProductPath());

		if (m.matches()) {
			log.debug("{} version detected '{}'", m.group("id"), m.group("version"));
			return m.group("version");
		}
		log.debug("Couldn't get version from path '{}'.", getProductPath());
		return "";
	}

	/**
	 * Waits for the container to provision.
	 *
	 * @param containerName container name
	 */
	public void waitForProvisioning(String containerName) {
		rootContainer.getExecutor().waitForProvisioning(containerName);
	}

	/**
	 * Waits until the defined standalone patch status.
	 *
	 * @param patchName patch name
	 * @param status patch status (true/false)
	 */
	public void waitForPatch(String patchName, boolean status) {
		rootContainer.getExecutor().waitForPatchStatus(patchName, status);
	}

	/**
	 * Restarts the container.
	 */
	public void restart() {
		rootContainer.restart();
	}

	/**
	 * Triggers specified provision provider. It will call providers functionality to provision pool of nodes required
	 * to satisfy needs of test deployment.
	 * When the container is marked as root provider will assign public IP to it. Otherwise local IP will be provided.
	 * Provider have to implement ProvisionProvider interface.
	 *
	 * @param containerList container list
	 */
	public void prepareNodes(List<Container> containerList) {
		// Create a temp list without child containers
		final List<Container> temp = new ArrayList<>();
		for (Container c : containerList) {
			if (!(c instanceof ChildContainer) && (c.getNode() == null || c.getNode().getHost() == null || "".equals(c.getNode().getHost()))) {
				temp.add(c);
			}
		}

		// Do not allow provisioning on empty container list
		if (temp.isEmpty()) {
			return;
		}
		// Check if there are nodes with the defined names
		provisionProvider.checkNodes(temp);
		provisionProvider.createServerPool(temp);
		provisionProvider.assignAddresses(temp);
		provisionProvider.loadIPTables(temp);
	}

	/**
	 * Return container according specified container name.
	 *
	 * @param containerName name of container
	 * @return Container object or null when not found.
	 */
	public Container getContainer(String containerName) {
		for (Container c : ContainerManager.getContainerList()) {
			if (c.getName().equals(containerName)) {
				return c;
			}
		}
		return null;
	}

	/**
	 * Deletes the container.
	 *
	 * @param containers containers array
	 */
	public void deleteContainers(Container... containers) {
		for (Container c : containers) {
			for (Container cl : ContainerManager.getContainerList()) {
				if (c.equals(cl)) {
					c.destroy();
					ContainerManager.getContainerList().remove(c);
					break;
				}
			}
		}
	}

	/**
	 * Gets the container list.
	 *
	 * @return container list
	 */
	public List<Container> getContainerList() {
		return ContainerManager.getContainerList();
	}

	/**
	 * Gets the broker list.
	 *
	 * @return broker list
	 */
	public List<Broker> getBrokerList() {
		return ContainerManager.getBrokers();
	}

	/**
	 * Builds defined bundles with specific maven goals on local host.
	 */
	public void buildBundles() {
		for (MavenProject project : bundlesToBuild) {
			log.debug("Invoking maven project: {}", project);
			try {
				MavenPomInvoker.buildMvnProject(project);
			} catch (URISyntaxException | MavenInvocationException e) {
				log.error("Invocation of maven target \"{}\" failed.", project);
				throw new FaframException("Invocation of maven target \"" + project + "\" failed.", e);
			}
		}
	}

	/**
	 * Utility method for waiting on custom condition.
	 * Check {@link org.jboss.fuse.qa.fafram8.util.callables.Response} and other classes
	 * in this package.
	 *
	 * @param methodBlock callable which is executed every 3 seconds until it returns success
	 * @param secondsTimeout repeat callable until it's success or timeout (in seconds)
	 * @param <T> type of expected data response
	 * @return {@link Response} wrapper with boolean success/fail and nullable data response
	 */
	public <T> Response<T> waitFor(Callable<Response<T>> methodBlock, long secondsTimeout) {
		return rootContainer.getExecutor().waitFor(methodBlock, secondsTimeout);
	}
}
