# commons-ports-cache

Interface hexagonal de cache. Sem dependências de Redis, Memcached ou qualquer infra.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-ports-cache</artifactId>
</dependency>
```

## CachePort\<String, V\>

```java
public interface CachePort<String, V> {
    Optional<V> get(String key);
    void put(String key, V value);
    void put(String key, V value, Duration ttl);
    void remove(String key);
    void clear();
    boolean contains(String key);
    Set<String> keys();
    long size();
}
```

## Implementar um port customizado

```java
// Em domínio/application — sem conhecer Redis
public class UserCacheService {

    private final CachePort<String, UserDto> cache;

    public Optional<UserDto> getUser(String userId) {
        return cache.get("user:" + userId);
    }

    public void cacheUser(UserDto user, Duration ttl) {
        cache.put("user:" + user.id(), user, ttl);
    }

    public void evict(String userId) {
        cache.remove("user:" + userId);
    }
}

// Wiring com Redis (adaptador)
@Bean
public CachePort<String, UserDto> userCache(RedisTemplate<String, UserDto> template) {
    return new RedisCacheAdapter<>(template, "users:");
}

// Wiring in-memory (testes)
@Bean
@Profile("test")
public CachePort<String, UserDto> userCache() {
    return new InMemoryCacheAdapter<>();
}
```

## Adapters disponíveis

| Adapter | Módulo |
|---------|--------|
| `RedisCacheAdapter<V>` | `commons-adapters-cache-redis` |
| `MemcachedCacheAdapter<V>` | `commons-adapters-cache-memcached` |
| Auto-config Redis | `commons-spring-starter-cache` |
