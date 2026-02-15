# Commons Rate Limiting

A comprehensive rate limiting library for Java applications using the token bucket algorithm. This module provides flexible, high-performance rate limiting with support for both in-memory and distributed (Redis) backends.

## Features

- **Token Bucket Algorithm**: Efficient rate limiting with burst support
- **Multiple Backends**: In-memory and Redis-based implementations
- **Spring Integration**: Servlet filters, AOP annotations, and auto-configuration
- **Flexible Configuration**: Builder pattern with common presets
- **Production Ready**: Comprehensive metrics, monitoring, and error handling
- **Thread Safe**: Concurrent access support with atomic operations

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-rate-limiting</artifactId>
    <version>${commons.version}</version>
</dependency>
```

### Basic Usage

```java
import com.marcusprado02.commons.app.ratelimiting.*;

// Create rate limiter: 100 requests per minute
RateLimitConfig config = RateLimitConfig.perMinute(100);
RateLimiter rateLimiter = RateLimiterFactory.inMemory()
    .withConfig(config)
    .build();

// Check rate limit
String userId = "user123";
RateLimitResult result = rateLimiter.tryConsume(userId);

if (result.isAllowed()) {
    // Process request
} else {
    // Rate limit exceeded
    Duration retryAfter = result.getRetryAfter();
    // Handle rate limit violation
}
```

## Configuration Options

### Rate Limit Configuration

```java
// Simple configurations
RateLimitConfig perSecond = RateLimitConfig.perSecond(10);   // 10 req/sec
RateLimitConfig perMinute = RateLimitConfig.perMinute(100);  // 100 req/min  
RateLimitConfig perHour = RateLimitConfig.perHour(1000);     // 1000 req/hour

// Advanced configuration with burst support
RateLimitConfig advanced = RateLimitConfig.builder()
    .capacity(150)                        // Burst capacity (max tokens)
    .refillRate(100)                      // 100 tokens per refill period
    .refillPeriod(Duration.ofMinutes(1))  // Refill every minute
    .build();

// Custom configurations
RateLimitConfig custom = RateLimitConfig.withBurst(
    100,                    // Rate per period
    150,                    // Burst capacity  
    Duration.ofMinutes(1)   // Time period
);
```

### Rate Limiter Types

#### In-Memory Rate Limiter

```java
// High performance, single-node
RateLimiter rateLimiter = RateLimiterFactory.inMemory()
    .withConfig(RateLimitConfig.perMinute(100))
    .build();
```

#### Redis Rate Limiter

```java
// Distributed across multiple nodes
JedisPool jedisPool = new JedisPool("localhost", 6379);

RateLimiter rateLimiter = RateLimiterFactory.redis(jedisPool)
    .withConfig(RateLimitConfig.perMinute(100))
    .withKeyPrefix("api_rate_limit")
    .build();
```

## Spring Integration

### Auto Configuration

Add to `application.yml`:

```yaml
commons:
  rate-limiting:
    enabled: true
    default:
      limit: 100
      window: 1m
      burst-capacity: 120
    redis:
      enabled: false
      key-prefix: "app_rate_limit"
    filter:
      enabled: true
      url-patterns: ["/api/**"]
      order: 1
    aspect:
      enabled: true
```

### Annotation-Based Rate Limiting

```java
@RestController
public class ApiController {

    // Rate limit by IP address
    @RateLimit(
        keyExpression = "@ipExtractor.getClientIp(#request)",
        limit = 100,
        window = 1,
        windowUnit = TimeUnit.MINUTES
    )
    @GetMapping("/api/data")
    public ResponseEntity<String> getData(HttpServletRequest request) {
        return ResponseEntity.ok("data");
    }

    // Rate limit by user ID with burst support
    @RateLimit(
        keyExpression = "#userId",
        limit = 10,
        window = 1,
        windowUnit = TimeUnit.SECONDS,
        burstCapacity = 15
    )
    @PostMapping("/api/users/{userId}/actions")
    public ResponseEntity<Void> performAction(@PathVariable String userId) {
        return ResponseEntity.ok().build();
    }

