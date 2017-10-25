package org.jboss.fuse.qa.fafram8.test.common;

import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.exception.ValidatorException;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.After;
import org.junit.Test;

public class BundleValidatorTest {
	private Fafram fafram;

	@Test(expected = ValidatorException.class)
	public void bundleDoesNotExistTest() {
		fafram = new Fafram().containers(
				RootContainer.builder().defaultRoot().build()
		).bundles("src/test/resources/nonexistent.xml");
		fafram.setup();
	}

	@Test
	public void bundleExistsTest() {
		fafram = new Fafram().bundles("src/test/resources/deployTest.xml");
		fafram.setup();
	}

	@After
	public void after() {
		fafram.tearDown(true);
	}
}
