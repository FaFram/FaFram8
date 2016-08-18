package org.jboss.fuse.qa.fafram8.modifier.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.exceptions.CopyFileException;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.modifier.Modifier;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.ssh.NodeSSHClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Archive modifier class.
 * Created by avano on 8.10.15.
 */
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public class ArchiveModifier extends Modifier {
	private Path archiveTargetPath = Paths.get(SystemProperty.getArchiveTarget()).toAbsolutePath();
	private String[] archiveFiles = SystemProperty.getArchivePattern().split(" *, " + "*"); //ignore spaces around comma

	@Override
	public void execute(Container container) {
		if (archiveFiles.length == 0) {
			log.info("Nothing to archive.");
			return;
		}

		if (super.getExecutor() != null) {
			archiveRemoteFiles(container);
		} else {
			archiveLocalFiles(container);
		}
	}

	/**
	 * Archives files on localhost.
	 */
	private void archiveLocalFiles(Container container) {
		log.info("Archiving files with patterns: {}", archiveFiles);

		try {
			// setup Ant Directory Scanner
			final DirectoryScanner scanner = new DirectoryScanner();
			scanner.setIncludes(archiveFiles);
			// set base dir to target/
			scanner.setBasedir(container.getFusePath());
			scanner.setCaseSensitive(false);
			// perform scan
			scanner.scan();
			final String[] foundFiles = scanner.getIncludedFiles();

			log.info("Archiving {} file" + (foundFiles.length > 1 ? "s" : "") + " to {}", foundFiles.length, archiveTargetPath);
			for (String fileName : foundFiles) {
				//scanner returns paths relative to fuseDir
				final Path p = Paths.get(container.getFusePath(), fileName);
				log.debug("Archiving file {}", fileName);
				//create target directory structure
				final Path target = getTargetPath(fileName);
				Files.createDirectories(target.getParent());
				// for instance copy
				// from: $FUSE_HOME/data/log/fuse.log
				// to: target/archived/data/log/fuse.log
				Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			log.error("Failed to archive files with following patterns: {}", archiveFiles, e);
			// Don't throw the exception here, the properties and modifiers will not unset
		}
	}

	/**
	 * Archives files on remote.
	 */
	private void archiveRemoteFiles(Container container) {
		final int endIndex = 6;
		final String randomFolder = super.getExecutor().getClient().getHost() + "-" + UUID.randomUUID().toString().substring(0, endIndex);
		final NodeSSHClient sshClient = (NodeSSHClient) super.getExecutor().getClient();

		for (String s : archiveFiles) {
			final String response = super.getExecutor().executeCommand(
					"find " + container.getFusePath() + " -type f -wholename \""
							+ container.getFusePath() + File.separator + s + "\"");
			if (!(response == null || response.isEmpty())) {
				for (String filePath : response.split("\n")) {
					try {
						final File archivedFile = Paths.get(archiveTargetPath.toAbsolutePath().toString(), randomFolder,
								StringUtils.substringAfterLast(filePath, File.separator)).toFile();
						FileUtils.writeStringToFile(archivedFile, sshClient.readFileFromRemote(filePath));
					} catch (IOException | CopyFileException e) {
						log.error("Failed to archived file {} from remote machine {}!", filePath, sshClient, e);
						throw new FaframException("Failed to archived file " + filePath + " from remote machine " + sshClient + "!", e);
					}
				}
			}
		}
	}

	/**
	 * Factory method.
	 *
	 * @return new archive modifier instance
	 */
	public static ArchiveModifier registerArchiver() {
		return new ArchiveModifier();
	}

	/**
	 * Constructs the path to the target file path.
	 *
	 * @param fileName file name to use
	 * @return absolute target path
	 */
	private Path getTargetPath(String fileName) {
		final Container container = ContainerManager.getRootContainerByHost("localhost");
		if (System.getenv("WORKSPACE") == null) {
			return Paths.get(archiveTargetPath.toString(), StringUtils.substringBetween(
					Paths.get(container.getFusePath(), fileName).toAbsolutePath().toString(),
					Paths.get(SystemProperty.getBaseDir(), "target").toAbsolutePath().toString(),
					fileName),
					fileName).toAbsolutePath();
		} else {
			// Jenkins env
			final String[] path = Paths.get(container.getFusePath()).toAbsolutePath().toString().split(File.separator);
			final String folder = path[path.length - 2] + File.separator + path[path.length - 1];
			return Paths.get(archiveTargetPath.toString(), folder, fileName).toAbsolutePath();
		}
	}
}
