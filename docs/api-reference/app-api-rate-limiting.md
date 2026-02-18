# API Reference: API Rate Limiting

## Visão Geral

`commons-app-api-rate-limiting` fornece rate limiting distribuído para proteção de APIs contra abuso e sobrecarga.

**Quando usar:**
- Limitar requisições por usuário/IP/API key
- Proteger APIs públicas contra abuso
- Implementar tiers de serviço (free/premium)
- Prevenir ataques DDoS e brute force
- Controlar custos de APIs externas

**Algoritmos suportados:**
- Token Bucket
- Sliding Window
- Fixed Window
- Leaky Bucket

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-api-rate-limiting</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### RateLimiter

Interface principal para rate limiting.

```java
public interface RateLimiter {
    
    /**
     * Tenta adquirir permissão.
     * 
     * @param key identificador único (userId, IP, etc.)
     * @return true se permitido, false se rate limit excedido
     */
    boolean tryAcquire(String key);
    
    /**
     * Tenta adquirir N permissões.
     */
    boolean tryAcquire(String key, int permits);
    
    /**
     * Retorna informações sobre limite.
     */
    RateLimitInfo getInfo(String key);
    
    /**
     * Reseta rate limit para key.
     */
    void reset(String key);
}
```

### RateLimitInfo

Informações sobre o rate limit.

```java
public record RateLimitInfo(
    long limit,           // Limite máximo
    long remaining,       // Requisições restantes
    Instant resetAt,      // Quando reseta
    Duration retryAfter   // Tempo para retry (se excedido)
) {
    public boolean isExceeded() {
        return remaining == 0;
    }
    
    public HttpHeaders toHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Limit", String.valueOf(limit));
        headers.add("X-RateLimit-Remaining", String.valueOf(remaining));
        headers.add("X-RateLimit-Reset", String.valueOf(resetAt.getEpochSecond()));
        
        if (isExceeded()) {
            headers.add("Retry-After", String.valueOf(retryAfter.toSeconds()));
        }
        
        return headers;
    }
}
```

---

## 🪣 Token Bucket Algorithm

### Implementation

```java
@Component
public class TokenBucketRateLimiter implements RateLimiter {
    
    private final CacheProvider cache;
    private final long capacity;
    private final long refillRate;  // tokens per second
    private final Duration refillPeriod;
    
    public TokenBucketRateLimiter(
        CacheProvider cache,
        long capacity,
        long refillRate,
        Duration refillPeriod
    ) {
        this.cache = cache;
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.refillPeriod = refillPeriod;
    }
    
    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 1);
    }
    
    @Override
    public boolean tryAcquire(String key, int permits) {
        String bucketKey = "rate-limit:token-bucket:" + key;
        
        // Get current bucket state
        TokenBucket bucket = cache.get(bucketKey, TokenBucket.class)
            .orElse(new TokenBucket(capacity, Instant.now()));
        
        // Refill tokens
        bucket = refillTokens(bucket);
        
        // Try to consume tokens
        if (bucket.tokens() >= permits) {
            bucket = bucket.consume(permits);
            cache.set(bucketKey, bucket, refillPeriod);
            return true;
        }
        
        // Rate limit exceeded
        cache.set(bucketKey, bucket, refillPeriod);
        return false;
    }
    
    private TokenBucket refillTokens(TokenBucket bucket) {
        Instant now = Instant.now();
        Duration elapsed = Duration.between(bucket.lastRefill(), now);
        
        long tokensToAdd = (elapsed.toSeconds() * refillRate) / refillPeriod.toSeconds();
        
        if (tokensToAdd > 0) {
            long newTokens = Math.min(capacity, bucket.tokens() + tokensToAdd);
            return new TokenBucket(newTokens, now);
        }
        
        return bucket;
    }
    
    @Override
    public RateLimitInfo getInfo(String key) {
        String bucketKey = "rate-limit:token-bucket:" + key;
        
        TokenBucket bucket = cache.get(bucketKey, TokenBucket.class)
            .orElse(new TokenBucket(capacity, Instant.now()));
        
        bucket = refillTokens(bucket);
        
        Instant resetAt = bucket.lastRefill().plus(refillPeriod);
        Duration retryAfter = bucket.tokens() == 0
            ? Duration.between(Instant.now(), resetAt)
            : Duration.ZERO;
        
        return new RateLimitInfo(
            capacity,
            bucket.tokens(),
            resetAt,
            retryAfter
        );
    }
    
    @Override
    public void reset(String key) {
        cache.delete("rate-limit:token-bucket:" + key);
    }
    
    private record TokenBucket(long tokens, Instant lastRefill) {
        TokenBucket consume(int permits) {
            return new TokenBucket(tokens - permits, lastRefill);
        }
    }
}
```

