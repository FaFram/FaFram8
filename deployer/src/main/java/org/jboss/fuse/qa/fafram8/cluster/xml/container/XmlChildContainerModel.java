package org.jboss.fuse.qa.fafram8.cluster.xml.container;

import org.jboss.fuse.qa.fafram8.cluster.xml.toplevel.CommandsModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.util.ProfilesModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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
public class XmlChildContainerModel {
	@XmlElement
	private String name;

	@XmlElement
	private String jvmOpts;

	@XmlElement(name = "commands")
	private CommandsModel commandsModel;

	@XmlElement(name = "profiles")
	private ProfilesModel profilesModel;

	@XmlAttribute
	private String id;

	@XmlAttribute
	private String ref;

	@XmlAttribute
	private int instances = 1;

	@XmlAttribute
	private boolean template = false;

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

	@XmlAttribute
	private String sameNodeAs;
}
