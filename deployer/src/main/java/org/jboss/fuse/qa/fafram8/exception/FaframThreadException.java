package org.jboss.fuse.qa.fafram8.exception;

/**
 * Exception used by threads when creating cluster.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class FaframThreadException extends RuntimeException {

	/**
	 * Constructor.
	 */
	public FaframThreadException() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param cause cause
	 */
	public FaframThreadException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 */
	public FaframThreadException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 * @param cause cause
	 */
	public FaframThreadException(String message, Throwable cause) {
		super(message, cause);
	}
}
