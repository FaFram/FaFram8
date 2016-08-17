package org.jboss.fuse.qa.fafram8.cluster.xml.container;

import org.jboss.fuse.qa.fafram8.cluster.xml.toplevel.BundlesModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.toplevel.CommandsModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.util.JvmMemoryOptsModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.util.NodeModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.util.ProfilesModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.util.UsersModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import lombok.Getter;
import lombok.Setter;

/**
 * XmlRootContainerModel represents the XML element <root>.
 * Created by avano on 26.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class XmlRootContainerModel {
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

	@XmlAttribute
	private boolean fabric;

	@XmlElement
	private JvmMemoryOptsModel jvmMemoryOpts;

	@XmlElement
	private NodeModel node;

	@XmlElement
	private String workingDir;

	@XmlElement
	private UsersModel usersModel;

	@XmlElement(name = "bundles")
	private BundlesModel bundlesModel;
}
