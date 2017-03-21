package org.jboss.fuse.qa.fafram8.exceptions;

/**
 * Common exception in SSH client that will be thrown if session timeouts.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class SessionTimeoutException extends SSHClientException {
	/**
	 * Constructor.
	 */
	public SessionTimeoutException() {
	}

	/**
	 * Constructor.
	 *
	 * @param cause cause
	 */
	public SessionTimeoutException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 */
	public SessionTimeoutException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 * @param cause cause
	 */
	public SessionTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}
}
