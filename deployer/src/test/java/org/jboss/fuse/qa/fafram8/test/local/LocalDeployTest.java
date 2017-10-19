package org.jboss.fuse.qa.fafram8.test.local;

import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.Rule;
import org.junit.Test;

public class LocalDeployTest {
	@Rule
	public Fafram fafram = new Fafram().bundles("src/test/resources/deployTest.xml");

	@Test
	public void deployTest() throws Exception {
		Thread.sleep(10000L);
		assertTrue(fafram.executeCommand("log:display").contains("Hello from Camel route!"));
	}
}
