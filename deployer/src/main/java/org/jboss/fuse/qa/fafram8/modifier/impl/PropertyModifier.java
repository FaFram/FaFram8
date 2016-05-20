package org.jboss.fuse.qa.fafram8.modifier.impl;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.exceptions.CopyFileException;
import org.jboss.fuse.qa.fafram8.modifier.Modifier;
import org.jboss.fuse.qa.fafram8.modifier.ModifierExecutor;
import org.jboss.fuse.qa.fafram8.ssh.NodeSSHClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Property modifier class.
 * Created by jludvice on 20.8.15.
 */
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true, exclude = {"executor"})
public final class PropertyModifier extends Modifier {
	private String filePath;
	private String key;
	private String value;
	private boolean extend;

	/**
	 * Private constructor.
	 *
	 * @param filePath file path
	 * @param key key
	 * @param value value
	 * @param extend extend
	 */
	private PropertyModifier(String filePath, String key, String value, boolean extend) {
		this(null, filePath, key, value, extend);
	}

	/**
	 * Private constructor.
	 *
	 * @param ip ip to execute on
	 * @param filePath file path
	 * @param key key
	 * @param value value
	 * @param extend extend
	 */
	private PropertyModifier(String ip, String filePath, String key, String value, boolean extend) {
		super.setHost(ip);
		this.filePath = filePath;
		this.key = key;
		this.value = value;
		this.extend = extend;
	}

	/**
	 * Factory method - command for put/replace entry in property file.
	 *
	 * @param filePath path to property file - absolute, or relative to $FUSE_HOME
	 * @param key key in property file
	 * @param value value for key
	 * @return command instance
	 */
	public static PropertyModifier putProperty(final String filePath, final String key, final String value) {
		return new PropertyModifier(filePath, key, value, false);
	}

	/**
	 * Factory method - command for put/replace entry in property file.
	 *
	 * @param ip ip to execute on
	 * @param filePath path to property file - absolute, or relative to $FUSE_HOME
	 * @param key key in property file
	 * @param value value for key
	 * @return command instance
	 */
	public static PropertyModifier putProperty(String ip, String filePath, String key, String value) {
		return new PropertyModifier(ip, filePath, key, value, false);
	}

	/**
	 * Factory method - command to append to existing entry.
	 *
	 * @param filePath path to property file - absolute, or relative to $FUSE_HOME
	 * @param key key in property file
	 * @param value value to be appended to existing key=value
	 * @return command instance
	 */
	public static PropertyModifier extendProperty(final String filePath, final String key, final String value) {
		return new PropertyModifier(filePath, key, value, true);
	}

	@Override
	public void execute() {
		if (super.getExecutor() == null) {
			localExecute();
		} else {
			remoteExecute();
		}
	}

	/**
	 * Modifies properties on localhost.
	 */
	private void localExecute() {
		final Container container = ModifierExecutor.getRootContainerByHost(super.getExecutor().getClient().getHost());
		final Properties p = new Properties();

		Path path = Paths.get(filePath);
		if (!path.isAbsolute()) {
			path = Paths.get(container.getFusePath() + File.separator + filePath);
		}
		// load property file if it exists
		if (Files.exists(path)) {
			try (InputStream is = Files.newInputStream(path)) {
				p.load(is);
			} catch (IOException e) {
				log.error("Can't load property file {}.", filePath, e);
				throw new FaframException("Can't load property file " + filePath + ".", e);
			}
		} else {
			log.debug("Creating new property file {}", filePath);
		}

		if (extend) {
			p.put(key, p.getProperty(key, "") + value);
		} else {
			p.put(key, value);
		}

		try (OutputStream os = Files.newOutputStream(path)) {
			p.store(os, "property file edited by Fuse ModifierExecutor");
		} catch (IOException e) {
			log.error("Failed to store modified property file {}", filePath, e);
			throw new FaframException("Failed to store modified property file " + filePath + ".", e);
		}
	}

	/**
	 * Modifies properties on remote.
	 */
	private void remoteExecute() {
		final Container container = ModifierExecutor.getRootContainerByHost(super.getExecutor().getClient().getHost());
		final Properties p = new Properties();
		final String path = container.getFusePath() + File.separator + filePath;

		final NodeSSHClient sshClient = (NodeSSHClient) super.getExecutor().getClient();

		final String response = super.getExecutor().executeCommandSilently("stat " + path);
		if (!(response == null || response.isEmpty())) {
			// Load file from remote if exists
			try (StringReader stringReader = new StringReader(sshClient.readFileFromRemote(path))) {
				p.load(stringReader);
			} catch (CopyFileException | IOException ex) {
				log.error("Failed to load properties files {} from remote machine {}", path, sshClient, ex);
				throw new FaframException("Failed to load properties file " + path + " from remote machine " + sshClient, ex);
			}
		}

		// Modify properties
		if (extend) {
			p.put(key, p.getProperty(key, "") + value);
		} else {
			p.put(key, value);
		}

		// Override property file or create new one if it doesn't exists
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			p.store(output, "property file edited by Fuse ModifierExecutor");
			try (ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray())) {
				sshClient.writeFileToRemote(input, path);
			}
		} catch (IOException | CopyFileException e) {
			log.error("Failed to store modified property file {}", filePath, e);
			throw new FaframException("Failed to store modified property file " + filePath + ".", e);
		}
	}
}
