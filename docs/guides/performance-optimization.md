# Performance Optimization Guide

## Overview

This guide covers **performance optimization** techniques for building high-performance microservices.

**Key Topics:**
- Profiling & monitoring
- Async processing patterns
- Caching strategies
- Database optimization
- JVM tuning
- Load testing
- Scalability patterns

---

## 📊 Profiling & Monitoring

### Application Performance Monitoring (APM)

```java
@Configuration
public class ApmConfig {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

@Service
public class OrderService {
    
    @Timed(value = "order.creation", description = "Time to create order")
    public Result<Order> createOrder(CreateOrderRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Process order
            Result<Order> result = processOrder(request);
            
            sample.stop(Timer.builder("order.creation.duration")
                .tag("status", result.isSuccess() ? "success" : "error")
                .register(meterRegistry));
            
            return result;
            
        } catch (Exception e) {
            sample.stop(Timer.builder("order.creation.duration")
                .tag("status", "exception")
                .register(meterRegistry));
            throw e;
        }
    }
}
```

### Custom Metrics

```java
@Component
public class PerformanceMetrics {
    
    private final MeterRegistry registry;
    private final Counter requestCounter;
    private final Timer responseTimer;
    private final DistributionSummary responseSizeDistribution;
    
    public PerformanceMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        this.requestCounter = Counter.builder("http.requests.total")
            .tag("application", "my-app")
            .register(registry);
        
        this.responseTimer = Timer.builder("http.response.time")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        
        this.responseSizeDistribution = DistributionSummary.builder("http.response.size")
            .baseUnit("bytes")
            .register(registry);
    }
    
    public void recordRequest(String method, String uri, int status, long durationMs, long responseSize) {
        requestCounter.increment();
        
        responseTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        responseSizeDistribution.record(responseSize);
        
        registry.counter("http.requests",
            "method", method,
            "uri", uri,
            "status", String.valueOf(status)
        ).increment();
    }
}
```

### Slow Query Detection

```java
@Component
@Aspect
public class SlowQueryDetector {
    
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000;
    
    @Around("execution(* org.springframework.data.repository.Repository+.*(..))")
    public Object detectSlowQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            if (duration > SLOW_QUERY_THRESHOLD_MS) {
                String methodName = joinPoint.getSignature().toShortString();
                Object[] args = joinPoint.getArgs();
                
                log.warn("Slow query detected")
                    .field("method", methodName)
                    .field("duration", duration + "ms")
                    .field("args", Arrays.toString(args))
                    .log();
                
                // Publish metric
                meterRegistry.counter("slow.queries.total",
                    "method", methodName
                ).increment();
            }
        }
    }
}
```

---

## ⚡ Async Processing

### CompletableFuture Patterns

```java
@Service
public class AsyncOrderService {
    
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final Executor asyncExecutor;
    
    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
    
    public CompletableFuture<Result<Order>> createOrderAsync(CreateOrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            
            // 1. Validate inventory (async)
            CompletableFuture<Boolean> inventoryCheck = 
                CompletableFuture.supplyAsync(
                    () -> inventoryService.checkAvailability(request.items()),
                    asyncExecutor
                );
            
            // 2. Validate payment method (async)
            CompletableFuture<Boolean> paymentCheck = 
                CompletableFuture.supplyAsync(
                    () -> paymentService.validatePaymentMethod(request.paymentMethod()),
                    asyncExecutor
                );
            
            // 3. Wait for both checks
            try {
                Boolean hasInventory = inventoryCheck.get(5, TimeUnit.SECONDS);
                Boolean paymentValid = paymentCheck.get(5, TimeUnit.SECONDS);
                
                if (!hasInventory || !paymentValid) {
                    return Result.error(Error.of("VALIDATION_FAILED", "Order validation failed"));
                }
                
            } catch (TimeoutException e) {
                return Result.error(Error.of("TIMEOUT", "Validation timeout"));
            } catch (Exception e) {
                return Result.error(Error.of("ERROR", e.getMessage()));
            }
            
            // 4. Create order
            Result<Order> orderResult = Order.create(request);
            
            if (orderResult.isError()) {
                return orderResult;
            }
            
            Order order = orderResult.get();
            
            // 5. Save order
            orderRepository.save(order);
            
            // 6. Send confirmation email (fire-and-forget)
            CompletableFuture.runAsync(
                () -> emailService.sendOrderConfirmation(order),
                asyncExecutor
            ).exceptionally(ex -> {
                log.error("Failed to send confirmation email")
                    .exception(ex)
                    .field("orderId", order.id().value())
                    .log();
                return null;
            });
            
            return Result.ok(order);
            
        }, asyncExecutor);
    }
}
```

