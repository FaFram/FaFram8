package org.jboss.fuse.qa.fafram8.cluster.broker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Class represents broker in fafram. Main purpose is to provide all necessary commands for creating broker in Fabric.
 * Created by mmelko on 09/10/15.
 */
public class Broker {

	public static final String STANDALONE = "StandAlone";
	public static final String MASTERSLAVE = "MasterSlave";

	@Setter
	@Getter
	private String name;

	@Setter
	@Getter
	private boolean standalone = false;

	@Setter
	@Getter
	private boolean ssl = false;

	@Setter
	@Getter
	private boolean assignContainer = false;

	@Setter
	@Getter
	private String kind = "";

	@Setter
	@Getter
	private String group = "default";

	@Setter
	@Getter
	private String data = "";

	@Setter
	@Getter
	private String networksUsername = "";

	@Getter
	@Setter
	private String networksPassword = "";

	@Setter
	@Getter
	private String parentProfile = "";

	//list of contanier names which broker is assigned to
	@Setter
	@Getter
	private List<String> containers = new ArrayList<>();

	@Setter
	@Getter
	private List<String> networks = new ArrayList<>();

	@Setter
	@Getter
	private Map<String, String> pids = new HashMap<>();

	/**
	 * Constructor.
	 *
	 * @param name - name of the broker which is going to be created.
	 */
	public Broker(String name) {
		this.name = name;
	}

	/**
	 * Copy constructor.
	 *
	 * @param broker broker which is cloned.
	 */
	public Broker(Broker broker) {
		this.name = broker.getName();
		this.ssl = broker.isSsl();
		this.kind = broker.getKind();
		this.group = broker.getGroup();
		this.data = broker.getData();
		this.networks = new ArrayList<>(broker.getNetworks());
		this.parentProfile = broker.getParentProfile();
		this.containers = new ArrayList<>(broker.getContainers());
		this.pids = new HashMap<>(broker.getPids());
	}

	/**
	 * Creates a list of all commands needed for creation of the broker.
	 *
	 * @return list of commands.
	 */
	public List<String> getCreateCommands() {
		final List<String> commands = new LinkedList<>();
		commands.add(mqCreate());

		for (Map.Entry<String, String> entry : pids.entrySet()) {
			commands.add("profile-edit " + mqServerPidModification(entry.getKey(), entry.getValue()) + getProfileName());
		}

		if (assignContainer) {
			for (String container : containers) {
				commands.add("container-add-profile " + container + " " + getProfileName());
			}
		}

		return commands;
	}

	/**
	 * Get name of the profile of the broker.
	 *
	 * @return profile.
	 */
	public String getProfileName() {
		return "mq-broker-" + group + "." + name;
	}

	/**
	 * Creates mq-create command.
	 *
	 * @return mq-create command.
	 */
	private String mqCreate() {
		final StringBuilder cmd = new StringBuilder("mq-create ");
		if (!ssl) {
			cmd.append("--no-ssl");
		}
		if (!"".equals(kind)) {
			cmd.append(" --kind ").append(kind);
		}
		if (!"".equals(parentProfile)) {
			cmd.append(" --parent-profile ").append(parentProfile);
		}
		if (!"".equals(group)) {
			cmd.append(" --group ").append(group);
		}
		if (!"".equals(data)) {
			cmd.append(" --data ").append(data);
		}
		for (String network : networks) {
			cmd.append(" --network ").append(network);
		}
		if (!"".equals(networksUsername)) {
			cmd.append(" --network-username ").append(networksUsername);
		}
		if (!"".equals(networksPassword)) {
			cmd.append(" --network-password ").append(networksPassword);
		}
		cmd.append(" ").append(name);
		return cmd.toString();
	}

	/**
	 * Creates brokerPID modifier for broker profile.
	 *
	 * @param key key of pid
	 * @param value value of pid
	 * @return proper value.
	 */
	private String mqServerPidModification(String key, String value) {
		return "--pid io.fabric8.mq.fabric.server-" + name + "/" + key + "=" + value + " ";
	}

	/**
	 * Adds network name into list of networks. Group name represents the name of network.
	 *
	 * @param name name of the network/group
	 */
	public void addNetwork(String name) {
		this.networks.add(name);
	}

