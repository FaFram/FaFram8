package org.jboss.fuse.qa.fafram8.test.remote;

import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
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
 * Test for windows fabric deployment.
 *
 * Windows snapshot on OpenStack: http://dashboard.centralci.eng.rdu2.redhat.com/dashboard/project/images/3fe107d5-df68-4f43-954d-a71d2ae4a3aa/
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class RemoteWindowsTest {
	private Container root = RootContainer.builder().name("windows-root").withFabric("--resolver localip").build();

	private Container ssh = SshContainer.builder().name("windows-ssh").parent(root).build();
	private Container ssh2 = SshContainer.builder().name("second-ssh").directory("/home/hudson/test/dir").profiles("hawtio").parent(root).sameNodeAs(ssh).build();
	private Container childSsh = ChildContainer.builder().name("child-root").parent(ssh).build();

	@Rule
	public Fafram fafram = new Fafram().provider(FaframProvider.OPENSTACK).containers(root, ssh, ssh2, childSsh);

	@BeforeClass
	public static void Before() {
		// For windows...
		System.setProperty(FaframConstant.OPENSTACK_IMAGE, "3fe107d5-df68-4f43-954d-a71d2ae4a3aa");
		System.setProperty(FaframConstant.OPENSTACK_FLAVOR, "4");
		System.setProperty(FaframConstant.HOST_USER, "hudson");
		System.setProperty(FaframConstant.HOST_PASSWORD, "redhat");
		// add correct path on windows
		System.setProperty(FaframConstant.FUSE_ZIP, FaframTestBase.CURRENT_WIN_LOCAL_URL);
		System.setProperty(FaframConstant.START_WAIT_TIME, "600");
	}

	@AfterClass
	public static void After() {
		System.clearProperty(FaframConstant.FUSE_ZIP);
		System.clearProperty(FaframConstant.OPENSTACK_IMAGE);
		System.clearProperty(FaframConstant.OPENSTACK_FLAVOR);
		System.clearProperty(FaframConstant.HOST_USER);
		System.clearProperty(FaframConstant.HOST_PASSWORD);
		System.clearProperty(FaframConstant.START_WAIT_TIME);
	}

	@Test
	public void testWindowsCluster() throws Exception {
		System.out.println(root.executeCommand("container-list"));
		assertTrue(ssh2.executeCommand("shell:info | grep \"Karaf base\"").contains("C:\\Users\\hudson\\test\\dircontainers\\second-ssh"));
		assertTrue(ssh.executeCommand("shell:info | grep \"Karaf base\"").contains("C:\\Users\\hudson\\containers\\windows-ssh\\fafram"));
		ssh2.restart();

		ssh.stop();
		ssh.start();
	}
}
