package org.jboss.fuse.qa.fafram8.test.local;

import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.After;
import org.junit.Test;

/**
 * Test if ensemble can be created in rule / in test.
 * Created by avano on 11.4.16.
 */
public class LocalCreateEnsembleTest {
	private Fafram fafram;
	private final Container root = RootContainer.builder().defaultRoot().withFabric().build();
	private final Container child1 = ChildContainer.builder().name("child1").parent(root).build();
	private final Container child2 = ChildContainer.builder().name("child2").parent(root).build();

	@Test
	public void ruleEnsembleTest() {
		fafram = new Fafram().ensemble(root, child1, child2).containers(child1, child2, root).addUser("fafram", "fafram", "admin").setup();
		assertTrue(root.executeCommand("container-list").contains("-1"));
		assertTrue(root.executeCommand("container-list").contains("-2"));
		assertTrue(root.executeCommand("container-list").contains("-3"));
	}

	@Test
	public void inTestEnsembleTest() {
		fafram = new Fafram().containers(child2, root, child1).addUser("fafram", "fafram", "admin").setup();
		fafram.ensemble("root", "child1", "child2");
		assertTrue(root.executeCommand("container-list").contains("-1"));
		assertTrue(root.executeCommand("container-list").contains("-2"));
		assertTrue(root.executeCommand("container-list").contains("-3"));
	}

	@After
	public void teardown() {
		if (fafram != null) {
			fafram.tearDown();
		}
	}
}
