package org.jboss.fuse.qa.fafram8.cluster.node;

import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.ssh.NodeSSHClient;
import org.jboss.fuse.qa.fafram8.ssh.SSHClient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Class representing node.
 * Created by mmelko on 09/10/15.
 */
@ToString
@AllArgsConstructor
public class Node {
	/**
	 * Constructor.
	 */
	protected Node() {
	}

	/**
	 * Setter.
	 * @param host host
	 */
	public void setHost(String host) {
		this.host = host;
		this.executor = createExecutor();
	}

	/**
	 * Setter.
	 * @param port port
	 */
	public void setPort(int port) {
		this.port = port;
		this.executor = createExecutor();
	}

	/**
	 * Setter.
	 * @param username username
	 */
	public void setUsername(String username) {
		this.username = username;
		this.executor = createExecutor();
	}

	/**
	 * Setter.
	 * @param password password
	 */
	public void setPassword(String password) {
		this.password = password;
		this.executor = createExecutor();
	}

	@Getter
	@Setter
	private String nodeId;

	@Getter
	private String host;

	@Getter
	private int port = SystemProperty.getHostPort();

	@Getter
	private String username = SystemProperty.getHostUser();

	@Getter
	private String password = SystemProperty.getHostPassword();

	@Getter
	@Setter
	private Executor executor;

	@Getter
	@Setter
	private String privateKey;

	@Getter
	@Setter
	private String passPhrase;

	/**
	 * Creates the executor from the attributes - usable when you are changing the IP in the openstack provider.
	 * @return executor instance
	 */
	public Executor createExecutor() {
		final SSHClient nodeClient = new NodeSSHClient()
				.host(this.getHost())
				.port(this.getPort())
				.username(this.getUsername())
				.password(this.getPassword());
		return new Executor(nodeClient, this.host);
	}

	/**
	 * Builder getter.
	 *
	 * @return builder instance
	 */
	public static NodeBuilder builder() {
		return new NodeBuilder(null);
	}

	/**
	 * Builder getter.
	 *
	 * @param node node that will be copied
	 * @return builder instance
	 */
	public static NodeBuilder builder(Node node) {
		return new NodeBuilder(node);
	}

	/**
	 * Node builder class - this class returns the Node object and it is the only way the node should be built.
	 */
	public static class NodeBuilder {
		// Node instance
		private Node node;

		/**
		 * Constructor.
		 *
		 * @param node node that will be copied
		 */
		public NodeBuilder(Node node) {
			if (node != null) {
				this.node = node;
			} else {
				this.node = new Node();
			}
		}

		/**
		 * Setter.
		 *
		 * @param nodeId node ID
		 * @return this
		 */
		public NodeBuilder nodeId(String nodeId) {
			node.setNodeId(nodeId);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param host host
		 * @return this
		 */
		public NodeBuilder host(String host) {
			node.setHost(host);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param port port
		 * @return this
		 */
		public NodeBuilder port(int port) {
			node.setPort(port);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param username username
		 * @return this
		 */
		public NodeBuilder username(String username) {
			node.setUsername(username);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param password password
		 * @return this
		 */
		public NodeBuilder password(String password) {
			node.setPassword(password);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param executor node executor
		 * @return this
		 */
		public NodeBuilder executor(Executor executor) {
			node.setExecutor(executor);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param privateKey private key
		 * @return this
		 */
		public NodeBuilder privateKey(String privateKey) {
			node.setPrivateKey(privateKey);
			return this;
		}

		/**
		 * Setter.
		 *
		 * @param passPhrase pass phrase
		 * @return this
		 */
		public NodeBuilder passPhrase(String passPhrase) {
			node.setPassPhrase(passPhrase);
			return this;
		}

		/**
		 * Builds the instance.
		 *
		 * @return node instance
		 */
		public Node build() {
			final int defaultPort = 22;
			// If there is no port specified, use the default one
			if (node.getPort() == 0) {
				node.setPort(defaultPort);
			}

			Executor executor = null;
			// Create the executor if we are not on localhost
			if (!"localhost".equals(node.getHost())) {
				executor = node.createExecutor();
			}
			return new Node(node.getNodeId(), node.getHost(), node.getPort(), node.getUsername(), node.getPassword(),
					executor, node.getPrivateKey(), node.getPassPhrase());
		}
	}
}
