package org.jboss.fuse.qa.fafram8.test.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.modifier.ModifierExecutor;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.resource.Fafram;
import org.jboss.fuse.qa.fafram8.util.Option;
import org.jboss.fuse.qa.fafram8.util.OptionUtils;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies parsing the child containers from XML file.
 *
 * Created by avano on 27.7.16.
 */
public class ParserChildTest {
	private static Fafram fafram;

	@BeforeClass
	public static void init() {
		fafram = new Fafram().config("src/test/resources/parserChildTest.xml");
		fafram.initConfiguration();
	}

	@Test
	public void parseChildContainerTest() {
		assertEquals("List size", 1, fafram.getContainerList().size());
		Container c = fafram.getContainerList().get(0);
		assertEquals("3", c.getName());
		// One added + one urandom
		assertEquals("Jvm opts list size", 2, OptionUtils.get(c.getOptions(), Option.JVM_OPTS).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.JVM_OPTS).contains("1"));
		assertEquals("Cmds list size", 2, OptionUtils.get(c.getOptions(), Option.COMMANDS).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.COMMANDS).contains("1"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.COMMANDS).contains("2"));
		assertEquals("Profiles list size", 2, OptionUtils.get(c.getOptions(), Option.PROFILE).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.PROFILE).contains("1"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.PROFILE).contains("3"));
		assertEquals("Jmx user", "1", OptionUtils.getString(c.getOptions(), Option.JMX_USER));
		assertEquals("Jmx pw", "1", OptionUtils.getString(c.getOptions(), Option.JMX_PASSWORD));
		assertEquals("Resolver", "localip", OptionUtils.getString(c.getOptions(), Option.RESOLVER));
		assertEquals("Manual ip", "2", OptionUtils.getString(c.getOptions(), Option.MANUAL_IP));
		assertEquals("Parent name", "3", c.getParentName());
		assertEquals("Version", "1", OptionUtils.getString(c.getOptions(), Option.VERSION));
	}

	@AfterClass
	public static void tearDown() {
		SystemProperty.clearAllProperties();
		ModifierExecutor.clearAllModifiers();
		ContainerManager.clearAllLists();
	}
}
