package org.jboss.fuse.qa.fafram8.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;

/**
 * Command history for Executor.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class ExecutorCommandHistory {
	private static final int MAX_BUILDER_LENGTH = 10;
	private StringBuilder builder;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	private String name;

	/**
	 * Constructor.
	 *
	 * @param name name of the executor
	 */
	public ExecutorCommandHistory(String name) {
		this.name = name;
		builder = new StringBuilder();
		builder.append("Executor: ").append(name).append("\n\n");
	}

	/**
	 * Logs command and its response to StringBuilder.
	 *
	 * @param cmd executed command
	 * @param response response of the executed command
	 */
	public void log(String cmd, String response) {
		builder.append(dateFormat.format(new Date()))
				.append("\n")
				.append("Command: ").append(cmd)
				.append("\n")
				.append("Response: ").append(response)
				.append("\n")
				.append("------------------------------------------------------------------")
				.append("\n");
	}

	/**
	 * Returns the StringBuilder with all logged messages.
	 *
	 * @return final String of all executed commands
	 */
	public String getLog() {
		if (this.name == null) {
			return "";
		}
		if (builder.length() >= MAX_BUILDER_LENGTH) {
			builder.append("\n================================================================================================\n");
		}

		return builder.toString();
	}
}
