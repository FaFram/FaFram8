package org.jboss.fuse.qa.fafram8.modifier.impl;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
import org.jboss.fuse.qa.fafram8.modifier.Modifier;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Adds the default user to the container.
 */
@ToString
@EqualsAndHashCode(callSuper = true, exclude = {"executor"})
@Slf4j
public final class DefaultUserModifier extends Modifier {
	private static PropertyModifier mod;

	private DefaultUserModifier(String ip, String filePath, String key, String value) {
		mod = PropertyModifier.putProperty(ip, filePath, key, value);
	}

	/**
	 * Registers the default user modifier.
	 * @param ip host
	 * @param filePath file path
	 * @param key key
	 * @param value value
	 * @return default user modifier instance
	 */
	public static DefaultUserModifier addDefaultUser(String ip, String filePath, String key, String value) {
		return new DefaultUserModifier(ip, filePath, key, value);
	}

	@Override
	public void execute(Container container) {
		if (!mod.getValue().contains(",")) {
			if (StringUtils.startsWithAny(ContainerManager.getProductVersion(), "6.0", "6.1", "6.2", "6.3")) {
				mod.setValue(mod.getValue() + ",Administrator");
				log.info("Setting role for default user to Administrator");
			} else {
				mod.setValue(mod.getValue() + ",_g_:admingroup");
				log.info("Setting role for default user to group admingroup");
			}
		}
		if (super.getExecutor() != null) {
			mod.setExecutor(super.getExecutor());
		}
		mod.execute(container);
	}
}
