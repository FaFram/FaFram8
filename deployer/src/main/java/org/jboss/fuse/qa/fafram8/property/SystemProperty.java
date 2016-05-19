package org.jboss.fuse.qa.fafram8.property;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * System property class. This class manages the system and external properties.
 * Created by avano on 20.8.15.
 */
@Slf4j
public class SystemProperty {
	private static SystemProperty instance = null;

	@Getter
	// Set of defined system properties - used to clear all the properties during shutdown
	private static Set<String> systemProperties = null;

	// External properties from our and user's fafram.properties file
	private static Properties externalProperties = null;

	/**
	 * Constructor.
	 */
	protected SystemProperty() {
	}

	/**
	 * Gets the instance.
	 *
	 * @return instance
	 */
	public static SystemProperty getInstance() {
		if (instance == null) {
			instance = new SystemProperty();
			systemProperties = new HashSet<>();
			externalProperties = new Properties();
		}

		return instance;
	}

	/**
	 * Sets the system property and adds it to the set.
	 *
	 * @param property property
	 * @param value value
	 */
	public static void set(String property, String value) {
		// Force the initialization
		SystemProperty.getInstance();

		// Check if such property exists - if yes do nothing
		if (System.getProperty(property) == null) {
			log.trace(String.format("Setting system property %s to value '%s'", property, value));
			System.setProperty(property, value);
			getSystemProperties().add(property);
		}
	}

	/**
	 * Force sets the system property even if the property exists already.
	 *
	 * @param property property
	 * @param value value
	 */
	public static void forceSet(String property, String value) {
		// Clear the property first and then re-use the set method
		System.clearProperty(property);
		set(property, value);
	}

	/**
	 * Clears all set properties.
	 */
	public static void clearAllProperties() {
		// Force the initialization - for example when the unzip fails
		SystemProperty.getInstance();

		for (String prop : getSystemProperties()) {
			System.clearProperty(prop);
			log.trace("System property " + prop + " cleared.");
		}

		// Clear all the properties at the end so that they will not stay here when executing multiple tests
		systemProperties.clear();
		externalProperties.clear();
	}

	/**
	 * Getter.
	 *
	 * @return remote host
	 */
	public static String getHost() {
		return System.getProperty(FaframConstant.HOST, "localhost");
	}

	/**
	 * Getter.
	 *
	 * @return host ssh port
	 */
	public static int getHostPort() {
		return Integer.parseInt(System.getProperty(FaframConstant.HOST_PORT, "22"));
	}

	/**
	 * Getter.
	 *
	 * @return host login
	 */
	public static String getHostUser() {
		return System.getProperty(FaframConstant.HOST_USER, "fuse");
	}

	/**
	 * Getter.
	 *
	 * @return host password
	 */
	public static String getHostPassword() {
		return System.getProperty(FaframConstant.HOST_PASSWORD, "fuse");
	}

	/**
	 * Getter.
	 *
	 * @return fuse user
	 */
	public static String getFuseUser() {
		return System.getProperty(FaframConstant.FUSE_USER, "fafram");
	}

	/**
	 * Getter.
	 *
	 * @return fuse password
	 */
	public static String getFusePassword() {
		return System.getProperty(FaframConstant.FUSE_PASSWORD, "fafram");
	}

	/**
	 * Getter.
	 *
	 * @return product group id
	 */
	public static String getFuseGroup() {
		return System.getProperty(FaframConstant.FUSE_GROUP);
	}

	/**
	 * Getter.
	 *
	 * @return product artifact id
	 */
	public static String getFuseId() {
		return System.getProperty(FaframConstant.FUSE_ID);
	}

	/**
	 * Getter.
	 *
	 * @return product version
	 */
	public static String getFuseVersion() {
		// First try to parse the fuse version from the zip (with standard naming)
		if (getFuseZip() != null && (getFuseZip().contains("jboss-fuse-full") || getFuseZip().contains("jboss-a-mq")
				|| getFuseZip().contains("jboss-fuse-karaf"))) {
			final String fileName = StringUtils.substringAfterLast(getFuseZip(), "/");
			final Pattern regex = Pattern.compile("[0-9]\\.[0-9]\\.[0-9]\\.redhat\\-[0-9][0-9][0-9]");
			final Matcher matcher = regex.matcher(fileName);
			if (matcher.find()) {
				return matcher.group();
			}
		}
		return System.getProperty(FaframConstant.FUSE_VERSION);
	}

	/**
	 * Getter.
	 *
	 * @return product url
	 */
	public static String getFuseZip() {
		return System.getProperty(FaframConstant.FUSE_ZIP);
	}

	/**
	 * Getter.
	 *
	 * @return karaf start wait time
	 */
	public static int getStartWaitTime() {
		return Integer.parseInt(System.getProperty(FaframConstant.START_WAIT_TIME, "120"));
	}

	/**
	 * Getter.
	 *
	 * @return karaf stop wait time
	 */
	public static int getStopWaitTime() {
		return Integer.parseInt(System.getProperty(FaframConstant.STOP_WAIT_TIME, "60"));
	}

