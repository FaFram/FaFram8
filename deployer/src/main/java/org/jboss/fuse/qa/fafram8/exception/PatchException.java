package org.jboss.fuse.qa.fafram8.exception;

/**
 * Exception related to patching issues.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class PatchException extends RuntimeException {

	/**
	 * Constructor.
	 */
	public PatchException() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param cause cause
	 */
	public PatchException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 */
	public PatchException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 * @param cause cause
	 */
	public PatchException(String message, Throwable cause) {
		super(message, cause);
	}
}
