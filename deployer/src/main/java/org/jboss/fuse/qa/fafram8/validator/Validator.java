package org.jboss.fuse.qa.fafram8.validator;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.exception.ValidatorException;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.patcher.Patcher;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import java.io.File;
import java.net.InetAddress;

import lombok.extern.slf4j.Slf4j;

/**
 * Properties validator class. Validates if specified property combinations are valid with given deployment mode.
 * Created by avano on 21.9.15.
 */
@Slf4j
public final class Validator {
	/**
	 * Private constructor.
	 */
	private Validator() {
	}

	/**
	 * Validate method.
	 */
	public static void validate() {
		log.info("Validating containers...");
		validateContainers();
		log.info("Validation complete!");
	}

	/**
	 * Validates containers in container list.
	 */
	private static void validateContainers() {
		validatePatch();
		for (Container c : ContainerManager.getContainerList()) {
			if (c instanceof RootContainer) {
				validateRootContainer(c);
			} else if (c instanceof ChildContainer) {
				validateChildContainer(c);
			} else {
				validateSshContainer(c);
			}
		}
	}

	/**
	 * Validates root container.
	 *
	 * @param c root container
	 */
	private static void validateRootContainer(Container c) {
		if (SystemProperty.getProvider().equals(FaframProvider.STATIC)) {
			if (c.getNode() == null) {
				throw new ValidatorException("Root container (" + c.getName() + ") node is null!");
			}
			validateZip(c.getNode().getHost() == null ? SystemProperty.getHost() : c.getNode().getHost());
			validateHost();
		} else {
			validateZip(null);
		}

		if (c.getName() == null || "".equals(c.getName())) {
			throw new ValidatorException("Root name can't be empty!");
		}

		if (SystemProperty.getProvider().equals(FaframProvider.STATIC)) {
			if (c.getNode().getPort() == 0 || c.getNode().getUsername() == null
					|| c.getNode().getPassword() == null) {
				throw new ValidatorException("Atleast one of root container node attributes (" + c.getName() + ") is not set!");
			}
		}

		if (c.getParent() != null || c.getParentName() != null) {
			throw new ValidatorException("Root container (" + c.getName() + ") can't have a parent! (Parent container"
					+ " or parent container name was set)");
		}
	}

	/**
	 * Validates child container.
	 *
	 * @param c child container
	 */
	private static void validateChildContainer(Container c) {
		if ("".equals(c.getName()) || c.getName() == null) {
			throw new ValidatorException("Child name can't be empty!");
		}

		if (c.getParentName() == null && c.getParent() == null) {
			throw new ValidatorException("Child container (" + c.getName() + ") must have a parent!");
		}

		if (c.getParentName() != null) {
			if (ContainerManager.getContainer(c.getParentName()) == null) {
				throw new ValidatorException(String.format("Parent of %s (%s) does not exist in container list!", c.getName(), c.getParentName()));
			}
		}
	}

	/**
	 * Validates ssh container.
	 *
	 * @param c container
	 */
	private static void validateSshContainer(Container c) {
		if ("".equals(c.getName())) {
			throw new ValidatorException("SSH container name can't be empty!");
		}

		if (SystemProperty.getProvider().equals(FaframProvider.STATIC)) {
			if (c.getNode() == null) {
				throw new ValidatorException("SSH container node is null!");
			}

			if (c.getNode().getHost() == null || c.getNode().getUsername() == null || c.getNode().getPassword() == null) {
				throw new ValidatorException("Atleast one of ssh container (" + c.getName() + ") node attributes is not set!");
			}
		}

		if (c.getParentName() == null && c.getParent() == null) {
			throw new ValidatorException("SSH container (" + c.getName() + ") must have a parent!");
		}

		if (c.getParentName() != null) {
			if (ContainerManager.getContainer(c.getParentName()) == null) {
				throw new ValidatorException(String.format("Parent of %s (%s) does not exist in container list!", c.getName(), c.getParentName()));
			}
		}
	}

	/**
	 * Validates zip property.
	 */
	private static void validateZip(String host) {
		validateNonNullZip(host);
		validateNullZip(host);
	}

	/**
	 * Validates null zip.
	 */
	private static void validateNullZip(String host) {
		final String zipFile = SystemProperty.getFuseZip();

		if ((!"localhost".equals(host) || !SystemProperty.getProvider().equals(FaframProvider.STATIC)) && zipFile == null) {
			throw new ValidatorException(FaframConstant.FUSE_ZIP + " property is not set on remote!");
		}

		// If the maven properties are not set and not using custom zip
		if (zipFile == null && (SystemProperty.getFuseId() == null || SystemProperty.getFuseGroup() == null || SystemProperty.getFuseVersion()
				== null)) {
			throw new ValidatorException("Maven properties and zip file properties are not set!");
		}
	}

	/**
	 * Validates non null zip.
	 */
	private static void validateNonNullZip(String host) {
		final String zipFile = SystemProperty.getFuseZip();
		if ("".equals(zipFile)) {
			throw new ValidatorException(FaframConstant.FUSE_ZIP + " property is empty!");
		}

		// If we are on localhost and using custom zip
		if ("localhost".equals(host) && (zipFile != null && zipFile.startsWith("file")) && !SystemProperty.getProvider().equals(FaframProvider.OPENSTACK)) {
			if (!new File(StringUtils.substringAfter(zipFile, "file:")).exists()) {
				throw new ValidatorException(String.format("Specified file (%s) does not exist!", zipFile));
			}
		}
	}

	/**
	 * Validates host property.
	 */
	private static void validateHost() {
		final int timeout = 30000;
		final String host = SystemProperty.getHost();

		if ("".equals(host)) {
			throw new ValidatorException(FaframConstant.HOST + " property is empty!");
		}

		if (!"localhost".equals(host)) {
			try {
				final InetAddress inet = InetAddress.getByName(host);
				if (!inet.isReachable(timeout)) {
					throw new ValidatorException(String.format("Specified host (%s) is not reachable!", host));
				}
			} catch (Exception ex) {
				throw new ValidatorException("Unknown host " + host + "!");
			}
		}
	}

	/**
	 * Validates patch property.
	 */
	private static void validatePatch() {
		final String patch = SystemProperty.getPatch();

		// Do nothing if we don't have a patch
		if (patch == null || "".equals(patch)) {
			return;
		}

		// If we get patch by name substring
		if (!StringUtils.startsWithAny(patch, "http", "file", "scp")) {
			if (Patcher.getPatches().length == 0) {
				throw new ValidatorException(String.format("Specified patch (%s) not found", patch));
			}
		}

		if (patch.startsWith("file")) {
			if (!new File(StringUtils.substringAfter(patch, "file://")).exists()) {
				throw new ValidatorException(String.format("Specified file (%s) does not exist!", patch));
			}
		}
	}
}
