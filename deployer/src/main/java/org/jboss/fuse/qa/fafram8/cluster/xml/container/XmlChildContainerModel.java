package org.jboss.fuse.qa.fafram8.cluster.xml.container;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.Getter;
import lombok.Setter;

/**
 * XmlChildContainerModel represents the XML element <child>.
 * Created by avano on 26.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class XmlChildContainerModel extends XmlContainerModel {
	@XmlElement
	private String parentName;

	@XmlElement
	private String version;

	@XmlElement
	private String jmxUser;

	@XmlElement
	private String jmxPassword;

	@XmlElement
	private String resolver;

	@XmlElement
	private String manualIp;
}
