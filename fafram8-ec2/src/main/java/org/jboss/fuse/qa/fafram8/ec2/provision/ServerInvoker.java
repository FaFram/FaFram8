package org.jboss.fuse.qa.fafram8.ec2.provision;

import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.compute.AWSEC2TemplateOptions;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;

import com.google.common.collect.Iterables;

import java.util.concurrent.Callable;

import lombok.extern.slf4j.Slf4j;

/**
 * Server invoker class. Creates separate thread for every spawned server.
 *
 * Sep 17, 2016 Red Hat
 *
 * @author tplevko@redhat.com
 */
@Slf4j
public class ServerInvoker implements Callable {

	//Name of the node
	private String nodeName;

	private Ec2Client ec2Client;

	/**
	 * Constructor for thread worker.
	 *
	 * @param nodeName name of the node
	 * @param EC2 computeService to work with
	 */
	public ServerInvoker(String nodeName, Ec2Client ec2Client) {
		this.nodeName = nodeName;
		this.ec2Client = ec2Client;
	}

	/**
	 * Method executed in thread.
	 *
	 * @return created server
	 */
	@Override
	public NodeMetadata call() {
		log.info("Creating server inside thread for container: " + nodeName);
		final ComputeService computeService = ContextBuilder.newBuilder("aws-ec2")
				.credentials(ec2Client.getAccessKey(), ec2Client.getSecretKey())
				.endpoint(ec2Client.getUrl())
				.buildView(ComputeServiceContext.class).getComputeService();
		final String publicAddress;
		final NodeMetadata initialNodeMetadata;
		final TemplateBuilder templateBuilder = computeService.templateBuilder();
		final String fullId = ec2Client.getRegion() + "/" + ec2Client.getImageId();

		final Template template = templateBuilder
				.hardwareId(ec2Client.getInstanceType())
				.locationId(ec2Client.getRegion())
				.imageId(fullId).options(TemplateOptions.NONE)
				.options(buildTemplateOptions())
				.build();

		log.debug("Creating {} node from template: {}",
				fullId, template);

		try {
			initialNodeMetadata = createNode(computeService, template, nodeName);
			publicAddress = Iterables.getFirst(initialNodeMetadata.getPublicAddresses(), null);

			log.info("Started node '{}', its public IP address is {}",
					nodeName, publicAddress);
		} catch (RunNodesException e) {
			throw new RuntimeException("Unable to create aws-ec2 node from template " + template, e);
		}

		try {
			ec2Client.verifyServerStarted(publicAddress);
		} catch (Exception e) {
			computeService.destroyNode(initialNodeMetadata.getId());
			throw e;
		}
		return initialNodeMetadata;
	}

	private NodeMetadata createNode(ComputeService computeService, Template template, String serverName) throws RunNodesException {
		return Iterables.getOnlyElement(computeService.createNodesInGroup(ec2Client.getNamePrefix() + "-" + serverName, 1, template));
	}

	private EC2TemplateOptions buildTemplateOptions() {
		final AWSEC2TemplateOptions templateOptions = new AWSEC2TemplateOptions();

		templateOptions.keyPair(ec2Client.getKeyPair());
		templateOptions.securityGroupIds(ec2Client.getSecurityGroup());
//		templateOptions.nameTask(nodeName);

		return templateOptions;
	}
}
