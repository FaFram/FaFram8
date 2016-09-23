package org.jboss.fuse.qa.fafram8.openstack.provision;

import org.jboss.fuse.qa.fafram8.openstack.exception.InvokerPoolInterruptedException;
import org.jboss.fuse.qa.fafram8.openstack.exception.UniqueServerNameException;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.openstack.OSFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
 * OpenStack client class that is able to working with OpenStack.
 * <p/>
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
@ToString
public final class OpenStackClient {
	@Getter
	@Setter
	private String url;

	@Getter
	@Setter
	private String user;

	@Getter
	@Setter
	private String password;

	@Getter
	@Setter
	private String tenant;

	@Getter
	@Setter
	private String image;

	@Getter
	@Setter
	private String namePrefix;

	@Getter
	@Setter
	private String flavor;

	@Getter
	@Setter
	private String keypair;

	@Getter
	@Setter
	private String networks;

	@Getter
	@Setter
	private String floatingIpPool;

	@Getter
	@Setter
	private String addressType;

	@Getter
	@Setter
	private OSClient osClient;

	// Number of threads
	private static final int POOL_SIZE = 5;

	// Log wait time
	private static final int LOG_WAIT_TIME = 5;

	// Server boot timeout
	private static final int BOOT_TIMEOUT = 120000;

	// List of floating addresses allocated by OpenStackProvisionProvider
	private static final List<FloatingIP> floatingIPs = new LinkedList<>();

	// List of all created OpenStack nodes a.k.a. servers
	private static final List<Server> serverRegister = new LinkedList<>();

	private static final String OS4_PROPERTIES = "OS4.properties";
	private static final String OS7_PROPERTIES = "OS7.properties";

	private static final int CORES_PER_INSTANCE = 2;
	private static final int MEMORY_PER_INSTANCE = 4096;

	@java.beans.ConstructorProperties({"url", "user", "password", "tenant", "image", "namePrefix", "flavor", "keypair", "networks", "floatingIpPool", "addressType", "osClient"})
	OpenStackClient(String url, String user, String password, String tenant, String image, String namePrefix, String flavor, String keypair, String networks, String floatingIpPool, String addressType) {
		this.url = url;
		this.user = user;
		this.password = password;
		this.tenant = tenant;
		this.image = image;
		this.namePrefix = namePrefix;
		this.flavor = flavor;
		this.keypair = keypair;
		this.networks = networks;
		this.floatingIpPool = floatingIpPool;
		this.addressType = addressType;

		this.osClient = OSFactory.builder()
				.endpoint(this.getUrl())
				.credentials(this.getUser(), this.getPassword())
				.tenantName(this.getTenant())
				.authenticate();
	}

	/**
	 * Builder.
	 *
	 * @return OenStackClientBuilder
	 */
	public static OpenStackClientBuilder builder() {
		return new OpenStackClientBuilder();
	}

	/**
	 * Calling this method will spawn thread workers to create OpenStack nodes in parallel.
	 *
	 * @param machineNames list of machines names that should be spawned
	 * @throws ExecutionException when one of the spawning threads fails
	 * @throws InterruptedException when one of the spawning threads is interrupted
	 */
	public void spawnServersByNames(String... machineNames) throws ExecutionException, InterruptedException {
		spawnServersByNames(Arrays.asList(machineNames));
	}

	/**
	 * Calling this method will spawn thread workers to create OpenStack nodes in parallel.
	 *
	 * @param machineNames list of machines names that should be spawned
	 * @return set of created servers
	 * @throws ExecutionException when one of the spawning threads fails
	 * @throws InterruptedException when one of the spawning threads is interrupted
	 */
	public Set<Server> spawnServersByNames(List<String> machineNames) throws ExecutionException, InterruptedException {
		log.debug("Initializing ServerInvokerPool.");
		final Set<Future<Server>> futureServerSet = new HashSet<>();
		final Set<Server> servers = new HashSet<>();
		final ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
		for (String name : machineNames) {
			log.trace("Spawning invoker thread for container: " + name);
			final Callable<Server> callable = new ServerInvoker(name, this);
			final Future<Server> future = executor.submit(callable);
			futureServerSet.add(future);
		}
		executor.shutdown();
		log.trace("Waiting for ServerInvoker threads to finish a job.");
		try {
			while (!executor.awaitTermination(LOG_WAIT_TIME, TimeUnit.SECONDS)) {
				log.trace("Waiting for ServerInvoker threads to finish a job.");
			}
		} catch (InterruptedException ie) {
			throw new InvokerPoolInterruptedException(ie.getMessage());
		}
		log.debug("ServerInvokerPool done.");

		for (Future<Server> future : futureServerSet) {
			final Server server = future.get();
			log.debug("Spawned server " + server.getName());
			servers.add(server);
			serverRegister.add(server);
		}

		return servers;
	}

