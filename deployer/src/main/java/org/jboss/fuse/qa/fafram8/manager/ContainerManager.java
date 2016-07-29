package org.jboss.fuse.qa.fafram8.manager;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;

import org.jboss.fuse.qa.fafram8.cluster.broker.Broker;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.exception.BundleUploadException;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.invoker.MavenPomInvoker;
import org.jboss.fuse.qa.fafram8.patcher.Patcher;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.util.Option;
import org.jboss.fuse.qa.fafram8.util.OptionUtils;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Container manager class. This class is responsible for all actions related to containers - setting up fabric,
 * patching, etc.
 * <p/>
 * Created by avano on 2.9.15.
 */
@Slf4j
public class ContainerManager {
	// Singleton instance
	private static ContainerManager instance = null;

	// List of all containers
	private static List<Container> containerList = null;

	// List of bundles that will be installed into the _default_ root container only.
	private static List<String> bundles = null;

	// List of commands that will be executed on the _default_ root container only.
	private static List<String> commands = null;

	//List of all brokers in cluster
	private static List<Broker> brokers = null;

	private static List<String> ensembleList = null;

	/**
	 * Constructor.
	 */
	protected ContainerManager() {
	}

	/**
	 * Gets the singleton instance.
	 *
	 * @return singleton instance
	 */
	public static ContainerManager getInstance() {
		if (instance == null) {
			instance = new ContainerManager();
			containerList = new ArrayList<>();
			bundles = new ArrayList<>();
			commands = new ArrayList<>();
			brokers = new ArrayList<>();
			ensembleList = new ArrayList<>();
		}

		return instance;
	}

	/**
	 * Gets the bundles list.
	 *
	 * @return bundles list
	 */
	public static List<String> getBundles() {
		// Force the initialization
		getInstance();
		return bundles;
	}

	/**
	 * Gets the command list.
	 *
	 * @return command list
	 */
	public static List<String> getCommands() {
		// Force the initialization
		getInstance();
		return commands;
	}

	/**
	 * Gets the container list.
	 *
	 * @return container list
	 */
	public static List<Container> getContainerList() {
		// Force the initialization
		getInstance();
		return containerList;
	}

	/**
	 * Gets the ensemble list.
	 *
	 * @return list of container names
	 */
	public static List<String> getEnsembleList() {
		// Force the initialization
		getInstance();
		return ensembleList;
	}

	/**
	 * Gets the container by its name.
	 *
	 * @param name container name
	 * @return container instance
	 */
	public static Container getContainer(String name) {
		for (Container c : containerList) {
			if (name.equals(c.getName())) {
				return c;
			}
		}
		return null;
	}

	/**
	 * Returns the first root container.
	 *
	 * @return root container
	 */
	public static Container getRoot() {
		for (Container c : ContainerManager.getContainerList()) {
			if (c.isRoot()) {
				return c;
			}
		}
		// This should never happen
		throw new FaframException("Root not found in container list!");
	}

	/**
	 * Get list the broker list.
	 *
	 * @return list of all brokers
	 */
	public static List<Broker> getBrokers() {
		//Force the initialization
		getInstance();
		return brokers;
	}

	/**
	 * Clears all the lists in this class - containerList, bundles, commands.
	 */
	public static void clearAllLists() {
		// Force the initialization
		getInstance();
		for (int i = containerList.size() - 1; i >= 0; i--) {
			containerList.remove(i);
		}
		for (int i = bundles.size() - 1; i >= 0; i--) {
			bundles.remove(i);
		}
		for (int i = commands.size() - 1; i >= 0; i--) {
			commands.remove(i);
		}
		for (int i = brokers.size() - 1; i >= 0; i--) {
			brokers.remove(i);
		}
		for (int i = ensembleList.size() - 1; i >= 0; i--) {
			ensembleList.remove(i);
		}

		log.debug("Container manager lists cleared");
	}

	/**
	 * Sets up fabric on specified container.
	 *
	 * @param c container
	 */
	public static void setupFabric(Container c) {
		if (!c.isFabric()) {
			return;
		}

		// Construct the fabric create arguments from fabric property and profiles
		String fabricString = OptionUtils.getString(c.getOptions(), Option.FABRIC_CREATE);

		for (String profile : OptionUtils.get(c.getOptions(), Option.PROFILE)) {
			fabricString += " --profile " + profile;
		}

		c.executeCommand("fabric:create" + (fabricString.startsWith(" ") ? StringUtils.EMPTY : " ") + fabricString);

		try {
			c.getExecutor().waitForProvisioning(c);
		} catch (FaframException ex) {
			// Container is not provisioned in time
			throw new FaframException("Container " + c.getName() + " did not provision in time");
		}
		// ENTESB-5110: Reconnect the client after fabric:create
		c.getExecutor().reconnect();
		uploadBundles(c);
	}

