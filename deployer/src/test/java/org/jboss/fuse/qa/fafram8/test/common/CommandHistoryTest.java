package org.jboss.fuse.qa.fafram8.test.common;

import static org.junit.Assert.assertTrue;

import org.apache.commons.io.FileUtils;

import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

/**
 * Command history test.
 * Created by avano on 24.11.15.
 */
@Slf4j
public class CommandHistoryTest {
	private Fafram fafram = new Fafram();

	@Test
	public void commandHistoryTest() throws Exception {
		fafram.setup();
		fafram.executeCommand("echo hello");
		File path = new File(Paths.get("target", "archived").toAbsolutePath().toString());
		final String[] files = path.list();
		Arrays.sort(files);
		final String fileName = files[files.length - 1];
		String content = FileUtils.readFileToString(new File(Paths.get("target", "archived", fileName).toAbsolutePath().toString()));
		assertTrue(content.contains("echo hello"));
		fafram.executeCommand("echo hi");
		content = FileUtils.readFileToString(new File(Paths.get("target", "archived", fileName).toAbsolutePath().toString()));
		assertTrue(content.contains("echo hi"));
	}

	@After
	public void after() {
		if (fafram != null) {
			fafram.tearDown();
		}
	}
}
