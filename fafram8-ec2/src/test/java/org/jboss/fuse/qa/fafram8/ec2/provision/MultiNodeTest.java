package org.jboss.fuse.qa.fafram8.ec2.provision;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.jclouds.compute.domain.NodeMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Spawn multiple nodes.
 *
 * Sep 16, 2016 Red Hat
 *
 * @author tplevko@redhat.com
 */
@Slf4j
public class MultiNodeTest {

	private static Ec2Client ec2Client;
	private static List<String> nodeNames = new ArrayList<>();
	private static final String imageIam = "ami-bd1f90aa";

	@BeforeClass
	public static void init() {
		nodeNames = Arrays.asList("node1", "node2");
	}

	@AfterClass
	public static void tearDown() {
		ec2Client.releaseResources();
	}

	@Test
	public void testOverridingValues() throws Exception {
		ec2Client = Ec2Client.builder().defaultEc2client().imageId(imageIam).build();
		ec2Client.spawnServersByNames(nodeNames);

		final NodeMetadata server1 = ec2Client.getServerFromRegister("node1");
		final NodeMetadata server2 = ec2Client.getServerFromRegister("node2");

		log.info("The node info: imageId: {}, imageHostName: {}.", server1.getImageId(), server1.getHostname());
		log.info("The node info: imageId: {}, imageHostName: {}.", server2.getImageId(), server2.getHostname());

		assertTrue(server1.getImageId().contains(imageIam));
		assertTrue(server2.getImageId().contains(imageIam));
	}
}