	/**
	 * Assign floating IP address to specified server.
	 *
	 * @param serverID ID of the server
	 * @return floating IP assigned to server
	 */
	public String assignFloatingAddress(String serverID) {
		final FloatingIP ip = osClient.compute().floatingIps().allocateIP(this.floatingIpPool);
		floatingIPs.add(ip);
		final Server server = osClient.compute().servers().get(serverID);
		osClient.compute().floatingIps().addFloatingIP(server, ip.getFloatingIpAddress());
		return ip.getFloatingIpAddress();
	}

	/**
	 * Release allocated OpenStack resources. Method will delete created servers and release allocated floating IPs.
	 */
	public void releaseResources() {
		log.info("Releasing allocated OpenStack resources.");
		for (int i = floatingIPs.size() - 1; i >= 0; i--) {
			final FloatingIP ip = floatingIPs.get(i);
			log.info("Deallocating floating IP: " + ip.getFloatingIpAddress());
			osClient.compute().floatingIps().deallocateIP(ip.getId());
			floatingIPs.remove(i);
		}

		for (int i = serverRegister.size() - 1; i >= 0; i--) {
			final Server server = serverRegister.get(i);
			log.info("Terminating server: " + server.getName());
			osClient.compute().servers().delete(server.getId());
			serverRegister.remove(i);
		}

		log.info("All OpenStack resources has been released successfully");
	}

	/**
	 * Method for getting Server from server register of created Servers by this OpenStack client. This method checks that is only one Server with that name.
	 *
	 * @param serverName server name to be found
	 * @return found server
	 */
	public Server getServerFromRegister(String serverName) {
		final List<Server> registerList = new LinkedList<>();
		for (Server s : serverRegister) {
			if (s.getName().equals(serverName)) {
				registerList.add(s);
			}
		}

		// Check that there not two servers with the same name in the register
		if (registerList.size() != 1) {
			for (Object obj : registerList) {
				log.error("Server with not unique name detected in server register: ", obj.toString());
			}
			throw new UniqueServerNameException(
					"Server name is not unique in server register. More than 1 (" + registerList.size() + ") server with specified name: " + serverName + " detected");
		} else {
			// Now check that the name is unique in the OpenStack (e.g. old resources still exists)
			final List<Server> equalsList = getServers(registerList.get(0).getName());
			if (equalsList.size() != 1) {
				for (Object obj : equalsList) {
					log.error("Server with not unique name detected on OpenStack: ", obj.toString());
				}
				throw new UniqueServerNameException(
						"Server name is not uniqueon OpenStack. More than 1 (" + equalsList.size() + ") server with specified name: " + serverName + " detected");
			} else {
				return registerList.get(0);
			}
		}
	}

	/**
	 * Method for getting Server a.k.a OpenStack node object model. All nodes created  by this client should
	 * have specific name prefix. This method checks all servers on OpenStack not only servers created by this instance.
	 *
	 * @param serverName name of the node
	 * @return Server representation of openstack node object
	 */
	public Server getServerByName(String serverName) {
		final List<Server> equalsList = getServers(serverName);
		if (equalsList.size() != 1) {
			for (Object obj : equalsList) {
				log.error("Server with not unique name detected: ", obj.toString());
			}
			throw new UniqueServerNameException(
					"Server name is not unique. More than 1 (" + equalsList.size() + ") server with specified name: " + serverName + " detected");
		} else {
			return equalsList.get(0);
		}
	}

	/**
	 * Gets the count of the servers with name ("name"). Used to check if there are already some servers with defined name.
	 *
	 * @param name container name
	 * @return list of servers with given name
	 */
	public List<Server> getServers(String name) {
		final Map<String, String> filter = new HashMap<>();
		filter.put("name", name);

		final List<? extends Server> serverList = osClient.compute().servers().list(filter);
		final List<Server> equalsList = new ArrayList<>();

		for (Server server : serverList) {
			if (name.equals(server.getName())) {
				equalsList.add(server);
			}
		}

		return equalsList;
	}

