package org.jboss.fuse.qa.fafram8.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class ContainerCommandHistory {
	private StringBuilder builder = new StringBuilder();
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	public ContainerCommandHistory(String name) {
		builder.append("Executor: ").append(name).append("\n");
	}

	public void log(String cmd, String response) {
		builder.append(dateFormat.format(new Date()))
				.append("\n")
				.append(cmd)
				.append("\n")
				.append(response)
				.append("\n")
				.append("------------------------------------------------------------------")
				.append("\n");
	}

	public String getLog() {
		if (builder.length() == 0) {
			return null;
		}
		builder.append("================================================================================================================").append("\n");
		return builder.toString();
	}
}
