package org.jboss.fuse.qa.fafram8.exception;

/**
 * General exception for container.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
public class ContainerException extends RuntimeException {

	/**
	 * Constructor.
	 */
	public ContainerException() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param cause cause
	 */
	public ContainerException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 */
	public ContainerException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message messgae
	 * @param cause cause
	 */
	public ContainerException(String message, Throwable cause) {
		super(message, cause);
	}
}
