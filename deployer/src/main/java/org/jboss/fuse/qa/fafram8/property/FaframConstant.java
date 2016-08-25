package org.jboss.fuse.qa.fafram8.property;

/**
 * Fafram system properties names.
 * Created by avano on 20.8.15.
 */
public final class FaframConstant {

	/**
	 * Private constructor.
	 */
	private FaframConstant() {
	}

	// Host IP address
	public static final String HOST = "host";

	// Host port
	public static final String HOST_PORT = "host.port";

	// Host login
	public static final String HOST_USER = "host.user";

	// Host password
	public static final String HOST_PASSWORD = "host.password";

	// Fuse user
	public static final String FUSE_USER = "fuse.user";

	// Fuse password
	public static final String FUSE_PASSWORD = "fuse.password";

	// Product group id
	public static final String FUSE_GROUP = "fuse.group";

	// Product artifact id
	public static final String FUSE_ID = "fuse.id";

	// Product version
	public static final String FUSE_VERSION = "fuse.version";

	// Product URL
	public static final String FUSE_ZIP = "fuse.zip";

	// Full path to unzipped product directory
	public static final String FUSE_PATH = "fuse.path";

	// Karaf start wait time
	public static final String START_WAIT_TIME = "start.wait.time";

	// Karaf stop wait time
	public static final String STOP_WAIT_TIME = "stop.wait.time";

	// Fabric provision wait time
	public static final String PROVISION_WAIT_TIME = "provision.wait.time";

	// Patch install wait time
	public static final String PATCH_WAIT_TIME = "patch.wait.time";

	// Name of the working folder on remote host
	public static final String FAFRAM_FOLDER = "fafram.folder";

	// Patch
	public static final String PATCH = "patch";

	// Fabric
	public static final String FABRIC = "fabric";

	// Special working directory on machine where fafram folder should be created.
	// Use when working on perf machines.
	public static final String WORKING_DIRECTORY = "fafram.working.directory";

	// If "true" OpenStackProvisionManager will not release OS nodes after test
	public static final String KEEP_OS_RESOURCES = "keep.os.resources";

	// Keep folder
	public static final String KEEP_FOLDER = "keepFolder";

	// Keep containers running after the test
	public static final String KEEP_CONTAINERS = "keepContainers";

	// Sets KEEP_OS_RESOURCES and KEEP_CONTAINERS to true
	public static final String KEEP_ALL = "keepAll";

	// Patches folder
	public static final String PATCH_DIR = "fafram.patch.dir";

	// Skip default user flag
	public static final String SKIP_DEFAULT_USER = "fafram.skip.default.user";

	// Patch standalone before fabric flag
	public static final String PATCH_STANDALONE = "fafram.patch.standalone";

	// If remote cluster should be cleaned
	public static final String CLEAN = "fafram.clean";

	// Skip default jvm opts modifier flag
	public static final String SKIP_DEFAULT_JVM_OPTS = "fafram.skip.default.jvm.opts";

	// Suppress start flag
	public static final String SUPPRESS_START = "fafram.suppress.start";

	// Archive files target path
	public static final String ARCHIVE_TARGET = "fafram.archive.target";

	// Archive files pattern
	public static final String ARCHIVE_PATTERN = "fafram.archive.pattern";

	// Base directory path
	public static final String BASE_DIR = "fafram.base.dir";

	// Broker start wait time
	public static final String BROKER_START_WAIT_TIME = "broker.start.wait.time";

	// Jira URL
	public static final String JIRA_URL = "jira.url";

	// Fabric "topology" configuration path for Configuration Parser
	public static final String FABRIC_CONFIG_PATH = "fabric.config.path";

	// Jira user
	public static final String JIRA_USER = "jira.user";

	// Jira password
	public static final String JIRA_PASSWORD = "jira.password";

	// Openstack url
	public static final String OPENSTACK_URL = "openstack.url";

	// Openstack tenant
	public static final String OPENSTACK_TENANT = "openstack.tenant";

	// Openstack user
	public static final String OPENSTACK_USER = "openstack.user";

	// Openstack password
	public static final String OPENSTACK_PASSWORD = "openstack.password";

	// Openstack image UUID
	public static final String OPENSTACK_IMAGE = "openstack.image";

	// Openstack instance name prefix
	public static final String OPENSTACK_NAME_PREFIX = "openstack.namePrefix";

	// Openstack flavor
	public static final String OPENSTACK_FLAVOR = "openstack.flavor";

	// Openstack keypair
	public static final String OPENSTACK_KEYPAIR = "openstack.keypair";

	// Openstack address type
	public static final String OPENSTACK_ADDRESS_TYPE = "openstack.addressType";

	// Comma separated list of openstack tenant networks
	public static final String OPENSTACK_NETWORKS = "openstack.networks";

	// Openstack Floating IP pool ID
	public static final String OPENSTACK_FLOATING_IP_POOL = "openstack.floatingIpPool";

	// If offline environment should be used (a.k.a turn off internet in cluster)
	public static final String OFFLINE = "fafram.offline";

	// Property used for specifying local path to custom iptables configuration file that will be copied and executed on
	// remote hosts
	public static final String IPTABLES_CONF_FILE_PATH = "iptables.conf.file.path";

	//Specify infrastructure provider class name a.k.a. provisioning provider
	// TODO(rjakubco): I stole this unused property for setting the class name that is required in fafram. Is it needed for something else?
	// This should be just internal property for FaFram
	public static final String PROVIDER = "provider.class.name";

	// Skip broker waiting
	public static final String SKIP_BROKER_WAIT = "fafram.skip.broker.wait";

	// Default root container name
	public static final String DEFAULT_ROOT_NAME = "fafram.default.root.name";

	// Openstack machine boot wait time
	public static final String OPENSTACK_WAIT_TIME = "openstack.wait.time";

	// Path to java home that will be used for Fuse (root and containers)
	public static final String JAVA_HOME = "fafram.java.home";

	// Fafram properties file path
	public static final String FAFRAM_CONFIG_URL = "fafram.config.url";

	// Semicolon separated list of commands that should be executed
	public static final String ADDITIONAL_COMMANDS = "additional.commands";

	// Flag if default repositories should be used
	public static final String CLEAR_DEFAULT_REPOSITORIES = "clear.default.repositories";

	// Property used when you don't want to use threads for creating/destroying containers
	public static final String WITH_THREADS = "with.threads";

	// Property for setting if fafram should try to restart container if it ended in failed stated
	public static final String RETRY_PROVISIONING = "retry.provisioning";

	// Flag that ssh containers do not have public ip
	public static final String WITHOUT_PUBLIC_IP = "without.public.ip";
}
