package org.jboss.fuse.qa.fafram8.util;

import org.apache.commons.io.FileUtils;

import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;

/**
 * Command history singleton. Holds the executed commands and responses and it is able to dump all the data into a file.
 * Created by avano on 24.11.15.
 */
@Slf4j
public class CommandHistory {
	private static CommandHistory instance = null;
	private static File file = null;
	private static SimpleDateFormat dateFormat = null;

	/**
	 * Constructor.
	 */
	protected CommandHistory() {
	}

	/**
	 * Instance getter.
	 *
	 * @return instance
	 */
	public static CommandHistory getInstance() {
		if (instance == null) {
			instance = new CommandHistory();
			dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
			file = new File(Paths.get(SystemProperty.getArchiveTarget(), dateFormat.format(new Date()) + ".txt").toAbsolutePath().toString());
		}

		return instance;
	}

	/**
	 * Logs the command and it's response into a file.
	 *
	 * @param cmd command
	 * @param response response
	 */
	public static void log(String cmd, String response) {
		// Force initialization
		CommandHistory.getInstance();

		final StringBuilder builder = new StringBuilder();
		builder.append(dateFormat.format(new Date()))
				.append("\n")
				.append(cmd)
				.append("\n")
				.append(response)
				.append("\n")
				.append("------------------------------------------------------------------")
				.append("\n");
		try {
			FileUtils.write(file, builder.toString(), true);
		} catch (IOException e) {
			throw new FaframException(e);
		}
	}

	/**
	 * Logs the command and it's response into a file.
	 */
	public static void log(String history) {
		if (history == null) {
			return;
		}
		// Force initialization
		CommandHistory.getInstance();
		try {
			FileUtils.write(file, history, true);
		} catch (IOException e) {
			throw new FaframException(e);
		}
	}
}
