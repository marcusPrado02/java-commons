# Port: Cache

## Visão Geral

`commons-ports-cache` define contratos para cache distribuído, abstraindo implementações como Redis, Memcached, ou in-memory caches.

**Quando usar:**
- Cache de queries frequentes
- Session storage distribuído
- Rate limiting com token bucket
- Distributed locking
- Reduce database load

**Implementações disponíveis:**
- `commons-adapters-cache-redis` - Redis
- `commons-adapters-cache-memcached` - Memcached
- `commons-adapters-persistence-inmemory` - In-memory (testing)

---

## 📦 Instalação

```xml
<!-- Port (interface) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-cache</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapter (implementação) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-cache-redis</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Interfaces

### CacheProvider

Interface principal para cache.

```java
public interface CacheProvider {
    
    /**
     * Armazena valor no cache.
     */
    <T> Result<Void> set(String key, T value, Duration ttl);
    
    /**
     * Recupera valor do cache.
     */
    <T> Optional<T> get(String key, Class<T> type);
    
    /**
     * Remove valor do cache.
     */
    Result<Void> delete(String key);
    
    /**
     * Verifica se key existe.
     */
    boolean exists(String key);
    
    /**
     * Incrementa valor numérico.
     */
    Result<Long> increment(String key);
    
    /**
     * Incrementa por delta.
     */
    Result<Long> incrementBy(String key, long delta);
    
    /**
     * Define expiração para key existente.
     */
    Result<Void> expire(String key, Duration ttl);
    