### Usage

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final RateLimiter rateLimiter;
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<?> createOrder(
        @RequestBody CreateOrderRequest request,
        @AuthenticationPrincipal User user
    ) {
        // Check rate limit (100 requests per minute)
        if (!rateLimiter.tryAcquire(user.id().value())) {
            RateLimitInfo info = rateLimiter.getInfo(user.id().value());
            
            return ResponseEntity.status(429)
                .headers(info.toHeaders())
                .body(Map.of(
                    "error", "rate_limit_exceeded",
                    "message", "Too many requests",
                    "retryAfter", info.retryAfter().toSeconds()
                ));
        }
        
        // Process request
        Result<OrderId> result = orderService.createOrder(request.toCommand());
        
        // Add rate limit headers
        RateLimitInfo info = rateLimiter.getInfo(user.id().value());
        
        return result
            .map(orderId -> ResponseEntity
                .status(201)
                .headers(info.toHeaders())
                .body(Map.of("orderId", orderId.value()))
            )
            .getOrElse(problem -> 
                ResponseEntity.badRequest().body(problem)
            );
    }
}
```

---

## 🪟 Sliding Window Algorithm

### Implementation

```java
@Component
public class SlidingWindowRateLimiter implements RateLimiter {
    
    private final CacheProvider cache;
    private final long maxRequests;
    private final Duration window;
    
    @Override
    public boolean tryAcquire(String key) {
        String windowKey = "rate-limit:sliding-window:" + key;
        Instant now = Instant.now();
        Instant windowStart = now.minus(window);
        
        // Get timestamps of requests in window
        List<Instant> timestamps = cache.get(windowKey, List.class)
            .orElse(new ArrayList<>());
        
        // Remove timestamps outside window
        timestamps = timestamps.stream()
            .filter(ts -> ts.isAfter(windowStart))
            .collect(Collectors.toList());
        
        // Check if under limit
        if (timestamps.size() < maxRequests) {
            timestamps.add(now);
            cache.set(windowKey, timestamps, window);
            return true;
        }
        
        return false;
    }
    
    @Override
    public RateLimitInfo getInfo(String key) {
        String windowKey = "rate-limit:sliding-window:" + key;
        Instant now = Instant.now();
        Instant windowStart = now.minus(window);
        
        List<Instant> timestamps = cache.get(windowKey, List.class)
            .orElse(new ArrayList<>());
        
        timestamps = timestamps.stream()
            .filter(ts -> ts.isAfter(windowStart))
            .collect(Collectors.toList());
        
        long remaining = Math.max(0, maxRequests - timestamps.size());
        
        Instant oldestTimestamp = timestamps.isEmpty()
            ? now
            : timestamps.get(0);
        
        Instant resetAt = oldestTimestamp.plus(window);
        
        Duration retryAfter = remaining == 0
            ? Duration.between(now, resetAt)
            : Duration.ZERO;
        
        return new RateLimitInfo(
            maxRequests,
            remaining,
            resetAt,
            retryAfter
        );
    }
}
```

---

## 🎯 Rate Limiting Strategies

### By User ID

```java
@Component
public class UserRateLimitingFilter implements Filter {
    
    private final RateLimiter rateLimiter;
    
    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Extract user ID from JWT
        String userId = extractUserId(httpRequest);
        
        if (userId != null && !rateLimiter.tryAcquire(userId)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            RateLimitInfo info = rateLimiter.getInfo(userId);
            
            httpResponse.setStatus(429);
            info.toHeaders().forEach((name, values) ->
                values.forEach(value -> httpResponse.addHeader(name, value))
            );
            
            httpResponse.getWriter().write(
                "{\"error\":\"rate_limit_exceeded\"}"
            );
            return;
        }
        
        chain.doFilter(request, response);
    }
}
```

### By IP Address

```java
@Component
public class IpRateLimitingFilter implements Filter {
    
    private final RateLimiter rateLimiter;
    
    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Extract IP (consider X-Forwarded-For)
        String ip = getClientIp(httpRequest);
        
