package org.jboss.fuse.qa.fafram8.downloader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;

import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.manager.NodeManager;
import org.jboss.fuse.qa.fafram8.manager.RemoteNodeManager;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

/**
 * Downloader class. This class provides the container URL for the unzipper. If the URL is not a local URL, it
 * downloads the container and returns the local URL.
 * Created by avano on 19.8.15.
 */
@Slf4j
public final class Downloader {
	// File separator
	private static final String SEP = File.separator;

	/**
	 * Private constructor.
	 */
	private Downloader() {
	}

	/**
	 * Downloads/Gets the product zip on localhost.
	 *
	 * @return path to the downloaded zip
	 */
	public static String getProduct() {
		// If the FUSE_ZIP is not set, get the artifact from maven
		if (SystemProperty.getFuseZip() == null) {
			log.info("Getting product from local repository");
			return getProductFromMaven();
		} else {
			// We are using custom zip on local
			log.info("Getting product from " + SystemProperty.getFuseZip());
			return getProductFromUrl();
		}
	}

	/**
	 * Downloads/Gets the product zip to the remote host.
	 *
	 * @param executor executor with assign ssh client
	 * @param nodeManager NodeManager specific for root container where zip should be downloaded
	 * @return absolute path to the file
	 */
	public static String getProduct(Executor executor, NodeManager nodeManager) {
		// We are using custom zip on local
		log.info("Getting product from " + SystemProperty.getFuseZip());
		// We already know we are downloading Fuse distro on remote. Retype NodeManager...
		return getProductFromUrl(executor, (RemoteNodeManager) nodeManager);
	}

	/**
	 * Gets the product zip from maven.
	 *
	 * @return absolute path to the file
	 */
	private static String getProductFromMaven() {
		final String localRepo;

		// If we use custom local repository, use it
		if (System.getProperty("maven.repo.local") != null) {
			localRepo = System.getProperty("maven.repo.local");
		} else {
			locateMaven();
			localRepo = getMavenLocalRepository();
		}

		return getArtifactPath(localRepo);
	}

	/**
	 * Gets the product zip from url on localhost.
	 *
	 * @return absolute path to the file
	 */
	private static String getProductFromUrl() {
		// Get the protocol from the property
		final String protocol = StringUtils.substringBefore(SystemProperty.getFuseZip(), ":");
		final String location;
		switch (protocol) {
			case "http":
				try {
					final File fuseZip = new File("target" + SEP + StringUtils.substringAfterLast(SystemProperty.getFuseZip(), "/"));
					FileUtils.copyURLToFile(new URL(SystemProperty.getFuseZip()), fuseZip);
					location = fuseZip.getAbsolutePath();
					break;
				} catch (IOException e) {
					throw new FaframException("Provided property " + FaframConstant.FUSE_ZIP + " cannot be converted to URL!", e);
				}
			case "scp":
				throw new UnsupportedOperationException("not implemented");
			case "file":
				// Strip the protocol from the path
				location = StringUtils.substringAfter(SystemProperty.getFuseZip(), ":");
				break;
			default:
				throw new FaframException("Unsupported protocol " + protocol);
		}

		return location;
	}

	/**
	 * Gets the product zip from url on remote.
	 *
	 * @param executor executor with ssh client connected to desired remote host
	 * @return absolute path to the file
	 */

