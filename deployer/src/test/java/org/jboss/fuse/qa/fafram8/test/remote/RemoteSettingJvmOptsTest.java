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

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Test for setting jvm opts on all type of containers.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class RemoteSettingJvmOptsTest {
	// Perm parameters are not propagated to jvm-opts so only this can be visible
	private String rootJvmOpts = "-Xms1024M -Xmx3048M";
	private String rootJvmOpts2 = "-XX:PermSize=128M -XX:MaxPermSize=512M";
	private String sshJvmOpts = "-Xms1024M -Xmx1048M -XX:PermSize=256M -XX:MaxPermSize=600M";
	private String childJvmOpts = "-Xms1024M -Xmx1048M -XX:PermSize=128M -XX:MaxPermSize=400M";
	private final Container root = RootContainer.builder().name("test-jvm-opts-root").withFabric().jvmMemoryOpts("1024M", "3048M", "128M", "512M").jvmOpts("-XX:PermSize=128M -XX:MaxPermSize=512M").build();
	private final Container ssh = SshContainer.builder().name("test-jvm-opts-ssh").parent(root).jvmOpts("-Xms1024M", "-Xmx1048M", "-XX:PermSize=256M", "-XX:MaxPermSize=600M").build();
	private final Container sshChild = ChildContainer.builder().name("test-jvm-opts-ssh-child").parent(ssh).jvmOpts("-Xms1024M", "-Xmx1048M", "-XX:PermSize=128M", "-XX:MaxPermSize=400M").build();

	@Rule
	public Fafram fafram = new Fafram().withFabric().provider(FaframProvider.OPENSTACK).containers(root, ssh, sshChild);

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
	public void testSettingJvmOpts() throws Exception {
		log.debug(root.executeCommand("container-edit-jvm-options -f " + root.getName()));
		log.debug(root.executeCommand("container-edit-jvm-options -f " + ssh.getName()));
		log.debug(root.executeCommand("container-edit-jvm-options -f " + sshChild.getName()));

		assertTrue(root.executeCommand("container-edit-jvm-options -f " + root.getName()).contains(rootJvmOpts));
		assertTrue(root.executeCommand("container-edit-jvm-options -f " + root.getName()).contains(rootJvmOpts2));
		assertTrue(root.executeCommand("container-edit-jvm-options -f " + ssh.getName()).contains(sshJvmOpts));
		assertTrue(root.executeCommand("container-edit-jvm-options -f " + sshChild.getName()).contains(childJvmOpts));
	}
}