        if (!rateLimiter.tryAcquire(ip)) {
            ((HttpServletResponse) response).setStatus(429);
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        return request.getRemoteAddr();
    }
}
```

### By API Key

```java
@Component
public class ApiKeyRateLimitingFilter implements Filter {
    
    private final RateLimiter rateLimiter;
    private final ApiKeyService apiKeyService;
    
    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Extract API key
        String apiKey = httpRequest.getHeader("X-API-Key");
        
        if (apiKey == null) {
            ((HttpServletResponse) response).setStatus(401);
            return;
        }
        
        // Get tier for API key
        Optional<ApiKeyTier> tier = apiKeyService.getTier(apiKey);
        
        if (tier.isEmpty()) {
            ((HttpServletResponse) response).setStatus(403);
            return;
        }
        
        // Apply tier-specific rate limit
        RateLimiter tierLimiter = getRateLimiter(tier.get());
        
        if (!tierLimiter.tryAcquire(apiKey)) {
            ((HttpServletResponse) response).setStatus(429);
            return;
        }
        
        chain.doFilter(request, response);
    }
}
```

---

## 🎚️ Tiered Rate Limiting

### Rate Limit Tiers

```java
public enum ApiKeyTier {
    FREE(100, Duration.ofHours(1)),      // 100 req/hour
    BASIC(1000, Duration.ofHours(1)),    // 1000 req/hour
    PREMIUM(10000, Duration.ofHours(1)), // 10000 req/hour
    ENTERPRISE(100000, Duration.ofHours(1)); // 100000 req/hour
    
    private final long maxRequests;
    private final Duration window;
    
    ApiKeyTier(long maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }
    
    public long maxRequests() {
        return maxRequests;
    }
    
    public Duration window() {
        return window;
    }
}
```

### Tiered Limiter Factory

```java
@Component
public class TieredRateLimiterFactory {
    
    private final CacheProvider cache;
    private final Map<ApiKeyTier, RateLimiter> limiters;
    
    public TieredRateLimiterFactory(CacheProvider cache) {
        this.cache = cache;
        this.limiters = createLimiters();
    }
    
    private Map<ApiKeyTier, RateLimiter> createLimiters() {
        Map<ApiKeyTier, RateLimiter> limiters = new EnumMap<>(ApiKeyTier.class);
        
        for (ApiKeyTier tier : ApiKeyTier.values()) {
            limiters.put(
                tier,
                new TokenBucketRateLimiter(
                    cache,
                    tier.maxRequests(),
                    tier.maxRequests(),
                    tier.window()
                )
            );
        }
        
        return limiters;
    }
    
    public RateLimiter getLimiter(ApiKeyTier tier) {
        return limiters.get(tier);
    }
}
```

### Tiered Endpoint

```java
@RestController
@RequestMapping("/api/search")
public class SearchController {
    
    private final TieredRateLimiterFactory limiterFactory;
    private final ApiKeyService apiKeyService;
    private final SearchService searchService;
    