	/**
	 * Create new OpenStack node. Method will create Server object model, boot it,
	 * add to register and wait for active status.
	 *
	 * @param serverName name of the new node
	 */

	/**
	 * Create new OpenStack node. Method will create Server object model, boot it,
	 * add to register and wait for active status.
	 *
	 * @param serverName name of the new node
	 * @return spawned server
	 */
	public Server spawnNewServer(String serverName) {
		return spawnNewServer(serverName, this.getImage());
	}

	/**
	 * Create new OpenStack node. Method will create Server object model, boot it,
	 * add to register and wait for active status.
	 *
	 * @param serverName name of the new node
	 * @param imageID ID of image to spawn
	 * @return spawned server
	 */
	public Server spawnNewServer(String serverName, String imageID) {
		this.waitForResources(1);
		log.info("Spawning new server: " + this.namePrefix + "-" + serverName);
		final ServerCreate server = osClient.compute().servers().serverBuilder().image(imageID)
				.name(this.namePrefix + "-" + serverName)
				.flavor(this.flavor)
				.keypairName(this.keypair)
				.networks(Arrays.asList(this.networks.split(","))).build();
		final Server node = osClient.compute().servers().bootAndWaitActive(server, BOOT_TIMEOUT);
		serverRegister.add(node);
		return node;
	}

	/**
	 * Method for deleting OpenStack node by server name that was spawned by this instance that is registered in server register.
	 * All nodes created  by OpenStackProvisionProvider have defined name prefix.
	 *
	 * @param serverName name of the node
	 */
	public void deleteSpawnedServer(String serverName) {
		final Server server = getServerFromRegister(serverName);
		osClient.compute().servers().delete(server.getId());
		serverRegister.remove(server);
	}

	/**
	 * Method for deleting OpenStack node by server name.
	 *
	 * @param serverName server full name
	 */
	public void deleteServer(String serverName) {
		osClient.compute().servers().delete(getServerByName(serverName).getId());
	}

	/**
	 * Get the free cores on OS.
	 *
	 * @return free cores
	 */
	public int getFreeCores() {
		final int max = osClient.compute().quotaSets().limits().getAbsolute().getMaxTotalCores();
		final int current = osClient.compute().quotaSets().limits().getAbsolute().getTotalCoresUsed();
		return max - current;
	}

	/**
	 * Get the free memory on OS.
	 *
	 * @return free memory
	 */
	public int getFreeMemory() {
		final int max = osClient.compute().quotaSets().limits().getAbsolute().getMaxTotalRAMSize();
		final int current = osClient.compute().quotaSets().limits().getAbsolute().getTotalRAMUsed();
		return max - current;
	}