	/**
	 * Adds network into list of networks. Group name represents the name of network.
	 *
	 * @param name name of the network/group
	 * @param user username
	 * @param password password
	 */
	public void addNewtork(String name, String user, String password) {
		this.networks.add(name);
		this.networksPassword = password;
		this.networksUsername = user;
	}

	/**
	 * Adds pid which will be modified during broker creation.
	 *
	 * @param pid - persistent id
	 * @param value - new value
	 */
	public void addPid(String pid, String value) {
		this.pids.put(pid, value);
	}

	/**
	 * Method which is used  for the broker init.
	 *
	 * @return Broker builder.
	 */
	public static BrokerBuilder builder() {
		return new BrokerBuilder();
	}

	/**
	 * Method which should be used for the broker init.
	 *
	 * @param broker broker object which will be cloned.
	 * @return Broker builder with copied broker.
	 */
	public static BrokerBuilder builder(Broker broker) {
		return new BrokerBuilder(broker);
	}

	/**
	 * Broker builder class. Class returns Broker object and it's the only way how brokers should be built.
	 */
	public static class BrokerBuilder {

		private Broker tempBroker;

		/**
		 * Constructor.
		 */
		public BrokerBuilder() {
			this.tempBroker = new Broker("broker");
		}

		/**
		 * Constructor.
		 *
		 * @param b broker which will be copied.
		 */
		public BrokerBuilder(Broker b) {
			this.tempBroker = new Broker(b);
		}

		/**
		 * Broker name setter.
		 *
		 * @param name - name of the broker
		 * @return this
		 */
		public BrokerBuilder name(String name) {
			this.tempBroker.setName(name);
			return this;
		}

		/**
		 * SSL usage setter.
		 *
		 * @param ssl if true use ssl
		 * @return this
		 */
		public BrokerBuilder ssl(Boolean ssl) {
			this.tempBroker.setSsl(ssl);
			return this;
		}

		/**
		 * Broker kind setter.
		 *
		 * @param kind -  Use Broker.STANDALONE or Broker.MASTERSLAVE kind
		 * @return this
		 */
		public BrokerBuilder kind(String kind) {
			this.tempBroker.setKind(kind);
			return this;
		}

		/**
		 * Broker group setter.
		 *
		 * @param group name of the group which broker is a member
		 * @return this
		 */
		public BrokerBuilder group(String group) {
			this.tempBroker.setGroup(group);
			return this;
		}

		/**
		 * Persistent adapter setter.
		 *
		 * @param data - persistent adapter
		 * @return this
		 */
		public BrokerBuilder data(String data) {
			this.tempBroker.setData(data);
			return this;
		}

		/**
		 * Parent profile setter.
		 *
		 * @param parentProfile name of the parent profile
		 * @return this
		 */
		public BrokerBuilder parentProfile(String parentProfile) {
			this.tempBroker.setParentProfile(parentProfile);
			return this;
		}

		/**
		 * Containers setter.
		 *
		 * @param containers all containers which broker will be deployed on
		 * @return this
		 */
		public BrokerBuilder containers(String... containers) {
			tempBroker.containers.addAll(Arrays.asList(containers));
			return this;
		}

		/**
		 * Adds broker network with credentials.
		 *
		 * @param network name of the network
		 * @param username username
		 * @param password password
		 * @return this
		 */
		public BrokerBuilder addNetwork(String network, String username, String password) {
			this.tempBroker.addNewtork(network, username, password);
			return this;
		}

		/**
		 * Adds broker network which broker will be connected to.
		 *
		 * @param network name of the network. Name of group represents network in fabric
		 * @return this
		 */
		public BrokerBuilder addNetwork(String network) {
			this.tempBroker.addNetwork(network);
			return this;
		}

		/**
		 * Adds pid for change.
		 *
		 * @param pid persistent id
		 * @param value new value
		 * @return this
		 */
		public BrokerBuilder addPid(String pid, String value) {
			this.tempBroker.getPids().put(pid, value);
			return this;
		}

		/**
		 * Build the broker.
		 *
		 * @return new broker.
		 */
		public Broker build() {
			return tempBroker;
		}
	}
}
