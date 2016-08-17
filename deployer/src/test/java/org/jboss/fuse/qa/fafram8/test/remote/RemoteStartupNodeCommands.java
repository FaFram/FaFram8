package org.jboss.fuse.qa.fafram8.test.remote;

import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.Rule;
import org.junit.Test;

/**
 * Created by avano on 1.8.16.
 */
public class RemoteStartupNodeCommands {
	private Container root = RootContainer.builder().defaultRoot().startupNodeCommands("touch /tmp/first", "touch /tmp/second").build();
	@Rule
	public Fafram fafram = new Fafram().containers(root);

	@Test
	public void startupCmdsTest() {
		assertTrue(root.executeNodeCommand("ls /tmp").contains("first"));
		assertTrue(root.executeNodeCommand("ls /tmp").contains("second"));
	}
}
