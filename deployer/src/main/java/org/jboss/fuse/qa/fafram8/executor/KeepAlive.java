package org.jboss.fuse.qa.fafram8.executor;

import java.util.TimerTask;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends keep alive message using the specified executor.
 * <p>
 * Created by avano on 17.8.16.
 */
@AllArgsConstructor
@Slf4j
public class KeepAlive extends TimerTask {
	private Executor executor;

	@Override
	public void run() {
		log.trace("Sending keepAlive to " + executor.getName());
		executor.executeCommandSilently("echo keepAlive", false);
	}
}
