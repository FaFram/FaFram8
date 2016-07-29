package org.jboss.fuse.qa.fafram8.configuration;

import org.jboss.fuse.qa.fafram8.cluster.xml.toplevel.FaframModel;
import org.jboss.fuse.qa.fafram8.cluster.xml.util.UserModel;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

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
	// Fafram instance
	private Fafram fafram;

	//Parsed object cluster representation.
	private FaframModel faframModel;

	//Unique name incrementer.
	private int uniqueNameIncrement = 0;

	@Setter
	private String path;

	/**
	 * Constructor.
	 */
	public ConfigurationParser(Fafram fafram) {
		this.fafram = fafram;
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
	}

	/**
	 * Applies the configuration from the XML file.
	 * Sets system properties,
	 * build containers,
	 * build brokers,
	 * set bundles,
	 * set commands,
	 * set users,
	 * set ensemble.
	 */
	public void applyConfiguration() {
		if (faframModel.getConfigurationModel() != null) {
			faframModel.getConfigurationModel().applyConfiguration(fafram);
		}
		if (faframModel.getContainersModel() != null) {
			faframModel.getContainersModel().buildContainers();
		}
		if (faframModel.getBrokersModel() != null) {
			faframModel.getBrokersModel().buildBrokers();
		}
		if (faframModel.getBundlesModel() != null) {
			for (String s : faframModel.getBundlesModel().getBundles()) {
				fafram.bundles(s);
			}
		}
		if (faframModel.getCommandsModel() != null) {
			for (String s : faframModel.getCommandsModel().getCommands()) {
				fafram.commands(s);
			}
		}
		if (faframModel.getEnsembleModel() != null) {
			final String[] containers = faframModel.getEnsembleModel().getEnsemble().split(",");
			for (String s : containers) {
				fafram.ensemble(s);
			}
		}
		if (faframModel.getUsersModel() != null) {
			for (UserModel userModel : faframModel.getUsersModel().getUsers()) {
				fafram.addUser(userModel.getName(), userModel.getPassword(), userModel.getRoles());
			}
		}
	}
}

