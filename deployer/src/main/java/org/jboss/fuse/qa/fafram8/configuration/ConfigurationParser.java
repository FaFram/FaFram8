package org.jboss.fuse.qa.fafram8.configuration;

import org.jboss.fuse.qa.fafram8.cluster.xml.toplevel.FaframModel;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.File;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Fafram8 XML configuration parser class.
 * <p/>
 * Created by mmelko on 9/8/15.
 */
@Slf4j
public class ConfigurationParser {
	//Parsed object cluster representation.
	private FaframModel faframModel;

	//Unique name incrementer.
	private int uniqueNameIncrement = 0;

	@Setter
	private String path;

	/**
	 * Constructor.
	 */
	public ConfigurationParser() {
	}

	/**
	 * Parse referenced Fafram8 XML configuration.
	 *
	 * @param path path to Fafram8 XML configuration
	 * @throws JAXBException if an error was encountered while creating the Unmarshaller object.
	 */
	public void parseConfigurationFile(String path) throws JAXBException {
		log.info("Configuration parser started.");

		log.trace("Creating unmarshaller.");
		final JAXBContext jaxbContext = JAXBContext.newInstance(FaframModel.class);
		final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		log.trace("Unmarshalling cluster model from " + path);
		faframModel = (FaframModel) jaxbUnmarshaller.unmarshal(new File(path));
		faframModel.getContainersModel().buildContainers();
		//TODO(ecervena): provisional debug logging
//		for (ContainerModel containerModel : faframModel.getContainerModelList()) {
//			log.debug(containerModel.toString());
//		}
	}

	/**
	 * Call of this method will set parsed framework configuration and build container
	 * objects parsed from Fafram8 XML configuration.
	 */
	public void buildContainers() {
		log.debug("Building containers.");

//		for (ContainerModel containerModel : faframModel.getContainerModelList()) {
//			log.info(containerModel.toString());
//			for (int i = 1; i <= containerModel.getInstances(); i++) {
//
//				Container container = null;
//
//				switch (containerModel.getContainerType()) {
//					case "root": {
//						final RootContainer.RootBuilder builder = RootContainer.builder().name(returnUniqueName(containerModel));
//						if (containerModel.getUsername() != null) {
//							builder.user(containerModel.getUsername());
//						}
//						if (containerModel.getPassword() != null) {
//							builder.password(containerModel.getPassword());
//						}
//						if (containerModel.isFabric()) {
//							builder.withFabric();
//						}
//
//						final Node node = Node.builder().host(containerModel.getNode().getHost())
//								.username(containerModel.getNode().getUsername())
//								.password(containerModel.getNode().getPassword())
//								.build();
//						builder.node(node);
//						container = builder.build();
//						break;
//					}
//					case "ssh": {
//						final SshContainer.SshBuilder builder = SshContainer.builder().name(returnUniqueName(containerModel));
//						final Node node = Node.builder().host(containerModel.getNode().getHost())
//								.username(containerModel.getNode().getUsername())
//								.password(containerModel.getNode().getPassword())
//								.build();
//						builder.node(node);
//						final Container parentContainer = ContainerManager.getContainer(containerModel.getParentContainer());
//						if (parentContainer == null) {
//							throw new FaframException("Parent container does not exists.");
//						}
//						builder.parent(parentContainer);
//						container = builder.build();
//						break;
//					}
//					case "child": {
//						final ChildContainer.ChildBuilder builder = ChildContainer.builder().name(returnUniqueName(containerModel));
//						final Container parentContainer = ContainerManager.getContainer(containerModel.getParentContainer());
//						if (parentContainer == null) {
//							throw new FaframException("Parent container does not exists.");
//						}
//						builder.parent(parentContainer);
//						container = builder.build();
//						break;
//					}
//					default:
//						break;
//				}
//
//				if (container != null) {
//					ContainerManager.getContainerList().add(container);
//				}
//			}
//			resetUniqueNameIncrement();

//		}
	}
}