    // Rate limit with custom message
    @RateLimit(
        keyExpression = "'global'",
        limit = 1000,
        window = 1,
        windowUnit = TimeUnit.HOURS,
        message = "Global rate limit exceeded. Try again in an hour."
    )
    @GetMapping("/api/global")
    public ResponseEntity<String> globalEndpoint() {
        return ResponseEntity.ok("global data");
    }
}
```

### Servlet Filter

```java
@Configuration
public class RateLimitingConfig {

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitFilter(
            RateLimiter rateLimiter) {
        
        RateLimitingFilter filter = new RateLimitingFilter(
            rateLimiter,
            RateLimitingFilter.KeyExtractors.byIpAddress(),
            RateLimitingFilter.SkipPredicates.forPaths("/health", "/actuator/**")
        );
        
        FilterRegistrationBean<RateLimitingFilter> registration = 
            new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
```

### Exception Handling

```java
@RestControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(
            RateLimitExceededException ex, 
            HttpServletResponse response) {
        
        // Add standard rate limiting headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(ex.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(ex.getRemaining()));
        
        if (ex.getRetryAfter() != null) {
            response.setHeader("Retry-After", String.valueOf(ex.getRetryAfter().getSeconds()));
        }
        
        Map<String, Object> error = Map.of(
            "error", "Rate limit exceeded",
            "message", ex.getMessage(),
            "limit", ex.getLimit(),
            "remaining", ex.getRemaining(),
            "utilization", String.format("%.1f%%", ex.getUtilization() * 100)
        );
        
        return ResponseEntity.status(429).body(error);
    }
}
```

## Advanced Usage

### Async Operations

```java
// Async rate limiting (useful for reactive applications)
CompletableFuture<RateLimitResult> future = rateLimiter.tryConsumeAsync("user123");
future.thenAccept(result -> {
    if (result.isAllowed()) {
        // Process request asynchronously
    } else {
        // Handle rate limit
    }
});
```

### Probing Without Consuming

```java
// Check rate limit state without consuming tokens
RateLimitResult probe = rateLimiter.probe("user123");
System.out.println("Available tokens: " + probe.getRemainingTokens());
System.out.println("Utilization: " + probe.getUtilization() * 100 + "%");
```

### Multiple Token Consumption

```java
// Consume multiple tokens for expensive operations
RateLimitResult result = rateLimiter.tryConsume("user123", 5);
if (result.isAllowed()) {
    // Perform operation that costs 5 tokens
}
```

### Statistics and Monitoring

```java
// Get rate limiter statistics
RateLimiterStats stats = rateLimiter.getStats();

System.out.println("Total requests: " + stats.getTotalRequests());
System.out.println("Allowed requests: " + stats.getAllowedRequests());
System.out.println("Rejected requests: " + stats.getRejectedRequests());
System.out.println("Allow rate: " + stats.getAllowRate() + "%");
System.out.println("Average response time: " + stats.getAverageResponseTime() + "ms");
```

### Custom Key Extractors

```java
public class CustomKeyExtractors {
    
    // Rate limit by API key
    public static Function<HttpServletRequest, String> byApiKey() {
        return request -> request.getHeader("X-API-Key");
    }
    
    // Rate limit by user ID from JWT token
    public static Function<HttpServletRequest, String> byJwtUserId() {
        return request -> {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                // Extract user ID from JWT token
                return JwtUtil.extractUserId(token.substring(7));
            }
            return null;
        };
    }
    
    // Combined rate limiting (IP + User)  
    public static Function<HttpServletRequest, String> byIpAndUser() {
        return request -> {
            String ip = request.getRemoteAddr();
            String userId = extractUserId(request);
            return userId != null ? ip + ":" + userId : ip;
        };
    }
}
```

## HTTP Headers

When rate limiting is applied, the following headers are automatically added to responses:

- `X-RateLimit-Limit`: The rate limit ceiling for the given key
- `X-RateLimit-Remaining`: The number of requests remaining in the current window  
- `X-RateLimit-Reset`: The time when the rate limit window resets (Unix timestamp)
- `Retry-After`: Seconds until the client can retry (when rate limited)

## Common Patterns

### API Rate Limiting by Tier

```java
@Service
public class TieredRateLimiter {
    
