package com.rahul.urlshortener.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "url_mappings")
public class UrlMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 16)
	private String shortCode;

	@Column(nullable = false, length = 2048)
	private String originalUrl;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant expiresAt;

	@Column(nullable = false)
	private long clickCount;

	public Long getId() {
		return id;
	}

	public String getShortCode() {
		return shortCode;
	}

	public void setShortCode(String shortCode) {
		this.shortCode = shortCode;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public void setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public long getClickCount() {
		return clickCount;
	}

	public void setClickCount(long clickCount) {
		this.clickCount = clickCount;
	}
}
