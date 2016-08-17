package org.jboss.fuse.qa.fafram8.test.common;

import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for additional.commands property.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class AdditionalCommandsTest {
	private static String profileName = "fafram-command-profile";

	@Rule
	public Fafram fafram = new Fafram().name("testroot").withFabric()
			.commands("profile-create --parent default fafram-command-profile", "profile-edit -f camel-jetty fafram-command-profile");

	@BeforeClass
	public static void beforeClass() {
		System.setProperty(FaframConstant.ADDITIONAL_COMMANDS,
				"profile-create --parent default test-profile; fabric:profile-edit --pid io.fabric8.agent/org.ops4j.pax.url.mvn.repositories="
						+ "'file:${runtime.home}/${karaf.default.repository}@snapshots@id=karaf-default, file:${runtime.data}/maven/upload@snapshots@id=fabric-upload' "
						+ profileName + ";profile-edit -f camel-jms " + profileName);
	}

	@Test
	public void additionalCommandsTest() throws Exception {
		assertTrue(fafram.executeCommand("profile-list").contains("test-profile"));
		assertTrue(fafram.executeCommand("profile-display " + profileName).contains("camel-jms"));
		assertTrue(fafram.executeCommand("profile-display " + profileName).contains("file:${runtime.home}/${karaf.default.repository}@snapshots@id=karaf-default"));
		assertTrue(fafram.executeCommand("profile-display " + profileName).contains("file:${runtime.data}/maven/upload@snapshots@id=fabric-upload"));
	}

	@AfterClass
	public static void afterClass() {
		System.clearProperty(FaframConstant.ADDITIONAL_COMMANDS);
	}
}
