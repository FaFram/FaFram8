package org.jboss.fuse.qa.fafram8.test.remote;

import static org.junit.Assert.assertEquals;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.resource.Fafram;
import org.jboss.fuse.qa.fafram8.test.base.FaframTestBase;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

/**
 * Test if a command can be executed on remote node.
 * Created by avano on 11.4.16.
 */
public class RemoteExecuteNodeCommandTest {
	private Container root = RootContainer.builder().defaultRoot().name("build-excommand-root").withFabric().build();
	private Container ssh = SshContainer.builder().name("build-excommand-container").parent(root).build();

	@Rule
	public Fafram fafram = new Fafram().fuseZip(FaframTestBase.CURRENT_LOCAL_URL).provider(FaframProvider.OPENSTACK).containers(root, ssh);

	@Test
	public void executeNodeCommandTest() throws Exception {
		final List<String> responses = root.executeNodeCommands("echo \"test\" >> /tmp/test", "cat /tmp/test");
		assertEquals("Cat response", "test", responses.get(1));
		final List<String> responsesSsh = ssh.executeNodeCommands("echo \"testssh\" >> /tmp/testssh", "cat /tmp/testssh");
		assertEquals("Cat response", "testssh", responsesSsh.get(1));
	}
}
