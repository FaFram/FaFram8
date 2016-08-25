package org.jboss.fuse.qa.fafram8.test.remote;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.provision.provider.OpenStackProvisionProvider;
import org.jboss.fuse.qa.fafram8.resource.Fafram;
import org.jboss.fuse.qa.fafram8.test.base.FaframTestBase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;

/**
 * Remote onlyConnect property test.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class RemoteOnlyConnectTest {
	private static OpenStackProvisionProvider osm = OpenStackProvisionProvider.getInstance();
	private static String ipAddress;

	static {
		log.info("Spawning testing node...");
		final String serverName = "only-connect-" + new Date().getTime();
		ipAddress = osm.getClient().assignFloatingAddress(osm.getClient().spawnNewServer(serverName).getId());
		System.setProperty(FaframConstant.FUSE_ZIP, FaframTestBase.CURRENT_LOCAL_URL);
	}

	private static Container root = RootContainer.builder().name("only-connect-root").node(ipAddress).build();

	@ClassRule
	public static Fafram fafram = new Fafram().provider(FaframProvider.OPENSTACK).containers(root);

	@Before
	public void setUp() throws Exception {
		System.setProperty(FaframConstant.KEEP_CONTAINERS, "true");
		System.setProperty(FaframConstant.KEEP_OS_RESOURCES, "true");
		fafram.tearDown();
	}

	@Test
	public void onlyConnectTest() {
		final Fafram fafram2 = new Fafram().containers(RootContainer.builder().name("only-connect").node(ipAddress).onlyConnect().build()).setup();

		final String response = fafram2.executeCommand("osgi:list -t 0 | grep \"Apache Karaf :: Shell :: Console\"");
		System.out.println("response : " + response);
		assertNotNull(response);
		assertTrue(response.contains("Apache Karaf :: Shell :: Console"));
		assertTrue(response.contains("[Active"));
		assertTrue(response.contains("[Created"));
	}

	@Test
	public void cleanPropetyTest() {
		System.setProperty(FaframConstant.CLEAN, "false");
		final Fafram fafram2 = new Fafram().containers(RootContainer.builder().name("only-connect").node(ipAddress).build()).setup();

		final String response = fafram2.executeCommand("osgi:list -t 0 | grep \"Apache Karaf :: Shell :: Console\"");
		System.out.println("response : " + response);
		assertNotNull(response);
		assertTrue(response.contains("Apache Karaf :: Shell :: Console"));
		assertTrue(response.contains("[Active"));
		assertTrue(response.contains("[Created"));
	}

	@After
	public void tearDown() throws Exception {
		System.clearProperty(FaframConstant.CLEAN);
	}

	@AfterClass
	public static void after() {
		System.clearProperty(FaframConstant.KEEP_CONTAINERS);
		System.clearProperty(FaframConstant.KEEP_OS_RESOURCES);
		osm.releaseResources();
	}
}
