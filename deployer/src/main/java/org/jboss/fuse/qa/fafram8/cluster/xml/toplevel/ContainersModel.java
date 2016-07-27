package org.jboss.fuse.qa.fafram8.cluster.xml.toplevel;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.xml.util.UserModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.containers.XmlChildContainerModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.containers.XmlContainerModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.containers.XmlRootContainerModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.containers.XmlSshContainerModel;
import org.jboss.fuse.qa.fafram8.exception.FaframException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * ContainerModel class represents XML to object model mapping.
 * Containers in XML configuration are mapped to this class.
 * <p/>
 * Created by ecervena on 1/11/16.
 */
@XmlRootElement(name = "containers")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@ToString
@Slf4j
public class ContainersModel {
	@XmlElements({
			@XmlElement(name="root", type=XmlRootContainerModel.class),
			@XmlElement(name="child", type=XmlChildContainerModel.class),
			@XmlElement(name="ssh", type=XmlSshContainerModel.class),
	})
	private List<XmlContainerModel> containerModelList;

	public void x() {
		System.out.println(containerModelList.size());
	}

	public void buildContainers() {
		for (XmlContainerModel xmlContainerModel : containerModelList) {
			if (!xmlContainerModel.isTemplate()) {
				System.out.println(buildRootContainer((XmlRootContainerModel) xmlContainerModel).toString());
			}
		}
	}

	public Container buildRootContainer(XmlRootContainerModel root) {
		RootContainer.RootBuilder builder;

		if (root.getRef() != null) {
			builder = RootContainer.builder(buildRootContainer((XmlRootContainerModel)getModel(root.getRef())));
		} else {
			builder = RootContainer.builder();
		}

		builder.name(root.getName());
		if (root.getJvmOpts() != null) {
			builder.jvmOpts(root.getJvmOpts());
		}
		if (root.getCommandsModel() != null) {
			builder.commands(root.getCommandsModel().getCommands().toArray(new String[root.getCommandsModel().getCommands().size()]));
		}
		if (root.getProfilesModel() != null) {
			builder.profiles(root.getProfilesModel().getProfiles().toArray(new String[root.getProfilesModel().getProfiles().size()]));
		}
		if (root.isFabric()) {
			builder.withFabric();
		}
		if (root.getJvmMemoryOpts() != null) {
			builder.jvmMemoryOpts(root.getJvmMemoryOpts().getXms(), root.getJvmMemoryOpts().getXmx(), root.getJvmMemoryOpts().getPermMem(),
					root.getJvmMemoryOpts().getMaxPermMem());
		}
		if (root.getNode() != null) {
			builder.node(root.getNode().createNode());
		}
		if (root.getWorkingDir() != null) {
			builder.directory(root.getWorkingDir());
		}
		if (root.getUsersModel() != null) {
			for (UserModel user : root.getUsersModel().getUsers()) {
				builder.addUser(user.getName(), user.getPassword(), user.getRoles());
			}
		}
		if (root.getBundlesModel() != null) {
			builder.bundles(root.getBundlesModel().getBundles().toArray(new String[root.getBundlesModel().getBundles().size()]));
		}
		return builder.build();
	}

	public XmlContainerModel getModel(String id) {
		for (XmlContainerModel xmlContainerModel : containerModelList) {
			if (id.equals(xmlContainerModel.getId())) {
				return xmlContainerModel;
			}
		}
		throw new FaframException("Ref " + id + " not found");
	}
//
//	public Container getBase(XmlContainerModel model) {
//		if (model instanceof XmlRootContainerModel) {
//			RootContainer.RootBuilder builder = null;
//			log.error("Root container");
//			if (model.getRef() != null) {
//				RootContainer.builder(getBase(getModel(model.getRef())));
//			} else {
//				RootContainer.builder();
//			}
//
//		}
//		return null;
//	}
}


