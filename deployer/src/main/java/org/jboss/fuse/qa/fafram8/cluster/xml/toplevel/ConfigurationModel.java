package org.jboss.fuse.qa.fafram8.cluster.xml.toplevel;

import org.jboss.fuse.qa.fafram8.cluster.xml.util.OneAttributeModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * ConfigurationModel represents the XML element <configuration>.
 * Created by avano on 26.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@ToString
@Getter
@Setter
public class ConfigurationModel {
	@XmlElement
	private String provider;

	@XmlElement
	private String hostUser;

	@XmlElement
	private String hostPassword;

	@XmlElement
	private String hostPort;

	@XmlElement
	private String fuseUser;

	@XmlElement
	private String fusePassword;

	@XmlElement
	private String fuseZip;

	@XmlElement
	private String startWaitTime;

	@XmlElement
	private String stopWaitTime;

	@XmlElement
	private String provisionWaitTime;

	@XmlElement
	private boolean skipBrokerWait;

	@XmlElement
	private boolean offline;

	@XmlElement
	private OneAttributeModel iptables;

	@XmlElement
	private OneAttributeModel jdk;

	@XmlElement
	private boolean withoutDefaultUser;
}
