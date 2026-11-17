package com.rahul.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(UrlNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(UrlNotFoundException ex) {

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(UrlExpiredException.class)
	public ResponseEntity<Map<String, String>> handleExpired(UrlExpiredException ex) {

		return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("error", "Validation failed");
		body.put("details", fieldErrors);
		return ResponseEntity.badRequest().body(body);
	}
}
