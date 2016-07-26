package org.jboss.fuse.qa.fafram8.test.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.resource.Fafram;
import org.jboss.fuse.qa.fafram8.test.base.FaframTestBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Tests killing of child, ssh and root container by fafram.killContainer() method.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class RemoteKillingContainersTest {
	private static String childName = "build-child-container";
	private static String sshName = "build-ssh-container";

	private Container root = RootContainer.builder().defaultRoot().name("build-root").withFabric().build();
	private Container child = ChildContainer.builder().name(childName).parent(root).build();
	private Container ssh = SshContainer.builder().name(sshName).parent(root).build();


	public Fafram fafram = new Fafram().fuseZip(FaframTestBase.CURRENT_LOCAL_URL).provider(FaframProvider.OPENSTACK).containers(root, child, ssh);

	@Before
	public void setUp() throws Exception {
		fafram.setup();
	}

	@After
	public void tearDown() throws Exception {
		fafram.tearDown(true);
	}

	@Test
	public void killTest() throws Exception {
		child.kill();
		String response = fafram.executeCommand("exec ps aux | grep " + childName);
		assertFalse(response.contains("karaf.base"));

		final Executor executor = ssh.getNode().getExecutor();

		executor.executeCommand("pkill -9 -f karaf");
		response = executor.executeCommand("ps aux | grep " + sshName);
		assertFalse(response.contains("karaf.base"));

		root.kill();

		assertNull(fafram.executeCommand("list"));

		if(SystemProperty.isWithThreads()){
			try {
				fafram.tearDown();
			} catch (FaframException e) {
				// cool
			}
		} else {
			fafram.tearDown();
		}

	}
}
