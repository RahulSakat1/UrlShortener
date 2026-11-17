package com.rahul.urlshortener.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Fixed-window rate limiter: each client IP may call /api/** a limited number
 * of times per minute. Counters live in Redis (INCR + EXPIRE). If Redis is
 * unavailable the filter fails open.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

	private final StringRedisTemplate redis;
	private final int limitPerMinute;

	public RateLimitFilter(StringRedisTemplate redis, @Value("${app.rate-limit.per-minute:30}") int limitPerMinute) {
		this.redis = redis;
		this.limitPerMinute = limitPerMinute;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String key = "rate:" + request.getRemoteAddr();
		boolean blocked = false;
		try {
			Long count = redis.opsForValue().increment(key);
			if (count != null && count == 1L) {
				redis.expire(key, Duration.ofSeconds(60));
			}
			blocked = count != null && count > limitPerMinute;
		} catch (RuntimeException ex) {
			log.warn("Rate limiter could not reach Redis, allowing request: {}", ex.getMessage());
		}

		if (blocked) {
			response.setStatus(429);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\":\"Too many requests. Try again in a minute.\"}");
			return;
		}
		chain.doFilter(request, response);
	}
}
