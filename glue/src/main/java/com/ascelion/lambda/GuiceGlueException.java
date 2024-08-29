package com.ascelion.lambda;

public class GuiceGlueException extends RuntimeException {
	public GuiceGlueException(String message, Throwable cause) {
		super(message, cause);
	}

	public GuiceGlueException(String message) {
		super(message);
	}
}
