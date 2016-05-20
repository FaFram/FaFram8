package org.jboss.fuse.qa.fafram8.modifier.impl;

import org.apache.commons.io.FileUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.modifier.Modifier;
import org.jboss.fuse.qa.fafram8.ssh.NodeSSHClient;
import org.jboss.fuse.qa.fafram8.ssh.SSHClient;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

/**
 * Root name modifier class. This class gets the FaframConstant.FAFRAM_ROOT_NAMES property, parses it and sets the root names
 * on both local and remote nodes.
 * Created by avano on 11.1.16.
 */
@Slf4j
public final class RootNameModifier extends Modifier {
	private Container container;

	/**
	 * Private constructor.
	 */
	private RootNameModifier(Container container, String host) {
		this.container = container;
		super.setHost(host);
	}

	/**
	 * Factory method.
	 *
	 * @param container container to execute on
	 * @param host current host when the modifier is executed
	 * @return random modifier instance
	 */
	public static RootNameModifier setRootName(Container container, String host) {
		return new RootNameModifier(container, host);
	}

	@Override
	public void execute() {
		if ("localhost".equals(container.getNode().getHost())) {
			final File configFile = new File(container.getFusePath() + File.separator + "etc" + File.separator + "system.properties");
			try {
				String fileContent = FileUtils.readFileToString(configFile);
				fileContent = fileContent.replaceAll("karaf.name = root", "karaf.name = " + container.getName());
				FileUtils.write(configFile, fileContent);
			} catch (IOException e) {
				throw new FaframException("Error while setting root name: " + e);
			}
		} else {
			modifyRemoteRootName();
		}
	}

	/**
	 * Modifies the root name on remote. Creates a new ssh client, connects to the IP using supplied credentials
	 * and modifies the remote system properties file.
	 */
	private void modifyRemoteRootName() {
		final int defaultPort = 22;
		final SSHClient c = new NodeSSHClient();
		c.setHost(container.getNode().getHost());
		c.setPort(container.getNode().getPort());
		c.setUsername(container.getNode().getUsername());
		c.setPassword(container.getNode().getPassword());
		try {
			c.connect(true);
			log.debug("Setting root name to " + container.getName() + " on " + container.getNode().getHost());
			c.executeCommand("sed -i 's#\\<karaf.name = root\\>#karaf.name = " + container.getName() + "#g' "
					+ container.getFusePath() + "etc" + File.separator + "system.properties", true);
			c.disconnect();
		} catch (Exception e) {
			throw new FaframException("Error while setting root name on " + container.getNode().getHost() + ": " + e);
		}
	}

	@Override
	public String toString() {
		return String.format("RootNamesModifier(%s@%s)", container.getName(), container.getNode().getHost());
	}
}