    private final Map<String, RateLimiter> tierLimiters = Map.of(
        "free", RateLimiterFactory.redis(jedisPool)
            .withConfig(RateLimitConfig.perHour(100))
            .build(),
        "premium", RateLimiterFactory.redis(jedisPool)
            .withConfig(RateLimitConfig.perHour(1000))
            .build(),
        "enterprise", RateLimiterFactory.redis(jedisPool)
            .withConfig(RateLimitConfig.perHour(10000))
            .build()
    );
    
    public RateLimitResult checkLimit(String apiKey, String userTier) {
        RateLimiter limiter = tierLimiters.get(userTier);
        return limiter != null ? limiter.tryConsume(apiKey) : RateLimitResult.allowed();
    }
}
```

### Graceful Degradation

```java
@Service
public class GracefulRateLimiter {
    
    public <T> T executeWithRateLimit(String key, Supplier<T> operation, Supplier<T> fallback) {
        RateLimitResult result = rateLimiter.tryConsume(key);
        
        if (result.isAllowed()) {
            return operation.get();
        } else {
            // Log rate limit hit
            log.warn("Rate limit exceeded for key: {}, utilization: {}%", 
                key, result.getUtilization() * 100);
            
            // Return cached or simplified response
            return fallback.get();
        }
    }
}
```

### Distributed Rate Limiting with Circuit Breaker

```java
@Component
public class ResilientRateLimiter {
    
    private final RateLimiter primaryLimiter;    // Redis-based
    private final RateLimiter fallbackLimiter;   // In-memory
    private final CircuitBreaker circuitBreaker;
    
    public RateLimitResult tryConsume(String key) {
        return circuitBreaker.executeSupplier(() -> primaryLimiter.tryConsume(key))
            .recover(throwable -> {
                log.warn("Primary rate limiter failed, using fallback", throwable);
                return fallbackLimiter.tryConsume(key);
            });
    }
}
```

## Configuration Reference

### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `commons.rate-limiting.enabled` | boolean | `true` | Enable/disable rate limiting |
| `commons.rate-limiting.default.limit` | long | `100` | Default rate limit (requests per window) |
| `commons.rate-limiting.default.window` | Duration | `1m` | Default time window |
| `commons.rate-limiting.default.burst-capacity` | long | `0` | Burst capacity (0 = same as limit) |
| `commons.rate-limiting.redis.enabled` | boolean | `false` | Enable Redis backend |
| `commons.rate-limiting.redis.key-prefix` | String | `rate_limiter` | Redis key prefix |
| `commons.rate-limiting.filter.enabled` | boolean | `false` | Enable servlet filter |
| `commons.rate-limiting.filter.url-patterns` | List&lt;String&gt; | `["/api/**"]` | URL patterns for filter |
| `commons.rate-limiting.filter.order` | int | `1` | Filter order in chain |
| `commons.rate-limiting.aspect.enabled` | boolean | `true` | Enable AOP aspect |

### Required Dependencies

**Core (always required):**
```xml
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
</dependency>
```

**For Redis support (optional):**
```xml
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

**For Spring integration (optional):**
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
</dependency>
```

## Performance Considerations

### In-Memory vs Redis

- **In-Memory**: 
  - Pros: Fastest performance, no network latency
  - Cons: No shared state across instances, lost on restart
  - Use for: Single-node applications, development

- **Redis**:
  - Pros: Distributed state, persistent across restarts
  - Cons: Network latency, requires Redis infrastructure  
  - Use for: Multi-node applications, production environments

### Optimization Tips

1. **Use appropriate bucket sizes**: Larger buckets reduce Redis operations
2. **Cache rate limiters**: Reuse rate limiter instances for the same configuration
3. **Monitor statistics**: Use built-in metrics to optimize configuration
4. **Consider key cardinality**: High-cardinality keys consume more memory

## License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.
