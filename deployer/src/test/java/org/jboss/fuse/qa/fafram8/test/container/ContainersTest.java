package org.jboss.fuse.qa.fafram8.test.container;

import static org.junit.Assert.assertEquals;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.resource.Fafram;
import org.jboss.fuse.qa.fafram8.test.base.FaframTestBase;

import org.junit.Rule;
import org.junit.Test;

/**
 * Created by avano on 5.2.16.
 */
public class ContainersTest {
	@Rule
	public Fafram fafram = new Fafram().provider(FaframProvider.OPENSTACK).fuseZip(FaframTestBase.CURRENT_LOCAL_URL).containers(
			RootContainer.builder().defaultRoot().withFabric().name("cintest-root").build()
	);

	@Test
	public void addContainerInTestTest() {
		final Container ssh = SshContainer.builder().name("test-ssh").parentName("cintest-root").build();
		fafram.containers(ssh);
		assertEquals("SSH response", "1", ssh.executeCommand("echo 1"));
	}
}
