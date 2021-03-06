package org.jboss.fuse.qa.fafram8.modifier.impl;

import org.apache.commons.io.FileUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.modifier.Modifier;

import java.io.File;
import java.io.IOException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Root name modifier class. This class gets the FaframConstant.FAFRAM_ROOT_NAMES property, parses it and sets the root names
 * on both local and remote nodes.
 * Created by avano on 11.1.16.
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
public final class RootNameModifier extends Modifier {
	@Getter
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
	public void execute(Container container) {
		if (this.container.equals(container)) {
			if ("localhost".equals(this.container.getNode().getHost())) {
				final File configFile = new File(container.getFusePath() + File.separator + "etc" + File.separator + "system.properties");
				try {
					String fileContent = FileUtils.readFileToString(configFile);
					fileContent = fileContent.replaceAll("karaf.name = root", "karaf.name = " + container.getName());
					FileUtils.write(configFile, fileContent);
				} catch (IOException e) {
					throw new FaframException("Error while setting root name: " + e);
				}
			} else {
				modifyRemoteRootName(container);
			}
		} else {
			log.debug("Skipping this modifier because this is not correct container");
		}
	}

	/**
	 * Modifies the root name on remote. Creates a new ssh client, connects to the IP using supplied credentials
	 * and modifies the remote system properties file.
	 */
	private void modifyRemoteRootName(Container container) {
		try {
			log.trace("Connecting the executor to set the remote root name");
			log.debug("Setting root name to " + container.getName() + " on " + container.getNode().getHost());
			container.getNode().getExecutor().executeCommandSilently("sed -i 's#\\<karaf.name = root\\>#karaf.name = " + container.getName() + "#g' "
					+ container.getFusePath() + "etc" + File.separator + "system.properties");
		} catch (Exception e) {
			throw new FaframException("Error while setting root name on " + container.getNode().getHost() + ": " + e);
		}
	}

	@Override
	public String toString() {
		return String.format("RootNamesModifier(%s@%s)", this.container.getName(), this.container.getNode().getHost());
	}
}
