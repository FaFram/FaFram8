package org.jboss.fuse.qa.fafram8.provision.provider;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.cluster.node.Node;
import org.jboss.fuse.qa.fafram8.exception.EmptyContainerListException;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.exception.InstanceAlreadyExistsException;
import org.jboss.fuse.qa.fafram8.exception.OfflineEnvironmentException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.openstack.provision.OpenStackClient;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.ssh.NodeSSHClient;

import org.openstack4j.model.compute.Server;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenStackProvisionProvider class used for calling all OpenStack node operations. Using authenticated OpenStackClient singleton.
 * <p/>
 * Created by ecervena on 24.9.15.
 * TODO(ecervena): this should be probably singleton
 */
@Slf4j
public class OpenStackProvisionProvider implements ProvisionProvider {
	// static property holding created OpenStackProvisionProvider singleton
	private static OpenStackProvisionProvider provider = null;

	// Authenticated OpenStackClient instance
	@Getter
	private OpenStackClient client = null;

	private static final String OFFLINE_IPTABLES_FILE = "iptables-no-internet";

	private String ipTablesFilePath;

	/**
	 * Constructor.
	 */
	public OpenStackProvisionProvider() {
		if (client == null) {
			if (SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_NAME_PREFIX) == null) {
				SystemProperty.set(FaframConstant.OPENSTACK_NAME_PREFIX, "fafram8." + new Date().getTime());
			}

			client = OpenStackClient.builder()
					.url(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_URL))
					.tenant(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_TENANT))
					.user(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_USER))
					.password(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_PASSWORD))
					.image(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_IMAGE))
					.flavor(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_FLAVOR))
					.keypair(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_KEYPAIR))
					.networks(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_NETWORKS))
					.addressType(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_ADDRESS_TYPE))
					.floatingIpPool(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_FLOATING_IP_POOL))
					.namePrefix(SystemProperty.getOpenstackServerNamePrefix())
					.build();
		}
	}

	/**
	 * Singleton access method.
	 *
	 * @return OpenStackProvisionProvider instance
	 */
	public static OpenStackProvisionProvider getInstance() {
		if (provider == null) {
			provider = new OpenStackProvisionProvider();
		}

		return provider;
	}

	/**
	 * This method will use ConfigurationParser singleton to parse XML representation of OpenStack infrastructure
	 * and spawn thread for each container to create its OpenStack node. Each thread will create one OpenStack node,
	 * assign its serverId to container object model and register new server to OpenStackProvisionProvider's ServerList.
	 * IP addresses are assigned to containers later. Root container will get floating public IP and register it.
	 * Others will get only local IP.
	 *
	 * @param containerList list of containers
	 */
	@Override
	public void createServerPool(List<Container> containerList) {
		final List<String> containerNames = new ArrayList<>();
		for (Container container : containerList) {
			containerNames.add(container.getName());
		}
		int instancesCount = 0;
		for (Container c : containerList) {
			instancesCount += ((c instanceof RootContainer || c instanceof SshContainer) ? 1 : 0);
		}
		client.waitForResources(instancesCount);
		log.info("Spawning OpenStack infrastructure.");
		try {
			client.spawnServersByNames(containerNames);
		} catch (ExecutionException | InterruptedException e) {
			throw new FaframException("Cannot create OpenStack infrastructure.", e);
		}
	}

	/**
	 * Assign IP addresses to list of containers. Container marked as root will get public IP.
	 * Check that all servers for containers are online and SSH server is online.
	 *
	 * @param containerList list of containers to assign addresses
	 */
	@Override
	public void assignAddresses(List<Container> containerList) {
		if (containerList.isEmpty()) {
			throw new EmptyContainerListException("Container list is empty!");
		}
		for (Container container : containerList) {
			// No longer parse System properties because they could be cleared. Work only with properties set on client
			final Server server =
					client.getServerFromRegister(client.getNamePrefix() + "-" + container.getName());
			if (container.getNode() == null) {
				// We dont have any info, use the defaults
				container.setNode(Node.builder().port(SystemProperty.getHostPort()).username(SystemProperty.getHostUser())
						.password(SystemProperty.getHostPassword()).build());
			}
			container.getNode().setNodeId(server.getId());

			final String ip = client.assignFloatingAddress(server.getId());
			log.info("Assigning public IP: " + ip + " for container: " + container.getName() + " on machine: " + server.getName());
			container.getNode().setHost(ip);
			container.getNode().setExecutor(container.getNode().createExecutor());
		}

//		for (Container container : containerList) {
			// Iterate over all container and try to connect to them (exclude child containers)
//			if (!(container instanceof ChildContainer)) {
//				container.getNode().getExecutor().connect();
//			}
//		}
		//TODO(avano): delete this? ^
	}

	/**
	 * Release allocated OpenStack resources. Method will delete created servers and release allocated floating IPs.
	 */
	@Override
	public void releaseResources() {
		if (SystemProperty.isKeepOsResources()) {
			log.warn("Keeping OpenStack resources. Don't forget to release them later!");
			return;
		}
		client.releaseResources();
	}

	@Override
	public void loadIPTables(List<Container> containerList) {
		// "If" for deciding if this method should be used is moved here so the Fafram method is clean(Only for you ecervena <3)
		if (SystemProperty.getIptablesConfFilePath().isEmpty() && !SystemProperty.isOffline()) {
			// There was no iptables configuration file set so the user doesn't want to change environment.
			return;
		}

		log.info("Loading iptables configuration files.");

		// For each container in container list execute and set a correct iptables configuration file
		for (Container c : containerList) {
			if (c instanceof ChildContainer) {
				// If the child container is child then skip. The file will be copied and executed for all ssh containers
				// and root. It doesn't make sense to do also for child containers.
				continue;
			}
			executeIpTables(c);
		}

		log.info("IPTables configuration files successfully loaded on all nodes! Environment configuration according to {} file.",
				this.ipTablesFilePath);
	}

	@Override
	public void cleanIpTables(List<Container> containerList) {
		// Do nothing
	}

	@Override
	public void checkNodes(List<Container> containerList) {
		for (Container c : containerList) {
			if (client.getServers(SystemProperty.getOpenstackServerNamePrefix() + "-" + c.getName()).size() != 0) {
				throw new InstanceAlreadyExistsException(
						"Instance " + SystemProperty.getOpenstackServerNamePrefix() + "-" + c.getName() + " already exists!");
			}
		}
	}

	/**
	 * Sets correct path and name to iptables configuration file depending on the type of environment.
	 *
	 * @param executor connected executor to root node
	 */
	private void setCorrectIpTablesFilePath(Executor executor) {
		// This is special case when you want to use default offline configuration.
		if (SystemProperty.isOffline()) {
			// setting path to default iptables configuration file in home folder of user
			this.ipTablesFilePath = OFFLINE_IPTABLES_FILE;
		} else {
			// Otherwise you want to copy iptables configuration file from local machine to remote node -> create path to file on remote node
			final String directory =
					("".equals(SystemProperty.getWorkingDirectory())) ? executor.executeCommand("pwd") : SystemProperty.getWorkingDirectory();

			// Path to copied iptables file on remote nodes
			this.ipTablesFilePath =
					directory + File.separator + StringUtils.substringAfterLast(SystemProperty.getIptablesConfFilePath(), File.separator);
		}
		log.info("Setting iptables configuration in environment to {}.", this.ipTablesFilePath);
	}

	/**
	 * Executes command line commands on node to successfully load and configure iptables on provided container. If the
	 * custom iptables configuration file is used then this methods also copies cofniguration file to specified node of
	 * container.
	 *
	 * @param container container on which the iptables should be configured
	 */
	private void executeIpTables(Container container) {
		log.trace("Connnecting node executor to set the IPTables");
		container.getNode().getExecutor().connect();
		setCorrectIpTablesFilePath(container.getNode().getExecutor());
		try {
			// if offline environment then skip this. The iptables configuration should be present in the image itself.
			if (!SystemProperty.isOffline()) {
				((NodeSSHClient) container.getNode().getExecutor().getClient()).copyFileToRemote(SystemProperty.getIptablesConfFilePath(), this
						.ipTablesFilePath);
			}

			log.debug("Executing iptables configuration file on node: " + container.getNode().getExecutor().toString());

			final String response = container.getNode().getExecutor().executeCommandSilently("stat " + this.ipTablesFilePath);

			if (response == null || response.isEmpty()) {
				throw new OfflineEnvironmentException(
						"Configuration file for iptables" + " doesn't exists on node: " + container.getNode().getHost() + ".",
						new FileNotFoundException("File " + this.ipTablesFilePath + " doesn't exists."));
			}
			container.getNode().getExecutor().executeCommand("sudo iptables-restore " + this.ipTablesFilePath);
			log.debug("Iptables successfully configured on node {}.", container.getNode().getExecutor());
		} catch (Exception e) {
			throw new FaframException("There was problem setting iptables on node: " + container.getNode().getHost(), e);
		}
	}
}
