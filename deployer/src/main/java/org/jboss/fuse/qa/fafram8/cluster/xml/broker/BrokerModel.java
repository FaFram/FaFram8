package org.jboss.fuse.qa.fafram8.cluster.xml.broker;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import lombok.Getter;
import lombok.Setter;

/**
 * Class representing the <broker> element.
 * Created by avano on 28.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class BrokerModel {
	@XmlAttribute
	private String id;

	@XmlAttribute
	private String ref;

	@XmlAttribute
	private boolean template;

	@XmlElement
	private String name;

	@XmlElement
	private boolean ssl = false;

	@XmlElement
	private String kind;

	@XmlElement
	private String group;

	@XmlElement
	private String data;

	@XmlElement
	private String parentProfile;

	@XmlElement(name = "networks")
	private NetworksModel networksModel;

	@XmlElement(name = "pids")
	private PidsModel pidsModel;

	@XmlElement
	private String containerFilter;
}
