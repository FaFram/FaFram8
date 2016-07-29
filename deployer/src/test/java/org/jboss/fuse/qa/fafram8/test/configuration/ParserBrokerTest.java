package org.jboss.fuse.qa.fafram8.test.configuration;

import static org.junit.Assert.assertEquals;

import org.jboss.fuse.qa.fafram8.cluster.broker.Broker;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by avano on 28.7.16.
 */
public class ParserBrokerTest {
	private static Fafram fafram;

	@BeforeClass
	public static void init() {
		fafram = new Fafram().config("src/test/resources/parserBrokerTest.xml");
		fafram.initConfiguration();
	}

	@Test
	public void parseBrokerTest() {
		assertEquals("List size", 1, fafram.getBrokerList().size());
		Broker b = fafram.getBrokerList().get(0);
		assertEquals("Name", "name", b.getName());
		assertEquals("Ssl", true, b.isSsl());
		assertEquals("Group", "myGroup", b.getGroup());
		assertEquals("Data", "/data", b.getData());
		assertEquals("ParentProfile", "default", b.getParentProfile());
		assertEquals("Pid", "myvalue", b.getPids().get("mypid"));
		assertEquals("Network", "tcp://localhost:61616", b.getNetworks().get(0));
	}

	@AfterClass
	public static void tearDown() {
		fafram.tearDown();
	}
}
