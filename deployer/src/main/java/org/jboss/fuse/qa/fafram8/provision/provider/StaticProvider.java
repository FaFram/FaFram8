package org.jboss.fuse.qa.fafram8.provision.provider;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.exception.OfflineEnvironmentException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.ssh.NodeSSHClient;
import org.jboss.fuse.qa.fafram8.ssh.SSHClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Default provision provider implementation. This implemenation does nothing. Used in case of pre-configured
 * existing hosts hosts.
 * <p/>
 * Created by ecervena on 13.10.15.
 */
@Slf4j
public class StaticProvider implements ProvisionProvider {

	private static final String SAVED_IPTABLES = "ipTablesSaved";

	/**
	 * Killing Fuse processes on provided containers and theirs nodes.
	 *
	 * @param containerList list of containers
	 */
	@Override
	public void createServerPool(List<Container> containerList) {
		log.info("Assuming static test infrastructure. Dynamic server provision is skipped.");
		if (SystemProperty.isClean()) {
			log.info("Cleaning resources");
			Executor executor;

			// Set for keeping track of already connected hosts and skipping them if necessary
			final Set<String> ipAddresses = new HashSet<>();
			for (Container c : containerList) {
				if (c instanceof ChildContainer || ipAddresses.contains(c.getNode().getHost())) {
					continue;
				}
				ipAddresses.add(c.getNode().getHost());

				final SSHClient sshClient = new NodeSSHClient().defaultSSHPort().host(c.getNode().getHost())
						.username(c.getNode().getUsername()).password(c.getNode().getPassword());
				executor = new Executor(sshClient, c.getNode().getHost());
				log.debug("Killing Fuse process on node: ", executor);
				try {
					executor.connect();
					executor.executeCommandSilently("pkill -9 -f karaf.base");
				} catch (Exception e) {
					throw new FaframException("Exception when killing Fuse on provides nodes (StaticProvider):", e);
				}
			}
		}
	}

	/**
	 * Does nothing.
	 *
	 * @param containerList list of containers to assign addresses
	 */
	@Override
	public void assignAddresses(List<Container> containerList) {
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void releaseResources() {
	}

	/**
	 * This is experimental method for StaticProvider. It saves default iptables configuration to special file before
	 * executing specified custom iptables configuration. Iptables are restored back to default after the test or if there
	 * was a exception in FaFram setup.
	 * <p/>
	 * USE ONLY ON YOUR OWN RISK!!!
	 *
	 * @param containerList list of containers
	 */
	@Override
	public void loadIPTables(List<Container> containerList) {
		// "If" for deciding if this method should be used is moved here so the Fafram method is clean(Only for you ecervena <3)
		if (SystemProperty.getIptablesConfFilePath().isEmpty()) {
			// There was no iptables configuration file set so the user doesn't want to change environment.
			return;
		}

		log.info("Loading iptables configuration files.");
		SSHClient sshClient;
		Executor executor;
		String remoteFilePath = "";

		for (Container c : containerList) {
			if (c instanceof ChildContainer) {
				//If the child container is child then skip. The file will be copied and executed for all ssh containers
				//and root. It doesn't make sense to do also for child containers.
				continue;
			}

			sshClient = new NodeSSHClient().defaultSSHPort().host(c.getNode().getHost())
					.username(c.getNode().getUsername()).password(c.getNode().getPassword());
			executor = new Executor(sshClient, c.getNode().getHost());
			log.debug("Loading iptables on node {}.", executor);
			try {
				executor.connect();

				final String directory = ("".equals(SystemProperty.getWorkingDirectory()))
						? executor.executeCommandSilently("pwd") : SystemProperty.getWorkingDirectory();

				// Path to copied iptables file on remote nodes
				remoteFilePath =
						directory + File.separator + StringUtils.substringAfterLast(SystemProperty.getIptablesConfFilePath(), File.separator);

				// Copy iptables configuration file from local to all remote nodes
				((NodeSSHClient) sshClient).copyFileToRemote(SystemProperty.getIptablesConfFilePath(), remoteFilePath);

				final String response = executor.executeCommandSilently("stat " + SystemProperty.getIptablesConfFilePath());

				if (response == null || response.isEmpty()) {
					throw new OfflineEnvironmentException("Configuration file for iptables"
							+ " doesn't exists on node: " + c.getNode().getHost() + ".",
							new FileNotFoundException("File " + SystemProperty.getIptablesConfFilePath() + " doesn't exists."));
				}
				log.debug("Saving default iptables configuration on node {}.", executor);
				executor.executeCommand("sudo iptables-save > " + SAVED_IPTABLES);

				executor.executeCommand("sudo iptables-restore " + SystemProperty.getIptablesConfFilePath());
				log.debug("Iptables successfully configured on node {}.", executor);
			} catch (Exception e) {
				throw new OfflineEnvironmentException(e);
			}
		}

		log.info("IPTables configuration files successfully loaded on all nodes! Environment configured according to {} file."
				, remoteFilePath);
	}

	@Override
	public void checkNodes(List<Container> containerList) {
		// Do nothing
	}

	/**
	 * Experimental method for cleaning iptables configuration on all nodes. This method restore iptables back to default
	 * using the "sudo iptables-restore savedFile" command.
	 * <p/>
	 * USE ONLY ON YOUR OWN RISK!!!
	 *
	 * @param containerList list of containers
	 */
	@Override
	public void cleanIpTables(List<Container> containerList) {
		// "If" for deciding if this method should be used is moved here so the Fafram method is clean(Only for you ecervena <3)
		if (SystemProperty.getIptablesConfFilePath().isEmpty()) {
			// There was no iptables configuration file set so the user doesn't want to change environment.
			return;
		}
		log.info("Cleaning iptables configuration to default one.");
		SSHClient sshClient;
		Executor executor;

		for (Container c : containerList) {
			if (c instanceof ChildContainer) {
				//If the child container is child then skip. The file will be copied and executed for all ssh containers
				//and root. It doesn't make sense to do also for child containers.
				continue;
			}

			sshClient = new NodeSSHClient().defaultSSHPort().host(c.getNode().getHost())
					.username(c.getNode().getUsername()).password(c.getNode().getPassword());
			executor = new Executor(sshClient, c.getNode().getHost());
			log.debug("Restoring iptables on node {} back to default.", executor);
			try {
				executor.connect();

				executor.executeCommand("sudo iptables-restore " + SAVED_IPTABLES);
				log.debug("Iptables restored on node {}.", executor);
			} catch (Exception e) {
				throw new OfflineEnvironmentException(e);
			}
		}

		log.info("IPTables configuration files successfully restored on all nodes. Set the environment configuration back to default.");
	}
}
