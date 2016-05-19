package org.jboss.fuse.qa.fafram8.exception;

/**
 * Exception used in threads when creating/deleting containers.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class ContainerThreadException extends RuntimeException {

	/**
	 * Constructor.
	 */
	public ContainerThreadException() {
	}

	/**
	 * Constructor.
	 *
	 * @param cause cause
	 */
	public ContainerThreadException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 */
	public ContainerThreadException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 * @param cause cause
	 */
	public ContainerThreadException(String message, Throwable cause) {
		super(message, cause);
	}
}
