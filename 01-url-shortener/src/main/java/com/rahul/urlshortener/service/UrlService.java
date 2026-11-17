package com.rahul.urlshortener.service;

import com.rahul.urlshortener.exception.UrlExpiredException;
import com.rahul.urlshortener.exception.UrlNotFoundException;
import com.rahul.urlshortener.model.UrlMapping;
import com.rahul.urlshortener.repository.UrlMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class UrlService {

	private static final Logger log = LoggerFactory.getLogger(UrlService.class);

	private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int CODE_LENGTH = 7;
	private static final String CACHE_PREFIX = "url:";
	private static final Duration MAX_CACHE_TTL = Duration.ofHours(1);

	private final SecureRandom random = new SecureRandom();
	private final UrlMappingRepository repository;
	private final StringRedisTemplate redis;

	public UrlService(UrlMappingRepository repository, StringRedisTemplate redis) {
		this.repository = repository;
		this.redis = redis;
	}

	public UrlMapping create(String originalUrl, Integer expiryDays) {
		UrlMapping mapping = new UrlMapping();
		mapping.setShortCode(generateUniqueCode());
		mapping.setOriginalUrl(originalUrl);
		mapping.setCreatedAt(Instant.now());
		if (expiryDays != null) {
			mapping.setExpiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS));
		}
		return repository.save(mapping);
	}

	/**
	 * Returns the original URL for a short code. Redis first, PostgreSQL on a cache
	 * miss.
	 */
	public String resolve(String code) {
		String cached = cacheGet(code);
		if (cached != null) {
			repository.incrementClicks(code);
			return cached;
		}

		UrlMapping mapping = repository.findByShortCode(code).orElseThrow(() -> new UrlNotFoundException(code));

		if (mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(Instant.now())) {
			throw new UrlExpiredException(code);
		}

		cachePut(code, mapping.getOriginalUrl(), cacheTtl(mapping));
		repository.incrementClicks(code);
		return mapping.getOriginalUrl();
	}

	public UrlMapping getStats(String code) {
		return repository.findByShortCode(code).orElseThrow(() -> new UrlNotFoundException(code));
	}

	private Duration cacheTtl(UrlMapping mapping) {
		if (mapping.getExpiresAt() == null) {
			return MAX_CACHE_TTL;
		}
		Duration remaining = Duration.between(Instant.now(), mapping.getExpiresAt());
		return remaining.compareTo(MAX_CACHE_TTL) < 0 ? remaining : MAX_CACHE_TTL;
	}

	
	private String cacheGet(String code) {
		try {
			
			return redis.opsForValue().get(CACHE_PREFIX + code);
		
		} catch (RuntimeException ex) {
		
			log.warn("Redis read failed, falling back to database: {}", ex.getMessage());
			
			return null;
		}
	}

	private void cachePut(String code, String url, Duration ttl) {
		try {
			redis.opsForValue().set(CACHE_PREFIX + code, url, ttl);
		} catch (RuntimeException ex) {
			log.warn("Redis write failed: {}", ex.getMessage());
		}
	}

	private String generateUniqueCode() {
		for (int attempt = 0; attempt < 10; attempt++) {
		
			String code = randomCode();
			
			if (!repository.existsByShortCode(code)) {
				
				return code;
			}
		}
		throw new IllegalStateException("Could not generate a unique short code");
	}

	private String randomCode() {
		
		StringBuilder sb = new StringBuilder(CODE_LENGTH);
		
		for (int i = 0; i < CODE_LENGTH; i++) {
			
			sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}
}
