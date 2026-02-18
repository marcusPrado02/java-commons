# Redis Adapter Guide

## Overview

This guide covers the **Redis adapter** (`commons-adapters-cache-redis`) for distributed caching, locking, and pub/sub with Redis.

**Key Features:**
- Distributed caching
- Distributed locks
- Pub/Sub messaging
- Session storage
- Rate limiting
- Leaderboards & counters

---

## 📦 Installation

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-cache-redis</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Spring Boot starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

---

## ⚙️ Configuration

### Redis Properties

```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD}
    database: 0
    timeout: 2000ms
    
    # Connection pool (Lettuce)
    lettuce:
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 4
        max-wait: -1ms
      shutdown-timeout: 100ms
    
    # Sentinel (High Availability)
    sentinel:
      master: mymaster
      nodes:
        - sentinel1:26379
        - sentinel2:26379
        - sentinel3:26379
    
    # Cluster
    cluster:
      nodes:
        - redis1:6379
        - redis2:6379
        - redis3:6379
      max-redirects: 3
```

### Spring Boot Auto-Configuration

```java
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // JSON serialization
        Jackson2JsonRedisSerializer<Object> serializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        serializer.setObjectMapper(mapper);
        
        // Key serializer: String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value serializer: JSON
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public CacheProvider redisCacheProvider(RedisTemplate<String, Object> redisTemplate) {
        return new RedisCacheProviderAdapter(redisTemplate);
    }
}
```

---

## 💾 Caching Examples

### Basic Cache Operations

```java
@Service
public class UserCacheService {
    
    private final CacheProvider cache;
    private static final Duration TTL = Duration.ofHours(1);
    
    public Result<User> getUser(UserId userId) {
        String key = "user:" + userId.value();
        
        // Try cache first
        Optional<User> cached = cache.get(key, User.class);
        
        if (cached.isPresent()) {
            log.info("Cache hit").field("userId", userId.value()).log();
            return Result.ok(cached.get());
        }
        
        // Cache miss - load from database
        return userRepository.findById(userId)
            .map(user -> {
                cache.set(key, user, TTL);
                log.info("Cache miss - loaded from DB")
                    .field("userId", userId.value())
                    .log();
                return user;
            })
            .map(Result::ok)
            .orElse(Result.error(Error.of("USER_NOT_FOUND", "User not found")));
    }
    
    public Result<Void> updateUser(User user) {
        return userRepository.save(user)
            .andThen(saved -> {
                // Invalidate cache
                String key = "user:" + user.id().value();
                cache.delete(key);
                
                log.info("User updated, cache invalidated")
                    .field("userId", user.id().value())
                    .log();
                
                return Result.ok();
            });
    }
}
```

### Cache-Aside Pattern

```java
@Service
public class ProductService {
    
    private final ProductRepository productRepository;
    private final CacheProvider cache;
    
    public Result<Product> getProduct(ProductId productId) {
        return cacheAside(
            "product:" + productId.value(),
            () -> productRepository.findById(productId),
            Product.class,
            Duration.ofMinutes(30)
        );
    }
    
    private <T> Result<T> cacheAside(
        String key,
        Supplier<Optional<T>> loader,
        Class<T> type,
        Duration ttl
    ) {
        // Check cache
        Optional<T> cached = cache.get(key, type);
        if (cached.isPresent()) {
            return Result.ok(cached.get());
        }
        
        // Load from source
        Optional<T> loaded = loader.get();
        
        if (loaded.isEmpty()) {
            return Result.error(Error.of("NOT_FOUND", "Resource not found"));
        }
        
        T value = loaded.get();
        
        // Store in cache
        cache.set(key, value, ttl);
        
        return Result.ok(value);
    }
}
```

### Multi-Level Cache

```java
@Service
public class MultiLevelCacheService {
    
    private final CacheManager localCache;  // Caffeine (in-memory)
    private final CacheProvider redisCache; // Redis (distributed)
    private final ProductRepository repository;
    
    public Result<Product> getProduct(ProductId productId) {
        String key = "product:" + productId.value();
        
        // L1: Local cache (fast, but not shared)
        Cache caffeineCache = localCache.getCache("products");
        Product local = caffeineCache.get(key, Product.class);
        
        if (local != null) {
            log.debug("L1 cache hit").field("productId", productId.value()).log();
            return Result.ok(local);
        }
        
        // L2: Redis cache (slower, but shared across instances)
        Optional<Product> redis = redisCache.get(key, Product.class);
        
        if (redis.isPresent()) {
            Product product = redis.get();
            
            // Warm L1 cache
            caffeineCache.put(key, product);
            
            log.debug("L2 cache hit").field("productId", productId.value()).log();
            return Result.ok(product);
        }
        
        // L3: Database (slowest)
        return repository.findById(productId)
            .map(product -> {
                // Warm both caches
                caffeineCache.put(key, product);
                redisCache.set(key, product, Duration.ofMinutes(30));
                
                log.debug("Cache miss - loaded from DB")
                    .field("productId", productId.value())
                    .log();
                
                return Result.ok(product);
            })
            .orElse(Result.error(Error.of("NOT_FOUND", "Product not found")));
    }
}
```

