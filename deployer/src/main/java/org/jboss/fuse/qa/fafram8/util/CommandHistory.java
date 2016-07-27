package org.jboss.fuse.qa.fafram8.util;

import org.apache.commons.io.FileUtils;

import org.jboss.fuse.qa.fafram8.cluster.container.ChildContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;
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
	 * Logs the command history of one Executor to file.
	 *
	 * @param history whole history of commands and responses from ExecutorCommandHistory
	 */
	public static void writeLogToFile(String history) {
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

	/**
	 * Writes all logs from all Executors to a file.
	 */
	public static void writeLogs() {
		for (Container c : ContainerManager.getContainerList()) {
			if (c.getNode() != null && !(c instanceof ChildContainer)) {
				if (c.getNode().getExecutor() != null) {
					writeLogToFile(c.getNode().getExecutor().getHistory().getLog());
				}
			}
		}

		for (Container c : ContainerManager.getContainerList()) {
			if (c.getExecutor() != null) {
				writeLogToFile(c.getExecutor().getHistory().getLog());
			}
		}

		// This delimeter is added after one test case
		writeLogToFile("\n////////////////////////////////////////////////////////////////////////////////////////////////////////////////\n");
		writeLogToFile("****************************************************************************************************************");
		writeLogToFile("\n////////////////////////////////////////////////////////////////////////////////////////////////////////////////\n\n");
	}
}
