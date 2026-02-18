# JPA Adapter Guide

## Overview

This guide covers the **JPA adapter** (`commons-adapters-persistence-jpa`) for relational databases with Spring Data JPA and Hibernate.

**Key Features:**
- Repository pattern
- Entity mapping
- Transaction management
- Query methods
- Specifications (dynamic queries)
- Auditing
- Performance optimization

---

## 📦 Installation

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-persistence-jpa</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Spring Boot starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Database driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## ⚙️ Configuration

### Application Properties

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate  # validate | update | create | create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 25
          fetch_size: 50
        order_inserts: true
        order_updates: true
        query:
          in_clause_parameter_padding: true
    show-sql: false  # Use logging instead
    open-in-view: false  # Disable OSIV anti-pattern

# Logging
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## 🗃️ Entity Mapping

### Domain Entity

```java
// Domain layer (pure business logic)
public class Order extends AggregateRoot<OrderId> {
    
    private CustomerId customerId;
    private List<OrderItem> items;
    private Money total;
    private OrderStatus status;
    private LocalDateTime createdAt;
    
    // Business methods
    public Result<Void> ship() {
        if (status != OrderStatus.CONFIRMED) {
            return Result.error(Error.of(
                "INVALID_STATUS",
                "Order must be confirmed to ship"
            ));
        }
        
        this.status = OrderStatus.SHIPPED;
        registerEvent(new OrderShippedEvent(id(), customerId));
        
        return Result.ok();
    }
}
```

### JPA Entity

```java
// Infrastructure layer (JPA mapping)
@Entity
@Table(name = "orders")
public class OrderEntity {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;
    
    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<OrderItemEntity> items = new ArrayList<>();
    
    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Version
    @Column(name = "version")
    private Long version;  // Optimistic locking
    
    // Hibernate requires no-arg constructor
    protected OrderEntity() {}
    
    // Factory method from domain
    public static OrderEntity from(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.id = order.id().value();
        entity.customerId = order.customerId().value();
        entity.total = BigDecimal.valueOf(order.total().amount());
        entity.status = order.status();
        entity.createdAt = order.createdAt();
        
        entity.items = order.items().stream()
            .map(item -> OrderItemEntity.from(item, entity))
            .toList();
        
        return entity;
    }
    
    // Conversion to domain
    public Order toDomain() {
        return Order.reconstruct(
            OrderId.from(id),
            CustomerId.from(customerId),
            items.stream().map(OrderItemEntity::toDomain).toList(),
            new Money(total.doubleValue(), "USD"),
            status,
            createdAt
        );
    }
}

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;
    
    @Column(name = "product_id", nullable = false)
    private String productId;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    protected OrderItemEntity() {}
    
    public static OrderItemEntity from(OrderItem item, OrderEntity order) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.order = order;
        entity.productId = item.productId().value();
        entity.productName = item.productName();
        entity.quantity = item.quantity();
        entity.price = BigDecimal.valueOf(item.price().amount());
        return entity;
    }
    
    public OrderItem toDomain() {
        return OrderItem.create(
            ProductId.from(productId),
            productName,
            quantity,
            new Money(price.doubleValue(), "USD")
        );
    }
}
```

---

## 📊 Repository Implementation

### Spring Data JPA Repository