---

## 🔒 Distributed Lock

### Basic Lock

```java
@Service
public class OrderProcessingService {
    
    private final DistributedLock distributedLock;
    private final OrderRepository orderRepository;
    
    public Result<Void> processOrders() {
        String lockKey = "order-processing-job";
        Duration lockTTL = Duration.ofMinutes(5);
        
        // Try to acquire lock
        boolean acquired = distributedLock.tryLock(lockKey, lockTTL);
        
        if (!acquired) {
            log.info("Lock already held by another instance")
                .field("lockKey", lockKey)
                .log();
            return Result.ok();  // Skip this run
        }
        
        try {
            log.info("Lock acquired, processing orders")
                .field("lockKey", lockKey)
                .log();
            
            // Process orders (only one instance executes this)
            List<Order> pendingOrders = orderRepository.findPendingOrders();
            
            for (Order order : pendingOrders) {
                processOrder(order);
            }
            
            return Result.ok();
            
        } finally {
            // Release lock
            distributedLock.unlock(lockKey);
            
            log.info("Lock released")
                .field("lockKey", lockKey)
                .log();
        }
    }
}
```

### Lock with Automatic Release

```java
@Service
public class ScheduledTaskService {
    
    private final DistributedLock distributedLock;
    
    @Scheduled(fixedDelay = 60000)  // Every minute
    public void processTask() {
        String lockKey = "scheduled-task";
        Duration lockTTL = Duration.ofMinutes(2);
        
        // Execute with lock (automatic release)
        Result<Void> result = distributedLock.withLock(
            lockKey,
            lockTTL,
            () -> {
                log.info("Executing scheduled task");
                
                // Task logic
                performTask();
                
                return Result.ok();
            }
        );
        
        if (result.isError()) {
            log.error("Task failed")
                .error(result.getError())
                .log();
        }
    }
}
```

### Redis Lock Implementation

```java
public class RedisDistributedLock implements DistributedLock {
    
    private final StringRedisTemplate redisTemplate;
    private final String lockValue = UUID.randomUUID().toString();
    
    @Override
    public boolean tryLock(String lockKey, Duration ttl) {
        // SET key value NX EX ttl
        // NX = Only set if not exists
        // EX = Expiration time in seconds
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            lockKey,
            lockValue,
            ttl.getSeconds(),
            TimeUnit.SECONDS
        );
        
        return Boolean.TRUE.equals(success);
    }
    
    @Override
    public void unlock(String lockKey) {
        // Lua script to delete only if we own the lock
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";
        
        redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            List.of(lockKey),
            lockValue
        );
    }
}
```

---

## 📈 Rate Limiting

### Token Bucket Rate Limiter

```java
@Service
public class RedisRateLimiter {
    
    private final StringRedisTemplate redisTemplate;
    
    public boolean allowRequest(String key, int maxRequests, Duration window) {
        String rateLimitKey = "rate_limit:" + key;
        
        // Increment counter
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        
        if (count == null) {
            return false;
        }
        
        // Set expiration on first request
        if (count == 1) {
            redisTemplate.expire(rateLimitKey, window);
        }
        
        // Check if within limit
        return count <= maxRequests;
    }
}

// Usage
@RestController
public class ApiController {
    
    private final RedisRateLimiter rateLimiter;
    
    @GetMapping("/api/products")
    public ResponseEntity<?> getProducts(@RequestHeader("X-API-Key") String apiKey) {
        // 100 requests per minute
        boolean allowed = rateLimiter.allowRequest(
            "api:" + apiKey,
            100,
            Duration.ofMinutes(1)
        );
        
        if (!allowed) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-RateLimit-Limit", "100")
                .header("X-RateLimit-Remaining", "0")
                .body("Rate limit exceeded");
        }
        
        // Process request
        return ResponseEntity.ok(products);
    }
}
```

### Sliding Window Rate Limiter

```java
@Service
public class SlidingWindowRateLimiter {
    
    private final StringRedisTemplate redisTemplate;
    
    public boolean allowRequest(String key, int maxRequests, Duration window) {
        String rateLimitKey = "rate_limit:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();
        
        // Lua script for atomic sliding window
        String script = 
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1]) " +
            "local count = redis.call('ZCARD', KEYS[1]) " +
            "if count < tonumber(ARGV[2]) then " +
            "    redis.call('ZADD', KEYS[1], ARGV[3], ARGV[3]) " +
            "    redis.call('EXPIRE', KEYS[1], ARGV[4]) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";
        
        Long allowed = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            List.of(rateLimitKey),
            String.valueOf(windowStart),
            String.valueOf(maxRequests),
            String.valueOf(now),
            String.valueOf(window.getSeconds())
        );
        
        return Long.valueOf(1).equals(allowed);
    }
}
```

---

## 🎯 Counters & Leaderboards

### View Counter

