package org.jboss.fuse.qa.fafram8.test.common;

import static org.junit.Assert.fail;

import org.jboss.fuse.qa.fafram8.junit.FaframTestRunner;
import org.jboss.fuse.qa.fafram8.junit.Jira;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test runner test.
 * Created by avano on 29.10.15.
 */
@RunWith(FaframTestRunner.class)
public class TestRunnerTest {
	@Test
	@Jira("ENTESB-64") // Let's hope it will remain open
	public void shouldSkipOpenTest() {
		fail("Test shouldn't be run when the issue is still in open state");
	}

	@Test
	@Jira("ENTESB-2") // Let's hope it will remain closed
	public void shouldRunClosedTest() {
	}

	@Test
	@Jira("ENTESB-3") // Let's hope it will remain resolved
	public void shouldRunResolvedTest() {
	}

	@Test
	@Jira("ENTESB-128") // Resolved security issue
	public void shouldRunSecurityIssue() {
	}

	@Test
	@Jira("ENTESB-3530")
	public void shouldRunSameVersion() {
	}

	@Test
	@Jira("ENTESB-2930")
	public void shouldRunOlderVersion() {
	}

	@Test
	@Jira("ENTESB-4894")
	public void shouldntRunNewerVersion() {
		fail("Test shouldn't be run when the issue has higher fix version than current version");
	}

	@Test
	@Jira({"ENTESB-3", "ENTESB-64"})
	public void shouldSkipOneOpen() {
		fail("Test shouldn't run because one issue is still open");
	}

	@Test
	@Jira({"ENTESB-3", "ENTESB-2"})
	public void shouldRunBothValid() {
	}
}