    /**
     * Remove todas as keys (cuidado!).
     */
    Result<Void> clear();
}
```

### CacheEntry

Representa entrada de cache com metadados.

```java
public record CacheEntry<T>(
    String key,
    T value,
    Instant createdAt,
    Instant expiresAt
) {
    public Duration ttl() {
        return Duration.between(Instant.now(), expiresAt);
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
```

---

## 💡 Basic Usage

### Simple Get/Set

```java
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final CacheProvider cache;
    
    public Result<User> getUser(UserId userId) {
        String cacheKey = "user:" + userId.value();
        
        // Try cache first
        Optional<User> cached = cache.get(cacheKey, User.class);
        
        if (cached.isPresent()) {
            return Result.ok(cached.get());
        }
        
        // Cache miss - load from database
        return userRepository.findById(userId)
            .map(user -> {
                // Store in cache for 1 hour
                cache.set(cacheKey, user, Duration.ofHours(1));
                return user;
            });
    }
    
    public Result<Void> updateUser(UserId userId, UpdateUserCommand command) {
        return userRepository.update(userId, command)
            .andThen(() -> {
                // Invalidate cache
                String cacheKey = "user:" + userId.value();
                cache.delete(cacheKey);
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
        return getFromCache(productId)
            .or(() -> loadFromDatabase(productId));
    }
    
    private Result<Product> getFromCache(ProductId productId) {
        String key = cacheKey(productId);
        
        return cache.get(key, Product.class)
            .map(Result::ok)
            .orElse(Result.fail(Problem.of("CACHE.MISS", "Not in cache")));
    }
    
    private Result<Product> loadFromDatabase(ProductId productId) {
        return productRepository.findById(productId)
            .andThen(product -> {
                String key = cacheKey(productId);
                cache.set(key, product, Duration.ofMinutes(30));
            });
    }
    
    private String cacheKey(ProductId productId) {
        return "product:" + productId.value();
    }
}
```

---

## 🔄 Advanced Caching Patterns

### Cache Wrapper

```java
public class CachedRepository<T, ID> {
    
    private final Repository<T, ID> repository;
    private final CacheProvider cache;
    private final String prefix;
    private final Duration ttl;
    
    public Result<T> findById(ID id) {
        String key = cacheKey(id);
        
        Optional<T> cached = cache.get(key, (Class<T>) Object.class);
        
        if (cached.isPresent()) {
            return Result.ok(cached.get());
        }
        
        return repository.findById(id)
            .andThen(entity -> cache.set(key, entity, ttl));
    }
    
    public Result<Void> save(T entity) {
        return repository.save(entity)
            .andThen(() -> {
                ID id = extractId(entity);
                String key = cacheKey(id);
                cache.delete(key);
            });
    }
    
    private String cacheKey(ID id) {
        return prefix + ":" + id.toString();
    }
}
```

### Multi-Level Cache

```java
@Service
public class MultiLevelCacheService {
    
    private final CacheProvider localCache;     // In-memory
    private final CacheProvider distributedCache; // Redis
    private final ProductRepository repository;
    
    public Result<Product> getProduct(ProductId productId) {
        String key = "product:" + productId.value();
        
        // L1: Local cache
        Optional<Product> local = localCache.get(key, Product.class);
        if (local.isPresent()) {
            return Result.ok(local.get());
        }
        
        // L2: Distributed cache
        Optional<Product> distributed = distributedCache.get(key, Product.class);
        if (distributed.isPresent()) {
            Product product = distributed.get();
            // Warm up local cache
            localCache.set(key, product, Duration.ofMinutes(5));
            return Result.ok(product);
        }
        
        // L3: Database
        return repository.findById(productId)
            .andThen(product -> {
                distributedCache.set(key, product, Duration.ofHours(1));
                localCache.set(key, product, Duration.ofMinutes(5));
            });
    }
}
```

---

## 🔢 Counter Pattern

### View Counter

```java
@Service
public class ViewCounterService {
    
    private final CacheProvider cache;
    
    public Result<Long> incrementViews(ArticleId articleId) {
        String key = "views:article:" + articleId.value();
        
        return cache.incrementBy(key, 1)
            .andThen(count -> {
                // Set expiration if first increment
                if (count == 1) {
                    cache.expire(key, Duration.ofDays(30));
                }
            });
    }
    
    public Optional<Long> getViews(ArticleId articleId) {
        String key = "views:article:" + articleId.value();
        return cache.get(key, Long.class);
    }
    
    public Result<Map<ArticleId, Long>> getMostViewed(int limit) {
        // Implementation depends on cache adapter
        // Redis: ZREVRANGE views:articles 0 limit
        return Result.ok(Map.of());
    }
}
```

### Rate Limiting with Cache

```java
@Service
public class RateLimiterService {
    
    private final CacheProvider cache;
    
    public boolean tryAcquire(String clientId, int maxRequests, Duration window) {
        String key = "rate-limit:" + clientId;
        
        // Get current count
        Long count = cache.get(key, Long.class).orElse(0L);
        
        if (count >= maxRequests) {
            return false;
        }
        
        // Increment
        cache.incrementBy(key, 1);
        
        // Set expiration on first request
        if (count == 0) {
            cache.expire(key, window);
        }
        
        return true;
    }
}
```

---

## 🔒 Distributed Lock

### DistributedLock Interface

```java
public interface DistributedLock {
    
    /**
     * Tenta adquirir lock.
     * 
     * @return true se lock adquirido, false se já travado
     */
    boolean tryLock(String lockKey, Duration ttl);
    
    /**
     * Libera lock.
     */
    void unlock(String lockKey);
    
    /**
     * Executa ação com lock.
     */
    <T> Result<T> withLock(
        String lockKey,
        Duration ttl,
        Supplier<Result<T>> action
    );
}
```

### Redis Lock Implementation

```java
public class RedisDistributedLock implements DistributedLock {
    
    private final CacheProvider cache;
    private final String lockValue;
    
    public RedisDistributedLock(CacheProvider cache) {
        this.cache = cache;
        this.lockValue = UUID.randomUUID().toString();
    }
    
    @Override
    public boolean tryLock(String lockKey, Duration ttl) {
        String key = "lock:" + lockKey;
        
        // SET key value NX EX ttl
        // NX = only if not exists
        return cache.setIfAbsent(key, lockValue, ttl)
            .isOk();
    }
    
    @Override
    public void unlock(String lockKey) {
        String key = "lock:" + lockKey;
        
        // Only delete if we own the lock
        Optional<String> currentValue = cache.get(key, String.class);
        
        if (currentValue.isPresent() && currentValue.get().equals(lockValue)) {
            cache.delete(key);
        }
    }
    
    @Override
    public <T> Result<T> withLock(
        String lockKey,
        Duration ttl,
        Supplier<Result<T>> action
    ) {
        boolean acquired = tryLock(lockKey, ttl);
        
        if (!acquired) {
            return Result.fail(Problem.of(
                "LOCK.ACQUISITION_FAILED",
                "Could not acquire lock: " + lockKey
            ));
        }
        
        try {
            return action.get();
        } finally {
            unlock(lockKey);
        }
    }
}
```

### Usage with Scheduled Tasks

```java
@Component
public class OrderProcessorJob implements ScheduledTask {
    
    private final DistributedLock distributedLock;
    private final OrderService orderService;
    
    @Override
    public String name() {
        return "process-pending-orders";
    }
    
    @Override
    public String cronExpression() {
        return "0 */5 * * * ?"; // Every 5 minutes
    }
    
    @Override
    public Result<Void> execute() {
        // Only one instance should process orders
        return distributedLock.withLock(
            "process-orders",
            Duration.ofMinutes(5),
            () -> {
                List<Order> pending = orderService.findPendingOrders();
                
                for (Order order : pending) {
                    orderService.process(order);
                }
                
                return Result.ok();
            }
        );
    }
}
```

---

## 📊 Cache Statistics

### CacheStats

```java
public record CacheStats(
    long hits,
    long misses,
    long evictions,
    long size
) {
    public double hitRate() {
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }
    
    public double missRate() {
        return 1.0 - hitRate();
    }
}
```

### Cache Monitoring

```java
@Service
public class CacheMonitoringService {
    
    private final CacheProvider cache;
    private final MetricsFacade metrics;
    
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    
    public <T> Optional<T> getWithMetrics(String key, Class<T> type) {
        Optional<T> value = cache.get(key, type);
        
        if (value.isPresent()) {
            hits.incrementAndGet();
            metrics.incrementCounter("cache.hit", "key", key);
        } else {
            misses.incrementAndGet();
            metrics.incrementCounter("cache.miss", "key", key);
        }
        
        return value;
    }
    
    public CacheStats getStats() {
        return new CacheStats(
            hits.get(),
            misses.get(),
            0,
            0
        );
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void recordMetrics() {
        CacheStats stats = getStats();
        
        metrics.recordGauge("cache.hit_rate", stats.hitRate());
        metrics.recordGauge("cache.miss_rate", stats.missRate());
    }
}
```

---

## 🧪 Testing

### In-Memory Cache for Tests

```java
public class InMemoryCacheProvider implements CacheProvider {
    
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    
    @Override
    public <T> Result<Void> set(String key, T value, Duration ttl) {
        Instant expiresAt = Instant.now().plus(ttl);
        cache.put(key, new CacheEntry<>(key, value, Instant.now(), expiresAt));
        return Result.ok();
    }
    
    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        CacheEntry<?> entry = cache.get(key);
        
        if (entry == null) {
            return Optional.empty();
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            return Optional.empty();
        }
        
        return Optional.of((T) entry.value());
    }
    
    @Override
    public Result<Void> delete(String key) {
        cache.remove(key);
        return Result.ok();
    }
    
    @Override
    public boolean exists(String key) {
        return cache.containsKey(key) && !cache.get(key).isExpired();
    }
    
    @Override
    public Result<Void> clear() {
        cache.clear();
        return Result.ok();
    }
}
```

### Test Example

```java
class UserServiceTest {
    
    private CacheProvider cache;
    private UserRepository userRepository;
    private UserService userService;
    
    @BeforeEach
    void setUp() {
        cache = new InMemoryCacheProvider();
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository, cache);
    }
    
    @Test
    void shouldCacheUserOnFirstLoad() {
        // Given
        UserId userId = UserId.generate();
        User user = User.create("john@example.com", "John Doe");
        
        when(userRepository.findById(userId))
            .thenReturn(Result.ok(user));
        
        // When: First call
        Result<User> result1 = userService.getUser(userId);
        
        // Then: Loaded from database
        assertThat(result1.isOk()).isTrue();
        verify(userRepository, times(1)).findById(userId);
        
        // When: Second call
        Result<User> result2 = userService.getUser(userId);
        
        // Then: Loaded from cache (no additional DB call)
        assertThat(result2.isOk()).isTrue();
        verify(userRepository, times(1)).findById(userId); // Still 1
    }
    
    @Test
    void shouldInvalidateCacheOnUpdate() {
        // Given: User in cache
        UserId userId = UserId.generate();
        User user = User.create("john@example.com", "John Doe");
        cache.set("user:" + userId.value(), user, Duration.ofHours(1));
        
        // When: Update user
        UpdateUserCommand command = new UpdateUserCommand("Jane Doe");
        userService.updateUser(userId, command);
        
        // Then: Cache invalidated
        Optional<User> cached = cache.get("user:" + userId.value(), User.class);
        assertThat(cached).isEmpty();
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use TTL adequado
cache.set(key, value, Duration.ofMinutes(30));  // ✅ Finite TTL

// ✅ Trate cache miss gracefully
Optional<User> cached = cache.get(key, User.class);
if (cached.isEmpty()) {
    // Load from database
}

// ✅ Invalidate cache on updates
userRepository.update(user);
cache.delete("user:" + user.id());  // ✅ Sincronizar!

// ✅ Use prefixes para namespacing
String key = "tenant:" + tenantId + ":user:" + userId;

// ✅ Monitore hit rate
metrics.recordGauge("cache.hit_rate", hitRate);
```

### ❌ DON'T

```java
// ❌ NÃO cache sem TTL
cache.set(key, value, Duration.ofDays(365));  // ❌ Infinito efetivamente!

// ❌ NÃO assuma que cache sempre tem valor
User user = cache.get(key, User.class).get();  // ❌ NoSuchElementException!

// ❌ NÃO esqueça de invalidar cache
userRepository.update(user);
// ❌ Cache desatualizado!

// ❌ NÃO cache objetos muito grandes
cache.set(key, hugeObject);  // ❌ Consome muita memória!

// ❌ NÃO use cache para dados críticos sem fallback
return cache.get(key).get();  // ❌ E se cache cair?
```

---

## Ver Também

- [Redis Adapter](../../../commons-adapters-cache-redis/) - Redis implementation
- [Multi-Tenancy](../app-multi-tenancy.md) - Tenant-aware caching
- [Rate Limiting](../app-api-rate-limiting.md) - Rate limiting with cache
- [Scheduler](../app-scheduler.md) - Distributed locks
