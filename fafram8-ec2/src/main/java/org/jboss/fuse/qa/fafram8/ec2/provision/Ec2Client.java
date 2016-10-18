package org.jboss.fuse.qa.fafram8.ec2.provision;

import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.compute.AWSEC2TemplateOptions;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.ec2.EC2Api;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.ec2.features.InstanceApi;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * EC2 client class, which is used to work with servers on EC2. The main purpose of this client is to start/stop,
 * create/delete servers on EC2.
 *
 * Sep 15, 2016 Red Hat
 *
 * @author tplevko@redhat.com
 */
@Slf4j
@ToString
public final class Ec2Client {

	@Getter
	@Setter
	private String url;

	@Getter
	@Setter
	private String accessKey;

	@Getter
	@Setter
	private String secretKey;

	@Getter
	@Setter
	private String region;

	@Getter
	@Setter
	private String imageId;

	@Getter
	@Setter
	private String instanceType;

	@Getter
	@Setter
	private String keyPair;

	@Getter
	@Setter
	private String securityGroup;

	@Getter
	@Setter
	private String namePrefix;

	private final ComputeServiceContext computeServiceContext;
	private final ComputeService computeService;
	private final InstanceApi ec2Api;

	// Number of threads
	private static final int POOL_SIZE = 5;

	// Log wait time
	private static final int LOG_WAIT_TIME = 5;

	// Server boot timeout in seconds
	private static final int BOOT_TIMEOUT_SEC = 180;

	// List of all created EC2 nodes metadata
	private static final List<NodeMetadata> serverRegister = new LinkedList<>();
	private static final String EC2_PROPERTIES = "EC2_east.properties";
	private static final int SSH_PORT = 22;

	private Ec2Client(String url, String accessKey, String secretKey, String region, String imageId, String instanceType, String keyPair,
			String securityGroups, String namePrefix) {

		this.url = url;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.region = region;
		this.imageId = imageId;
		this.instanceType = instanceType;
		this.keyPair = keyPair;
		this.securityGroup = securityGroups;
		this.namePrefix = namePrefix;

		final ContextBuilder contextBuilder = ContextBuilder.newBuilder("aws-ec2")
				.credentials(accessKey, secretKey)
				.endpoint(url);

		computeServiceContext = contextBuilder.buildView(ComputeServiceContext.class);
		computeService = computeServiceContext.getComputeService();
		ec2Api = computeServiceContext.unwrapApi(EC2Api.class).getInstanceApiForRegion(region).get();
	}

	/**
	 * Ec2Client builder.
	 *
	 * @return OenStackClientBuilder
	 */
	public static Ec2ClientBuilder builder() {
		return new Ec2ClientBuilder();
	}

	public void spawnServersByNames(String... machineNames) throws InterruptedException, ExecutionException {
		spawnServersByNames(Arrays.asList(machineNames));
	}

