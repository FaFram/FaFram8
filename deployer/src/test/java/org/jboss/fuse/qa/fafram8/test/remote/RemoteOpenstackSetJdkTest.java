package org.jboss.fuse.qa.fafram8.test.remote;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.property.Openstack;
import org.jboss.fuse.qa.fafram8.resource.Fafram;
import org.jboss.fuse.qa.fafram8.test.base.FaframTestBase;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Test for setting default JDK path to all types of containers.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class RemoteOpenstackSetJdkTest {
	private final Container root = RootContainer.builder().name("root-jdk-test").withFabric().build();
	private final Container ssh = SshContainer.builder().name("ssh-jdk-test").parent(root).build();
	private final Container rootChild = ChildContainer.builder().name("root-child-jdk-test").parent(root).build();
	private final Container sshChild = ChildContainer.builder().name("ssh-child-jdk-test").parent(ssh).build();

	@Rule
	public Fafram fafram = new Fafram().withFabric().provider(FaframProvider.OPENSTACK).containers(root, ssh, rootChild, sshChild).jdk(Openstack.JDK8);

	@BeforeClass
	public static void before() {
		System.setProperty(FaframConstant.WITH_THREADS, "");
		System.setProperty(FaframConstant.FUSE_ZIP, FaframTestBase.CURRENT_LOCAL_URL);
	}

	@After
	public void tearDown() {
		System.clearProperty(FaframConstant.WITH_THREADS);
		System.clearProperty(FaframConstant.FUSE_ZIP);
	}

	@Test
	public void testSettingJavaHomeForSshContainer() throws Exception {
		log.debug(ssh.executeNodeCommand("ps aux | grep " + ssh.getName()));
		log.debug(sshChild.executeNodeCommand("ps aux | grep " + sshChild.getName()));
		log.debug(fafram.executeNodeCommand("ps aux | grep " + root.getName()));
		log.debug(fafram.executeNodeCommand("ps aux | grep " + rootChild.getName()));

		assertTrue(ssh.executeNodeCommand("ps aux | grep " + ssh.getName()).contains(Openstack.JDK8.getPath()));
		assertTrue(sshChild.executeNodeCommand("ps aux | grep " + sshChild.getName()).contains(Openstack.JDK8.getPath()));
		assertTrue(fafram.executeNodeCommand("ps aux | grep \"karaf.base=" + StringUtils.removeEnd(root.getFusePath(), "/") + "\"")
				.contains(Openstack.JDK8.getPath()));
		assertTrue(fafram.executeNodeCommand("ps aux | grep " + rootChild.getName()).contains(Openstack.JDK8.getPath()));
	}
}