```java
// Infrastructure layer
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {
    
    // Query methods (Spring Data auto-implementation)
    List<OrderEntity> findByCustomerId(String customerId);
    
    List<OrderEntity> findByStatus(OrderStatus status);
    
    List<OrderEntity> findByCreatedAtBetween(
        LocalDateTime start,
        LocalDateTime end
    );
    
    @Query("SELECT o FROM OrderEntity o WHERE o.total >= :minTotal")
    List<OrderEntity> findHighValueOrders(@Param("minTotal") BigDecimal minTotal);
}

// Domain repository adapter
@Component
public class JpaOrderRepository implements OrderRepository {
    
    private final OrderJpaRepository jpaRepository;
    
    @Override
    public Result<Order> save(Order order) {
        try {
            OrderEntity entity = OrderEntity.from(order);
            OrderEntity saved = jpaRepository.save(entity);
            return Result.ok(saved.toDomain());
            
        } catch (Exception e) {
            log.error("Failed to save order")
                .exception(e)
                .field("orderId", order.id().value())
                .log();
            
            return Result.error(Error.of("SAVE_ERROR", e.getMessage()));
        }
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.value())
            .map(OrderEntity::toDomain);
    }
    
    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return jpaRepository.findByCustomerId(customerId.value()).stream()
            .map(OrderEntity::toDomain)
            .toList();
    }
    
    @Override
    public Result<Void> delete(OrderId orderId) {
        try {
            jpaRepository.deleteById(orderId.value());
            return Result.ok();
        } catch (Exception e) {
            return Result.error(Error.of("DELETE_ERROR", e.getMessage()));
        }
    }
}
```

---

## 🔍 Dynamic Queries with Specifications

### Specification

```java
public class OrderSpecifications {
    
    public static Specification<OrderEntity> hasStatus(OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
    
    public static Specification<OrderEntity> hasCustomerId(String customerId) {
        return (root, query, cb) -> cb.equal(root.get("customerId"), customerId);
    }
    
    public static Specification<OrderEntity> createdBetween(
        LocalDateTime start,
        LocalDateTime end
    ) {
        return (root, query, cb) -> cb.between(root.get("createdAt"), start, end);
    }
    
    public static Specification<OrderEntity> totalGreaterThan(BigDecimal minTotal) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("total"), minTotal);
    }
    
    public static Specification<OrderEntity> hasItems() {
        return (root, query, cb) -> cb.isNotEmpty(root.get("items"));
    }
}
```

### Repository with Specifications

```java
@Repository
public interface OrderJpaRepository extends 
    JpaRepository<OrderEntity, String>,
    JpaSpecificationExecutor<OrderEntity> {
}

@Component
public class OrderSearchService {
    
    private final OrderJpaRepository repository;
    
    public List<Order> searchOrders(OrderSearchCriteria criteria) {
        Specification<OrderEntity> spec = Specification.where(null);
        
        if (criteria.status() != null) {
            spec = spec.and(OrderSpecifications.hasStatus(criteria.status()));
        }
        
        if (criteria.customerId() != null) {
            spec = spec.and(OrderSpecifications.hasCustomerId(criteria.customerId()));
        }
        
        if (criteria.minTotal() != null) {
            spec = spec.and(OrderSpecifications.totalGreaterThan(criteria.minTotal()));
        }
        
        if (criteria.startDate() != null && criteria.endDate() != null) {
            spec = spec.and(OrderSpecifications.createdBetween(
                criteria.startDate(),
                criteria.endDate()
            ));
        }
        
        return repository.findAll(spec).stream()
            .map(OrderEntity::toDomain)
            .toList();
    }
}
```

---

## 💼 Transaction Management

### Declarative Transactions

```java
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    
    @Transactional
    public Result<Order> createOrder(CreateOrderRequest request) {
        // Create order
        Result<Order> orderResult = Order.create(request);
        
        if (orderResult.isError()) {
            return orderResult;
        }
        
        Order order = orderResult.get();
        
        // Save order (within transaction)
        Result<Order> savedResult = orderRepository.save(order);
        
        if (savedResult.isError()) {
            // Transaction will rollback automatically
            return savedResult;
        }
        
        // Publish events (after commit)
        publishDomainEvents(order);
        
        return savedResult;
    }
    
    @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId orderId) {
        return orderRepository.findById(orderId);
    }
}
```

### Transactional Event Publishing

