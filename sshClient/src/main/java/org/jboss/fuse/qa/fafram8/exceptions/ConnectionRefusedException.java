package org.jboss.fuse.qa.fafram8.exceptions;

/**
 * General exception when connection is refused by SSH. This should help with filtering common exception and more serious.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class ConnectionRefusedException extends SSHClientException {
	/**
	 * Constructor.
	 */
	public ConnectionRefusedException() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 */
	public ConnectionRefusedException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 * @param cause cause
	 */
	public ConnectionRefusedException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 *
	 * @param cause cause
	 */
	public ConnectionRefusedException(Throwable cause) {
		super(cause);
	}
}
