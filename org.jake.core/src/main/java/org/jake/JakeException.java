package org.jake;

/**
 * Exception launched to break the build.
 * Throw this exception when you want to break the build for whatever reason (compilation error, test failures, ...).
 * 
 * @author Jerome Angibaud
 */
public class JakeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JakeException(String message) {
		super(message);
	}

}
