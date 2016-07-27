package org.jboss.fuse.qa.fafram8.cluster.xml.containers;

import org.jboss.fuse.qa.fafram8.cluster.xml.toplevel.CommandsModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.util.ProfilesModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import lombok.Getter;
import lombok.Setter;

/**
 * XmlChildContainerModel holds the common fields of all (root/child/ssh) container models.
 * Created by avano on 26.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class XmlContainerModel {
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
	private int instances;

	@XmlAttribute
	private boolean template = false;
}