### @Async Methods

```java
@Service
public class NotificationService {
    
    @Async("asyncExecutor")
    public CompletableFuture<Void> sendNotification(UserId userId, String message) {
        try {
            // Simulate slow operation
            Thread.sleep(2000);
            
            log.info("Notification sent")
                .field("userId", userId.value())
                .log();
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Notification failed")
                .exception(e)
                .log();
            
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Async
    public void sendBulkNotifications(List<UserId> userIds, String message) {
        // Process in batches
        int batchSize = 100;
        
        for (int i = 0; i < userIds.size(); i += batchSize) {
            List<UserId> batch = userIds.subList(
                i,
                Math.min(i + batchSize, userIds.size())
            );
            
            List<CompletableFuture<Void>> futures = batch.stream()
                .map(userId -> sendNotification(userId, message))
                .toList();
            
            // Wait for batch to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();
        }
    }
}
```

### Virtual Threads (Java 21+)

```java
@Configuration
public class VirtualThreadConfig {
    
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

@Service
public class VirtualThreadService {
    
    @Async("virtualThreadExecutor")
    public CompletableFuture<Result<Data>> fetchData(String url) {
        try {
            // Virtual threads are lightweight - can create millions
            HttpResponse response = httpClient.get(url).execute();
            
            if (response.status() == 200) {
                Data data = parseResponse(response.body());
                return CompletableFuture.completedFuture(Result.ok(data));
            } else {
                return CompletableFuture.completedFuture(
                    Result.error(Error.of("HTTP_ERROR", "Status: " + response.status()))
                );
            }
            
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public List<Result<Data>> fetchMultipleUrls(List<String> urls) {
        // Create virtual thread for each request
        List<CompletableFuture<Result<Data>>> futures = urls.stream()
            .map(this::fetchData)
            .toList();
        
        // Wait for all to complete
        return futures.stream()
            .map(CompletableFuture::join)
            .toList();
    }
}
```

---

## 💾 Caching Strategies

### Multi-Level Cache

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // L1: Caffeine (local, fast)
        CaffeineCacheManager caffeineManager = new CaffeineCacheManager();
        caffeineManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
        );
        
        // L2: Redis (distributed, slower)
        RedisCacheConfiguration redisConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
        
        RedisCacheManager redisManager = RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(redisConfig)
            .build();
        
        // Composite cache manager (L1 + L2)
        return new CompositeCacheManager(caffeineManager, redisManager);
    }
}

@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#productId.value()")
    public Optional<Product> findById(ProductId productId) {
        // Cache miss: load from database
        return productRepository.findById(productId);
    }
    
    @CacheEvict(value = "products", key = "#product.id().value()")
    public Result<Product> update(Product product) {
        return productRepository.save(product);
    }
    
    @CacheEvict(value = "products", allEntries = true)
    @Scheduled(cron = "0 0 3 * * ?")  // 3 AM daily
    public void evictAllCache() {
        log.info("Evicting all product cache");
    }
}
```

### Cache-Aside Pattern

```java
@Service
public class UserCacheService {
    
    private final UserRepository userRepository;
    private final CacheProvider cache;
    private static final Duration TTL = Duration.ofHours(1);
    
