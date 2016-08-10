package org.jboss.fuse.qa.fafram8.modifier.impl;

import org.apache.commons.io.FileUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.modifier.Modifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * JVM Opts modifier.
 * Created by avano on 8.10.15.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@Slf4j
@EqualsAndHashCode(callSuper = true)
@ToString(of = {"jvmMemOpts"})
public final class JvmMemoryModifier extends Modifier {
	private String xms = "768M";
	private String xmx = "1536M";
	private String permMem = "768M";
	private String maxPermMem = "1536M";

	private List<String> jvmMemOpts = new ArrayList<>();

	/**
	 * Private constructor.
	 */
	private JvmMemoryModifier() {
		jvmMemOpts.add("JAVA_MIN_MEM=" + xms);
		jvmMemOpts.add("JAVA_MAX_MEM=" + xmx);
		jvmMemOpts.add("JAVA_PERM_MEM=" + permMem);
		jvmMemOpts.add("JAVA_MAX_PERM_MEM=" + maxPermMem);
	}

	/**
	 * Private constructor.
	 *
	 * @param jvmOpts JVM options
	 */
	private JvmMemoryModifier(List<String> jvmOpts) {
		this.jvmMemOpts.addAll(jvmOpts);
	}

	@Override
	public void execute(Container container) {
		if (super.getExecutor() != null) {
			modifyRemoteJvmMemOpts(container);
		} else {
			modifyLocalJvmMemOpts(container);
		}
	}

	/**
	 * Sets default jvm options.
	 *
	 * @return jvm options modifier
	 */
	public static JvmMemoryModifier setDefaultJvmMemOpts() {
		return new JvmMemoryModifier();
	}

	/**
	 * Sets specified JVM memory opts.
	 *
	 * @param jvmMemOpts jvm memory opts for setting
	 * @return jvm options modifier
	 */
	public static JvmMemoryModifier setJvmMemOpts(List<String> jvmMemOpts) {
		return new JvmMemoryModifier(jvmMemOpts);
	}

	/**
	 * Modifies JVM Opts on localhost.
	 */
	private void modifyLocalJvmMemOpts(Container container) {
		// Files locations
		final File setenv = new File(container.getFusePath() + File.separator + "bin" + File.separator + "setenv");
		final File setenvBat = new File(container.getFusePath() + File.separator + "bin" + File.separator + "setenv.bat");

		try {
			if (!setenvBat.exists()) {
				setenvBat.createNewFile();
			}
			if (!setenv.exists()) {
				setenv.createNewFile();
			}
			if ((System.getProperty("os.name").startsWith("Windows"))) {
				for (String line : jvmMemOpts) {
					FileUtils.writeStringToFile(setenvBat, "SET " + line + "\n", true);
				}
			} else {
				for (String line : jvmMemOpts) {
					FileUtils.writeStringToFile(setenv, "export " + line + "\n", true);
				}
			}
		} catch (Exception ex) {
			log.error("Exception while modifying files: " + ex);
			throw new FaframException(ex);
		}
	}

	/**
	 * Modifies JVM memory opts on remote.
	 */
	private void modifyRemoteJvmMemOpts(Container container) {
		final String path = container.getFusePath() + File.separator + "bin" + File.separator + "setenv";
		final StringBuilder builder = new StringBuilder();
		for (String line : jvmMemOpts) {
			builder.append("export " + line + "\n");
		}
		String content = builder.toString();
		// Remove original files
		if ((System.getProperty("os.name").startsWith("Windows"))) {
			// Changes to win
			content = content.replaceAll("export", "SET");
			super.getExecutor().executeCommandSilently("printf \"" + content + "\" >> " + path + ".bat");
		} else {
			// Print content into the files
			super.getExecutor().executeCommandSilently("printf \"" + content + "\" >> " + path);
		}
	}
}