	/**
	 * Calling this method spawns thread workers to create EC2 nodes in parallel.
	 *
	 * @param machineNames list of machines names that should be spawned
	 * @return set of created servers
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public Set<NodeMetadata> spawnServersByNames(List<String> machineNames) throws InterruptedException, ExecutionException {

		log.debug("Initializing ServerInvokerPool.");
		final Set<Future<NodeMetadata>> futureServerSet = new HashSet<>();
		final Set<NodeMetadata> servers = new HashSet<>();
		final ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
		for (String name : machineNames) {
			log.trace("Spawning invoker thread for container: " + name);
			final Callable<NodeMetadata> callable = new ServerInvoker(name, this);
			final Future<NodeMetadata> future = executor.submit(callable);
			futureServerSet.add(future);
		}
		executor.shutdown();
		log.trace("Waiting for ServerInvoker threads to finish a job.");
		try {
			while (!executor.awaitTermination(LOG_WAIT_TIME, TimeUnit.SECONDS)) {
				log.trace("Waiting for ServerInvoker threads to finish a job.");
			}
		} catch (InterruptedException ie) {
			throw new RuntimeException("Invoker interupted.", ie);
		}
		log.debug("ServerInvokerPool done.");

		for (Future<NodeMetadata> future : futureServerSet) {
			final NodeMetadata server = future.get();
			log.debug("Spawned server " + server.getName());
			servers.add(server);
			serverRegister.add(server);
		}

		return servers;
	}

	/**
	 * Spawns single EC2 server with provided properties.
	 *
	 * @param serverName the name of newly created server.
	 * @return
	 */
	public NodeMetadata spawnNewServer(String serverName) {

		final NodeMetadata initialNodeMetadata;
		final TemplateBuilder templateBuilder = computeService.templateBuilder();
		final String fullId = region + "/" + imageId;
		final String publicAddress;

		final Template template = templateBuilder
				.hardwareId(instanceType)
				.locationId(region)
				.imageId(fullId)
				.options(buildTemplateOptions())
				.build();

		log.debug("Creating {} node from template: {}",
				fullId, template);

		try {
			initialNodeMetadata = createNode(template, serverName);
			publicAddress = Iterables.getFirst(initialNodeMetadata.getPublicAddresses(), null);

			log.info("Started node '{}' from image {}, its public IP address is {}",
					serverName, imageId, publicAddress);
		} catch (RunNodesException e) {
			throw new RuntimeException("Unable to create aws-ec2 node from template " + template, e);
		}

		try {
			verifyServerStarted(publicAddress);
		} catch (Exception e) {
			computeService.destroyNode(initialNodeMetadata.getId());
			throw e;
		}
		serverRegister.add(initialNodeMetadata);
		return initialNodeMetadata;
	}

	/**
	 * Release allocated EC2 resources. Method will delete created servers.
	 */
	public void releaseResources() {
		log.info("Releasing allocated EC2 resources.");

		for (int i = serverRegister.size() - 1; i >= 0; i--) {
			final NodeMetadata server = serverRegister.get(i);
			log.info("Terminating server: " + server.getName());

			ec2Api.terminateInstancesInRegion(region, server.getId().split("/")[1]);

			serverRegister.remove(i);
		}

		log.info("All Ec2 resources have been released successfully");
	}

	/**
	 * Method for getting Server from server register of created Servers by this Ec2 client. This method checks that is
	 * only one Server with that name.
	 *
	 * @param serverName server name to be found
	 * @return found server
	 */
	public NodeMetadata getServerFromRegister(String serverName) {

		final List<NodeMetadata> registerList = new LinkedList<>();
		for (NodeMetadata s : serverRegister) {

			if (s.getName().contains(serverName)) {
				registerList.add(s);
			}
		}

		// Check that there are not two servers with the same name in the register
		if (registerList.size() != 1) {
			for (Object obj : registerList) {
				log.error("Server with not unique name detected in server register: ", obj.toString());
			}
			throw new RuntimeException(
					"Server name is not unique in server register. More than 1 (" + registerList.size()
					+ ") server with specified name: " + serverName + " detected");
		} else {
			// Now check that the name is unique in the Ec2 (e.g. old resources still exists)
			final List<NodeMetadata> equalsList = getServers(registerList.get(0).getName());
			if (equalsList.size() != 1) {
				for (Object obj : equalsList) {
					log.error("Server with not unique name detected on Ec2: ", obj.toString());
				}
				throw new RuntimeException(
						"Server name is not uniqueon Ec2. More than 1 (" + equalsList.size() + ") server with specified name: " + serverName + " detected");
			} else {
				return registerList.get(0);
			}
		}
	}

