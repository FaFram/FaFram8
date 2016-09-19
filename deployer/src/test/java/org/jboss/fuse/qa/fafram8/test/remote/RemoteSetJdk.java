package org.jboss.fuse.qa.fafram8.test.remote;

import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.Openstack;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.After;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Test for setting JDK.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class RemoteSetJdk {
	private Fafram fafram;

	@After
	public void tearDown() throws Exception {
		System.clearProperty(FaframConstant.JAVA_HOME);
		fafram.tearDown();
	}

	@Test
	public void testOpenstackJdk7() throws Exception {
		final Container root = RootContainer.builder().name("test-os-jdk7-root").defaultRoot().build();
		fafram = new Fafram().withFabric().containers(root).jdk(Openstack.JDK7);

		fafram.setup();

		assertTrue(fafram.executeNodeCommand("ps aux | grep karaf").contains("/qa/tools/opt/x86_64/jdk1.7.0"));
	}

	@Test
	public void testSetJavaPath() throws Exception {
		final String javaPath = "/qa/tools/opt/jdk1.7.0_last/";
		final Container root = RootContainer.builder().name("test-os-jdk-path-root").defaultRoot().build();
		fafram = new Fafram().withFabric().containers(root).jdk(javaPath);
		fafram.setup();

		assertTrue(fafram.executeNodeCommand("ps aux | grep karaf").contains(javaPath));
	}

	@Test
	public void testOverrideJavaHomeWithSystemProperty() throws Exception {
		System.setProperty(FaframConstant.JAVA_HOME, Openstack.JDK8.getPath());
		final Container root = RootContainer.builder().name("test-java-property-root").defaultRoot().build();
		fafram = new Fafram().withFabric().containers(root).jdk(Openstack.JDK7);
		fafram.setup();

		assertTrue(fafram.executeNodeCommand("ps aux | grep karaf").contains("/qa/tools/opt/x86_64/jdk1.8.0"));
	}
}