	/**
	 * Executes defined commands right after fabric-create.
	 *
	 * @param c container to execute on
	 */
	public static void executeStartupCommands(Container c) {
		for (String command : OptionUtils.get(c.getOptions(), Option.COMMANDS)) {
			c.executeCommand(command);
		}

		// Execute additional commands provided by system property
		for (String command : SystemProperty.getAdditionalCommands().split(";")) {
			if (!command.isEmpty()) {
				c.executeCommand(command);
			}
		}

		if (c.isFabric()) {
			c.waitForProvisioning();
		}
	}

	/**
	 * Uploads bundles to fabric maven proxy on root container (remote).
	 *
	 * @param c container to execute on
	 */
	public static void uploadBundles(Container c) {
		if (c.getOptions().get(Option.BUNDLES) != null) {
			for (String bundle : OptionUtils.get(c.getOptions(), Option.BUNDLES)) {
				uploadBundle(c, bundle);
			}
		}
	}

	/**
	 * Uploads bundle to fabric maven proxy on container (remote). The container should be root with fabric and its own maven upload proxy.
	 *
	 * @param container container to which bundle should be uploaded
	 * @param projectPath path to pom.xml of the project that should be uploaded to root container
	 */
	public static void uploadBundle(Container container, String projectPath) {
		final String mavenProxy = StringUtils.substringAfter(StringUtils.substringAfter(container.getExecutor().executeCommandSilently("fabric:info | grep upload"), ":"), "://").trim();

		final MavenPomInvoker bundleInstaller = new MavenPomInvoker(projectPath, "http://" + container.getUser() + ":" + container.getPassword() + "@" + mavenProxy.replaceAll("(.+)(?=:8181)", container.getNode().getHost()));
		try {
			bundleInstaller.installFile();
		} catch (URISyntaxException | MavenInvocationException e) {
			throw new BundleUploadException(e);
		}
	}

	/**
	 * Patches fuse based on its mode (standalone / fabric).
	 *
	 * @param c Container instance
	 */
	public static void patchFuse(Container c) {
		if (SystemProperty.getPatch() != null) {
			if (SystemProperty.isFabric()) {
				patchFabric(c);
			} else {
				patchStandalone(c);
			}
		}
	}

	/**
	 * Patches the standalone before fabric creation.
	 *
	 * @param c container to execute on
	 */
	public static void patchStandaloneBeforeFabric(Container c) {
		if (SystemProperty.patchStandalone()) {
			patchStandalone(c);
		}
	}

	/**
	 * Configures root nodes - if using StaticProvider, add the node built from system properties.
	 */
	public static void configureRoots() {
		createRootIfNecessary();
		// Set the bundles and commands to the first root found
		for (Container c : containerList) {
			if (c instanceof RootContainer) {
				OptionUtils.get(c.getOptions(), Option.COMMANDS).addAll(ContainerManager.getCommands());
				OptionUtils.get(c.getOptions(), Option.BUNDLES).addAll(ContainerManager.getBundles());
				break;
			}
		}
		if (!SystemProperty.getProvider().equals(FaframProvider.STATIC)) {
			return;
		}
		for (Container c : containerList) {
			// If the host is not set in case of static deployment, we use localhost
			if (c instanceof RootContainer && c.getNode().getHost() == null) {
				c.getNode().setHost(SystemProperty.getHost());
			}
		}
	}

	/**
	 * If the container list is empty, it adds the default root built from system properties.
	 */
	private static void createRootIfNecessary() {
		if (ContainerManager.getContainerList().isEmpty()) {
			final Container c = RootContainer.builder().defaultRoot().build();
			log.info("Creating default root container");
			ContainerManager.getContainerList().add(c);
		}
	}

	/**
	 * Patches the standalone container.
	 *
	 * @param c container to execute on
	 */
	private static void patchStandalone(Container c) {
		for (String s : Patcher.getPatches()) {
			final String patchName = getPatchName(c.executeCommand("patch:add " + s));
			c.executeCommand("patch:install " + patchName);
			c.getExecutor().waitForPatchStatus(patchName, true);
		}
	}

	/**
	 * Patches fabric root and sets the default version to the patched version.
	 *
	 * @param c Container instance
	 */
	private static void patchFabric(Container c) {
		// Create a new version
		final String version = c.executeCommand("version-create").split(" ")[2];

		// We need to check if the are using old or new patching mechanism
		if (StringUtils.containsAny(SystemProperty.getFuseVersion(), "6.1", "6.2.redhat")) {
			for (String s : Patcher.getPatches()) {
				c.executeCommand("patch-apply -u " + SystemProperty.getFuseUser() + " -p " + SystemProperty.getFusePassword() + " --version " + version + " " + s);
			}
		} else {
			// 6.2.1 onwards
			for (String s : Patcher.getPatches()) {
				final String patchName = getPatchName(c.executeCommand("patch:add " + s));
				c.executeCommand("patch:fabric-install -u " + SystemProperty.getFuseUser() + " -p " + SystemProperty.getFusePassword() + " --upload --version " + version + " " + patchName);
			}
		}

		c.executeCommand("container-upgrade " + version + " " + c.getName());
		c.getExecutor().waitForProvisioning(c);
		c.executeCommand("version-set-default " + version);
	}