	/**
	 * Getter.
	 *
	 * @return fabric provision wait time
	 */
	public static int getProvisionWaitTime() {
		return Integer.parseInt(System.getProperty(FaframConstant.PROVISION_WAIT_TIME, "300"));
	}

	/**
	 * Getter.
	 *
	 * @return patch install wait time
	 */
	public static int getPatchWaitTime() {
		return Integer.parseInt(System.getProperty(FaframConstant.PATCH_WAIT_TIME, "120"));
	}

	/**
	 * Getter.
	 *
	 * @return keep folder
	 */
	public static boolean isKeepFolder() {
		return System.getProperty(FaframConstant.KEEP_FOLDER) != null;
	}

	/**
	 * Getter.
	 *
	 * @return fafram folder name
	 */
	public static String getFaframFolder() {
		return System.getProperty(FaframConstant.FAFRAM_FOLDER, "fafram");
	}

	/**
	 * Getter.
	 *
	 * @return fabric flag
	 */
	public static boolean isFabric() {
		return System.getProperty(FaframConstant.FABRIC) != null;
	}

	/**
	 * Getter.
	 *
	 * @return fabric settings
	 */
	public static String getFabric() {
		return System.getProperty(FaframConstant.FABRIC);
	}

	/**
	 * Getter.
	 *
	 * @return fuse path of the first root container it finds, system property otherwise
	 */
	public static String getFusePath() {
		for (Container c : ContainerManager.getContainerList()) {
			if (c instanceof RootContainer) {
				return c.getFusePath();
			}
		}
		log.warn("No root container found, returning deprecated system property");
		return System.getProperty(FaframConstant.FUSE_PATH);
	}

	/**
	 * Getter.
	 *
	 * @return patch
	 */
	public static String getPatch() {
		return System.getProperty(FaframConstant.PATCH);
	}

	/**
	 * Getter.
	 *
	 * @return absolute path to working directory
	 */
	public static String getWorkingDirectory() {
		return System.getProperty(FaframConstant.WORKING_DIRECTORY, "");
	}

	/**
	 * Getter.
	 *
	 * @return patch directory
	 */
	public static String getPatchDir() {
		return System.getProperty(FaframConstant.PATCH_DIR, "/home/fuse/patches");
	}

	/**
	 * Getter.
	 *
	 * @return skip default user flag
	 */
	public static boolean skipDefaultUser() {
		return System.getProperty(FaframConstant.SKIP_DEFAULT_USER) != null;
	}

	/**
	 * Getter.
	 *
	 * @return patch standalone flag
	 */
	public static boolean patchStandalone() {
		return System.getProperty(FaframConstant.PATCH_STANDALONE) != null;
	}

	/**
	 * Getter.
	 *
	 * @return skip default jvm opts flag
	 */
	public static boolean skipDefaultJvmOpts() {
		return System.getProperty(FaframConstant.SKIP_DEFAULT_JVM_OPTS) != null;
	}

	/**
	 * Getter.
	 *
	 * @return skip default jvm opts flag
	 */
	public static boolean suppressStart() {
		return System.getProperty(FaframConstant.SUPPRESS_START) != null;
	}

	/**
	 * Getter.
	 *
	 * @return archive target path
	 */
	public static String getArchiveTarget() {
		return System.getProperty(FaframConstant.ARCHIVE_TARGET, "target/archived");
	}

	/**
	 * Getter.
	 *
	 * @return archive pattern
	 */
	public static String getArchivePattern() {
		return System.getProperty(FaframConstant.ARCHIVE_PATTERN, "data/log/*,");
	}

	/**
	 * Getter.
	 *
	 * @return base directory path
	 */
	public static String getBaseDir() {
		return System.getProperty(FaframConstant.BASE_DIR);
	}

	/**
	 * Getter.
	 *
	 * @return whether to keep OS nodes after test or release them
	 */
	public static boolean isKeepOsResources() {
		return Boolean.parseBoolean(System.getProperty(FaframConstant.KEEP_OS_RESOURCES, "false"));
	}

	/**
	 * Getter.
	 *
	 * @return clean property
	 */
	public static Boolean isClean() {
		return Boolean.parseBoolean(System.getProperty(FaframConstant.CLEAN, "true"));
	}

	/**
	 * Get Fabric "topology" config path from fabric.config.path system property used by Configuration Parser.
	 *
	 * @return config path
	 */
	public static String getFabricConfigPath() {
		return System.getProperty(FaframConstant.FABRIC_CONFIG_PATH);
	}

	/**
	 * Get Fafram property config path from fafram.config.path system property.
	 *
	 * @return config path
	 */
	public static String getFaframConfigUrl() {
		return System.getProperty(FaframConstant.FAFRAM_CONFIG_URL);
	}

	/**
	 * Getter.
	 *
	 * @return broker start wait time
	 */
	public static int getBrokerStartWaitTime() {
		return Integer.parseInt(System.getProperty(FaframConstant.BROKER_START_WAIT_TIME, "60"));
	}

