package org.jboss.fuse.qa.fafram8.ec2.provision;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import org.jclouds.compute.domain.NodeMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * Test basic properties.
 *
 * Sep 17, 2016 Red Hat
 *
 * @author tplevko@redhat.com
 */
@Slf4j
public class NodePropertiesTest {

	private static Ec2Client ec2Client;
	private final String nodeName = "testserver";

	@AfterClass
	public static void tearDown() {
		ec2Client.releaseResources();
	}

	@Test
	public void testOverridingValues() throws Exception {

		ec2Client = Ec2Client.builder().defaultEc2client().build();

		ec2Client.spawnNewServer(nodeName);
		final NodeMetadata server = ec2Client.getServerFromRegister(ec2Client.getNamePrefix() + "-" + nodeName);

		Assert.assertNotNull("server discovered : {}", server.getName());
	}
}