    @GetMapping
    public ResponseEntity<?> search(
        @RequestParam String query,
        @RequestHeader("X-API-Key") String apiKey
    ) {
        // Validate API key
        Optional<ApiKeyTier> tierOpt = apiKeyService.getTier(apiKey);
        
        if (tierOpt.isEmpty()) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "invalid_api_key"));
        }
        
        ApiKeyTier tier = tierOpt.get();
        RateLimiter limiter = limiterFactory.getLimiter(tier);
        
        // Check rate limit
        if (!limiter.tryAcquire(apiKey)) {
            RateLimitInfo info = limiter.getInfo(apiKey);
            
            return ResponseEntity.status(429)
                .headers(info.toHeaders())
                .body(Map.of(
                    "error", "rate_limit_exceeded",
                    "tier", tier.name(),
                    "limit", info.limit(),
                    "retryAfter", info.retryAfter().toSeconds()
                ));
        }
        
        // Execute search
        List<SearchResult> results = searchService.search(query);
        
        RateLimitInfo info = limiter.getInfo(apiKey);
        
        return ResponseEntity.ok()
            .headers(info.toHeaders())
            .body(Map.of(
                "results", results,
                "tier", tier.name()
            ));
    }
}
```

---

## 🔧 Configuration

### YAML Configuration

```yaml
rate-limiting:
  default:
    algorithm: token-bucket
    capacity: 100
    refill-rate: 100
    refill-period: 1m
  
  endpoints:
    # Public endpoints - strict limits
    - path: /api/public/**
      algorithm: sliding-window
      max-requests: 10
      window: 1m
    
    # Authenticated endpoints - relaxed limits
    - path: /api/private/**
      algorithm: token-bucket
      capacity: 1000
      refill-rate: 1000
      refill-period: 1h
    
    # Admin endpoints - no limits
    - path: /api/admin/**
      enabled: false
  
  tiers:
    free:
      max-requests: 100
      window: 1h
    
    premium:
      max-requests: 10000
      window: 1h
```

### Programmatic Configuration

```java
@Configuration
public class RateLimitingConfig {
    
    @Bean
    public RateLimiter defaultRateLimiter(CacheProvider cache) {
        return new TokenBucketRateLimiter(
            cache,
            100,    // capacity
            100,    // refill rate
            Duration.ofMinutes(1)
        );
    }
    
    @Bean
    public RateLimiter strictRateLimiter(CacheProvider cache) {
        return new SlidingWindowRateLimiter(
            cache,
            10,     // max requests
            Duration.ofMinutes(1)
        );
    }
    
    @Bean
    public FilterRegistrationBean<UserRateLimitingFilter> userRateLimitFilter(
        RateLimiter rateLimiter
    ) {
        FilterRegistrationBean<UserRateLimitingFilter> registration = 
            new FilterRegistrationBean<>();
        
        registration.setFilter(new UserRateLimitingFilter(rateLimiter));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(2);
        
        return registration;
    }
}
```

---

## 📊 Monitoring

### Metrics

```java
@Component
public class RateLimitingMetricsCollector {
    
    private final MetricsFacade metrics;
    
    public void recordLimitExceeded(String key, String tier) {
        metrics.incrementCounter(
            "rate_limit.exceeded",
            "key", key,
            "tier", tier
        );
    }
    
    public void recordRequestAllowed(String key, String tier) {
        metrics.incrementCounter(
            "rate_limit.allowed",
            "key", key,
            "tier", tier
        );
    }
    
    public void recordRemainingQuota(String key, long remaining, long limit) {
        metrics.recordGauge(
            "rate_limit.remaining",
            remaining,
            "key", key
        );
        
        metrics.recordGauge(
            "rate_limit.usage_percent",
            ((double) (limit - remaining) / limit) * 100,
            "key", key
        );
    }
}
```

### Dashboard

```java
@RestController
@RequestMapping("/api/admin/rate-limits")
public class RateLimitDashboardController {
    
    private final CacheProvider cache;
    private final MetricsFacade metrics;
    
    @GetMapping("/stats")
    public ResponseEntity<RateLimitStats> getStats() {
        long totalExceeded = metrics.getCounter("rate_limit.exceeded");
        long totalAllowed = metrics.getCounter("rate_limit.allowed");
        
        double blockedRate = totalExceeded > 0
            ? ((double) totalExceeded / (totalExceeded + totalAllowed)) * 100
            : 0.0;
        
        return ResponseEntity.ok(new RateLimitStats(
            totalAllowed,
            totalExceeded,
            blockedRate
        ));
    }
    
    @GetMapping("/top-abusers")
    public ResponseEntity<List<RateLimitAbuser>> getTopAbusers() {
        // Query cache for keys with highest exceeded count
        List<RateLimitAbuser> abusers = cache.keys("rate-limit:*").stream()
            .map(key -> {
                long exceeded = metrics.getCounter(
                    "rate_limit.exceeded",
                    "key", key
                );
                return new RateLimitAbuser(key, exceeded);
            })
            .sorted(Comparator.comparing(RateLimitAbuser::exceededCount).reversed())
            .limit(10)
            .toList();
        
        return ResponseEntity.ok(abusers);
    }
}
```

---

##  Testing

### Unit Tests

```java
class TokenBucketRateLimiterTest {
    
    private CacheProvider cache;
    private TokenBucketRateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        cache = new InMemoryCacheProvider();
        rateLimiter = new TokenBucketRateLimiter(
            cache,
            10,     // capacity
            10,     // refill rate
            Duration.ofMinutes(1)
        );
    }
    
    @Test
    void shouldAllowRequestsUnderLimit() {
        String key = "user-123";
        
        // First 10 requests should succeed
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiter.tryAcquire(key)).isTrue();
        }
        
        // 11th request should fail
        assertThat(rateLimiter.tryAcquire(key)).isFalse();
    }
    
    @Test
    void shouldRefillTokensOverTime() throws InterruptedException {
        String key = "user-123";
        
        // Exhaust tokens
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire(key);
        }
        
        assertThat(rateLimiter.tryAcquire(key)).isFalse();
        
        // Wait for refill (10 tokens per minute = 1 token per 6 seconds)
        Thread.sleep(7000);
        
        // Should have 1 token now
        assertThat(rateLimiter.tryAcquire(key)).isTrue();
        assertThat(rateLimiter.tryAcquire(key)).isFalse();
    }
    
    @Test
    void shouldProvideCorrectInfo() {
        String key = "user-123";
        
        // Use 3 tokens
        rateLimiter.tryAcquire(key, 3);
        
        RateLimitInfo info = rateLimiter.getInfo(key);
        
        assertThat(info.limit()).isEqualTo(10);
        assertThat(info.remaining()).isEqualTo(7);
        assertThat(info.isExceeded()).isFalse();
    }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitingIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldReturnRateLimitHeaders() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("user", "password")
            .getForEntity("/api/orders", String.class);
        
        assertThat(response.getHeaders().containsKey("X-RateLimit-Limit")).isTrue();
        assertThat(response.getHeaders().containsKey("X-RateLimit-Remaining")).isTrue();
        assertThat(response.getHeaders().containsKey("X-RateLimit-Reset")).isTrue();
    }
    
    @Test
    void shouldReturn429WhenLimitExceeded() {
        String apiKey = "test-api-key";
        
        // Make 11 requests (limit is 10)
        for (int i = 0; i < 11; i++) {
            ResponseEntity<String> response = restTemplate
                .exchange(
                    "/api/search?query=test",
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders(apiKey)),
                    String.class
                );
            
            if (i < 10) {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            } else {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                assertThat(response.getHeaders().containsKey("Retry-After")).isTrue();
            }
        }
    }
    
    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Key", apiKey);
        return headers;
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use distributed cache (Redis) para clusters
RateLimiter limiter = new TokenBucketRateLimiter(
    redisCache,  // ✅ Compartilhado entre instâncias
    capacity,
    refillRate,
    period
);

// ✅ Retorne headers informativos
RateLimitInfo info = rateLimiter.getInfo(key);
return ResponseEntity.ok()
    .headers(info.toHeaders())  // ✅ X-RateLimit-* headers
    .body(response);

// ✅ Use diferentes limites por endpoint
@RateLimit(requests = 10, window = "1m")
public ResponseEntity<?> publicEndpoint() { }

@RateLimit(requests = 1000, window = "1h")
public ResponseEntity<?> authenticatedEndpoint() { }

// ✅ Monitore rate limiting
metrics.incrementCounter("rate_limit.exceeded", "endpoint", "/api/orders");

// ✅ Implemente graceful degradation
if (!rateLimiter.tryAcquire(key)) {
    return getCachedResponse();  // ✅ Fallback
}
```

### ❌ DON'T

```java
// ❌ NÃO use cache local em clusters
Map<String, Integer> localCache = new HashMap<>();  // ❌ Inconsistente!

// ❌ NÃO bloqueie requests sem informação
if (!rateLimiter.tryAcquire(key)) {
    return ResponseEntity.status(429).build();  // ❌ Sem headers!
}

// ❌ NÃO use limites muito restritivos
RateLimiter limiter = new TokenBucketRateLimiter(
    cache,
    1,  // ❌ 1 request per minute é muito restritivo!
    1,
    Duration.ofMinutes(1)
);

// ❌ NÃO ignore IP spoofing
String ip = request.getRemoteAddr();  // ❌ Pode ser proxy!
// ✅ Use X-Forwarded-For com validação

// ❌ NÃO rate-limite health checks
if (!rateLimiter.tryAcquire(key)) {
    return 429;  // ❌ Health checks falharão!
}
// ✅ Exclua /health, /metrics de rate limiting
```

---

## Ver Também

- [API Gateway](app-api-gateway.md) - Gateway com rate limiting
- [Resilience Guide](../guides/resilience.md) - Circuit breaker patterns
- [Observability Guide](../guides/observability.md) - Monitoring
- [Redis Adapter](../../commons-adapters-cache-redis/) - Distributed cache
