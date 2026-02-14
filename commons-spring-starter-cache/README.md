# Commons Spring Starter Cache

[![Maven Central](https://img.shields.io/maven-central/v/com.marcusprado02/commons-spring-starter-cache.svg)](https://central.sonatype.com/artifact/com.marcusprado02/commons-spring-starter-cache)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Spring Boot auto-configuration starter for Commons cache adapters, providing zero-configuration integration with **Redis** and **Memcached**.

## Features

- ✅ **Zero-configuration** cache setup with sensible defaults
- 🔄 **Multiple backends**: Redis and Memcached support
- ⚙️ **Flexible configuration** via Spring Boot properties
- 💪 **Type-safe** configuration with `CacheProperties`
- 🏥 **Health checks** via Spring Boot Actuator
- 🎯 **Conditional auto-configuration** based on classpath
- 🔑 **Key prefixes** for multi-tenancy support
- 📦 **Minimal dependencies** (only brings what you need)

## Installation

Add the starter dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.marcusprado02</groupId>
  <artifactId>commons-spring-starter-cache</artifactId>
  <version>${commons.version}</version>
</dependency>
```

### Backend Dependencies

Choose **one or both** cache backends:

#### Redis (recommended)

```xml
<dependency>
  <groupId>com.marcusprado02</groupId>
  <artifactId>commons-adapters-cache-redis</artifactId>
</dependency>
```

#### Memcached

```xml
<dependency>
  <groupId>com.marcusprado02</groupId>
  <artifactId>commons-adapters-cache-memcached</artifactId>
</dependency>
```

### Optional Dependencies

Enable health indicators:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Quick Start

### Redis (default)

With **zero configuration**, the starter will connect to `localhost:6379`:

```java
@Autowired
private CachePort<String, Object> cache;

public void example() {
  // Store value
  cache.put("user:123", user);

  // Retrieve value
  Optional<Object> value = cache.get("user:123");

  // Store with TTL
  cache.put("session:abc", session, Duration.ofHours(1));

  // Remove value
  cache.remove("user:123");
}
```

### Memcached

To use Memcached instead, set the cache type:

```yaml
commons:
  cache:
    type: memcached
```

## Configuration

### Redis Configuration

```yaml
commons:
  cache:
    type: redis  # default
    redis:
      enabled: true  # default
      host: localhost  # default
      port: 6379  # default
      password: secret  # optional
      key-prefix: "myapp:"  # optional, for multi-tenancy
```

### Memcached Configuration

```yaml
commons:
  cache:
    type: memcached
    memcached:
      enabled: true  # default
      host: localhost  # default
      port: 11211  # default
      key-prefix: "myapp:"  # optional, for multi-tenancy
      timeout: 3s  # connection timeout
```

### Disable Cache

```yaml
commons:
  cache:
    type: none
```

Or disable specific backends:

```yaml
commons:
  cache:
    redis:
      enabled: false
    memcached:
      enabled: false
```

## Health Indicators

When Spring Boot Actuator is on the classpath, a health indicator is automatically registered:

```bash
curl http://localhost:8080/actuator/health/cache
```

Response:

```json
{
  "status": "UP",
  "details": {
    "type": "RedisCacheAdapter",
    "status": "Cache read/write successful"
  }
}
```

## Advanced Usage

### Custom RedisTemplate

Override the auto-configured `RedisTemplate`:

```java
@Configuration
public class CacheConfig {

  @Bean
  public RedisTemplate<String, Object> cacheRedisTemplate(
      LettuceConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Custom serializers
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new JdkSerializationRedisSerializer());

    return template;
  }
}
```

### Custom MemcachedClient

Override the auto-configured client:

```java
@Configuration
public class CacheConfig {

  @Bean
  public MemcachedClient memcachedClient() throws IOException {
    return new MemcachedClient(
      AddrUtil.getAddresses("server1:11211 server2:11211")
    );
  }
}
```

### Multiple Cache Instances

Create named cache beans:

```java
@Configuration
public class MultipleCachesConfig {

  @Bean
  public CachePort<String, Object> userCache(
      @Qualifier("userRedisTemplate") RedisTemplate<String, Object> template) {
    return new RedisCacheAdapter<>(template, "users:");
  }

  @Bean
  public CachePort<String, Object> sessionCache(
      @Qualifier("sessionRedisTemplate") RedisTemplate<String, Object> template) {
    return new RedisCacheAdapter<>(template, "sessions:");
  }
}
```

## How It Works

### Auto-Configuration

The starter provides two auto-configuration classes:

1. **`RedisCacheAutoConfiguration`**
   - Activated when `RedisCacheAdapter` is on classpath
   - Configured via `commons.cache.redis.*` properties
   - Creates: `LettuceConnectionFactory`, `RedisTemplate`, `CachePort`

2. **`MemcachedCacheAutoConfiguration`**
   - Activated when `MemcachedCacheAdapter` is on classpath
   - Configured via `commons.cache.memcached.*` properties
   - Creates: `MemcachedClient`, `CachePort`

### Conditional Bean Creation

Beans are created **only when not already defined**, allowing full customization:

```java
@ConditionalOnMissingBean(CachePort.class)
public CachePort<String, Object> cachePort(...) {
  // Auto-configured only if user didn't provide one
}
```

### Type Selection

The `commons.cache.type` property controls which `CachePort` bean is created:

- `redis` (default): Creates Redis-backed cache
- `memcached`: Creates Memcached-backed cache
- `none`: Disables cache auto-configuration

## Testing

The starter includes comprehensive tests for both Redis and Memcached using Testcontainers:

### Example Test

```java
@SpringBootTest
@Testcontainers
class CacheIntegrationTest {

  @Container
  static GenericContainer<?> redis =
    new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("commons.cache.redis.host", redis::getHost);
    registry.add("commons.cache.redis.port", redis::getFirstMappedPort);
  }

  @Autowired
  private CachePort<String, Object> cache;

  @Test
  void shouldCacheData() {
    cache.put("key", "value");
    assertThat(cache.get("key")).hasValue("value");
  }
}
```

## Configuration Properties Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `commons.cache.type` | `CacheType` | `redis` | Cache backend: `redis`, `memcached`, `none` |
| `commons.cache.redis.enabled` | `boolean` | `true` | Enable Redis cache |
| `commons.cache.redis.host` | `String` | `localhost` | Redis server host |
| `commons.cache.redis.port` | `int` | `6379` | Redis server port |
| `commons.cache.redis.password` | `String` | - | Redis password (optional) |
| `commons.cache.redis.key-prefix` | `String` | `""` | Key prefix for all operations |
| `commons.cache.memcached.enabled` | `boolean` | `true` | Enable Memcached cache |
| `commons.cache.memcached.host` | `String` | `localhost` | Memcached server host |
| `commons.cache.memcached.port` | `int` | `11211` | Memcached server port |
| `commons.cache.memcached.key-prefix` | `String` | `""` | Key prefix for all operations |
| `commons.cache.memcached.timeout` | `Duration` | `3s` | Connection timeout |

## IDE Support

Enable auto-completion for configuration properties in IntelliJ IDEA / VS Code:

1. Ensure `spring-boot-configuration-processor` is on classpath (already included as optional dependency)
2. IDE will provide auto-completion for `commons.cache.*` properties

## Architecture

The starter follows the **Port-Adapter pattern** (Hexagonal Architecture):

```
┌──────────────────────────────────────┐
│   Spring Boot Application            │
├──────────────────────────────────────┤
│   commons-spring-starter-cache       │ ← Auto-configuration
│   ├─ RedisCacheAutoConfiguration     │
│   ├─ MemcachedCacheAutoConfiguration │
│   └─ CacheHealthIndicator            │
├──────────────────────────────────────┤
│   CachePort<K, V>                    │ ← Port (Interface)
├──────────────────────────────────────┤
│   Adapters                           │
│   ├─ RedisCacheAdapter               │ ← Redis Implementation
│   └─ MemcachedCacheAdapter           │ ← Memcached Implementation
└──────────────────────────────────────┘
```

## Comparison with Spring Cache

| Feature | Commons Cache Starter | Spring `@Cacheable` |
|---------|----------------------|-------------------- |
| **Programming Model** | Imperative (manual control) | Declarative (annotations) |
| **Use Case** | Direct cache access | Method-level caching |
| **Flexibility** | Full control over caching logic | Limited to method results |
| **Key Management** | Manual | Automatic (via SpEL) |
| **Backend Support** | Redis, Memcached | Many (via `CacheManager`) |
| **Port-Adapter** | ✅ Follows Hexagonal Architecture | ❌ Spring-specific |
| **Testing** | Easy to mock `CachePort` | Requires `CacheManager` setup |

**When to use Commons Cache:**
- ✅ Need direct cache access (not just method caching)
- ✅ Want port-adapter pattern for testability
- ✅ Prefer imperative over declarative
- ✅ Need fine-grained TTL control per key

**When to use `@Cacheable`:**
- ✅ Simple method-level caching
- ✅ Prefer declarative approach
- ✅ Already using Spring Cache infrastructure

## Examples

### E-commerce Product Cache

```java
@Service
public class ProductService {

