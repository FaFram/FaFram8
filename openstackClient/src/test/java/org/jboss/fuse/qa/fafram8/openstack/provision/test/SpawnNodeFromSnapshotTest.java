package org.jboss.fuse.qa.fafram8.openstack.provision.test;

import static org.junit.Assert.assertEquals;

import org.jboss.fuse.qa.fafram8.openstack.provision.OpenStackClient;

import org.junit.AfterClass;
import org.junit.Test;

import org.openstack4j.model.compute.Server;

import lombok.extern.slf4j.Slf4j;

/**
 * OpenStack client test for spawning node
 *
 * Created by ecervena on 24.9.15.
 */
@Slf4j
public class SpawnNodeFromSnapshotTest {

	@Test
	public void spawnNewNodeTest() {
		final OpenStackClient osm = OpenStackClient.builder().defaultOS7client().build();
		osm.spawnNewServer("hello-kitty");
		assertEquals("Wrong name returned.", "fafram8-hello-kitty", osm.getServerFromRegister("fafram8-hello-kitty").getName());
	}

	@AfterClass
	public static void clean() {
		final OpenStackClient osm = OpenStackClient.builder().defaultOS7client().build();
		final Server server = osm.getServerFromRegister("fafram8-hello-kitty");
		osm.deleteSpawnedServer(server.getName());
	}
}
