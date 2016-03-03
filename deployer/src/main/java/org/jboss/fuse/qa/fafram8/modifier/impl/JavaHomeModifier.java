package org.jboss.fuse.qa.fafram8.modifier.impl;

import org.apache.commons.io.FileUtils;

import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.modifier.Modifier;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import java.io.File;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Java home modifier.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class JavaHomeModifier extends Modifier {
	private String javaHomePath;

	/**
	 * Private constructor.
	 */
	private JavaHomeModifier() {
	}

	/**
	 * Private constructor.
	 *
	 * @param javaHomePath path to java home folder
	 */
	private JavaHomeModifier(String javaHomePath) {
		this.javaHomePath = javaHomePath;
	}

	@Override
	public void execute() {
		if (super.getExecutor() != null) {
			modifyRemoteJavaHome();
		} else {
			modifyLocalJavaHome();
		}
	}

	/**
	 * Sets java home path.
	 *
	 * @param javaHomePath file path to java home folder
	 * @return this
	 */
	public static JavaHomeModifier setJavaHome(String javaHomePath) {
		return new JavaHomeModifier(javaHomePath);
	}

	/**
	 * Modifies JAVA_HOME on local.
	 */
	private void modifyLocalJavaHome() {
		// Files locations
		final File setenv = new File(SystemProperty.getFusePath() + File.separator + "bin" + File.separator + "setenv");
		final File setenvBat = new File(SystemProperty.getFusePath() + File.separator + "bin" + File.separator + "setenv.bat");

		// File content
		final String line = "export JAVA_HOME=" + javaHomePath + "\n";
		try {
			if (!setenvBat.exists()) {
				setenvBat.createNewFile();
			}
			if (!setenv.exists()) {
				setenv.createNewFile();
			}
			if ((System.getProperty("os.name").startsWith("Windows"))) {
				FileUtils.writeStringToFile(setenvBat, line, true);
			} else {
				FileUtils.writeStringToFile(setenv, line, true);
			}
		} catch (Exception ex) {
			log.error("Exception while modifying files: " + ex);
			throw new FaframException(ex);
		}
	}

	/**
	 *Modifies JAVA_HOME on remote.
	 */
	private void modifyRemoteJavaHome() {
		final String path = SystemProperty.getFusePath() + File.separator + "bin" + File.separator + "setenv";
		String content = String.format("export JAVA_HOME=%s%n", javaHomePath);
		// Remove original files
		if ((System.getProperty("os.name").startsWith("Windows"))) {
			// Changes to win
			content = content.replaceAll("export", "SET");
			super.getExecutor().executeCommand("printf \"" + content + "\" >> " + path + ".bat");
		} else {
			// Print content into the files
			super.getExecutor().executeCommand("printf \"" + content + "\" >> " + path);
		}
	}
}
