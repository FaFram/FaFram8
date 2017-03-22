package org.jboss.fuse.qa.fafram8.exceptions;

/**
 * Auth fail exception thrown by JSCH when SSH is still not running correctly.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class AuthFailException extends SSHClientException {

	/**
	 * Constructor.
	 */
	public AuthFailException() {
	}

	/**
	 * Constructor.
	 *
	 * @param cause cause
	 */
	public AuthFailException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 */
	public AuthFailException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 * @param cause cause
	 */
	public AuthFailException(String message, Throwable cause) {
		super(message, cause);
	}
}