    public Optional<User> getUser(UserId userId) {
        String cacheKey = "user:" + userId.value();
        
        // 1. Try cache
        Optional<String> cachedJson = cache.get(cacheKey);
        
        if (cachedJson.isPresent()) {
            // Cache hit
            User user = deserialize(cachedJson.get());
            
            log.debug("User cache hit")
                .field("userId", userId.value())
                .log();
            
            return Optional.of(user);
        }
        
        // 2. Cache miss - load from database
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // 3. Store in cache
            String json = serialize(user);
            cache.set(cacheKey, json, TTL);
            
            log.debug("User cached")
                .field("userId", userId.value())
                .log();
        }
        
        return userOpt;
    }
}
```

### Query Result Caching

```java
@Service
public class ReportService {
    
    private final ReportRepository reportRepository;
    private final CacheProvider cache;
    
    public List<SalesReport> getDailySalesReport(LocalDate date) {
        String cacheKey = "sales:report:" + date;
        
        // Check cache
        Optional<String> cached = cache.get(cacheKey);
        
        if (cached.isPresent()) {
            return deserializeList(cached.get());
        }
        
        // Execute expensive query
        List<SalesReport> report = reportRepository.generateDailySalesReport(date);
        
        // Cache for 24 hours
        cache.set(cacheKey, serialize(report), Duration.ofHours(24));
        
        return report;
    }
}
```

---

## 🗄️ Database Optimization

### Connection Pooling (HikariCP)

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      
      # Performance
      auto-commit: false
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
```

### N+1 Query Prevention

```java
// ❌ BAD: N+1 queries
@Service
public class OrderService {
    
    public List<OrderDto> getAllOrders() {
        List<Order> orders = orderRepository.findAll();  // 1 query
        
        return orders.stream()
            .map(order -> new OrderDto(
                order.id(),
                order.items()  // N queries (lazy loading)
            ))
            .toList();
    }
}

// ✅ GOOD: Fetch join
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    
    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.items")
    List<OrderEntity> findAllWithItems();
}

@Service
public class OrderService {
    
    public List<OrderDto> getAllOrders() {
        List<OrderEntity> entities = orderRepository.findAllWithItems();  // 1 query
        
        return entities.stream()
            .map(entity -> OrderDto.from(entity.toDomain()))
            .toList();
    }
}
```

### Batch Operations

```java
@Service
public class BatchProductService {
    
    private final EntityManager entityManager;
    private static final int BATCH_SIZE = 100;
    
    @Transactional
    public void importProducts(List<Product> products) {
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            
            ProductEntity entity = ProductEntity.from(product);
            entityManager.persist(entity);
            
            // Flush batch
            if (i > 0 && i % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        
        // Flush remaining
        entityManager.flush();
        entityManager.clear();
    }
}
```

### Query Optimization

```java
@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {
    
    // ❌ BAD: Fetches all columns
    List<ProductEntity> findByCategory(String category);
    
    // ✅ GOOD: Project only needed fields
    @Query("SELECT new com.example.dto.ProductSummary(p.id, p.name, p.price) " +
           "FROM ProductEntity p WHERE p.category = :category")
    List<ProductSummary> findSummariesByCategory(@Param("category") String category);
    
    // ✅ GOOD: Use native query for complex queries
    @Query(value = """
        SELECT p.id, p.name, COUNT(r.id) as review_count, AVG(r.rating) as avg_rating
        FROM products p
        LEFT JOIN reviews r ON r.product_id = p.id
        WHERE p.category = :category
        GROUP BY p.id, p.name
        HAVING COUNT(r.id) > 10
        ORDER BY avg_rating DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopRatedProducts(
        @Param("category") String category,
        @Param("limit") int limit
    );
}
```

