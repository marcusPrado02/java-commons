# Commons Adapters Cache Redis

Redis implementation of `CachePort` using Spring Data Redis.

## Features

- ✅ Full `CachePort` implementation
- ✅ TTL (Time To Live) support
- ✅ JSON serialization with Jackson
- ✅ Key prefix support for multi-tenancy
- ✅ Graceful error handling with logging
- ✅ Type-safe operations with generics
- ✅ Testcontainers integration tests

## Installation

Add to your `pom.xml`:

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-cache-redis</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Configure RedisTemplate
RedisTemplate<String, User> redisTemplate = new RedisTemplate<>();
redisTemplate.setConnectionFactory(connectionFactory);
redisTemplate.setKeySerializer(new StringRedisSerializer());
redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
redisTemplate.afterPropertiesSet();

// Create cache adapter
RedisCacheAdapter<String, User> userCache = new RedisCacheAdapter<>(redisTemplate);

// Store value
User user = new User("john", "john@example.com");
userCache.put("user:1", user);

// Store with TTL
userCache.put("session:abc", sessionData, Duration.ofMinutes(30));

// Retrieve value
Optional<User> cachedUser = userCache.get("user:1");

// Check existence
boolean exists = userCache.contains("user:1");

// Remove value
userCache.remove("user:1");

// Clear all
userCache.clear();
```

### With Key Prefix

```java
// Useful for multi-tenancy or namespace isolation
RedisCacheAdapter<String, Product> productCache =
    new RedisCacheAdapter<>(redisTemplate, "tenant1:products:");

productCache.put("product-123", product);
// Actual Redis key: "tenant1:products:product-123"
```

### Spring Boot Configuration

```java
@Configuration
public class CacheConfig {

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);

    // String serializer for keys
    template.setKeySerializer(new StringRedisSerializer());

    // JSON serializer for values
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    GenericJackson2JsonRedisSerializer jsonSerializer =
        new GenericJackson2JsonRedisSerializer(mapper);
    template.setValueSerializer(jsonSerializer);

    template.afterPropertiesSet();
    return template;
  }

  @Bean
  public CachePort<String, MyEntity> myEntityCache(
      RedisTemplate<String, Object> redisTemplate) {
    return new RedisCacheAdapter<>(redisTemplate, "myapp:entities:");
  }
}
```

### application.yml

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

## Cache Operations

### get(K key)

Retrieve a value from cache. Returns `Optional.empty()` if key doesn't exist or on error.

```java
Optional<String> value = cache.get("mykey");
value.ifPresent(v -> System.out.println("Found: " + v));
````

### put(K key, V value)

Store a value without expiration.

```java
cache.put("user:123", userData);
```

### put(K key, V value, Duration ttl)

Store a value with TTL.

```java
// Cache for 5 minutes
cache.put("token:abc", tokenData, Duration.ofMinutes(5));

// Cache for 1 hour
cache.put("session:xyz", sessionData, Duration.ofHours(1));

// Cache for 30 seconds
cache.put("temp:data", tmpData, Duration.ofSeconds(30));
```

### remove(K key)

Remove a specific key.

```java
cache.remove("expired:key");
```

### clear()

Remove all keys (with current prefix).

```java
cache.clear(); // Removes all keys matching the prefix
```

### contains(K key)

Check if key exists.

```java
if (cache.contains("user:123")) {
    // Key exists
}
```

### keys()

Get all keys (with current prefix).

```java
Set<String> allKeys = cache.keys();
System.out.println("Total keys: " + allKeys.size());
```

### size()

Get total number of entries.

```java
long count = cache.size();
```

## Serialization

The adapter uses **GenericJackson2JsonRedisSerializer** for values, which:

- ✅ Supports complex objects with nested structures
- ✅ Handles Java 8 Date/Time API (Instant, LocalDateTime, etc.)
- ✅ Preserves type information in JSON
- ✅ Works with records, POJOs, and collections

### Serialization Example

```java
record Product(String id, String name, BigDecimal price, LocalDateTime createdAt) {}

RedisCacheAdapter<String, Product> cache = new RedisCacheAdapter<>(redisTemplate);

Product product = new Product(
    "P123",
    "Laptop",
    new BigDecimal("1299.99"),
    LocalDateTime.now()
);

cache.put("product:P123", product);
// Stored as JSON in Redis with type information
```

## Error Handling

All operations handle exceptions gracefully:

- **get()**: Returns `Optional.empty()` on error
- **put(), remove(), clear()**: Log errors but don't throw exceptions
- **contains()**: Returns `false` on error
- **keys(), size()**: Return empty results on error

All errors are logged with SLF4J at ERROR level.

## Performance Considerations

### Connection Pooling

Always use connection pooling in production:

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20    # Maximum connections
          max-idle: 10      # Maximum idle connections
          min-idle: 5       # Minimum idle connections
          max-wait: 2000ms  # Max wait for connection
```

### TTL Best Practices

```java
// Short-lived session data
cache.put("session:" + id, session, Duration.ofMinutes(30));

// Medium-lived cache
cache.put("user:" + id, user, Duration.ofHours(4));

// Long-lived reference data
cache.put("config:" + key, config, Duration.ofDays(1));

// Permanent data (no TTL)
cache.put("static:" + key, data);
```

### Key Naming Conventions

```java
// Use prefixes for organization
RedisCacheAdapter<String, User> userCache =
    new RedisCacheAdapter<>(redisTemplate, "myapp:users:");

RedisCacheAdapter<String, Session> sessionCache =
    new RedisCacheAdapter<>(redisTemplate, "myapp:sessions:");

RedisCacheAdapter<String, Product> productCache =
    new RedisCacheAdapter<>(redisTemplate, "myapp:products:");
```

## Testing

The module includes comprehensive tests using **Testcontainers**:

```java
@Testcontainers
class RedisCacheAdapterTest {

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
          .withExposedPorts(6379);

  // ... tests
}
```

Run tests:

```bash
./mvnw test -pl commons-adapters-cache-redis
```

## When to Use Redis Cache

✅ **Good for**:
- High-throughput caching needs
- Distributed applications (multiple instances)
- Session storage
- Rate limiting counters
- Temporary data with TTL
- Real-time leaderboards/rankings
- Pub/Sub messaging

❌ **Not suitable for**:
- Primary data storage (use a database)
- Long-term persistent data
- Complex queries (use a search engine)
- Large binary files (use object storage)

## Comparison with Other Cache Solutions

| Feature | Redis | In-Memory (Caffeine) | Hazelcast |
|---------|-------|---------------------|-----------|
| Distribution | ✅ External | ❌ Local | ✅ Embedded |
| Persistence | ✅ Optional | ❌ None | ✅ Optional |
| TTL Support | ✅ Yes | ✅ Yes | ✅ Yes |
| Pub/Sub | ✅ Yes | ❌ No | ✅ Yes |
| Complexity | Medium | Low | High |
| Latency | ~1ms | ~μs | ~1ms |

## Dependencies

- Spring Boot 3.2.2
- Spring Data Redis 3.2.2
- Lettuce (default Redis client)
- Jackson 2.x (JSON serialization)
- SLF4J (logging)

## Redis Versions

Tested with:
- ✅ Redis 7.x
- ✅ Redis 6.x
- ✅ Redis 5.x

## License

Part of Java Commons Platform - see root project for license details.
