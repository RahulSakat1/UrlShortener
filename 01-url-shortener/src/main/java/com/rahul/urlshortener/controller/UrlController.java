package com.rahul.urlshortener.controller;

import com.rahul.urlshortener.dto.CreateUrlRequest;
import com.rahul.urlshortener.dto.StatsResponse;
import com.rahul.urlshortener.dto.UrlResponse;
import com.rahul.urlshortener.model.UrlMapping;
import com.rahul.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class UrlController {

	private final UrlService urlService;
	private final String baseUrl;

	public UrlController(UrlService urlService, @Value("${app.base-url}") String baseUrl) {
		this.urlService = urlService;
		this.baseUrl = baseUrl;
	}

	@PostMapping("/api/urls")
	public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
		UrlMapping mapping = urlService.create(request.url(), request.expiryDays());
		UrlResponse body = new UrlResponse(mapping.getShortCode(), baseUrl + "/" + mapping.getShortCode(),
				mapping.getOriginalUrl(), mapping.getExpiresAt());
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/api/urls/{code}/stats")
	public StatsResponse stats(@PathVariable String code) {
		UrlMapping m = urlService.getStats(code);
		return new StatsResponse(m.getShortCode(), m.getOriginalUrl(), m.getClickCount(), m.getCreatedAt(),
				m.getExpiresAt());
	}

	@GetMapping("/{code:[0-9a-zA-Z]{4,16}}")
	public ResponseEntity<Void> redirect(@PathVariable String code) {
		String target = urlService.resolve(code);
		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
	}
}