	/**
	 * Get system property for openstack server name prefix.
	 *
	 * @return server name prefix set by system property
	 */
	public static String getOpenstackServerNamePrefix() {
		if (getExternalProperty(FaframConstant.OPENSTACK_NAME_PREFIX) != null) {
			return getExternalProperty(FaframConstant.OPENSTACK_NAME_PREFIX);
		}
		return "fafram8";
	}

	/**
	 * Getter.
	 *
	 * @return path to iptables configuration file
	 */
	public static String getIptablesConfFilePath() {
		return System.getProperty(FaframConstant.IPTABLES_CONF_FILE_PATH, "");
	}

	/**
	 * Getter.
	 *
	 * @return offline property
	 */
	public static Boolean isOffline() {
		return Boolean.parseBoolean(System.getProperty(FaframConstant.OFFLINE, "false"));
	}

	/**
	 * Getter.
	 *
	 * @return provider property
	 */
	public static String getProvider() {
		return System.getProperty(FaframConstant.PROVIDER, "StaticProvider");
	}

	/**
	 * Getter.
	 *
	 * @return skipBrokerWait property
	 */
	public static Boolean skipBrokerWait() {
		return Boolean.parseBoolean(System.getProperty(FaframConstant.SKIP_BROKER_WAIT, "false"));
	}

	/**
	 * Getter.
	 *
	 * @return default root name
	 */
	public static String getDefaultRootName() {
		return System.getProperty(FaframConstant.DEFAULT_ROOT_NAME, "root");
	}

	/**
	 * Getter.
	 *
	 * @return openstack wait time
	 */
	public static int getOpenstackWaitTime() {
		return Integer.parseInt(System.getProperty(FaframConstant.OPENSTACK_WAIT_TIME, "300"));
	}

	/**
	 * Getter.
	 *
	 * @return java home path
	 */
	public static String getJavaHome() {
		return System.getProperty(FaframConstant.JAVA_HOME, "");
	}

	/**
	 * Getter.
	 *
	 * @return keepContainers property
	 */
	public static boolean isKeepContainers() {
		return System.getProperty(FaframConstant.KEEP_CONTAINERS) != null
				&& Boolean.parseBoolean(System.getProperty(FaframConstant.KEEP_CONTAINERS));
	}

	/**
	 * Getter.
	 *
	 * @return comma seprated list of commands for execution
	 */
	public static String getAdditionalCommands() {
		return System.getProperty(FaframConstant.ADDITIONAL_COMMANDS, "");
	}

	/**
	 * Getter.
	 *
	 * @return flag if default repositories should be used
	 */
	public static boolean useDefaultRepositories() {
		return System.getProperty(FaframConstant.USE_DEFAULT_REPOSITORIES) != null;
	}

	/**
	 * Checks if keepAll property is set if it is then it sets keepOsResources and keepContainers properties to true.
	 */
	public static void checkKeepAllProperty() {
		// Only check if the property is set
		if (System.getProperty(FaframConstant.KEEP_ALL) != null) {
			forceSet(FaframConstant.KEEP_CONTAINERS, "true");
			forceSet(FaframConstant.KEEP_OS_RESOURCES, "true");
		}
	}

	/**
	 * Getter.
	 *
	 * @return no.threads property
	 */
	public static boolean isNoThreads() {
		return System.getProperty(FaframConstant.NO_THREADS) != null;
	}

	/**
	 * Gets the external property from the property file.
	 *
	 * @param property property key
	 * @return property value
	 */
	public static String getExternalProperty(String property) {
		// Force the initialization
		SystemProperty.getInstance();

		// If there is defined system property, use that first
		if (System.getProperty(property) != null) {
			return System.getProperty(property);
		}

		if (externalProperties.isEmpty()) {
			externalProperties = initProperties();
		}

		return externalProperties.getProperty(property);
	}

	/**
	 * Inits the fafram properties - loads the properties from fafram.properties and merge user changes.
	 *
	 * @return properties instance
	 */
	public static Properties initProperties() {
		final Properties p = new Properties();

		try {
			final List<URL> urls = new LinkedList<>();
			// If defined get property file from SystemProperty
			if (SystemProperty.getFaframConfigUrl() != null) {
				urls.add(new URL(SystemProperty.getFaframConfigUrl()));
				log.info("Loading Fafram configuration file on path: " + SystemProperty.getFaframConfigUrl());
			}
			// Get the property files URLs from classpath
			urls.addAll(Collections.list(SystemProperty.class.getClassLoader().getResources("fafram.properties")));

			log.debug("Fafram config path: " + urls.toString());

			// Merge user-defined properties with default properties
			// User-defined changes should be the first file and the fafram properties should be the second file
			// So we first add all our properties and then overwrite the properties defined by the user
			for (int i = urls.size() - 1; i >= 0; i--) {
				final URL u = urls.get(i);
				try (InputStream is = u.openStream()) {
					// Load the properties
					p.load(is);
				}
			}
		} catch (IOException e) {
			log.error("IOException while loading properties" + e);
		}

		return p;
	}
}
