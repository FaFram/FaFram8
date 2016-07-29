package org.jboss.fuse.qa.fafram8.cluster.xml.toplevel;

import org.jboss.fuse.qa.fafram8.cluster.broker.Broker;
import org.jboss.fuse.qa.fafram8.cluster.xml.broker.BrokerModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.broker.NetworkModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.broker.PidModel;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Class representing the <brokers> tag.
 * Created by avano on 28.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@Slf4j
public class BrokersModel {
	@XmlElement(name = "broker")
	private List<BrokerModel> brokers;

	/**
	 * Builds all brokers.
	 */
	public void buildBrokers() {
		Broker b;
		for (BrokerModel broker : brokers) {
			b = buildBroker(broker);
			log.trace("Parsed broker: " + b.toString());
			ContainerManager.getBrokers().add(b);
		}
	}

	/**
	 * Builds the broker object.
	 * @param broker broker model
	 * @return broker object
	 */
	private Broker buildBroker(BrokerModel broker) {
		final Broker.BrokerBuilder builder = Broker.builder();

		if (broker.getName() != null) {
			builder.name(broker.getName());
		}
		if (broker.isSsl()) {
			builder.ssl(broker.isSsl());
		}
		if (broker.getKind() != null) {
			builder.kind(broker.getKind());
		}
		if (broker.getGroup() != null) {
			builder.group(broker.getGroup());
		}
		if (broker.getData() != null) {
			builder.data(broker.getData());
		}
		if (broker.getParentProfile() != null) {
			builder.parentProfile(broker.getParentProfile());
		}
		if (broker.getNetworksModel() != null) {
			for (NetworkModel net : broker.getNetworksModel().getNetworks()) {
				builder.addNetwork(net.getUrl(), net.getUsername(), net.getPassword());
			}
		}
		if (broker.getPidsModel() != null) {
			for (PidModel pid : broker.getPidsModel().getPids()) {
				builder.addPid(pid.getPid(), pid.getValue());
			}
		}
		if (broker.getContainerFilter() != null) {
			builder.containers(ContainerManager.getContainersBySubstring(broker.getContainerFilter()));
		}

		return builder.build();
	}
}
