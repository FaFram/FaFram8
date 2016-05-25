package org.jboss.fuse.qa.fafram8.modifier.impl;

import org.apache.commons.io.IOUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.modifier.Modifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Random modifier class.
 * Modifier for better performance of Fuse on Openstack machines.
 * Created by avano on 16.9.15.
 */
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true, of = {"host"})
public final class JvmOptsModifier extends Modifier {
	private String additionalJvmOpts = "";

	/**
	 * Private constructor.
	 */
	private JvmOptsModifier(String additionalJvmOpts) {
		this.additionalJvmOpts = additionalJvmOpts;
	}

	/**
	 * Private constructor.
	 */
	private JvmOptsModifier() {
	}

	/**
	 * Factory method.
	 *
	 * @param jvmOptsForRoot additional jvm-opts for random modifier
	 * @return random modifier instance
	 */
	public static JvmOptsModifier addJvmOpts(List<String> jvmOptsForRoot) {
		final StringBuilder builder = new StringBuilder("");
		for (String opt : jvmOptsForRoot) {
			builder.append(" " + opt);
		}
		return new JvmOptsModifier(builder.toString());
	}

	/**
	 * Factory method.
	 *
	 * @return random modifier instance
	 */
	public static JvmOptsModifier addJvmOpts() {
		return new JvmOptsModifier();
	}

	@Override
	public void execute(Container container) {
		if (super.getExecutor() == null) {
			localExecute(container);
		} else {
			remoteExecute(container);
		}
	}

	/**
	 * Adds random modifier to bin/setenv on localhost.
	 */
	public void localExecute(Container container) {
		try {
//			final Container container = ModifierExecutor.getRootContainerByHost("localhost");
			final String filePath = container.getFusePath() + File.separator + "bin" + File.separator + "setenv";
			final FileInputStream fis = new FileInputStream(filePath);
			String content = IOUtils.toString(fis);

			// Default java opts from karaf + randomness location
			content += "\nexport JAVA_OPTS=\"-Xms$JAVA_MIN_MEM -Xmx$JAVA_MAX_MEM -XX:+UnlockDiagnosticVMOptions -XX:+UnsyncloadClass "
					+ additionalJvmOpts + "\"\n";
			final FileOutputStream fos = new FileOutputStream(filePath, false);
			IOUtils.write(content, fos);

			fis.close();
			fos.close();
		} catch (Exception ex) {
			log.error("Error while manipulating the files " + ex);
		}
	}

	/**
	 * Adds random modifier to bin/setenv on remote host.
	 */
	public void remoteExecute(Container container) {
//		final Container container = ModifierExecutor.getRootContainerByHost(super.getExecutor().getClient().getHost());
		final String filePath = container.getFusePath() + File.separator + "bin" + File.separator + "setenv";

		final String response = super.getExecutor().executeCommandSilently("printf \" \nexport JAVA_OPTS=\\\"-Xms\\$JAVA_MIN_MEM -Xmx\\$JAVA_MAX_MEM "
				+ "-XX:+UnlockDiagnosticVMOptions -XX:+UnsyncloadClass -Djava.security.egd=file:/dev/./urandom" + additionalJvmOpts + "\\\" \" >> " + filePath);
		if (!response.isEmpty()) {
			log.error("Setting property on remote host failed. Response should be empty but was: {}.", response);
		}
	}
}
