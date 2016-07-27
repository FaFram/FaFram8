package org.jboss.fuse.qa.fafram8.test.configuration;

import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.modifier.ModifierExecutor;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by ecervena on 1/11/16.
 */
public class ParserTest {

	private static Fafram fafram;

	@BeforeClass
	public static void init() {
		fafram = new Fafram().config("src/test/resources/ParserTestConfiguration.xml");
		fafram.initConfiguration();
	}

	@Test
	public void parseRootContainerTest() {
//		assertEquals("root1", fafram.getContainerList().get(0).getName());
//		assertEquals("fafram8-ssh-container-a", fafram.getContainerList().get(1).getName());
//		assertEquals("fafram8-ssh-container-b", fafram.getContainerList().get(2).getName());
	}

	@AfterClass
	public static void tearDown() {
		SystemProperty.clearAllProperties();
		ModifierExecutor.clearAllModifiers();
		ContainerManager.clearAllLists();
	}
}
