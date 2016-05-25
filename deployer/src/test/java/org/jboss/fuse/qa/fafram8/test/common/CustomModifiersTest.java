package org.jboss.fuse.qa.fafram8.test.common;

import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.modifier.Modifier;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom modifier test.
 * Created by avano on 25.2.16.
 */
@Slf4j
public class CustomModifiersTest {
	private Fafram fafram;

	@Test
	public void customModifiersInTest() {
		fafram = new Fafram().suppressStart().setup();
		fafram.modifiers(new MyModifier());

		assertTrue(new File(fafram.getProductPath() + "/modifier.log").exists());
	}

	@Test
	public void customModifiersStartupTest() {
		fafram = new Fafram().suppressStart().modifiers(new MyModifier()).setup();

		assertTrue(new File(fafram.getProductPath() + "/modifier.log").exists());
	}

	@After
	public void clean() {
		if (fafram != null) {
			fafram.tearDown();
		}
	}

	class MyModifier extends Modifier {
		@Override
		public void execute(Container container) {
			try {
				new File(container.getFusePath() + "/modifier.log").createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