```java
@Service
public class ViewCounterService {
    
    private final CacheProvider cache;
    
    public void incrementViews(ProductId productId) {
        String key = "views:" + productId.value();
        cache.increment(key);
        
        // Set expiration (7 days)
        cache.expire(key, Duration.ofDays(7));
    }
    
    public long getViews(ProductId productId) {
        String key = "views:" + productId.value();
        return cache.get(key, Long.class).orElse(0L);
    }
    
    public Map<ProductId, Long> getTopViewedProducts(int limit) {
        // Get all view keys
        Set<String> keys = redisTemplate.keys("views:*");
        
        return keys.stream()
            .map(key -> {
                String productIdStr = key.replace("views:", "");
                ProductId productId = ProductId.from(productIdStr);
                Long views = getViews(productId);
                return Map.entry(productId, views);
            })
            .sorted(Map.Entry.<ProductId, Long>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }
}
```

### Leaderboard (Sorted Set)

```java
@Service
public class GameLeaderboardService {
    
    private final StringRedisTemplate redisTemplate;
    
    public void updateScore(String userId, double score) {
        String key = "leaderboard:2024";
        
        // Add or update score
        redisTemplate.opsForZSet().add(key, userId, score);
    }
    
    public List<LeaderboardEntry> getTopPlayers(int limit) {
        String key = "leaderboard:2024";
        
        // Get top N players (highest scores)
        Set<ZSetOperations.TypedTuple<String>> topScores = 
            redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
        
        return topScores.stream()
            .map(tuple -> new LeaderboardEntry(
                tuple.getValue(),
                tuple.getScore()
            ))
            .toList();
    }
    
    public Optional<Long> getUserRank(String userId) {
        String key = "leaderboard:2024";
        
        // Get rank (0-indexed, higher score = lower rank)
        Long rank = redisTemplate.opsForZSet().reverseRank(key, userId);
        
        return Optional.ofNullable(rank).map(r -> r + 1);  // 1-indexed
    }
}

public record LeaderboardEntry(String userId, Double score) {}
```

---

## 📡 Pub/Sub Messaging

### Publisher

```java
@Service
public class RedisPubSubPublisher {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void publishEvent(String channel, DomainEvent event) {
        redisTemplate.convertAndSend(channel, event);
        
        log.info("Event published to Redis")
            .field("channel", channel)
            .field("eventType", event.getClass().getSimpleName())
            .log();
    }
}
```

### Subscriber

```java
@Service
public class RedisPubSubSubscriber {
    
    @RedisMessageListenerContainer
    public RedisMessageListenerContainer container(
        RedisConnectionFactory connectionFactory,
        MessageListenerAdapter listenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("order-events"));
        return container;
    }
    
    @Bean
    public MessageListenerAdapter listenerAdapter(OrderEventListener listener) {
        return new MessageListenerAdapter(listener, "handleMessage");
    }
}

@Component
public class OrderEventListener {
    
    public void handleMessage(OrderCreatedEvent event) {
        log.info("Received order event")
            .field("orderId", event.orderId().value())
            .log();
        
        // Process event
        processOrderCreated(event);
    }
}
```

---

## 🧪 Testing

### Embedded Redis Test

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.redis.host=localhost",
    "spring.redis.port=6370"  // Different port
})
class RedisCacheServiceTest {
    
    private static RedisServer redisServer;
    
    @BeforeAll
    static void startRedis() throws IOException {
        redisServer = new RedisServer(6370);
        redisServer.start();
    }
    
    @AfterAll
    static void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
    
    @Autowired
    private CacheProvider cache;
    
    @Test
    void shouldCacheUser() {
        // Given
        User user = User.create("john@example.com", "John Doe");
        String key = "user:" + user.id().value();
        
        // When
        cache.set(key, user, Duration.ofMinutes(10));
        
        // Then
        Optional<User> cached = cache.get(key, User.class);
        assertThat(cached).isPresent();
        assertThat(cached.get().email()).isEqualTo("john@example.com");
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use TTL para evitar memory leaks
cache.set(key, value, Duration.ofMinutes(30));

// ✅ Prefixe keys por namespace
"user:" + userId
"product:" + productId

// ✅ Use distributed lock para jobs
distributedLock.withLock(lockKey, ttl, () -> processJob());

// ✅ Serialize para JSON
Jackson2JsonRedisSerializer

// ✅ Monitor memory usage
redis.call("INFO", "memory")
```

### ❌ DON'T

```java
// ❌ NÃO armazene sem TTL
cache.set(key, value);  // ❌ No expiration!

// ❌ NÃO use keys genéricos
cache.set(userId, userData);  // ❌ Collision risk!

// ❌ NÃO bloqueie sem timeout
distributedLock.lock(key);  // ❌ Deadlock risk!

// ❌ NÃO cache dados huge
cache.set(key, hugeObject);  // ❌ > 1MB

// ❌ NÃO ignore connection pool
// Configure lettuce pool properly
```

---

## Ver Também

- [Cache Port](../api-reference/ports/cache.md) - Port interface
- [Rate Limiting](../api-reference/app-api-rate-limiting.md) - Rate limiter patterns
- [Distributed Lock](../api-reference/app-scheduler.md) - Lock usage