### Read Replicas

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource primary = new HikariDataSource();
        primary.setJdbcUrl("jdbc:postgresql://primary:5432/mydb");
        primary.setReadOnly(false);
        
        HikariDataSource replica = new HikariDataSource();
        replica.setJdbcUrl("jdbc:postgresql://replica:5432/mydb");
        replica.setReadOnly(true);
        
        Map<Object, Object> targets = new HashMap<>();
        targets.put("PRIMARY", primary);
        targets.put("REPLICA", replica);
        
        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(targets);
        routingDataSource.setDefaultTargetDataSource(primary);
        
        return routingDataSource;
    }
}

// Routing based on transaction read-only
public class RoutingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() 
            ? "REPLICA" 
            : "PRIMARY";
    }
}

@Service
public class ProductService {
    
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        // Uses replica
        return productRepository.findAll();
    }
    
    @Transactional
    public Result<Product> save(Product product) {
        // Uses primary
        return productRepository.save(product);
    }
}
```

---

## ☕ JVM Tuning

### JVM Options

```bash
# Heap size
-Xms2g              # Initial heap
-Xmx2g              # Maximum heap (same as Xms to avoid resizing)

# GC
-XX:+UseG1GC        # G1 garbage collector (default in Java 11+)
-XX:MaxGCPauseMillis=200  # Target max GC pause
-XX:G1HeapRegionSize=8m   # Region size

# GC Logging
-Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=100M

# Thread stack size
-Xss512k            # Stack size per thread

# Metaspace
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m

# Performance
-XX:+AlwaysPreTouch  # Touch all memory at startup
-XX:+UseStringDeduplication  # Deduplicate strings
```

### Dockerized JVM

```dockerfile
FROM eclipse-temurin:21-jre-alpine

# JVM options
ENV JAVA_OPTS="-Xms512m -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0"

COPY target/app.jar /app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
```

---

## 🧪 Load Testing

### Gatling Load Test

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class OrderLoadTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .authorizationHeader("Bearer ${accessToken}")
  
  val createOrder = scenario("Create Order")
    .exec(http("Create Order")
      .post("/api/orders")
      .body(StringBody("""{"customerId":"123","items":[{"productId":"456","quantity":2}]}"""))
      .check(status.is(201))
      .check(jsonPath("$.orderId").saveAs("orderId"))
    )
    .pause(1.second)
    .exec(http("Get Order")
      .get("/api/orders/${orderId}")
      .check(status.is(200))
    )
  
  setUp(
    createOrder.inject(
      rampUsersPerSec(10).to(100).during(2.minutes),  // Ramp up
      constantUsersPerSec(100).during(5.minutes),     // Sustain
      rampUsersPerSec(100).to(10).during(1.minute)    // Ramp down
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile(95).lt(500),  // P95 < 500ms
     global.successfulRequests.percent.gt(99)     // 99% success rate
   )
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use connection pooling
spring.datasource.hikari.maximum-pool-size: 20

// ✅ Add indexes for frequent queries
@Index(name = "idx_email", columnList = "email")

// ✅ Use pagination
PageRequest.of(page, size)

// ✅ Profile in production
@Timed(value = "service.operation")

// ✅ Use async for I/O operations
@Async
public CompletableFuture<Result<Data>> fetchData()
```

### ❌ DON'T

```java
// ❌ NÃO carregue tudo na memória
List<Product> all = repository.findAll();  // ❌ OOM!

// ❌ NÃO bloqueie threads
Thread.sleep(5000);  // ❌ Use async!

// ❌ NÃO ignore lazy loading
@OneToMany(fetch = FetchType.EAGER)  // ❌ N+1!

// ❌ NÃO faça queries em loop
for (Order order : orders) {
    getCustomer(order.customerId());  // ❌ N+1!
}

// ❌ NÃO use reflection excessivamente
// Cache Method/Field objects
```

---

## Ver Também

- [Observability](./observability.md) - Monitoring metrics
- [Caching](../api-reference/ports/cache.md) - Cache patterns
- [Async Processing](../api-reference/app-scheduler.md) - Batch jobs
