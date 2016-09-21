package org.jboss.fuse.qa.fafram8.test.local;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Local archive files test.
 * Created by avano on 9.10.15.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocalArchiveTest {
	private static String firstTestPath = "";

	@Test
	public void testA() {
		final Fafram fafram = new Fafram().suppressStart().archive("etc/users.properties").setup();
		final String baseDir = SystemProperty.getBaseDir();
		final String dirStructure;
		if (System.getenv("WORKSPACE") != null) {
			final String[] path = fafram.getProductPath().split(Pattern.quote(File.separator));
			dirStructure = path[path.length - 2] + File.separator + path[path.length - 1];
		} else {
			dirStructure = StringUtils.substringAfter(fafram.getProductPath(), baseDir + File.separator + "target");
		}
		fafram.tearDown();
		if (System.getenv("WORKSPACE") != null) {
			firstTestPath = Paths.get(baseDir, "deployer/target/archived", dirStructure, "etc", "users.properties").toAbsolutePath().toString();
		} else {
			firstTestPath = Paths.get(baseDir, "target", "archived", dirStructure, "etc", "users.properties").toAbsolutePath().toString();
		}
		assertTrue(new File(firstTestPath).exists());
	}

	@Test
	public void testB() {
		final Fafram fafram = new Fafram().suppressStart().archive("etc/users.properties").setup();
		final String baseDir = SystemProperty.getBaseDir();
		final String dirStructure;
		if (System.getenv("WORKSPACE") != null) {
			final String[] path = fafram.getProductPath().split(Pattern.quote(File.separator));
			dirStructure = path[path.length - 2] + File.separator + path[path.length - 1];
		} else {
			dirStructure = StringUtils.substringAfter(fafram.getProductPath(), baseDir + File.separator + "target");
		}
		fafram.tearDown();
		final String path;
		if (System.getenv("WORKSPACE") != null) {
			path = Paths.get(baseDir, "deployer/target/archived", dirStructure, "etc", "users.properties").toAbsolutePath().toString();
		} else {
			path = Paths.get(baseDir, "target", "archived", dirStructure, "etc", "users.properties").toAbsolutePath().toString();
		}

		assertTrue(new File(path).exists());
		// Assert that the first test archive is still present
		assertTrue(new File(firstTestPath).exists());
	}
}
