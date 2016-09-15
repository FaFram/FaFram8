package org.jboss.fuse.qa.fafram8.exception;

/**
 * Zip not found exception.
 * Created by avano on 15.9.16.
 */
public class ZipNotFoundException extends RuntimeException {
	/**
	 * Constructor.
	 */
	public ZipNotFoundException() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 */
	public ZipNotFoundException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param cause cause
	 */
	public ZipNotFoundException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param message message
	 * @param cause cause
	 */
	public ZipNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
