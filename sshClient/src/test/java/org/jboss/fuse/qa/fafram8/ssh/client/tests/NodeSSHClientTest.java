package org.jboss.fuse.qa.fafram8.ssh.client.tests;

import org.jboss.fuse.qa.fafram8.ssh.AbstractSSHClient;
import org.jboss.fuse.qa.fafram8.ssh.NodeSSHClient;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class NodeSSHClientTest {

	private static final String HOST = "10.8.50.177";

	@Test
	public void testFluent() throws Exception {
		Assert.fail("TODO rjakubco");
		final AbstractSSHClient client = new NodeSSHClient().hostname(HOST).defaultSSHPort().username("admin")
				.password("admin");
		client.connect();
		final String response = client.executeCommand("ping -c 1 google.com");
		Assert.assertTrue(response.contains("PING google.com"));
	}

	@Test
	public void testSetters() throws Exception {
		Assert.fail("TODO(rjakubco): implement");
		// everything wrong -> check if setters work correctly
		final AbstractSSHClient client = new NodeSSHClient().hostname("10.8.50.17").fuseSSHPort().username("admin")
				.password("admin");

		client.setHostname(HOST);
		client.setPort(22);
		client.setUsername("fuse");
		client.setPassword("fuse");

		client.connect();

		final String response = client.executeCommand("ping -c 1 google.com");
		Assert.assertTrue(response.contains("PING google.com"));
	}

	@Test//(expected = SSHClientException.class)
	public void testWrongPort() throws Exception {
		Assert.fail("TODO(rjakubco): implement");
		final AbstractSSHClient client = new NodeSSHClient().hostname("10.8.50.17").port(50).username("admin")
				.password("fuse");
		client.connect();
		System.out.println(client.executeCommand("ping -c 1 google.com"));
	}

	@Test//(expected = SSHClientException.class)
	public void testWrongHost() throws Exception {
		Assert.fail("TODO(rjakubco): implement");
		final AbstractSSHClient client = new NodeSSHClient().hostname("10.8.50.17").defaultSSHPort().username("admin")
				.password("fuse");
		client.connect();
		System.out.println(client.executeCommand("ping -c 1 google.com"));
	}

	@Test//(expected = SSHClientException.class)
	public void testAuthFail() throws Exception {
		Assert.fail("TODO(rjakubco): implement");
		final AbstractSSHClient client = new NodeSSHClient().hostname(HOST).defaultSSHPort().username("admin")
				.password("fuse");
		client.connect();
		System.out.println(client.executeCommand("ping -c 1 google.com"));
	}
}
