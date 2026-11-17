package com.rahul.urlshortener.dto;

import java.time.Instant;

public record UrlResponse(
		
		String shortCode, 
		String shortUrl, 
		String originalUrl, 
		Instant expiresAt) {
}
