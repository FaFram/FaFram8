package org.jboss.fuse.qa.fafram8.exception;

/**
 * Exception used for problems related to provision of container.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class ProvisionException extends RuntimeException {

	/**
	 * Constructor.
	 */
	public ProvisionException() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param cause cause
	 */
	public ProvisionException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 */
	public ProvisionException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 * @param cause cause
	 */
	public ProvisionException(String message, Throwable cause) {
		super(message, cause);
	}
}
