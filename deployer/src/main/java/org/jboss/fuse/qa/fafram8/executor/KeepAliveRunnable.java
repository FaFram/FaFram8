package org.jboss.fuse.qa.fafram8.executor;

import java.util.TimerTask;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends keep alive message using the specified executor.
 *
 * Created by avano on 17.8.16.
 */
@AllArgsConstructor
@Slf4j
public class KeepAliveRunnable extends TimerTask {
	private Executor executor;

	@Override
	public void run() {
		try {
			log.trace("Sending keepAlive");
			executor.executeCommandSilently("echo keepAlive");
		} catch (Exception ex) {
			// Ignore
		}
	}
}
