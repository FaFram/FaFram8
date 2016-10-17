package org.jboss.fuse.qa.fafram8.provision.provider;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.node.Node;
import org.jboss.fuse.qa.fafram8.ec2.provision.Ec2Client;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.exception.InstanceAlreadyExistsException;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import org.jclouds.compute.domain.NodeMetadata;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Class is used for EC2 node operations using Ec2Client.
 *
 * Sep 16, 2016 Red Hat
 *
 * @author tplevko@redhat.com
 */
@Slf4j
public class Ec2ProvisionProvider implements ProvisionProvider {

	private static Ec2ProvisionProvider provider = null;

	// Ec2Client instance
	@Getter
	private Ec2Client client = null;

	/**
	 * Constructor.
	 */
	public Ec2ProvisionProvider() {
		if (client == null) {
			client = Ec2Client.builder()
					.url(SystemProperty.getExternalProperty(FaframConstant.EC2_URL))
					.accessKeyID(SystemProperty.getExternalProperty(FaframConstant.EC2_ACCESS_KEY_ID))
					.secretKey(SystemProperty.getExternalProperty(FaframConstant.EC2_SECRET_KEY))
					.imageId(SystemProperty.getExternalProperty(FaframConstant.EC2_IMAGE_ID))
					.region(SystemProperty.getExternalProperty(FaframConstant.EC2_REGION))
					.instanceType(SystemProperty.getExternalProperty(FaframConstant.EC2_INSTANCE_TYPE))
					.keyPair(SystemProperty.getExternalProperty(FaframConstant.EC2_KEY_PAIR))
					.securityGroups(SystemProperty.getExternalProperty(FaframConstant.EC2_SECURITY_GROUPS))
					.namePrefix(SystemProperty.getExternalProperty(FaframConstant.EC2_NAME_PREFIX))
					.build();
		}
	}

	/**
	 * Singleton access method.
	 *
	 * @return OpenStackProvisionProvider instance
	 */
	public static Ec2ProvisionProvider getInstance() {
		if (provider == null) {
			provider = new Ec2ProvisionProvider();
		}

		return provider;
	}

	@Override
	public void createServerPool(List<Container> containerList) {
		final List<String> containerNames = new ArrayList<>();
		for (Container container : containerList) {
			containerNames.add(container.getName());
		}

		log.info("Spawning Ec2 infrastructure.");
		try {
			client.spawnServersByNames(containerNames);

		} catch (ExecutionException | InterruptedException e) {
			throw new FaframException("Cannot create Ec2 infrastructure.", e);
		}
	}

	/**
	 * The public IP address is set to be assigned by default, this is there therefore, so the compatibility is not
	 * broken.
	 *
	 * @param containerList
	 */
	@Override
	public void assignAddresses(List<Container> containerList) {

		if (containerList.isEmpty()) {
			throw new RuntimeException("Container list is empty!");
		}
		for (Container container : containerList) {
			final NodeMetadata server
					= client.getServerFromRegister(client.getNamePrefix() + "-" + container.getName());
			if (container.getNode() == null) {
				container.setNode(Node.builder().port(SystemProperty.getHostPort()).username(SystemProperty.getHostUser())
						.password(SystemProperty.getHostPassword()).build());
			}
			container.getNode().setNodeId(server.getId());

			final String ip = Iterables.getFirst(server.getPublicAddresses(), null);

			log.info("Assigning public IP: " + ip + " for container: " + container.getName() + " on machine: " + server.getName());
			container.getNode().setHost(ip);
			container.getNode().setExecutor(container.getNode().createExecutor());
		}
	}

	@Override
	public void releaseResources() {
		if (SystemProperty.isKeepOsResources()) {
			log.warn("Keeping Ec2 resources. Don't forget to release them later!");
			return;
		}
		client.releaseResources();
	}

	@Override
	public void loadIPTables(List<Container> containerList) {
		log.warn("Not implemented yet.");
		// This is not required at this moment
	}

	@Override
	public void cleanIpTables(List<Container> containerList) {
		log.warn("Not implemented yet.");
		// This is not required at this moment
	}

	@Override
	public void checkNodes(List<Container> containerList) {
		for (Container c : containerList) {
			if (client.getServers(SystemProperty.getExternalProperty("ec2.namePrefix") + "-" + c.getName()).size() != 0) {
				throw new InstanceAlreadyExistsException(
						"Instance " + SystemProperty.getOpenstackServerNamePrefix() + "-" + c.getName() + " already exists!");
			}
		}
	}
}
