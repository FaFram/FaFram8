package org.jboss.fuse.qa.fafram8.test.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.resource.Fafram;
import org.jboss.fuse.qa.fafram8.util.Option;
import org.jboss.fuse.qa.fafram8.util.OptionUtils;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by ecervena on 1/11/16.
 */
public class ParserRootTest {

	private static Fafram fafram;

	@BeforeClass
	public static void init() {
		fafram = new Fafram().config("src/test/resources/parserRootTest.xml");
		fafram.initConfiguration();
	}

	@Test
	public void parseRootContainerTest() {
		assertEquals("List size", 1, fafram.getContainerList().size());
		Container c = fafram.getContainerList().get(0);
		assertEquals("root-final", c.getName());
		// Two added + one urandom
		assertEquals("Jvm opts list size", 3, OptionUtils.get(c.getOptions(), Option.JVM_OPTS).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.JVM_OPTS).contains("someopts"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.JVM_OPTS).contains("-Dproperty=Value"));
		assertEquals("Node host", "9", c.getNode().getHost());
		assertEquals("Node port", 10, c.getNode().getPort());
		assertEquals("Node user", "11", c.getNode().getUsername());
		assertEquals("Node pass", "12", c.getNode().getPassword());
		assertEquals("Jvm mem opts list size", 4, OptionUtils.get(c.getOptions(), Option.JVM_MEM_OPTS).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.JVM_MEM_OPTS).contains("JAVA_MIN_MEM=9"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.JVM_MEM_OPTS).contains("JAVA_MAX_MEM=10"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.JVM_MEM_OPTS).contains("JAVA_PERM_MEM=11"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.JVM_MEM_OPTS).contains("JAVA_MAX_PERM_MEM=12"));
		assertEquals("Cmds list size", 4, OptionUtils.get(c.getOptions(), Option.COMMANDS).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.COMMANDS).contains("profile-edit root"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.COMMANDS).contains("temp1"));
		assertEquals("Bundles list size", 2, OptionUtils.get(c.getOptions(), Option.BUNDLES).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.BUNDLES).contains("bundle"));
		assertTrue(OptionUtils.get(c.getOptions(), Option.BUNDLES).contains("bundle1"));
		assertEquals("Profiles list size", 1, OptionUtils.get(c.getOptions(), Option.PROFILE).size());
		assertTrue(OptionUtils.get(c.getOptions(), Option.PROFILE).contains("p"));
	}

	@AfterClass
	public static void tearDown() {
		fafram.tearDown();
	}
}