	/**
	 * Gets the product zip from url on remote.
	 *
	 * @param executor executor with ssh client connected to desired remote host
	 * @param nodeManager RemoteNodeManager object for root container where zip will be downloaded
	 * @return absolute path to the file
	 */
	private static String getProductFromUrl(Executor executor, RemoteNodeManager nodeManager) {
		// Get the protocol from the property
		final String protocol = StringUtils.substringBefore(SystemProperty.getFuseZip(), ":");
		final String location;
		switch (protocol) {
			case "http":
				log.info(executor.executeCommand("curl -L -s -o " + nodeManager.getFolder()
						+ SEP + "fuse.zip " + SystemProperty.getFuseZip()));
				if (!executor.executeCommandSilently("file -i " + nodeManager.getFolder() + SEP + "fuse.zip").contains("application/zip")) {
					throw new FaframException("Something went wrong when downloading, downloaded file isn't a zip file!");
				}
				location = executor.executeCommandSilently("ls -d -1 " + nodeManager.getFolder() + SEP + "*");
				break;
			case "scp":
				// impossible to provide password to scp command without third party library ssh-pass
				throw new UnsupportedOperationException("not implemented");
			case "file":
				// Strip the protocol from the path
				location = StringUtils.substringAfter(SystemProperty.getFuseZip(), ":");
				break;
			default:
				throw new FaframException("Unsupported protocol " + protocol);
		}

		return location;
	}

	/**
	 * Checks for the maven and sets the maven.home property used by maven-invoker.
	 */
	private static void locateMaven() {
		log.debug("maven.home property is " + System.getProperty("maven.home"));
		if (System.getProperty("maven.home") != null) {
			// Do nothing as the maven.home is what we need
			return;
		}

		log.debug("M2_HOME env property is " + System.getenv("M2_HOME"));
		if (System.getenv("M2_HOME") != null) {
			SystemProperty.set("maven.home", System.getenv("M2_HOME"));
			return;
		}

		log.debug("M2_HOME system property is " + System.getProperty("M2_HOME"));
		if (System.getProperty("M2_HOME") != null) {
			SystemProperty.set("maven.home", System.getProperty("M2_HOME"));
			return;
		}

		final String path = System.getenv("PATH");
		final String[] pathParts = path.split(File.pathSeparator);

		for (String part : pathParts) {
			log.debug("Checking path for mvn: " + part);
			if (part.contains("mvn") || part.contains("maven")) {
				// Strip the /bin from mvn path if found
				final String mvnLocation;
				if (part.contains("bin")) {
					mvnLocation = StringUtils.substringBefore(part, "bin");
				} else {
					mvnLocation = part;
				}

				SystemProperty.set("maven.home", mvnLocation);
				log.debug("Maven found in " + mvnLocation);

				return;
			}
		}
	}

	/**
	 * Gets the maven local repository path.
	 *
	 * @return local repository path
	 */
	private static String getMavenLocalRepository() {
		// Get effective settings from maven
		final InvocationRequest req = new DefaultInvocationRequest();
		req.setGoals(Collections.singletonList("help:evaluate"));
		req.setProperties(new Properties());
		req.getProperties().setProperty("expression", "settings.localRepository");

		final Invoker invoker = new DefaultInvoker();

		String output = null;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos, true, "UTF-8")) {
			// Redirect invoker output to variable
			invoker.setOutputHandler(new PrintStreamHandler(ps, true));
			invoker.execute(req);
			output = baos.toString("UTF-8");
		} catch (IOException | MavenInvocationException e) {
			e.printStackTrace();
		}
		String repoPath = null;
		for (String line : output.split(System.lineSeparator())) {
			if (!line.startsWith("[")) {
				repoPath = line;
				break;
			}
		}
		if (repoPath == null) {
			log.error("Couldn't find local maven repo in '{}'", output);
		}
		log.debug("Local repository path is " + repoPath);
		return repoPath;
	}

	/**
	 * Gets the artifact path.
	 *
	 * @return artifact path
	 */
	private static String getArtifactPath(String localRepositoryPath) {
		final String groupPath = SystemProperty.getFuseGroup().replaceAll("\\.", SEP + SEP);

		// Construct the path to the artifact in local repo
		final String artifactPath = localRepositoryPath + SEP + groupPath + SEP + SystemProperty.getFuseId() + SEP
				+ SystemProperty.getFuseVersion() + SEP + SystemProperty.getFuseId() + "-" + SystemProperty
				.getFuseVersion() + ".zip";

		log.debug("Artifact path is " + artifactPath);

		final File artifact = new File(artifactPath);
		return artifact.getAbsolutePath();
	}
}
