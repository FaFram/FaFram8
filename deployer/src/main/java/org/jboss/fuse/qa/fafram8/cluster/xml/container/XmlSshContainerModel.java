package org.jboss.fuse.qa.fafram8.cluster.xml.container;

import org.jboss.fuse.qa.fafram8.cluster.xml.util.NodeModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.Getter;
import lombok.Setter;

/**
 * XmlSshContainerModel represents the XML element <ssh>.
 * Created by avano on 26.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class XmlSshContainerModel extends XmlContainerModel {
	@XmlElement
	private NodeModel node;

	@XmlElement
	private String version;

	@XmlElement
	private String env;

	@XmlElement
	private String workingDir;

	@XmlElement
	private String zookeeperPassword;

	@XmlElement
	private String resolver;

	@XmlElement
	private String manualIp;

	@XmlElement
	private String privateKey;

	@XmlElement
	private String passPhrase;

	@XmlElement
	private String proxyUri;

	@XmlElement
	private int minPort;

	@XmlElement
	private int maxPort;

	@XmlElement
	private String parentName;
}
