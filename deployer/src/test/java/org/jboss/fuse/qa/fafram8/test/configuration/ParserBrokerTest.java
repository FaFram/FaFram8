package org.jboss.fuse.qa.fafram8.test.configuration;

import static org.junit.Assert.assertEquals;

import org.jboss.fuse.qa.fafram8.cluster.broker.Broker;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies parsing the brokers from XML file.
 *
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
		assertEquals("Network password", "password", b.getNetworksPassword());
		assertEquals("Network user", "user", b.getNetworksUsername());
		assertEquals("Name", "name", b.getName());
		assertEquals("Ssl", false, b.isSsl());
		assertEquals("Group", "myGroup2", b.getGroup());
		assertEquals("Data", "/data", b.getData());
		assertEquals("ParentProfile", "default", b.getParentProfile());
		assertEquals("Pid", "myvalue", b.getPids().get("mypid"));
		assertEquals("Pid2", "myvalue2", b.getPids().get("mypid2"));
		assertEquals("Network", "tcp://localhost:61616", b.getNetworks().get(0));
	}

	@AfterClass
	public static void tearDown() {
		fafram.tearDown();
	}
}