  private final CachePort<String, Product> cache;
  private final ProductRepository repository;

  public Optional<Product> findById(String id) {
    // Try cache first
    return cache.get("product:" + id)
      .map(obj -> (Product) obj)
      .or(() -> {
        // Cache miss - fetch from DB
        Optional<Product> product = repository.findById(id);
        product.ifPresent(p -> cache.put("product:" + id, p, Duration.ofMinutes(10)));
        return product;
      });
  }

  public void updateProduct(Product product) {
    repository.save(product);
    cache.remove("product:" + product.getId());  // Invalidate cache
  }
}
```

### Session Management

```java
@Service
public class SessionService {

  private final CachePort<String, UserSession> cache;

  public void createSession(String sessionId, UserSession session) {
    cache.put("session:" + sessionId, session, Duration.ofHours(2));
  }

  public Optional<UserSession> getSession(String sessionId) {
    return cache.get("session:" + sessionId)
      .map(obj -> (UserSession) obj);
  }

  public void extendSession(String sessionId) {
    getSession(sessionId).ifPresent(session ->
      cache.put("session:" + sessionId, session, Duration.ofHours(2))
    );
  }

  public void logout(String sessionId) {
    cache.remove("session:" + sessionId);
  }
}
```

### Rate Limiting

```java
@Service
public class RateLimiter {