	/**
	 * Waits for the OS resources until the requested amount is free.
	 *
	 * @param instanceCount instance count
	 */
	public void waitForResources(int instanceCount) {
		final long sleepPeriod = 30000L;
		log.info("Waiting for OS resources");
		while (true) {
			final int freeCores = getFreeCores();
			final int freeMemory = getFreeMemory();
			log.trace(String.format("CPU needed: %s, CPU free: %s | Mem needed: %s, Mem free: %s",
					(CORES_PER_INSTANCE * instanceCount), freeCores, (MEMORY_PER_INSTANCE * instanceCount), freeMemory));
			if (freeCores >= (CORES_PER_INSTANCE * instanceCount) && freeMemory >= (MEMORY_PER_INSTANCE * instanceCount)) {
				break;
			}
			try {
				Thread.sleep(sleepPeriod);
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Builder class.
	 */
	public static class OpenStackClientBuilder {
		private String url;
		private String user;
		private String password;
		private String tenant;
		private String image;
		private String namePrefix;
		private String flavor;
		private String keypair;
		private String networks;
		private String floatingIpPool;
		private String addressType;

		/**
		 * Default constructor.
		 */
		OpenStackClientBuilder() {
		}

		/**
		 * Setter.
		 *
		 * @param url OpenStack URL
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder url(String url) {
			this.url = url;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param user OpenStack user
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder user(String user) {
			this.user = user;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param password OpenStack password
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder password(String password) {
			this.password = password;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param tenant OpenStack tenant
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder tenant(String tenant) {
			this.tenant = tenant;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param image OpenStack image ID
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder image(String image) {
			this.image = image;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param namePrefix name prefix that should be used for spawned machines
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder namePrefix(String namePrefix) {
			this.namePrefix = namePrefix;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param flavor OpenStack flavor
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder flavor(String flavor) {
			this.flavor = flavor;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param keypair OpenStack keypair
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder keypair(String keypair) {
			this.keypair = keypair;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param networks OpenStack networks list separated by commas
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder networks(String networks) {
			this.networks = networks;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param floatingIpPool floating IP pool
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder floatingIpPool(String floatingIpPool) {
			this.floatingIpPool = floatingIpPool;
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param addressType OpenStack address type
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder addressType(String addressType) {
			this.addressType = addressType;
			return this;
		}

		/**
		 * Setter for default parameters for OpenStack 4.
		 *
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder defaultOS4client() {
			final Properties p = readProperties(OS4_PROPERTIES);
			this.url = p.getProperty("openstack.url");
			this.user = p.getProperty("openstack.user");
			this.password = p.getProperty("openstack.password");
			this.tenant = p.getProperty("openstack.tenant");
			this.image = p.getProperty("openstack.image");
			this.namePrefix = p.getProperty("openstack.namePrefix");
			this.flavor = p.getProperty("openstack.flavor");
			this.keypair = p.getProperty("openstack.keypair");
			this.networks = p.getProperty("openstack.networks");
			this.floatingIpPool = p.getProperty("openstack.floatingIpPool");
			this.addressType = p.getProperty("openstack.addressType");
			return this;
		}

		/**
		 * Setter for default parameters for OpenStack 7.
		 *
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder defaultOS7client() {
			final Properties p = readProperties(OS7_PROPERTIES);
			this.url = p.getProperty("openstack.url");
			this.user = p.getProperty("openstack.user");
			this.password = p.getProperty("openstack.password");
			this.tenant = p.getProperty("openstack.tenant");
			this.image = p.getProperty("openstack.image");
			this.namePrefix = p.getProperty("openstack.namePrefix");
			this.flavor = p.getProperty("openstack.flavor");
			this.keypair = p.getProperty("openstack.keypair");
			this.networks = p.getProperty("openstack.networks");
			this.floatingIpPool = p.getProperty("openstack.floatingIpPool");
			this.addressType = p.getProperty("openstack.addressType");
			return this;
		}

		/**
		 * Setter for default parameters for spawning Windows machines on OpenStack 7.
		 *
		 * @return this
		 */
		public OpenStackClient.OpenStackClientBuilder defaultOS7WinClient() {
			final Properties p = readProperties(OS7_PROPERTIES);
			this.url = p.getProperty("openstack.url");
			this.user = p.getProperty("openstack.user");
			this.password = p.getProperty("openstack.password");
			this.tenant = p.getProperty("openstack.tenant");
			this.image = p.getProperty("openstack.windows.image");
			this.namePrefix = p.getProperty("openstack.namePrefix");
			this.flavor = p.getProperty("openstack.windows.flavor");
			this.keypair = p.getProperty("openstack.keypair");
			this.networks = p.getProperty("openstack.networks");
			this.floatingIpPool = p.getProperty("openstack.floatingIpPool");
			this.addressType = p.getProperty("openstack.addressType");
			return this;
		}

		/**
		 * Builds OpenStackClient.
		 *
		 * @return OpenStackClient instance
		 */
		public OpenStackClient build() {
			return new OpenStackClient(url, user, password, tenant, image, namePrefix, flavor, keypair, networks, floatingIpPool, addressType);
		}

		private Properties readProperties(String fileName) {
			final Properties p = new Properties();

			try {
				final List<URL> urls = new LinkedList<>();
				// If defined get property file from SystemProperty
				if (System.getProperty("openstack.config") != null) {
					urls.add(new URL(System.getProperty("openstack.config")));
					log.info("Loading OpenStack configuration file on path: " + System.getProperty("openstack.config"));
				}
				// Get the property files URLs from classpath
				urls.addAll(Collections.list(OpenStackClient.class.getClassLoader().getResources(fileName)));

				log.debug("OpenStack properties config path: " + urls.toString());

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
