package org.jboss.fuse.qa.fafram8.patcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.ssh.SSHClient;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Patcher class. This class provides the container URL for the patching mechanism.
 * Created by avano on 1.9.15.
 */
@Slf4j
public final class Patcher {

	private Patcher() {
	}

	/**
	 * Gets the patches on localhost.
	 *
	 * @return array of string with file uris
	 */
	public static String[] getPatches() {
		return getPatches(null);
	}

	/**
	 * Gets the patches on remote.
	 *
	 * @param client ssh client
	 * @return array of string with file uris
	 */
	public static String[] getPatches(SSHClient client) {
		final String patch = SystemProperty.getPatch();

		// If the property is not set, return empty array
		if (patch == null || "".equals(patch)) {
			return new String[0];
		}

		if ("latest".equals(patch)) {
			return getPatchFromDefaultLocation();
		}

		String prefix;
		try {
			prefix = StringUtils.substringBefore(patch, ":");
		} catch (Exception ignored) {
			// We have no protocol, we are getting patches by name
			return getPatchByName();
		}

		switch (prefix) {
			case "http":
			case "file":
				return new String[] {patch};
			case "scp":
				return getPatchFromScp(client);
			default:
				return getPatchByName();
		}
	}

	/**
	 * Returns list of available patches.
	 *
	 * @param c container to check fot patches
	 * @return list of names of patches
	 */
	public static List<String> getPatchNames(Container c) {
		final List<String> patchNames = new ArrayList<>();
		final String[] result = c.executeCommand("patch:list | grep -v installed").split("\\r?\\n");

		for (String patch : result) {
			patchNames.add(patch.split("\\s+")[0].trim());
		}

		return patchNames;
	}

	/**
	 * Gets the patch from default location (patchFolder/latest directory).
	 *
	 * @return array of string with file uris
	 */
	private static String[] getPatchFromDefaultLocation() {
		// Parse the fuse version, returns 6.1 or 6.2, etc.
		final String version = StringUtils.substring(SystemProperty.getFuseVersion(), 0, 3);

		// Path to default patch directory
		final File f = new File(SystemProperty.getPatchDir() + File.separator + "latest");

		// Get only the patches for current version
		final FilenameFilter versionFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.contains(version);
			}
		};

		final File[] patches = f.listFiles(versionFilter);
		final List<String> ret = new ArrayList<>();

		for (File file : patches) {
			ret.add(file.toURI().toString());
		}

		return ret.toArray(new String[ret.size()]);
	}

	/**
	 * Gets the patch from scp location.
	 *
	 * @param client ssh client
	 * @return array of string with file uris
	 */
	private static String[] getPatchFromScp(SSHClient client) {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	 * Gets the patches by the name.
	 *
	 * @return array of string with file uris
	 */
	private static String[] getPatchByName() {
		final String patch = SystemProperty.getPatch();

		// Parse the fuse version, returns 6.1 or 6.2, etc.
		final String version = StringUtils.substring(SystemProperty.getFuseVersion(), 0, 3);

		final String[] patchNames = patch.split(",");

		Arrays.sort(patchNames);

		// Get all the files
		final Collection<File> files = FileUtils.listFiles(new File(SystemProperty.getPatchDir()),
				new WildcardFileFilter("*" + version + "*"), DirectoryFileFilter.DIRECTORY);

		final List<File> fileList = new ArrayList<>();
		for (File f : files) {
			if (f.getName().contains(version)) {
				fileList.add(f);
			}
		}

		final List<String> patchPathList = new ArrayList<>();

		for (String patchString : patchNames) {
			for (File f : fileList) {
				if (f.getName().contains(patchString)) {
					patchPathList.add(f.toURI().toString());
					break;
				}
			}
		}

		log.debug("Patch location:");
		for (String s : patchPathList) {
			log.debug(s);
		}

		return patchPathList.toArray(new String[patchPathList.size()]);
	}
}
