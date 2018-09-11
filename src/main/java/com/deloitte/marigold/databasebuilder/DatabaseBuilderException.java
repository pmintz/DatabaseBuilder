package com.deloitte.marigold.databasebuilder;

public class DatabaseBuilderException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DatabaseBuilderException() {
		super();
	}

	public DatabaseBuilderException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DatabaseBuilderException(String message, Throwable cause) {
		super(message, cause);
	}

	public DatabaseBuilderException(String message) {
		super(message);
	}

	public DatabaseBuilderException(Throwable cause) {
		super(cause);
	}
	
}
