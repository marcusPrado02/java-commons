# commons-adapters-persistence-jpa

Implementação JPA de `Repository<E, ID>` e `PageableRepository<E, ID>` usando Jakarta Persistence 3.1 e Hibernate 6.x.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-persistence-jpa</artifactId>
</dependency>
```

## Repositório básico

```java
// 1. Entidade JPA
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id private String id;
    private String customerId;
    private String status;
    // getters/setters...
}

// 2. Adapter estende BaseGenericRepository
@Repository
public class JpaOrderRepository extends BaseGenericRepository<OrderEntity, String>
        implements OrderRepositoryPort {

    public JpaOrderRepository() {
        super(OrderEntity.class, String.class);
    }

    @PersistenceContext
    public void setEntityManager(EntityManager em) {
        this.entityManager = em;
    }
}
```

## Repositório paginado

```java
@Repository
public class JpaOrderPageableRepository
        extends PageableJpaRepository<OrderEntity, String>
        implements OrderPageableRepositoryPort {

    public JpaOrderPageableRepository() {
        super(OrderEntity.class, String.class);
    }

    @PersistenceContext
    public void setEntityManager(EntityManager em) {
        withEntityManager(em);
    }
}

// Uso
PageRequest req = new PageRequest(0, 20);
Specification<OrderEntity> spec = new CustomerOrderSpec(customerId);
PageResult<OrderEntity> page = repo.findAll(req, spec);

page.content();      // List<OrderEntity>
page.totalElements(); // long
page.totalPages();    // int
```

## Specification com JPA Criteria

```java
public class CustomerOrderSpec implements Specification<OrderEntity> {

    private final String customerId;

    @Override
    public boolean isSatisfiedBy(OrderEntity order) {
        return customerId.equals(order.getCustomerId());
    }

    @Override
    public Predicate toPredicate(Root<OrderEntity> root, CriteriaQuery<?> q, CriteriaBuilder cb) {
        return cb.equal(root.get("customerId"), customerId);
    }
}
```

## Idempotência JPA

```java
// Verificar antes de processar
JpaIdempotencyStoreAdapter idempotency = new JpaIdempotencyStoreAdapter(entityManager);

if (idempotency.exists(requestId)) {
    return cachedResponse(requestId);
}
// Processar...
idempotency.store(requestId, response, Duration.ofHours(24));
```

## Transação

```java
Transactional tx = new Transactional(entityManager);

Result<Order> result = tx.execute(() -> {
    Order order = orderRepo.save(newOrder);
    eventRepo.save(new OrderCreatedEvent(order.id()));
    return Result.ok(order);
});
```

## Spring Boot (sem starter)

```java
@Configuration
public class PersistenceConfig {

    @Bean
    public OrderRepositoryPort orderRepository(EntityManager em) {
        return new JpaOrderRepository().withEntityManager(em);
    }
}
```
