# Commons Adapters Cache Memcached

Memcached implementation of `CachePort` using the Spymemcached client.

## Features

- ✅ **CachePort Implementation**: Full implementation of all cache operations
- ✅ **JSON Serialization**: Automatic JSON serialization/deserialization with Jackson
- ✅ **TTL Support**: Time-to-live for cache entries
- ✅ **Key Prefix**: Multi-tenancy support with key prefixes
- ✅ **Type-Safe**: Generic types for keys and values
- ✅ **Graceful Error Handling**: Doesn't throw exceptions on cache failures
- ✅ **Local Key Tracking**: Workaround for Memcached's lack of key listing
- ✅ **Async Operations**: Uses Spymemcached's async API for better performance
- ✅ **Connection Pooling**: Built-in connection management
- ✅ **Testcontainers Support**: Easy integration testing

## Installation

### Maven

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-cache-memcached</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.marcusprado02.commons:commons-adapters-cache-memcached:0.1.0-SNAPSHOT'
```

## Quick Start

```java
import com.marcusprado02.commons.adapters.cache.memcached.MemcachedCacheAdapter;
import net.spy.memcached.MemcachedClient;
import java.net.InetSocketAddress;
import java.time.Duration;

// Create Memcached client
MemcachedClient client = new MemcachedClient(
    new InetSocketAddress("localhost", 11211)
);

// Create adapter
MemcachedCacheAdapter<String, String> cache =
    new MemcachedCacheAdapter<>(client, String.class);

// Use the cache
cache.put("user:123", "John Doe");
cache.put("session:abc", "active", Duration.ofMinutes(30));

cache.get("user:123").ifPresent(System.out::println); // "John Doe"

// Cleanup
cache.shutdown();
```

## Configuration

### Basic Configuration

```java
// Without key prefix
MemcachedClient client = new MemcachedClient(
    new InetSocketAddress("localhost", 11211)
);
MemcachedCacheAdapter<String, User> cache =
    new MemcachedCacheAdapter<>(client, User.class);
```

### With Key Prefix (Multi-tenancy)

```java
// With key prefix for tenant isolation
MemcachedCacheAdapter<String, User> tenantCache =
    new MemcachedCacheAdapter<>(client, User.class, "tenant1:");

tenantCache.put("user:123", user); // Stored as "tenant1:user:123"
```

### Multiple Servers (Clustering)

```java
// Connect to multiple Memcached servers
List<InetSocketAddress> servers = Arrays.asList(
    new InetSocketAddress("memcached1.example.com", 11211),
    new InetSocketAddress("memcached2.example.com", 11211),
    new InetSocketAddress("memcached3.example.com", 11211)
);

MemcachedClient client = new MemcachedClient(servers);
MemcachedCacheAdapter<String, String> cache =
    new MemcachedCacheAdapter<>(client, String.class);
```

### Custom Connection Factory

```java
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;

// Configure connection factory
ConnectionFactory cf = new ConnectionFactoryBuilder()
    .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
    .setOpTimeout(1000)  // 1 second timeout
    .setDaemon(true)
    .setFailureMode(FailureMode.Redistribute)
    .build();

MemcachedClient client = new MemcachedClient(cf, servers);
```

## Usage Examples

### Basic Operations

```java
MemcachedCacheAdapter<String, String> cache =
    new MemcachedCacheAdapter<>(client, String.class);

// Put without TTL (no expiration)
cache.put("key1", "value1");

// Put with TTL
cache.put("session:abc", "user-data", Duration.ofMinutes(30));

// Get value
Optional<String> value = cache.get("key1");
value.ifPresent(v -> System.out.println("Value: " + v));

// Remove value
cache.remove("key1");

// Check if key exists
boolean exists = cache.contains("key1");

// Get all keys
Set<String> keys = cache.keys();

// Get cache size
long size = cache.size();

// Clear all entries
cache.clear();
```

### Complex Objects

```java
record User(String name, String email, int age) {}

MemcachedCacheAdapter<String, User> cache =
    new MemcachedCacheAdapter<>(client, User.class);

User user = new User("John Doe", "john@example.com", 30);
cache.put("user:123", user);