	/**
	 * Gets the patch name from the patch-add response.
	 *
	 * @param patchAddResponse patch-add command response.
	 * @return patch name
	 */
	private static String getPatchName(String patchAddResponse) {
		// Get the 2nd row only
		String response = StringUtils.substringAfter(patchAddResponse, System.lineSeparator());
		// Replace multiple whitespaces
		response = response.replaceAll(" +", " ").trim();

		// Get the first string in this line
		final String patchName = response.split(" ")[0];
		log.debug("Patch name is " + patchName);
		return patchName;
	}

	/**
	 * Inits all of the brokers.
	 * All needed commands are put into commands and particular broker-profiles are assigned.
	 */
	public static void initBrokers() {
		if (brokers.isEmpty()) {
			return;
		}
		//Find the root
		final Container root = getRoot();

		for (Broker b : brokers) {
			//add all necessary command into list
			OptionUtils.get(root.getOptions(), Option.COMMANDS).addAll(b.getCreateCommands());
			// assign profiles to all commands
			for (String containerName : b.getContainers()) {
				final Container c = getContainer(containerName);
				if (c == null) {
					throw new FaframException("Container " + containerName + " not found!");
				}
				OptionUtils.get(c.getOptions(), Option.PROFILE).add(b.getProfileName());
			}
		}
	}

	/**
	 * Inits all of the brokers passed as parameter. This method is used only if Fafram is running.
	 * All needed commands are put into commands and particular broker-profiles are assigned.
	 *
	 * @param brokers list of new brokers which will be initialised
	 */
	public static void initBrokers(Broker... brokers) {
		final Container root = getRoot();

		for (Broker b : brokers) {
			if (b == null) {
				continue;
			}
			b.setAssignContainer(true);
			//execute all necessery commands
			final List<String> createCommands = b.getCreateCommands();
			root.executeCommands(createCommands.toArray(new String[createCommands.size()]));
			// assign profiles to all commands
			for (String containerName : b.getContainers()) {
				final Container c = getContainer(containerName);
				if (c == null) {
					throw new FaframException("Container " + containerName + " not found!");
				}
				OptionUtils.get(c.getOptions(), Option.PROFILE).add(b.getProfileName());
				//wait for provision
				c.waitForProvisioning();
				c.getExecutor().reconnect();
			}
		}
	}

	/**
	 * Creates an ensemble - it gets the first root container from the ensemble list and executes ensemble-add command on it.
	 */
	public static void createEnsemble() {
		if (ensembleList.isEmpty()) {
			return;
		}
		Container ensembleRoot = null;
		final StringBuilder ensembleString = new StringBuilder("");
		for (String s : ensembleList) {
			final Container c = getContainer(s);
			if (c == null) {
				throw new FaframException("Container " + s + " not found in container list");
			}
			if (c.isRoot() && ensembleRoot == null) {
				ensembleRoot = c;
			} else {
				ensembleString.append(s).append(" ");
			}
		}
		if (ensembleRoot == null) {
			throw new FaframException("No root container found in the ensemble list!");
		}
		// Maybe this will solve the insufficient roles that happen sometimes
		ensembleRoot.getExecutor().reconnect();
		ensembleRoot.executeCommand("ensemble-add --force " + ensembleString.toString());
	}

	/**
	 * Gets the containers by the name substring.
	 * @param containerFilter container substring to search
	 * @return array of maching containers
	 */
	public static String[] getContainersBySubstring(String containerFilter) {
		final List<String> list = new ArrayList<>();
		for (Container container : ContainerManager.getContainerList()) {
			if (container.getName().contains(containerFilter)) {
				list.add(container.getName());
			}
		}
		if (list.isEmpty()) {
			throw new FaframException("No containers matching filter " + containerFilter);
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Helping method for finding all child containers of given container.
	 *
	 * @param container container for finding its children
	 * @return set of child containers for given container
	 */
	public static Set<Container> getChildContainers(Container container) {
		final Set<Container> containers = new HashSet<>();
		for (Container c : ContainerManager.getContainerList()) {
			if (!(c instanceof RootContainer)) {
				if (c.getParent().getName().equals(container.getName())) {
					containers.add(c);
				}
			}
		}

		return containers;
	}

	/**
	 * Gets RootContainer with given host (Fafram8 doesn't support 2 root containers on the same node).
	 *
	 * @param host host
	 * @return root container with given host
	 */
	public static Container getRootContainerByHost(String host) {
		for (Container container : ContainerManager.getContainerList()) {
			if (container instanceof RootContainer) {
				if (host.equals(container.getNode().getHost())) {
					return container;
				}
			}
		}
		throw new FaframException("Container with given host doesn't exist!");
	}
}

