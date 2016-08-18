package org.jboss.fuse.qa.fafram8.test.remote;

import static org.junit.Assert.assertFalse;

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
 * test cases:
 * - join on windows
 * - 2 containers on the same node
 * - ssh container with join
 * - kill, stop, destroy, start, restart
 * - working dir set on ssh - what happens to join
 * http://dashboard.centralci.eng.rdu2.redhat.com/dashboard/project/images/b2005497-4aaf-4a64-817d-847d5d9319f9/
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class RemoteWindowsTest {
	private Container root = RootContainer.builder().name("windows-root").withFabric().build();

	private Container ssh = SshContainer.builder().name("windows-ssh").parent(root).build();
	private Container ssh2 = SshContainer.builder().name("second-ssh").profiles("hawtio").parent(root).sameNodeAs(ssh).build();
	private Container ssh3 = SshContainer.builder().name("third-ssh").directory("/home/fuse/test/dir/").parent(root).sameNodeAs(ssh).build();
	private Container childSsh = ChildContainer.builder().name("child-root").parent(ssh).build();

	@Rule
	public Fafram fafram = new Fafram().provider(FaframProvider.OPENSTACK).containers(root, ssh, ssh2, ssh3, childSsh);

	@BeforeClass
	public static void Before() {
		// For windows...
		System.setProperty(FaframConstant.OPENSTACK_IMAGE, "3fe107d5-df68-4f43-954d-a71d2ae4a3aa");
		System.setProperty(FaframConstant.OPENSTACK_FLAVOR, "4");
		System.setProperty(FaframConstant.HOST_USER, "hudson");
		System.setProperty(FaframConstant.HOST_PASSWORD, "redhat");
		// add correct path on windows
		System.setProperty(FaframConstant.FUSE_ZIP, FaframTestBase.CURRENT_WIN_LOCAL_URL);
//		System.setProperty(FaframConstant.WITH_THREADS, "");
	}

	@AfterClass
	public static void After() {
		System.clearProperty(FaframConstant.FUSE_ZIP);
	}

	@Test
	public void testWindowsCluster() throws Exception {
		ssh2.kill();
		assertFalse(ssh2.executeNodeCommand("ps aux | grep " + ssh2.getName()).contains("karaf.base"));

		ssh3.restart();

		ssh3.stop();

		ssh3.start();

	}
}