```java
@Service
public class TransactionalEventPublisher {
    
    private final EventPublisher eventPublisher;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(DomainEvent event) {
        eventPublisher.publish(event);
        
        log.info("Event published after commit")
            .field("eventType", event.getClass().getSimpleName())
            .log();
    }
}
```

---

## 🚀 Performance Optimization

### N+1 Problem Prevention

```java
// ❌ BAD: N+1 queries
@Service
public class OrderService {
    
    public List<OrderDto> getOrders() {
        List<Order> orders = orderRepository.findAll();
        
        // For each order, fetch items (N+1 queries!)
        return orders.stream()
            .map(order -> new OrderDto(
                order.id(),
                order.items()  // ❌ Lazy load triggers query
            ))
            .toList();
    }
}

// ✅ GOOD: Fetch join
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {
    
    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.items")
    List<OrderEntity> findAllWithItems();
    
    @Query("""
        SELECT o FROM OrderEntity o 
        LEFT JOIN FETCH o.items 
        WHERE o.customerId = :customerId
    """)
    List<OrderEntity> findByCustomerIdWithItems(@Param("customerId") String customerId);
}
```

### Batch Operations

```java
@Service
public class BatchOrderService {
    
    private final OrderJpaRepository repository;
    
    @Transactional
    public void saveAll(List<Order> orders) {
        // Hibernate will batch these (if configured)
        List<OrderEntity> entities = orders.stream()
            .map(OrderEntity::from)
            .toList();
        
        repository.saveAll(entities);
        
        // Flush and clear to prevent OutOfMemoryError
        entityManager.flush();
        entityManager.clear();
    }
}

// Configuration for batching
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 25  // Batch inserts/updates
        order_inserts: true
        order_updates: true
```

### Pagination

```java
@Service
public class OrderQueryService {
    
    private final OrderJpaRepository repository;
    
    public Page<Order> findOrders(int page, int size) {
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by("createdAt").descending()
        );
        
        Page<OrderEntity> entityPage = repository.findAll(pageable);
        
        return entityPage.map(OrderEntity::toDomain);
    }
}
```

---

## 📝 Auditing

### Auditable Entity

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    // Getters
}

@Entity
@Table(name = "orders")
public class OrderEntity extends AuditableEntity {
    // ... order fields
}
```

### Audit Configuration

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditConfig {
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // Get current user from security context
            return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
        };
    }
}
```

---

## 🧪 Testing

### Repository Test

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderJpaRepositoryTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private OrderJpaRepository repository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void shouldSaveOrder() {
        // Given
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID().toString());
        order.setCustomerId("customer-123");
        order.setTotal(BigDecimal.valueOf(100.00));
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());
        
        // When
        OrderEntity saved = repository.save(order);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        Optional<OrderEntity> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTotal()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use fetch joins para evitar N+1
@Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items")

// ✅ Use @Transactional(readOnly = true) para queries
@Transactional(readOnly = true)
public List<Order> findAll() {}

// ✅ Clear EntityManager em batch operations
entityManager.flush();
entityManager.clear();

// ✅ Use Pageable para large result sets
Page<Order> findAll(Pageable pageable);

// ✅ Version entities para optimistic locking
@Version
private Long version;
```

### ❌ DON'T

```java
// ❌ NÃO use EAGER fetch
@OneToMany(fetch = FetchType.EAGER)  // ❌ N+1!

// ❌ NÃO use Open Session in View
spring.jpa.open-in-view: true  // ❌ Anti-pattern!

// ❌ NÃO ignore batch size
// Configure batch_size for bulk operations

// ❌ NÃO retorne entities em APIs
return orderEntity;  // ❌ Use DTOs!

// ❌ NÃO faça changes fora de @Transactional
order.setStatus(SHIPPED);  // ❌ Won't persist!
```

---

## Ver Também

- [Persistence Port](../api-reference/ports/persistence.md) - Port interface
- [DDD](../api-reference/kernel-ddd.md) - Domain entities
- [Transactions](./spring-boot.md#transactions) - Transaction management