  private final CachePort<String, Integer> cache;

  public boolean isAllowed(String clientId, int maxRequests, Duration window) {
    String key = "ratelimit:" + clientId;

    Integer count = cache.get(key)
      .map(obj -> (Integer) obj)
      .orElse(0);

    if (count >= maxRequests) {
      return false;
    }

    cache.put(key, count + 1, window);
    return true;
  }
}
```

## Troubleshooting

### Cache not auto-configured

**Symptom:** No `CachePort` bean available

**Solutions:**
1. Ensure backend dependency is on classpath:
   ```xml
   <dependency>
     <groupId>com.marcusprado02</groupId>
     <artifactId>commons-adapters-cache-redis</artifactId>
   </dependency>
   ```

2. Check configuration:
   ```yaml
   commons:
     cache:
       type: redis  # or memcached
       redis:
         enabled: true
   ```

3. Enable auto-configuration debug:
   ```yaml
   logging:
     level:
       com.marcusprado02.commons.spring.cache: DEBUG
   ```

### Connection refused

**Symptom:** Health check fails with connection error

**Solutions:**
1. Verify Redis/Memcached is running:
   ```bash
   # Redis
   docker run -d -p 6379:6379 redis:7-alpine

   # Memcached
   docker run -d -p 11211:11211 memcached:1.6-alpine
   ```

2. Check host/port configuration:
   ```yaml
   commons:
     cache:
       redis:
         host: localhost  # or container name in Docker Compose
         port: 6379
   ```

### Serialization errors

**Symptom:** `SerializationException` when reading cached objects

**Solutions:**
1. Ensure classes are serializable
2. Use custom `RedisTemplate` with appropriate serializers
3. Consider using JSON serialization (already configured by default)

## Migration Guide

### From Spring Cache

Replace `@Cacheable`:

```java
// Before (Spring Cache)
@Cacheable(value = "products", key = "#id")
public Product findById(String id) {
  return repository.findById(id).orElse(null);
}

// After (Commons Cache)
public Optional<Product> findById(String id) {
  return cache.get("product:" + id)
    .map(obj -> (Product) obj)
    .or(() -> {
      Optional<Product> product = repository.findById(id);
      product.ifPresent(p -> cache.put("product:" + id, p));
      return product;
    });
}
```

### From direct Redis/Memcached usage

Replace client usage:

```java
// Before (direct RedisTemplate)
@Autowired
private RedisTemplate<String, Product> redisTemplate;

public void save(Product product) {
  redisTemplate.opsForValue().set("product:" + product.getId(), product);
}

// After (CachePort)
@Autowired
private CachePort<String, Object> cache;

public void save(Product product) {
  cache.put("product:" + product.getId(), product);
}
```

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for development guidelines.

## License

Apache License 2.0 - see [LICENSE](../LICENSE) for details.

## See Also

- [commons-adapters-cache-redis](../commons-adapters-cache-redis/README.md) - Redis adapter implementation
- [commons-adapters-cache-memcached](../commons-adapters-cache-memcached/README.md) - Memcached adapter implementation
- [commons-ports-cache](../commons-ports-cache/README.md) - CachePort interface
- [Spring Boot Starters](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.starters) - Official documentation
