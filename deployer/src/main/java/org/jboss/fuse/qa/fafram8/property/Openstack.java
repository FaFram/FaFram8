package org.jboss.fuse.qa.fafram8.property;

import lombok.Getter;

/**
 * Openstack class. It is just enum of possible javas on Openstack.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public enum Openstack {
	OPENJDK7("${FAFRAM_OPENJDK7_HOME}"), OPENJDK8("${FAFRAM_OPENJDK8_HOME}"), JDK7("${FAFRAM_NATIVE_TOOLS}/jdk1.7.0_last"), JDK8("${FAFRAM_NATIVE_TOOLS}/jdk1.8.0_last"),
	IBM7("${FAFRAM_NATIVE_TOOLS}/ibm-java-70"), IBM8("${FAFRAM_NATIVE_TOOLS}/ibm-java-80");

	@Getter
	private String path;

	/**
	 * Constructor.
	 *
	 * @param path path to java home
	 */
	Openstack(String path) {
		this.path = path;
	}
}
