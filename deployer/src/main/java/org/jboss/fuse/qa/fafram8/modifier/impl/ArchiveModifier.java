package org.jboss.fuse.qa.fafram8.modifier.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.exceptions.CopyFileException;
import org.jboss.fuse.qa.fafram8.junit.TestNameSingleton;
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
import java.util.regex.Pattern;

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
public final class ArchiveModifier extends Modifier {
	private Path archiveTargetPath = Paths.get(SystemProperty.getArchiveTarget()).toAbsolutePath();
	private String[] archiveFiles = SystemProperty.getArchivePattern().split(" *, " + "*"); //ignore spaces around comma

	/**
	 * Constructor.
	 * @param host host
	 */
	private ArchiveModifier(String host) {
		super.setHost(host);
	}

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
		log.info("Archiving files with patterns: \"{}\" relative to \"{}\"", archiveFiles, container.getFusePath());

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

			log.info("Archiving {} file" + (foundFiles.length != 1 ? "s" : "") + " to {}", foundFiles.length, archiveTargetPath);
			for (String fileName : foundFiles) {
				// scanner returns paths relative to fuseDir
				final Path p = Paths.get(container.getFusePath(), fileName);
				log.debug("Archiving file {}", fileName);
				// create target directory structure
				final Path target = getTargetPath(container, fileName);
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

		if (container.getFusePath() == null) {
			log.warn("Container fuse path was null, skipping archiver");
			return;
		}

		for (String s : archiveFiles) {
			final String response = super.getExecutor().executeCommand(
					"find " + container.getFusePath() + " -type f -wholename \""
							+ container.getFusePath() + (container.getFusePath().endsWith(File.separator) ? "" : File.separator) + s + "\"");
			if (!(response == null || response.isEmpty())) {
				for (String filePath : response.split("\n")) {
					try {
						final File archivedFile;
						if (TestNameSingleton.getInstance().getTestName() == null) {
							archivedFile = Paths.get(archiveTargetPath.toAbsolutePath().toString(), randomFolder,
									StringUtils.substringAfterLast(filePath, File.separator)).toFile();
						} else {
							archivedFile = Paths.get(archiveTargetPath.toAbsolutePath().toString(), randomFolder,
									TestNameSingleton.getInstance().getTestName(), StringUtils.substringAfterLast(filePath, File.separator)).toFile();
						}
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
	 * @param host host
	 * @return new archive modifier instance
	 */
	public static ArchiveModifier registerArchiver(String host) {
		return new ArchiveModifier(host);
	}

	/**
	 * Constructs the path to the target file path.
	 *
	 * @param fileName file name to use
	 * @return absolute target path
	 */
	private Path getTargetPath(Container container, String fileName) {
		if (System.getenv("WORKSPACE") == null) {
			// Get the generated path substring, for example for:
			// Full path: /tmp/target/container/2016-11-28-09-52-52-434/jboss-fuse-6.2.1.redhat-084/data/log/fuse.log
			// Open: /tmp/target
			// Close: data/log/fuse.log
			// It returns: container/2016-11-28-09-52-52-434/jboss-fuse-6.2.1.redhat-084
			final String targetPath = StringUtils.substringBetween(
					Paths.get(container.getFusePath(), fileName).toAbsolutePath().toString(),
					Paths.get(SystemProperty.getBaseDir(), "target").toAbsolutePath().toString(),
					fileName);
			return Paths.get(archiveTargetPath.toString(), targetPath, getFileName(fileName)).toAbsolutePath();
		} else {
			// Jenkins env
			final String[] path = Paths.get(container.getFusePath()).toAbsolutePath().toString().split(Pattern.quote(File.separator));
			final String folder = path[path.length - 2] + File.separator + path[path.length - 1];
			return Paths.get(archiveTargetPath.toString(), folder, getFileName(fileName)).toAbsolutePath();
		}
	}

	/**
	 * Returns the fileName with prepended test name if the testname is set.
	 * @param fileName file name
	 * @return file name with testname prepended if the testname is set
	 */
	private String getFileName(String fileName) {
		if (TestNameSingleton.getInstance().getTestName() != null) {
			return StringUtils.substringBeforeLast(fileName, File.separator) + File.separator + TestNameSingleton.getTestName()
					+ File.separator + StringUtils.substringAfterLast(fileName, File.separator);
		}
		return fileName;
	}
}
