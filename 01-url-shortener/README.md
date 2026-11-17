# URL Shortener (Spring Boot, Redis, PostgreSQL)

A REST service that turns long URLs into short links, with Redis caching for fast redirects,
per-IP rate limiting, link expiry and click analytics.

## Features
- Create short links (`POST /api/urls`) with optional expiry (1-365 days)
- Fast redirects: Redis cache first, PostgreSQL fallback; cache TTL never outlives the link
- Atomic click counting in the database (safe under concurrent traffic)
- Per-IP rate limiting on `/api/**` using Redis counters (default 30 requests/minute)
- Validation, global exception handling, proper HTTP codes (201, 302, 404, 410, 429)
- Resilient: if Redis is down, redirects still work from PostgreSQL
- Dockerfile + docker-compose, GitHub Actions CI, unit tests with JUnit and Mockito

## Tech stack
Java 17, Spring Boot 3, Spring Data JPA, PostgreSQL, Redis, Docker, JUnit 5, Mockito, Maven

## How it works
```
Client -> RateLimitFilter (Redis INCR) -> UrlController -> UrlService
                                                   |-> Redis (cache: url:{code})
                                                   |-> PostgreSQL (source of truth)
```

## Run it
**With Docker (easiest):**
```bash
docker compose up --build
```
**Locally:** start PostgreSQL and Redis (or `docker compose up postgres redis`), then:
```bash
mvn spring-boot:run
```

## API examples
```bash
# create a short link that expires in 7 days
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.example.com/some/very/long/path", "expiryDays": 7}'

# follow the short link (replace abc1234 with your code)
curl -i http://localhost:8080/abc1234

# stats
curl http://localhost:8080/api/urls/abc1234/stats
```

## Run tests
```bash
mvn test
```

## Design decisions
- **Why Redis?** Redirects are read-heavy. Caching removes most database reads.
- **Why an atomic `UPDATE ... SET clickCount = clickCount + 1`?** Avoids lost updates compared with read-modify-write.
- **Why fail-open rate limiting?** Rate limiting is protection, not a core feature; a Redis outage should not take the service down.
- **Known limitations / next steps:** fixed-window limiter (a sliding window or token bucket is smoother), one DB write per redirect for click counting (could be batched or sent through Kafka), no user accounts or custom aliases yet.
