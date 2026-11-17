package com.rahul.urlshortener.service;

import com.rahul.urlshortener.exception.UrlExpiredException;
import com.rahul.urlshortener.exception.UrlNotFoundException;
import com.rahul.urlshortener.model.UrlMapping;
import com.rahul.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UrlServiceTest {

    @Mock UrlMappingRepository repository;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    UrlService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new UrlService(repository, redis);
    }

    @Test
    void create_generatesSevenCharacterCodeAndSaves() {
        when(repository.existsByShortCode(anyString())).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(inv -> inv.getArgument(0));

        UrlMapping saved = service.create("https://example.com", 7);

        assertEquals(7, saved.getShortCode().length());
        assertEquals("https://example.com", saved.getOriginalUrl());
        assertNotNull(saved.getExpiresAt());
    }

    @Test
    void create_withoutExpiry_hasNoExpiryDate() {
        when(repository.existsByShortCode(anyString())).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(inv -> inv.getArgument(0));

        assertNull(service.create("https://example.com", null).getExpiresAt());
    }

    @Test
    void resolve_cacheHit_doesNotQueryDatabaseForUrl() {
        when(valueOps.get("url:abc1234")).thenReturn("https://cached.com");

        String result = service.resolve("abc1234");

        assertEquals("https://cached.com", result);
        verify(repository, never()).findByShortCode(anyString());
        verify(repository).incrementClicks("abc1234");
    }

    @Test
    void resolve_cacheMiss_loadsFromDatabaseAndCaches() {
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("abc1234");
        mapping.setOriginalUrl("https://db.com");
        when(valueOps.get("url:abc1234")).thenReturn(null);
        when(repository.findByShortCode("abc1234")).thenReturn(Optional.of(mapping));

        String result = service.resolve("abc1234");

        assertEquals("https://db.com", result);
        verify(valueOps).set(eq("url:abc1234"), eq("https://db.com"), any(Duration.class));
        verify(repository).incrementClicks("abc1234");
    }

    @Test
    void resolve_expiredLink_throwsExpired() {
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl("https://old.com");
        mapping.setExpiresAt(Instant.now().minusSeconds(60));
        when(valueOps.get(anyString())).thenReturn(null);
        when(repository.findByShortCode("old1234")).thenReturn(Optional.of(mapping));

        assertThrows(UrlExpiredException.class, () -> service.resolve("old1234"));
    }

    @Test
    void resolve_unknownCode_throwsNotFound() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(repository.findByShortCode("nope123")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> service.resolve("nope123"));
    }

    @Test
    void resolve_redisDown_fallsBackToDatabase() {
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl("https://db.com");
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(repository.findByShortCode("abc1234")).thenReturn(Optional.of(mapping));

        assertEquals("https://db.com", service.resolve("abc1234"));
    }
}
