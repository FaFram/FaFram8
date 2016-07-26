package org.jboss.fuse.qa.fafram8.test.provision;

import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.modifier.ModifierExecutor;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.resource.Fafram;
import org.jboss.fuse.qa.fafram8.test.base.FaframTestBase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/**
 *Created by ecervena on 25.9.15.
 */
public class OpenstackXmlProvisionTest {
	@Rule
	//TODO(ecervena): implement enhancement to add timestamp to container name
	public Fafram fafram = new Fafram().provider(FaframProvider.OPENSTACK)
			.withFabric().config("src/test/resources/OpenStackProvisionTestConfig.xml");

	@BeforeClass
	public static void init() {
		System.setProperty(FaframConstant.KEEP_OS_RESOURCES, "false");
		System.setProperty(FaframConstant.FUSE_ZIP, FaframTestBase.CURRENT_LOCAL_URL);
	}

	@AfterClass
	public static void clean() {
		System.clearProperty(FaframConstant.KEEP_OS_RESOURCES);
		System.clearProperty(FaframConstant.FUSE_ZIP);
		SystemProperty.clearAllProperties();
		ModifierExecutor.clearAllModifiers();
	}

	@Test
	public void fabricTest() {
		assertTrue(fafram.executeCommand("container-list | grep ecervena-root123").contains("success"));
		assertTrue(fafram.executeCommand("container-list | grep ecervena-node1123").contains("success"));
	}
}
