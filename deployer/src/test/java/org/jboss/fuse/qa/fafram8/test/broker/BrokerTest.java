package org.jboss.fuse.qa.fafram8.test.broker;

import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.broker.Broker;
import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mmelko on 18/03/16.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BrokerTest {

	static Container template = ChildContainer.builder()
			.name("broker1")
			.parentName("root")
			.build();

	static Broker brokerTemplate = Broker.builder()
			.name("broker1")
			.ssl(false)
			.group("broker1")
			.containers("broker1")
			.build();

	@ClassRule
	public static Fafram fafram = new Fafram().withFabric()
			.containers(
					RootContainer.builder().defaultRoot().build(),
					ChildContainer.builder(template).build())
			.brokers(
					Broker.builder(brokerTemplate).build()
			);

	@After
	public void killContainers() {
		final List<Container> containers = new ArrayList<>(fafram.getContainerList());

		for (Container c : containers) {
			if (!c.isRoot()) {
				c.destroy();
			}
		}
	}

	@Test
	@Ignore
	public void testBrokeratStartup() {
		assertTrue(fafram.getContainer(template.getName()).executeCommand("bstat").contains(brokerTemplate.getName()));
	}

	@Test
	@Ignore
	public void testMasterSlaveConfiguration() {
		Broker template = Broker.builder().name("ms").containers("ms1", "ms2").group("masterslave").kind(Broker.MASTERSLAVE).build();
		fafram.containers(ChildContainer.builder().name("ms1").parentName("root").build(),
				ChildContainer.builder().name("ms2").parentName("root").build())
				.brokers(Broker.builder(template).build());

		Assert.assertTrue(fafram.executeCommand("cluster-list").contains("amq/masterslave"));
		Assert.assertTrue(fafram.executeCommand("cluster-list | grep ms").contains("ms1"));
		Assert.assertTrue(fafram.executeCommand("cluster-list | grep ms").contains("ms2"));
	}

	@Test
	@Ignore
	public void testMeshConfiguration() {
		Broker bTemplate = Broker.builder()
				.group("mesh")
				.addNetwork("network")
				.kind(Broker.STANDALONE)
				.addPid("network.consumerTTL", "1")
				.addPid("network.messageTTL", "29")
				.addPid("network.replayWhenNoConsumers", "true")
				.addPid("network.replayDelay", "1000")
				.addPid("network.initialReconnectDelay", "1500")
				.build();

		Container cTemplate = ChildContainer.builder().parentName("root").name("mesh").build();

		fafram
				.containers(
						ChildContainer.builder(cTemplate).name("mesh1").build(),
						ChildContainer.builder(cTemplate).name("mesh2").build(),
						ChildContainer.builder(cTemplate).name("mesh3").build())
				.brokers(
						Broker.builder(bTemplate).name("mesh1").containers("mesh1").build(),
						Broker.builder(bTemplate).name("mesh2").containers("mesh2").build(),
						Broker.builder(bTemplate).name("mesh3").containers("mesh3").build());

		assertTrue(fafram.getContainer("mesh1").executeCommand("bstat").contains("mesh1"));
		assertTrue(fafram.getContainer("mesh2").executeCommand("bstat").contains("mesh2"));
		assertTrue(fafram.getContainer("mesh3").executeCommand("bstat").contains("mesh3"));
	}
}