cache.get("user:123").ifPresent(u -> {
    System.out.println("Name: " + u.name());
    System.out.println("Email: " + u.email());
});
```

### Lists and Collections

```java
MemcachedCacheAdapter<String, List<String>> cache =
    new MemcachedCacheAdapter<>(client,
        new TypeReference<List<String>>() {}.getClass());

List<String> tags = Arrays.asList("java", "cache", "memcached");
cache.put("post:123:tags", tags);
```

### Batch Operations

```java
// Put multiple values
Map<String, String> entries = Map.of(
    "key1", "value1",
    "key2", "value2",
    "key3", "value3"
);

entries.forEach(cache::put);

// Get multiple values
Set<String> keys = Set.of("key1", "key2", "key3");
Map<String, String> results = keys.stream()
    .collect(Collectors.toMap(
        k -> k,
        k -> cache.get(k).orElse(null)
    ));
```

### TTL and Expiration

```java
// Different TTL strategies
cache.put("short-lived", "data", Duration.ofSeconds(30));
cache.put("medium-lived", "data", Duration.ofMinutes(15));
cache.put("long-lived", "data", Duration.ofHours(1));
cache.put("daily", "data", Duration.ofDays(1));

// No expiration (until manually removed or memory full)
cache.put("permanent", "data"); // or Duration.ZERO
```

## Error Handling

The adapter handles errors gracefully and never throws exceptions from cache operations:

```java
// Safe to call even if Memcached is down
Optional<String> value = cache.get("key"); // Returns Optional.empty() on error
cache.put("key", "value"); // Logs error but doesn't throw

// Check logs for errors
// 2026-02-14 10:15:30 ERROR c.m.c.a.c.m.MemcachedCacheAdapter - Error getting key: user:123
```

### Error Categories

| Category | Handling | Example |
|----------|----------|---------|
| Network timeout | Returns empty/false | `get()` returns `Optional.empty()` |
| Connection failure | Logs error, continues | `put()` logs and returns |
| Serialization error | Throws RuntimeException | Invalid JSON structure |
| Thread interruption | Logs, sets interrupt flag | `InterruptedException` |

## Key Tracking Limitation

**Important**: Memcached doesn't provide a native way to list all keys. This adapter maintains a local `ConcurrentHashMap` to track keys for `keys()`, `clear()`, and `size()` operations.

### Implications

1. **Memory Usage**: Key tracker consumes heap memory
2. **Synchronization**: Keys must be managed through this adapter instance
3. **Restart Behavior**: Key tracker is cleared on application restart
4. **Multi-Instance**: Different adapter instances have different key views

### Workarounds

```java
// Option 1: Use a centralized key registry
Set<String> keySet = Collections.synchronizedSet(new HashSet<>());
cache.put("user:123", user);
keySet.add("user:123");

// Option 2: Use Redis for key listing (if you need it)
// Memcached is best for simple key-value caching without key enumeration

// Option 3: Disable key tracking (extend adapter and override)
class NoTrackingAdapter<K, V> extends MemcachedCacheAdapter<K, V> {
    @Override public Set<K> keys() { return Set.of(); }
    @Override public long size() { return -1; }
}
```

## Performance Considerations

### Connection Pooling

Spymemcached uses a single connection per server with multiplexed requests:

```java
// One client instance is shared across threads (thread-safe)
MemcachedClient client = new MemcachedClient(servers);

// Reuse adapter instances
@Bean
public MemcachedCacheAdapter<String, User> userCache(MemcachedClient client) {
    return new MemcachedCacheAdapter<>(client, User.class, "users:");
}
```

### TTL Best Practices

```java
// ✅ Good: Reasonable TTL based on data characteristics
cache.put("trending:posts", posts, Duration.ofMinutes(5));  // Frequently updated
cache.put("user:profile", user, Duration.ofHours(1));       // Moderately stable
cache.put("country:list", countries, Duration.ofDays(1));    // Rarely changes

// ❌ Bad: Too short (cache thrashing)
cache.put("config", data, Duration.ofSeconds(1));

// ❌ Bad: Too long for dynamic data
cache.put("stock:price", price, Duration.ofDays(7));
```

### Key Naming

```java
// ✅ Good: Structured, hierarchical keys
cache.put("user:123:profile", user);
cache.put("post:456:comments", comments);
cache.put("session:abc:cart", cart);

