package org.jboss.fuse.qa.fafram8.exception;

/**
 * Exception used for problems in Executor related to connection to different SSH servers.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class ConnectionException extends RuntimeException {

	/**
	 * Constructor.
	 */
	public ConnectionException() {
	}

	/**
	 * Constructor.
	 *
	 * @param cause cause
	 */
	public ConnectionException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 */
	public ConnectionException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 * @param cause cause
	 */
	public ConnectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
