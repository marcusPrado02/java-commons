# commons-adapters-persistence-inmemory

Implementação in-memory de `Repository<E, ID>` para testes e prototipagem. Thread-safe, suporta TTL por entidade e paginação.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-persistence-inmemory</artifactId>
  <scope>test</scope>
</dependency>
```

## Uso em testes

```java
// Repositório in-memory simples
InMemoryRepository<Order, OrderId> repo = new InMemoryRepository<>();

// Save + FindById
Order saved = repo.save(order);
Optional<Order> found = repo.findById(orderId);

// Delete
repo.deleteById(orderId);
repo.delete(order);

// Limpar entre testes
repo.clear();
```

## Com TTL

```java
InMemoryRepository<Session, SessionId> sessionRepo =
    new InMemoryRepository<>(Duration.ofMinutes(30));

// Entradas expiram automaticamente após 30 minutos
sessionRepo.save(session);
```

## Paginação

```java
InMemoryPageableRepository<Order, OrderId> pageRepo =
    new InMemoryPageableRepository<>(Order::id);

// Filtrar e paginar
Specification<Order> spec = order -> order.customerId().equals(customerId);
PageRequest req = new PageRequest(0, 10);
PageResult<Order> page = pageRepo.findAll(req, spec);
```

## Substituir em testes de integração

```java
// Produção
@Bean
@Profile("!test")
public OrderRepository orderRepository(EntityManager em) {
    return new JpaOrderRepository().withEntityManager(em);
}

// Testes
@Bean
@Profile("test")
public OrderRepository orderRepository() {
    return new InMemoryOrderRepository();
}

// InMemoryOrderRepository implementa o port
class InMemoryOrderRepository
        extends InMemoryRepository<Order, OrderId>
        implements OrderRepository {
    // sem código adicional para casos simples
}
```
