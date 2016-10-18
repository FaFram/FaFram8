package org.jboss.fuse.qa.fafram8.ec2.provision;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.jclouds.compute.domain.NodeMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * Test basic start/stop operations.
 *
 * Oct 17, 2016 Red Hat
 *
 * @author tplevko@redhat.com
 */
@Slf4j
public class StartStopTest {

	private static Ec2Client ec2Client;
	private final String nodeName = "testserver";

	@After
	public void tearDown() {
		ec2Client.deleteSpawnedServer(ec2Client.getNamePrefix() + "-" + nodeName);
	}

	@Test
	public void testOverridingValues() throws Exception {

		ec2Client = Ec2Client.builder().defaultEc2client().build();

		ec2Client.spawnNewServer(nodeName);
		final NodeMetadata server = ec2Client.getServerFromRegister(ec2Client.getNamePrefix() + "-" + nodeName);
		ec2Client.stopServer(server.getName());

		log.info("Wait 20 seconds for the server to stop");
		Thread.sleep(20000);

		Assert.assertEquals("SUSPENDED", ec2Client.getServers(server.getName()).get(0).getStatus().name());

		ec2Client.startServer(server.getName());

		log.info("Wait 20 seconds for the server to start");
		Thread.sleep(20000);

		Assert.assertEquals("RUNNING", ec2Client.getServers(server.getName()).get(0).getStatus().name());
	}
}
