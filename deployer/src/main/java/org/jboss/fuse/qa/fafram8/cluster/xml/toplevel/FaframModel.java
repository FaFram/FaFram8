package org.jboss.fuse.qa.fafram8.cluster.xml.toplevel;

import org.jboss.fuse.qa.fafram8.cluster.xml.util.UsersModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by ecervena on 1/11/16.
 * <p/>
 * ClusterModel class represents XML to object model mapping. Cluster XML configuration is mapped to this class.
 * Fafram containerList is initialized according to clusterModel.
 */

@XmlRootElement(name = "fafram", namespace = "urn:org.jboss.fuse.qa")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@ToString
public class FaframModel {
	@XmlElement(name = "configuration")
	private ConfigurationModel configurationModel;

	@XmlElement(name = "users")
	private UsersModel usersModel;

	@XmlElement(name = "commands")
	private CommandsModel commandsModel;

	@XmlElement(name = "bundles")
	private BundlesModel bundlesModel;

	@XmlElement(name = "ensemble")
	private EnsembleModel ensembleModel;

	@XmlElement(name = "containers")
	private ContainersModel containersModel;

	@XmlElement(name = "brokers")
	private BrokersModel brokersModel;
}
