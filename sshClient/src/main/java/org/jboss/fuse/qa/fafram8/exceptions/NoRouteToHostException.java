package org.jboss.fuse.qa.fafram8.exceptions;

/**
 * General exception thrown by SSH.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class NoRouteToHostException extends SSHClientException {
	/**
	 * Constructor.
	 */
	public NoRouteToHostException() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 */
	public NoRouteToHostException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 * @param cause cause
	 */
	public NoRouteToHostException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 *
	 * @param cause cause
	 */
	public NoRouteToHostException(Throwable cause) {
		super(cause);
	}
}
