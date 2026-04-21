# commons-ports-persistence

Interfaces hexagonais de persistência. Sem dependências de JPA, Spring ou qualquer infra.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-ports-persistence</artifactId>
</dependency>
```

## Repository\<E, ID\>

```java
public interface Repository<E, ID> {
    E save(E entity);
    Optional<E> findById(ID id);
    void delete(E entity);
    void deleteById(ID id);
    boolean existsById(ID id);
}
```

### Implementar em domínio

```java
// Port — no domínio (sem dependência de infra)
public interface OrderRepository extends Repository<Order, OrderId> {
    Optional<Order> findByCustomerAndStatus(CustomerId id, Order.Status status);
}

// Adapter — na camada de infra
@Repository
public class JpaOrderRepository
        extends BaseGenericRepository<OrderEntity, String>
        implements OrderRepository {
    // implementação JPA
}
```

## PageableRepository\<E, ID\>

```java
public interface PageableRepository<E, ID> extends Repository<E, ID> {
    PageResult<E> findAll(PageRequest req, Specification<E> spec);
    PageResult<E> search(PageRequest req, Specification<E> spec, Sort sort);
}
```

## PageRequest e PageResult

```java
// Requisição
PageRequest req = new PageRequest(
    0,    // page (zero-indexed)
    20    // size (max 10_000)
);

// Resultado
PageResult<Order> page = repository.findAll(req, spec);
List<Order>  items      = page.content();
long         total      = page.totalElements();
int          totalPages = page.totalPages();
boolean      hasNext    = page.hasNext();
```

## Specification

```java
// Implementar filtro de domínio
public class PendingOrderSpec implements Specification<Order> {
    @Override
    public boolean isSatisfiedBy(Order o) {
        return o.status() == Order.Status.PENDING;
    }
}

// Compor
Specification<Order> spec = new PendingOrderSpec()
    .and(new CustomerOrderSpec(customerId))
    .and(new RecentOrderSpec(Duration.ofDays(30)));
```

## Sort

```java
Sort sort = new Sort("createdAt", Sort.Direction.DESC);
PageResult<Order> page = repo.search(req, spec, sort);
```
