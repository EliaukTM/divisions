package com.haymai.division;

public class InvalidCodeException extends RuntimeException {
	public InvalidCodeException(String s) {
		super(s, new Throwable());
	}
}
