package org.jboss.fuse.qa.fafram8.test.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.cluster.node.Node;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.resource.Fafram;
import org.jboss.fuse.qa.fafram8.test.base.FaframTestBase;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Test for testing WORKING_DIRECTORY property on remote machine.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class RemoteWorkingDirectoryTest {
	private static final String DIR = "/home/fuse/bin";

	static {
		System.setProperty(FaframConstant.FUSE_ZIP, FaframTestBase.CURRENT_LOCAL_URL);
		System.setProperty(FaframConstant.WORKING_DIRECTORY, DIR);
	}

	private static Container root = RootContainer.builder().name("working-dir-root").node(
			Node.builder().build()).withFabric().build();
	private static Container root2 = RootContainer.builder().name("working-dir-root2").node(
			Node.builder().build()).directory("/home/fuse").withFabric().build();
	private static Container ssh = SshContainer.builder().name("working-dir-ssh").parent(root).node(
			Node.builder().build()).build();

	private static Container sshChild = ChildContainer.builder().name("test-jvm-opts-ssh-child").parent(ssh).build();
	private static Container childRoot = ChildContainer.builder().name("child-root").parent(root).build();
	private static Container child2Root = ChildContainer.builder().name("child2-root").parent(root).build();
	private static Container childRoot2 = ChildContainer.builder().name("child-root2").parent(root2).build();
	@ClassRule
	public static Fafram fafram = new Fafram().provider(FaframProvider.OPENSTACK).containers(root, root2, ssh, sshChild, childRoot, childRoot2, child2Root);

	@Test
	public void testWorkingDirectory() throws Exception {
		assertTrue(root.getFusePath().contains(DIR));
		assertTrue(ssh.executeNodeCommand("ps aux | grep karaf.base").contains(DIR));

		assertTrue(root2.getFusePath().contains("/home/fuse"));
		assertFalse(root2.getFusePath().contains(DIR));
	}

//	@Test
//	public void testWorkDirOnSSH() throws Exception {
//		final String sshDir = "/home/fuse/ssh/test/folder";
//		final Container ssh2 = SshContainer.builder().name("working-dir-ssh2").parent(root).node(ssh.getNode()).directory(sshDir).build();
//		ssh2.create();
//		assertTrue(ssh2.executeNodeCommand("ps aux | grep karaf.base").contains(sshDir));
//	}

	@AfterClass
	public static void tearDown() throws Exception {
		System.clearProperty(FaframConstant.WORKING_DIRECTORY);
		System.clearProperty(FaframConstant.FUSE_ZIP);
	}
}
