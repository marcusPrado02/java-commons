# Port: Persistence

## Visão Geral

Define contratos para persistência de aggregates e entities.

---

## Repository<T, ID>

Interface base para repositories.

```java
public interface Repository<T extends Entity<ID>, ID extends Identifier> {
    
    /**
     * Salva ou atualiza entity
     */
    Result<Void> save(T entity);
    
    /**
     * Busca por ID
     */
    Result<T> findById(ID id);
    
    /**
     * Deleta entity
     */
    Result<Void> delete(ID id);
    
    /**
     * Verifica se existe
     */
    boolean exists(ID id);
}
```

### Exemplo

```java
public interface OrderRepository extends Repository<Order, OrderId> {
    
    // Queries customizadas
    Optional<Order> findByNumber(OrderNumber number);
    
    List<Order> findByCustomerId(CustomerId customerId);
    
    List<Order> findByStatus(OrderStatus status, Pageable pageable);
}

// Implementação (JPA)
@Repository
public class JpaOrderRepository implements OrderRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public Result<Void> save(Order order) {
        try {
            entityManager.merge(order);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(Problem.of("ORDER.SAVE_FAILED", e.getMessage()));
        }
    }
    
    @Override
    public Result<Order> findById(OrderId id) {
        Order order = entityManager.find(OrderEntity.class, id.value());
        
        if (order == null) {
            return Result.fail(Problem.of("ORDER.NOT_FOUND", "Order not found"));
        }
        
        return Result.ok(order);
    }
}
```

---

## SpecificationRepository<T>

Repository com suporte a specifications (queries dinâmicas).

```java
public interface SpecificationRepository<T> extends Repository<T, ?> {
    
    List<T> findAll(Specification<T> spec);
    
    List<T> findAll(Specification<T> spec, Pageable pageable);
    
    long count(Specification<T> spec);
}
```

### Specification

```java
public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}

// Exemplo
public class OrderSpecifications {
    
    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> 
            cb.equal(root.get("status"), status);
    }
    
    public static Specification<Order> createdAfter(Instant date) {
        return (root, query, cb) -> 
            cb.greaterThan(root.get("createdAt"), date);
    }
    
    public static Specification<Order> forCustomer(CustomerId customerId) {
        return (root, query, cb) -> 
            cb.equal(root.get("customerId"), customerId.value());
    }
}

// Uso
List<Order> orders = orderRepository.findAll(
    hasStatus(OrderStatus.PENDING)
        .and(createdAfter(Instant.now().minus(7, ChronoUnit.DAYS)))
        .and(forCustomer(customerId)),
    PageRequest.of(0, 20)
);
```

---

## EventStore

Store especializado para Event Sourcing.

```java
public interface EventStore {
    
    /**
     * Salva eventos de um aggregate
     */
    Result<Void> saveEvents(
        String aggregateId, 
        List<DomainEvent> events, 
        long expectedVersion
    );
    
    /**
     * Carrega todos eventos de um aggregate
     */
    List<DomainEvent> loadEvents(String aggregateId);
    
    /**
     * Carrega eventos desde versão
     */
    List<DomainEvent> loadEvents(String aggregateId, long fromVersion);
}
```

### Exemplo

```java
@Service
public class AccountEventStoreRepository implements EventStore {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public Result<Void> saveEvents(
        String aggregateId,
        List<DomainEvent> events,
        long expectedVersion
    ) {
        try {
            long currentVersion = getCurrentVersion(aggregateId);
            
            if (currentVersion != expectedVersion) {
                return Result.fail(Problem.of(
                    "CONCURRENCY_CONFLICT",
                    "Expected version " + expectedVersion + " but was " + currentVersion
                ));
            }
            
            events.forEach(event -> {
                jdbcTemplate.update(
                    "INSERT INTO event_store (...) VALUES (...)",
                    aggregateId,
                    event.eventType(),
                    serialize(event),
                    ++currentVersion
                );
            });
            
            return Result.ok();
            
        } catch (Exception e) {
            return Result.fail(Problem.of("EVENT_STORE_ERROR", e.getMessage()));
        }
    }
}
```

---

## Adapters Disponíveis

| Adapter | Database | Características |
|---------|----------|-----------------|
| **JPA** | PostgreSQL, MySQL, Oracle | Relacional, ORM |
| **MongoDB** | MongoDB | NoSQL, document-oriented |
| **InMemory** | In-memory | Testing, development |

---

## Best Practices

### ✅ DO

```java
// ✅ Retorne Result para operações que podem falhar
Result<Order> findById(OrderId id);

// ✅ Use Optional para buscas que podem não encontrar
Optional<Order> findByNumber(OrderNumber number);

// ✅ Use types do domínio
Repository<Order, OrderId>  // ✅

// ✅ Queries customizadas com nome significativo
List<Order> findPendingOrdersOlderThan(Duration duration);
```

### ❌ DON'T

```java
// ❌ NÃO exponha detalhes de implementação
CriteriaQuery<Order> createQuery();  // ❌ JPA-specific

// ❌ NÃO use tipos primitivos para IDs
Repository<Order, String>  // ❌

// ❌ NÃO lance exceptions checked
Order findById(OrderId id) throws NotFoundException;  // ❌
```

---

## Ver Também

- [JPA Adapter](../adapters/persistence-jpa.md)
- [MongoDB Adapter](../adapters/persistence-mongodb.md)
- [DDD Building Blocks](../kernel-ddd.md)
