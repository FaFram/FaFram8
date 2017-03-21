package org.jboss.fuse.qa.fafram8.ssh;

import org.slf4j.Logger;

/**
 * Logger for JSCH.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class JSCHLogger implements com.jcraft.jsch.Logger {
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(JSCHLogger.class);

	@Override
	public boolean isEnabled(int pLevel) {
		return true;
	}

	@Override
	public void log(int pLevel, String pMessage) {
		log.trace(pMessage);
	}
}