// ❌ Bad: Flat, unstructured keys
cache.put("u123p", user);
cache.put("p456c", comments);
```

### Serialization Performance

```java
// JSON serialization overhead
// Small objects (~1KB): ~0.1ms
// Medium objects (~10KB): ~1ms
// Large objects (~100KB): ~10ms

// For very high performance, consider:
// 1. Smaller objects (normalize data)
// 2. Binary serialization (Protobuf, MessagePack)
// 3. Compression for large objects
```

## Testing

### With Testcontainers

```java
@Testcontainers
class MyServiceTest {

    @Container
    static GenericContainer<?> memcached = new GenericContainer<>(
        DockerImageName.parse("memcached:1.6-alpine")
    ).withExposedPorts(11211);

    static MemcachedClient client;
    static MemcachedCacheAdapter<String, String> cache;

    @BeforeAll
    static void setup() throws Exception {
        InetSocketAddress address = new InetSocketAddress(
            memcached.getHost(),
            memcached.getMappedPort(11211)
        );
        client = new MemcachedClient(address);
        cache = new MemcachedCacheAdapter<>(client, String.class);
    }

    @AfterAll
    static void cleanup() {
        client.shutdown();
    }

    @BeforeEach
    void clearCache() {
        client.flush(); // Clear all entries
    }

    @Test
    void shouldCacheData() {
        cache.put("test", "value");
        assertThat(cache.get("test")).contains("value");
    }
}
```

### With Docker Compose

```yaml
# docker-compose.yml
version: '3.8'
services:
  memcached:
    image: memcached:1.6-alpine
    ports:
      - "11211:11211"
    command: ["-m", "64"]  # 64MB memory limit
```

```bash
# Start Memcached
docker-compose up -d memcached

# Run tests
mvn test

# Stop Memcached
docker-compose down
```

## Comparison: Memcached vs Redis vs Caffeine

| Feature | Memcached | Redis | Caffeine |
|---------|-----------|-------|----------|
| **Type** | Distributed | Distributed | Local (in-memory) |
| **Data Structures** | Key-Value only | Rich (Hash, List, Set, etc.) | Key-Value only |
| **Persistence** | ❌ No | ✅ Yes (RDB, AOF) | ❌ No |
| **Key Listing** | ❌ No (requires workaround) | ✅ Yes (`KEYS`, `SCAN`) | ✅ Yes |
| **TTL Precision** | ❌ Seconds | ✅ Milliseconds | ✅ Nanoseconds |
| **Atomic Operations** | ✅ Limited (incr, decr, CAS) | ✅ Rich (INCR, ZINCRBY, etc.) | ✅ Yes |
| **Pub/Sub** | ❌ No | ✅ Yes | ❌ No |
| **Clustering** | ✅ Client-side | ✅ Native (Redis Cluster) | ❌ Single JVM |
| **Memory Management** | ✅ Slab allocator (efficient) | ✅ jemalloc | ✅ JVM heap |
| **Max Value Size** | ✅ 1MB (configurable) | ✅ 512MB | ✅ Limited by heap |
| **Performance** | ✅ Very fast, simple | ✅ Fast, feature-rich | ✅ Fastest (no network) |
| **Use Case** | Simple caching, sessions | Feature-rich caching, queues | Local caching, high-speed |

## When to Use Memcached

### ✅ Good Fit

- **Simple key-value caching**: No need for complex data structures
- **Session storage**: Fast, distributed session management
- **Page/fragment caching**: Web page components
- **Query result caching**: Database query results
- **API response caching**: External API call results
- **High-throughput caching**: Millions of requests/second
- **Horizontal scaling**: Add more servers easily
- **Consistent hash distribution**: Automatic key distribution

### ❌ Not Recommended

- **Complex data**: Need for lists, sets, sorted sets → Use Redis
- **Persistent caching**: Need to survive restart → Use Redis with persistence
- **Key enumeration**: Need to list all keys frequently → Use Redis
- **Pub/Sub messaging**: Real-time events → Use Redis
- **Transactions**: Need ACID guarantees → Use database
- **Single JVM caching**: No need for distribution → Use Caffeine
- **Large values**: > 1MB per value → Consider other solutions

## Advanced Configuration

### Memory Limit

```bash
# Start Memcached with 128MB memory
memcached -m 128

