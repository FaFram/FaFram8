package org.jboss.fuse.qa.fafram8.test.common;

import static org.junit.Assert.assertTrue;

import org.apache.commons.io.FileUtils;

import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

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
		fafram.executeCommand("echo hi");

		// Now it is required to destroy fafram to wrote command history to file
		fafram.tearDown();

		File path = new File(Paths.get("target", "archived").toAbsolutePath().toString());
		String[] extensions = new String[] { "txt" };
		List<File> files = (List<File>) FileUtils.listFiles(path, extensions, true);

		String content = FileUtils.readFileToString(files.get(0));
		assertTrue(content.contains("echo hello"));
		assertTrue(content.contains("echo hi"));
	}
}
