package org.jboss.fuse.qa.fafram8.cluster.xml.containers;

import org.jboss.fuse.qa.fafram8.cluster.xml.toplevel.BundlesModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.util.UsersModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.util.JvmMemoryOptsModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.util.NodeModel;

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
public class XmlRootContainerModel extends XmlContainerModel {
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
