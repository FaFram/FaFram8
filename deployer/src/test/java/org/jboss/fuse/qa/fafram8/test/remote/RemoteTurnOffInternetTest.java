package org.jboss.fuse.qa.fafram8.test.remote;

import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.cluster.node.Node;
import org.jboss.fuse.qa.fafram8.exceptions.KarafSessionDownException;
import org.jboss.fuse.qa.fafram8.exceptions.SSHClientException;
import org.jboss.fuse.qa.fafram8.exceptions.VerifyFalseException;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.resource.Fafram;
import org.jboss.fuse.qa.fafram8.test.base.FaframTestBase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Test for setting up offline environment when using OpenStack provision.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class RemoteTurnOffInternetTest {
	private Container root = RootContainer.builder().name("build-offline-root").withFabric().build();

	private Container ssh = SshContainer.builder().name("build-offline-ssh").parentName("build-offline-root").build();

	@Rule
	public Fafram fafram = new Fafram().provider(FaframProvider.OPENSTACK).containers(root, ssh).offline().suppressStart();

	@BeforeClass
	public static void before() {
		System.setProperty(FaframConstant.FUSE_ZIP, FaframTestBase.CURRENT_LOCAL_URL);
	}

	@AfterClass
	public static void clean() {
		System.clearProperty(FaframConstant.FUSE_ZIP);
	}

	@Test
	public void testInternet() throws VerifyFalseException, SSHClientException, KarafSessionDownException {
		String response = root.getNode().getExecutor().executeCommand("curl -vs google.com 2>&1");
		assertTrue(response.contains("Failed to connect") && response.contains("Network is unreachable"));

		response = ssh.executeNodeCommand("curl -vs google.com 2>&1");
		assertTrue(response.contains("Failed to connect") && response.contains("Network is unreachable"));
	}
}