# Or in Docker
docker run -d -p 11211:11211 memcached:1.6-alpine -m 128
```

### Eviction Policy

Memcached uses LRU (Least Recently Used) eviction by default:

```bash
# Default: LRU eviction
memcached -m 64

# No eviction (return error when full)
memcached -m 64 -M
```

### Max Connections

```bash
# Default: 1024 connections
memcached -c 1024

# Increase for high-concurrency apps
memcached -c 2048
```

### Statistics

```java
// Get cache statistics
Map<SocketAddress, Map<String, String>> stats = client.getStats();

stats.forEach((server, stat) -> {
    System.out.println("Server: " + server);
    System.out.println("  Total items: " + stat.get("curr_items"));
    System.out.println("  Total connections: " + stat.get("curr_connections"));
    System.out.println("  Get hits: " + stat.get("get_hits"));
    System.out.println("  Get misses: " + stat.get("get_misses"));
    System.out.println("  Bytes used: " + stat.get("bytes"));
});
```

## Best Practices

### ✅ Do's

1. **Reuse client instances** - Create once, reuse everywhere
2. **Use key prefixes** - Separate different data types
3. **Set reasonable TTLs** - Balance freshness and cache hit rate
4. **Monitor cache metrics** - Track hit rate, memory usage
5. **Handle failures gracefully** - Don't crash on cache errors

### ❌ Don'ts

1. **Don't store critical data only in cache** - Always have a source of truth
2. **Don't cache very large objects** - Keep values < 100KB ideally
3. **Don't use Memcached for locks** - Use Redis or ZooKeeper
4. **Don't forget to shutdown** - Call `cache.shutdown()` on app exit
5. **Don't ignore serialization costs** - Profile your serialization

## Troubleshooting

### Connection Issues

```java
// Problem: Can't connect to Memcached
// Solution: Check host, port, firewall

telnet localhost 11211  // Test connectivity
```

### Key Not Found

```java
// Problem: Key exists but get() returns empty
// Solution: Check TTL, check key prefix, verify type

cache.put("key", "value", Duration.ofSeconds(1));
Thread.sleep(2000);
cache.get("key"); // Returns empty (expired)
```

### Serialization Errors

```java
// Problem: RuntimeException on put()
// Solution: Ensure class is JSON serializable

// ❌ Bad: Lambda not serializable
cache.put("func", () -> "value");

// ✅ Good: Serializable POJO
cache.put("user", new User("John", "john@example.com"));
```

### Memory Issues

```bash
# Problem: High memory usage in Java heap
# Solution: Monitor key tracker size, consider Redis if needed

# Check number of tracked keys
long trackedKeys = cache.size();
System.out.println("Tracked keys: " + trackedKeys);

# If too many keys, consider not using keys(), clear(), size()
```

## Examples

### Complete Example: User Service

```java
import com.marcusprado02.commons.adapters.cache.memcached.MemcachedCacheAdapter;
import net.spy.memcached.MemcachedClient;
import java.time.Duration;
import java.util.Optional;

public class UserService {

    private final UserRepository repository;
    private final MemcachedCacheAdapter<String, User> cache;

    public UserService(UserRepository repository, MemcachedClient client) {
        this.repository = repository;
        this.cache = new MemcachedCacheAdapter<>(client, User.class, "user:");
    }

    public User getUser(String userId) {
        // Try cache first
        Optional<User> cached = cache.get(userId);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Cache miss: Load from database
        User user = repository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // Store in cache
        cache.put(userId, user, Duration.ofMinutes(15));

        return user;
    }

    public void updateUser(String userId, User user) {
        repository.save(user);

        // Invalidate cache
        cache.remove(userId);

        // Or update cache directly
        // cache.put(userId, user, Duration.ofMinutes(15));
    }

    public void deleteUser(String userId) {
        repository.deleteById(userId);
        cache.remove(userId);
    }
}
```

## Dependencies

- **Spymemcached**: 2.12.3
- **Jackson Core**: Latest (from parent)
- **Jackson Datatype JSR310**: Latest (from parent)
- **SLF4J API**: Latest (from parent)
- **JUnit Jupiter**: Test scope
- **Testcontainers**: 1.19.3 (test scope)
- **AssertJ**: Test scope

## License

This project is part of the Commons Platform and inherits its license.

## Support

For issues, questions, or contributions, please refer to the main Commons Platform repository.