	/**
	 * Gets the count of the servers with name ("name" + "rand_has"). Used to check if there are already some servers
	 * with defined name.
	 *
	 * @param name container name
	 * @return list of servers with given name
	 */
	public List<NodeMetadata> getServers(final String name) {

		final List<NodeMetadata> serverList = new ArrayList<>();
		final Predicate<ComputeMetadata> filter = new Predicate<ComputeMetadata>() {
			@Override
			public boolean apply(ComputeMetadata input) {
				if (input.getName().isEmpty() || input.getId().isEmpty()) {
					return false;
				}
				return (input.getId().contains(region) && input.getName().equals(name));
			}
		};

		final Set<? extends NodeMetadata> servers = computeService.listNodesDetailsMatching(filter);

		for (NodeMetadata server : servers) {

			log.info("name: " + server.getName() + "  id: " + server.getId() + " status: " + server.getStatus());
			serverList.add(server);
		}
		return serverList;
	}

	/**
	 * This method verifies, that the specified port is accessible on specified host. This is used mainly to verify, the
	 * SSH port on machines created on EC2 is accessible.
	 *
	 * @param host - the address of the host
	 * @param port - the port to be checked, whether it is accessible
	 * @return
	 */
	private static boolean serverListening(String host, int port) {
		log.trace("Trying to connect to server {} on port {} ", host, port);
		Socket s = null;

		try {
			s = new Socket(host, port);
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Delete the spawned server in EC2.
	 *
	 * @param serverName - the name of the server to be deleted.
	 */
	public void deleteSpawnedServer(String serverName) {

		log.info("Terminating server: " + serverName);

		final NodeMetadata server = getServerFromRegister(serverName);

		ec2Api.terminateInstancesInRegion(region, server.getId().split("/")[1]);

		serverRegister.remove(server);

		log.info("Deleted Ec2 server: " + serverName);
	}

	/**
	 * Creates server on EC2 using the template provided.
	 *
	 * @param template
	 * @return
	 * @throws RunNodesException
	 */
	private NodeMetadata createNode(Template template, String name) throws RunNodesException {
		return Iterables.getOnlyElement(computeService.createNodesInGroup(namePrefix + "-" + name, 1, template));
	}

	/**
	 * Builds the template for the server, that is being created.
	 *
	 * @return
	 */
	private EC2TemplateOptions buildTemplateOptions() {
		final AWSEC2TemplateOptions templateOptions = new AWSEC2TemplateOptions();

		templateOptions.keyPair(keyPair);
		templateOptions.securityGroupIds(securityGroup);

		return templateOptions;
	}

	public void startServer(String serverName) {

		final List<NodeMetadata> serverList = getServers(serverName);

		if (serverList.size() != 1) {
			throw new RuntimeException("There are multiple servers in the inventory with specified name!");
		} else {

			final NodeMetadata server = serverList.get(0);
			computeService.resumeNode(server.getId());
		}
	}

	public void startServers(String... servers) {

		for (String server : servers) {
			startServer(server);
		}
	}

	public void stopServer(String serverName) {

		final List<NodeMetadata> serverList = getServers(serverName);
		if (serverList.size() != 1) {
			throw new RuntimeException("There are multiple servers (" + serverList.size() + ") in the inventory with specified name!");
		} else {
			final NodeMetadata server = serverList.get(0);
			computeService.suspendNode(server.getId());
		}
	}

	public void stopServers(String... servers) {

		for (String server : servers) {
			stopServer(server);
		}
	}

	/**
	 * Verifies, the server was started and the specified port is accessible.
	 *
	 * @param publicAddress
	 */
	public void verifyServerStarted(String publicAddress) {

		final long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(BOOT_TIMEOUT_SEC);
		while (System.currentTimeMillis() < endTime) {
			if (serverListening(publicAddress, SSH_PORT)) {
				break;
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				log.warn("Waiting for instance's SHH port was interrupted: ", e);
				break;
			}
		}

		if (endTime <= System.currentTimeMillis()) {
			log.warn("Instance {} hasn't switched started in time: {} seconds.",
					publicAddress, BOOT_TIMEOUT_SEC);
			throw new RuntimeException("The specified server resources can't be accessed in time, please see EC2 for more details.");
		}
	}

	/**
	 * Builder class.
	 */
	public static class Ec2ClientBuilder {

		private String url;
		private String accessKeyID;
		private String secretKey;
		private String region;
		private String imageId;
		private String instanceType;
		private String keyPair;
		private String securityGroups;
		private String namePrefix;

		/**
		 * Default constructor.
		 */
		Ec2ClientBuilder() {
		}

		/**
		 * Setter.
		 *
		 * @param url EC2 URL
		 * @return this
		 */
		public Ec2Client.Ec2ClientBuilder url(String url) {
			this.url = url;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param accessKeyID EC2 accesskey ID
		 * @return this
		 */
		public Ec2Client.Ec2ClientBuilder accessKeyID(String accessKeyID) {
			this.accessKeyID = accessKeyID;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param secretKey EC2 secretKey
		 * @return this
		 */
		public Ec2Client.Ec2ClientBuilder secretKey(String secretKey) {
			this.secretKey = secretKey;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param region EC2 region
		 * @return this
		 */
		public Ec2Client.Ec2ClientBuilder region(String region) {
			this.region = region;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param imageId EC2 image (AMI) ID
		 * @return this
		 */
		public Ec2Client.Ec2ClientBuilder imageId(String imageId) {
			this.imageId = imageId;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param instanceTyp EC2 instance type
		 * @return this
		 */
		public Ec2Client.Ec2ClientBuilder instanceType(String instanceType) {
			this.instanceType = instanceType;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param keyPair EC2 keyPair
		 * @return this
		 */
		public Ec2Client.Ec2ClientBuilder keyPair(String keyPair) {
			this.keyPair = keyPair;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param namePrefix EC2 name prefix that should be used for spawned machines
		 * @return this
		 */
		public Ec2Client.Ec2ClientBuilder namePrefix(String namePrefix) {
			this.namePrefix = namePrefix;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param securityGroups EC2 securityGroups
		 * @return this
		 */
		public Ec2Client.Ec2ClientBuilder securityGroups(String securityGroups) {
			this.securityGroups = securityGroups;
			return this;
		}

		/**
		 * Setter for default parameters for EC2.
		 *
		 * @return this
		 */
		public Ec2Client.Ec2ClientBuilder defaultEc2client() {
			final Properties p = readProperties(EC2_PROPERTIES);
			this.url = p.getProperty("ec2.url");
			this.accessKeyID = p.getProperty("ec2.accessKeyID");
			this.secretKey = p.getProperty("ec2.secretKey");
			this.region = p.getProperty("ec2.region");
			this.imageId = p.getProperty("ec2.imageId");
			this.instanceType = p.getProperty("ec2.instanceType");
			this.keyPair = p.getProperty("ec2.keyPair");
			this.securityGroups = p.getProperty("ec2.securityGroups");
			this.namePrefix = p.getProperty("ec2.namePrefix");
			return this;
		}

		/**
		 * Builds EC2Client.
		 *
		 * @return EC2 instance
		 */
		public Ec2Client build() {
			return new Ec2Client(url, accessKeyID, secretKey, region, imageId, instanceType, keyPair, securityGroups, namePrefix);
		}

		private Properties readProperties(String fileName) {
			final Properties p = new Properties();

			try {
				final List<URL> urls = new LinkedList<>();
				// If defined get property file from SystemProperty
				if (System.getProperty("ec2.config") != null) {
					urls.add(new URL(System.getProperty("ec2.config")));
					log.info("Loading EC2 configuration file on path: " + System.getProperty("ec2.config"));
				}
				// Get the property files URLs from classpath
				urls.addAll(Collections.list(Ec2Client.class.getClassLoader().getResources(fileName)));

				log.debug("Ec2 properties config path: " + urls.toString());

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
}
