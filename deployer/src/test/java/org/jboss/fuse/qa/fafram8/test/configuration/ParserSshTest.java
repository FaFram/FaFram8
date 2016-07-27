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
 * Created by avano on 27.7.16.
 */
public class ParserSshTest {
	private static Fafram fafram;

	@BeforeClass
	public static void init() {
		fafram = new Fafram().config("src/test/resources/parserSshTest.xml");
		fafram.initConfiguration();
	}

	@Test
	public void parseSshContainerTest() {
		assertEquals("List size", 1, fafram.getContainerList().size());
		Container c = fafram.getContainerList().get(0);
		assertEquals("3", c.getName());
		// Three added + one urandom
		assertEquals("Jvm opts list size", 4, OptionUtils.get(c.getOptions(), Option.JVM_OPTS).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.JVM_OPTS).contains("1"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.JVM_OPTS).contains("2"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.JVM_OPTS).contains("3"));
		assertEquals("Cmds list size", 2, OptionUtils.get(c.getOptions(), Option.COMMANDS).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.COMMANDS).contains("2"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.COMMANDS).contains("3"));
		assertEquals("Profiles list size", 2, OptionUtils.get(c.getOptions(), Option.PROFILE).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.PROFILE).contains("1"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.PROFILE).contains("3"));
		assertEquals("Resolver", "localip", OptionUtils.getString(c.getOptions(), Option.RESOLVER));
		assertEquals("Manual ip", "2", OptionUtils.getString(c.getOptions(), Option.MANUAL_IP));
		assertEquals("Version", "3", OptionUtils.getString(c.getOptions(), Option.VERSION));
		assertEquals("Pass phrase", "3", OptionUtils.getString(c.getOptions(), Option.PASS_PHRASE));
		assertEquals("Min port", "3", OptionUtils.getString(c.getOptions(), Option.MIN_PORT));
		assertEquals("Max port", "3", OptionUtils.getString(c.getOptions(), Option.MAX_PORT));
		assertEquals("Proxy uri", "3", OptionUtils.getString(c.getOptions(), Option.PROXY_URI));
		assertEquals("Env size", 3, OptionUtils.get(c.getOptions(), Option.ENV).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.ENV).contains("1"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.ENV).contains("2"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.ENV).contains("3"));
		assertEquals("Node host", "3", c.getNode().getHost());
		assertEquals("Node user", "3", c.getNode().getUsername());
		assertEquals("Node pass", "3", c.getNode().getPassword());
		assertEquals("Zookeeper pw", "2", OptionUtils.getString(c.getOptions(), Option.ZOOKEEPER_PASSWORD));
		assertEquals("Workdir", "2", OptionUtils.getString(c.getOptions(), Option.WORKING_DIRECTORY));
		assertEquals("Private key", "1", OptionUtils.getString(c.getOptions(), Option.PRIVATE_KEY));

	}

	@AfterClass
	public static void tearDown() {
		SystemProperty.clearAllProperties();
		ModifierExecutor.clearAllModifiers();
		ContainerManager.clearAllLists();
	}
}
