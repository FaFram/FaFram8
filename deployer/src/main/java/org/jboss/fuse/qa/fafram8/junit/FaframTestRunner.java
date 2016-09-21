package org.jboss.fuse.qa.fafram8.junit;

import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.Version;

/**
 * Fafram JUnit Test runner class.
 * Created by avano on 29.10.15.
 */
@Slf4j
public class FaframTestRunner extends BlockJUnit4ClassRunner {
	private JiraClient jiraClient;
	private StringBuilder statusLine;

	/**
	 * Constructor.
	 *
	 * @param klass class
	 * @throws InitializationError initialization error
	 */
	public FaframTestRunner(Class<?> klass) throws InitializationError {
		super(klass);
		try {
			jiraClient = createClient();
		} catch (JiraException e) {
			log.warn("Couldn't create Jira client because of: " + e);
		}
		statusLine = new StringBuilder();
	}

	@Override
	public void runChild(FrameworkMethod method, RunNotifier notifier) {
		final Jira jira = method.getAnnotation(Jira.class);
		if (jira == null) {
			log.info("Starting " + method.getName());
			super.runChild(method, notifier);
			return;
		}

		boolean skip = false;

		for (String jiraId : jira.value()) {
			if (!handleJira(method, notifier, jiraId)) {
				skip = true;
				break;
			}
		}
		if (!skip) {
			if (statusLine.length() > 0) {
				statusLine.deleteCharAt(statusLine.length() - 1);
				log.info(String.format("Starting %s (%s)", method.getName(), statusLine.toString()));
			}
			statusLine = new StringBuilder();
			super.runChild(method, notifier);
		}
	}

	/**
	 * Checks if the JIRA fix version is the current version. It first parses the major version from the fixVersion list and checks if the
	 * major version is substring of current version.
	 *
	 * @param issue issue to check
	 * @return boolean true if the fixed version is current version, false otherwise
	 */
	private boolean matchVersion(Issue issue) {
		// Additional check if fixed version == current version
		final List<Version> fixVersions = issue.getFixVersions();
		// Resolved as wont fix for example
		if (fixVersions.size() == 0) {
			return true;
		}

		final String currentShortVersion = StringUtils.substringBefore(SystemProperty.getFuseVersion(), ".redhat");

		for (Version v : fixVersions) {
			final String versionShort = StringUtils.substringAfterLast(v.toString(), "-");
			if (currentShortVersion.compareTo(versionShort) >= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Handles the jira status.
	 * @param method current method
	 * @param notifier run notifier
	 * @param jiraValue jira ID
	 * @return true if the test should be run, false otherwise
	 */
	private boolean handleJira(FrameworkMethod method, RunNotifier notifier, String jiraValue) {
		if (jiraClient == null) {
			// If the jira client couldn't be initialized
			return true;
		}

		final Issue issue;
		try {
			issue = jiraClient.getIssue(jiraValue);
		} catch (Exception e) {
			log.warn("Running test, ignoring jira exception: " + e.getLocalizedMessage());
			return true;
		}

		if (!((StringUtils.equalsIgnoreCase(issue.getStatus().toString(), "resolved")
				|| StringUtils.equalsIgnoreCase(issue.getStatus().toString(), "closed")))) {
			log.info(String.format("Ignoring %s (JIRA %s is %s)", method.getName(), jiraValue.toUpperCase(), issue.getStatus().toString()));
			notifier.fireTestIgnored(describeChild(method));
			return false;
		} else {
			if (matchVersion(issue)) {
				statusLine.append(String.format("JIRA %s is %s", jiraValue.toUpperCase(), issue.getStatus().toString()));
				statusLine.append(",");
				return true;
			} else {
				log.info(String.format("Ignoring %s, versions do not match (Current version < Fix version)", method.getName()));
				notifier.fireTestIgnored(describeChild(method));
				return false;
			}
		}
	}

	/**
	 * Creates the jira client instance.
	 *
	 * @return jira client instance
	 */
	private JiraClient createClient() throws JiraException {
		if (SystemProperty.getExternalProperty(FaframConstant.JIRA_USER) == null
				|| SystemProperty.getExternalProperty(FaframConstant.JIRA_PASSWORD) == null) {
			return new JiraClient(SystemProperty.getExternalProperty(FaframConstant.JIRA_URL));
		} else {
			final BasicCredentials credentials = new BasicCredentials(SystemProperty.getExternalProperty(FaframConstant.JIRA_USER),
					SystemProperty.getExternalProperty(FaframConstant.JIRA_PASSWORD));
			return new JiraClient(SystemProperty.getExternalProperty(FaframConstant.JIRA_URL), credentials);
		}
	}
}
