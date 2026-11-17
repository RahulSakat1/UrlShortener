package com.rahul.urlshortener.exception;

public class UrlExpiredException extends RuntimeException {
	public UrlExpiredException(String code) {
		
		super("This link has expired: " + code);
	}
}
