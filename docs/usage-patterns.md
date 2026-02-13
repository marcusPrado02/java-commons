# Padrões de Uso: Java Commons Platform

Este documento apresenta padrões recomendados e exemplos práticos de uso dos módulos java-commons.

## Índice

1. [Setup Inicial](#setup-inicial)
2. [Domain-Driven Design](#domain-driven-design)
3. [Hexagonal Architecture](#hexagonal-architecture)
4. [Result Type Pattern](#result-type-pattern)
5. [Persistence](#persistence)
6. [Messaging & Events](#messaging--events)
7. [Observability](#observability)
8. [Resilience](#resilience)
9. [Transactional Outbox](#transactional-outbox)
10. [Idempotency](#idempotency)

---

## Setup Inicial

### 1. Importar BOM

Primeiro, importe o BOM para gestão centralizada de versões:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.marcusprado02.commons</groupId>
      <artifactId>commons-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 2. Adicionar Módulos Necessários

Spring Boot project típico:

```xml
<dependencies>
  <!-- Domain & Core -->
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-ddd</artifactId>
  </dependency>
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-result</artifactId>
  </dependency>
  
  <!-- Persistence -->
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-persistence</artifactId>
  </dependency>
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-persistence-jpa</artifactId>
  </dependency>
  
  <!-- Spring Boot Starters -->
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-spring-starter-observability</artifactId>
  </dependency>
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-spring-starter-outbox</artifactId>
  </dependency>
</dependencies>
```

---

## Domain-Driven Design

### Criar Entity

```java
package com.myapp.domain.user;

import com.marcusprado02.commons.kernel.ddd.entity.Entity;
import com.marcusprado02.commons.kernel.ddd.audit.AuditStamp;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import com.marcusprado02.commons.kernel.ddd.invariant.Invariant;

public class User extends Entity<UserId> {
  private Email email;
  private UserName name;
  private UserStatus status;
  
  // Constructor para criação
  public User(UserId id, TenantId tenantId, Email email, 
              UserName name, AuditStamp created) {
    super(id, tenantId, created);
    
    // Validações (invariantes)
    Invariant.notNull(email, "email");
    Invariant.notNull(name, "name");
    
    this.email = email;
    this.name = name;
    this.status = UserStatus.ACTIVE;
  }
  
  // Comportamento de domínio
  public void deactivate(AuditStamp updated) {
    Invariant.isTrue(status == UserStatus.ACTIVE, 
        "Only active users can be deactivated");
    
    this.status = UserStatus.INACTIVE;
    this.updateAudit(updated);
  }
  
  // Getters (sem setters!)
  public Email email() { return email; }
  public UserName name() { return name; }
  public UserStatus status() { return status; }
}
```

### Criar Value Object

```java
package com.myapp.domain.user;

import com.marcusprado02.commons.kernel.ddd.ValueObject;
import com.marcusprado02.commons.kernel.ddd.invariant.Invariant;

public final class Email extends ValueObject {
  private final String value;
  
  private Email(String value) {
    this.value = value;
  }
  
  public static Email of(String value) {
    // Validações
    Invariant.notBlank(value, "email");
    Invariant.isTrue(value.contains("@"), "email must contain @");
    Invariant.isTrue(value.length() <= 255, "email too long");
    
    return new Email(value.toLowerCase().trim());
  }
  
  public String value() {
    return value;
  }
  
  @Override
  protected Object[] equalityComponents() {
    return new Object[]{value};
  }
  
  @Override
  public String toString() {
    return value;
  }
}
```

### Criar Aggregate Root com Eventos

```java
package com.myapp.domain.order;

import com.marcusprado02.commons.kernel.ddd.entity.AggregateRoot;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import java.util.ArrayList;
import java.util.List;

public class Order extends AggregateRoot<OrderId> {
  private CustomerId customerId;
  private List<OrderLine> lines;
  private OrderStatus status;
  private Money totalAmount;
  
  public Order(OrderId id, TenantId tenantId, CustomerId customerId, 
               AuditStamp created) {
    super(id, tenantId, created, DomainEventRecorder.newBuffer());
    this.customerId = customerId;
    this.lines = new ArrayList<>();
    this.status = OrderStatus.DRAFT;
    this.totalAmount = Money.zero();
  }
  
  public void addLine(Product product, Quantity quantity) {
    Invariant.isTrue(status == OrderStatus.DRAFT, 
        "Cannot add lines to non-draft order");
    
    var line = new OrderLine(product, quantity);
    lines.add(line);
    recalculateTotal();
    
    // Registrar evento
    recordEvent(new OrderLineAdded(
      id(), 
      product.id(), 
      quantity.value(),
      Instant.now()
    ));
  }
  
  public void place(AuditStamp updated) {
    Invariant.isTrue(status == OrderStatus.DRAFT, "Order already placed");
    Invariant.isTrue(!lines.isEmpty(), "Cannot place empty order");
    
    this.status = OrderStatus.PLACED;
    this.updateAudit(updated);
    
    // Evento de domínio importante
    recordEvent(new OrderPlaced(
      id(),
      customerId,
      totalAmount,
      lines.size(),
      Instant.now()
    ));
  }
  
  private void recalculateTotal() {
    this.totalAmount = lines.stream()
      .map(OrderLine::amount)
      .reduce(Money.zero(), Money::add);
  }
}
```

### Domain Events

```java
package com.myapp.domain.order.events;

import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.event.EventId;

public record OrderPlaced(
  EventId eventId,
  OrderId orderId,
  CustomerId customerId,
  Money totalAmount,
  int lineCount,
  Instant occurredAt
) implements DomainEvent {
  
  // Constructor alternativo
  public OrderPlaced(OrderId orderId, CustomerId customerId, 
                     Money totalAmount, int lineCount, Instant occurredAt) {
    this(EventId.generate(), orderId, customerId, totalAmount, 
         lineCount, occurredAt);
  }
}
```

---

## Hexagonal Architecture

### Definir Port (Interface)

```java
package com.myapp.ports.persistence;

import com.marcusprado02.commons.ports.persistence.Repository;
import com.marcusprado02.commons.kernel.result.Option;

public interface UserRepository extends Repository<User, UserId> {
  Option<User> findByEmail(Email email);
  List<User> findByTenant(TenantId tenantId);
}
```

### Implementar Adapter (JPA)

```java
package com.myapp.adapters.persistence.jpa;

import com.marcusprado02.commons.adapters.persistence.jpa.base.BaseGenericRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class JpaUserRepository 
    extends BaseGenericRepository<User, UserId> 
    implements UserRepository {
  
  public JpaUserRepository(EntityManager entityManager) {
    super(User.class, UserId.class, entityManager);
  }
  
  @Override
  public Option<User> findByEmail(Email email) {
    var query = entityManager()
      .createQuery("SELECT u FROM UserEntity u WHERE u.email = :email", 
                   UserEntity.class)
      .setParameter("email", email.value());
    
    return query.getResultList().stream()
      .findFirst()
      .map(UserMapper::toDomain)
      .map(Option::of)
      .orElse(Option.none());
  }
  
  @Override
  public List<User> findByTenant(TenantId tenantId) {
    // Implementação...
  }
}
```

### Application Service (Use Case)

```java
package com.myapp.application.user;

import com.marcusprado02.commons.kernel.result.Result;

public class CreateUserUseCase {
  private final UserRepository userRepository;
  private final ClockProvider clockProvider;
  private final TenantProvider tenantProvider;
  
  public CreateUserUseCase(UserRepository userRepository, 
                           ClockProvider clockProvider,
                           TenantProvider tenantProvider) {
    this.userRepository = userRepository;
    this.clockProvider = clockProvider;
    this.tenantProvider = tenantProvider;
  }
  
  public Result<User, UserError> execute(CreateUserCommand cmd) {
    // 1. Validar se email já existe
    if (userRepository.findByEmail(cmd.email()).isPresent()) {
      return Result.failure(UserError.EMAIL_ALREADY_EXISTS);
    }
    
    // 2. Criar aggregate
    var user = new User(
      UserId.generate(),
      tenantProvider.currentTenant(),
      cmd.email(),
      cmd.name(),
      AuditStamp.now(cmd.actorId(), clockProvider)
    );
    
    // 3. Persistir
    userRepository.save(user);
    
    // 4. Retornar sucesso
    return Result.success(user);
  }
}
```

---

## Result Type Pattern

### Uso Básico

```java
// ✅ Método que pode falhar retorna Result
public Result<Order, OrderError> placeOrder(PlaceOrderCommand cmd) {
  // Validações
  if (lines.isEmpty()) {
    return Result.failure(OrderError.EMPTY_ORDER);
  }
  
  // Lógica de negócio
  var order = new Order(...);
  order.place(...);
  
  return Result.success(order);
}

// Consumidor trata ambos os casos
Result<Order, OrderError> result = service.placeOrder(cmd);

if (result.isSuccess()) {
  Order order = result.get();
  // Processar sucesso
} else {
  OrderError error = result.getError();
  // Processar erro
}
```

### Transformações Funcionais

```java
// map: transforma valor de sucesso
Result<OrderId, OrderError> orderIdResult = orderResult
  .map(Order::id);

// flatMap: encadeia operações que retornam Result
Result<Order, OrderError> result = validateRequest(cmd)
  .flatMap(valid -> createOrder(valid))
  .flatMap(order -> notifyCustomer(order))
  .map(order -> order);

// mapError: transforma erro
Result<Order, String> stringErrorResult = orderResult
  .mapError(OrderError::message);

// getOrElse: valor padrão se falha
Order order = orderResult.getOrElse(() -> Order.empty());
```

### Pattern Matching (Java 21+)

```java
return switch (result) {
  case Success(Order order) -> 
    ResponseEntity.ok(OrderDto.from(order));
  
  case Failure(OrderError.EMPTY_ORDER) -> 
    ResponseEntity.badRequest().body("Order is empty");
  
  case Failure(OrderError.INVALID_AMOUNT) ->
    ResponseEntity.badRequest().body("Invalid amount");
  
  case Failure(OrderError error) -> 
    ResponseEntity.internalServerError().body(error.message());
};
```

### Option para Valores Opcionais

```java
// Buscar entidade que pode não existir
Option<User> userOpt = userRepository.findById(userId);

// Pattern matching
return switch (userOpt) {
  case Some(User user) -> ResponseEntity.ok(user);
  case None() -> ResponseEntity.notFound().build();
};

// Transformações
Option<Email> emailOpt = userOpt.map(User::email);

// Conversão para Optional
Optional<User> optionalUser = userOpt.toOptional();
```

---

## Persistence

### Repository com Paginação

```java
package com.myapp.application.user;

import com.marcusprado02.commons.ports.persistence.PageableRepository;
import com.marcusprado02.commons.ports.persistence.model.*;

public class ListUsersUseCase {
  private final PageableRepository<User, UserId> userRepository;
  
  public PageResult<User> execute(PageRequest pageRequest) {
    return userRepository.findAll(pageRequest);
  }
  
  public PageResult<User> searchActive(SearchCriteria criteria, 
                                        PageRequest pageRequest) {
    var spec = Specification.<User>builder()
      .add(criteria)
      .build();
    
    return userRepository.findAll(spec, pageRequest);
  }
}
```

### Specification Pattern

```java
import com.marcusprado02.commons.ports.persistence.specification.*;

// Criar especificação dinamicamente
Specification<User> spec = Specification.<User>builder()
  .add(SearchCriteria.of("status", Operation.EQ, "ACTIVE"))
  .add(SearchCriteria.of("email", Operation.LIKE, "%@example.com"))
  .build();

// Usar no repositório
PageResult<User> users = userRepository.findAll(spec, pageRequest);
```

### Implementação JPA com Mapper

```java
@Entity
@Table(name = "users")
class UserEntity {
  @Id
  private String id;
  private String email;
  private String name;
  private String status;
  // ... campos de audit
}

class UserMapper {
  static User toDomain(UserEntity entity) {
    var user = new User(
      UserId.of(entity.getId()),
      TenantId.of(entity.getTenantId()),
      Email.of(entity.getEmail()),
      UserName.of(entity.getName()),
      AuditStamp.of(/* ... */)
    );
    
    // Reconstruir estado
    if ("INACTIVE".equals(entity.getStatus())) {
      user.deactivate(/* ... */);
    }
    
    return user;
  }
  
  static UserEntity toEntity(User user) {
    var entity = new UserEntity();
    entity.setId(user.id().value());
    entity.setEmail(user.email().value());
    entity.setName(user.name().value());
    entity.setStatus(user.status().name());
    return entity;
  }
}
```

---

## Messaging & Events

### Publicar Mensagens

```java
import com.marcusprado02.commons.ports.messaging.*;

public class OrderPlacedPublisher {
  private final MessagePublisher publisher;
  
  public void publishOrderPlaced(Order order) {
    var event = new OrderPlacedEvent(order.id(), order.customerId());
    
    var envelope = MessageEnvelope.builder()
      .payload(event)
      .messageId(MessageId.generate())
      .correlationId(CorrelationId.current())
      .topic("orders.placed")
      .headers(Map.of("version", "1.0"))
      .build();
    
    publisher.publish(envelope);
  }
}
```

### Consumir Mensagens

```java
import com.marcusprado02.commons.ports.messaging.*;

public class OrderPlacedConsumer implements MessageConsumer<OrderPlacedEvent> {
  
  @Override
  public void consume(MessageEnvelope<OrderPlacedEvent> envelope) {
    OrderPlacedEvent event = envelope.payload();
    
    // Processar evento
    processOrder(event.orderId());
    
    // Acknowledgeé automático após sucesso
  }
  
  @Override
  public void onError(MessageEnvelope<OrderPlacedEvent> envelope, Exception ex) {
    // Tratar erro (dead letter queue, retry, etc.)
    log.error("Failed to process order", ex);
  }
}
```

---

## Observability

### Correlation ID

Spring Boot starter auto-configura:

```java
import com.marcusprado02.commons.app.observability.context.RequestContext;

public class MyService {
  
  public void doSomething() {
    // Correlation ID automaticamente propagado
    String correlationId = RequestContext.getCorrelationId();
    
    log.info("Processing request"); // Log inclui correlation ID
  }
}
```

### Custom Logging

```java
import com.marcusprado02.commons.app.observability.logging.Logger;
import com.marcusprado02.commons.app.observability.logging.LoggerFactory;

public class OrderService {
  private static final Logger log = LoggerFactory.getLogger(OrderService.class);
  
  public void placeOrder(Order order) {
    log.info("Placing order", Map.of(
      "orderId", order.id().value(),
      "customerId", order.customerId().value(),
      "amount", order.totalAmount().value()
    ));
    
    // Lógica...
    
    log.debug("Order placed successfully");
  }
}
```

---

## Resilience

### Circuit Breaker

```java
import com.marcusprado02.commons.app.resilience.*;

public class ExternalApiClient {
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  
  public Result<Data, ApiError> fetchData(String id) {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("external-api");
    
    return cb.executeSupplier(() -> {
      // Chamada externa
      return callExternalApi(id);
    });
  }
}
```

### Retry Policy

```java
import com.marcusprado02.commons.app.resilience.retry.*;

public class ResilientService {
  private final RetryRegistry retryRegistry;
  
  public void saveWithRetry(Data data) {
    Retry retry = retryRegistry.retry("database-save");
    
    retry.executeRunnable(() -> {
      repository.save(data);
    });
  }
}
```

---

## Transactional Outbox

### Configuração Spring Boot

```yaml
# application.yml
commons:
  outbox:
    enabled: true
    polling-interval: 5s
    batch-size: 100
    max-retries: 3
```

### Publicar Eventos via Outbox

```java
import com.marcusprado02.commons.app.outbox.*;

@Service
@Transactional
public class OrderService {
  private final OrderRepository orderRepository;
  private final OutboxPublisher outboxPublisher;
  
  public void placeOrder(Order order) {
    // 1. Salvar aggregate (mesma transação)
    orderRepository.save(order);
    
    // 2. Pull eventos do aggregate
    List<DomainEvent> events = order.pullDomainEvents();
    
    // 3. Publicar via outbox (salvo na mesma transação)
    for (DomainEvent event : events) {
      outboxPublisher.publish(event);
    }
    
    // 4. Commit da transação: Order + Outbox Events salvos atomicamente
  }
}
```

### Polling Automático

Starter auto-configura polling que:
1. Busca eventos não processados no outbox
2. Publica no message broker
3. Marca como processado
4. Retry automático em caso de falha

---

## Idempotency

### Configuração Spring Boot

```yaml
commons:
  idempotency:
    enabled: true
    ttl: 24h
    storage: redis # ou 'memory' para testes
```

### Controller com Idempotency

```java
import com.marcusprado02.commons.app.idempotency.*;

@RestController
public class OrderController {
  private final IdempotencyService idempotencyService;
  private final PlaceOrderUseCase useCase;
  
  @PostMapping("/orders")
  public ResponseEntity<?> placeOrder(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @RequestBody OrderRequest request) {
    
    return idempotencyService.execute(
      idempotencyKey,
      () -> {
        Result<Order, OrderError> result = useCase.execute(request);
        return result.map(ResponseEntity::ok)
                     .getOrElse(error -> ResponseEntity.badRequest().body(error));
      }
    );
  }
}
```

Se requisição duplicada chegar:
- Retorna resposta anterior (cached)
- Não executa `useCase.execute()` novamente
- Garante idempotência

---

## Best Practices

### 1. Sempre Use Result para Casos de Negócio

```java
// ✅ BOM
public Result<Order, OrderError> placeOrder(OrderRequest req) {
  if (req.isEmpty()) {
    return Result.failure(OrderError.EMPTY);
  }
  return Result.success(new Order(req));
}

// ❌ RUIM
public Order placeOrder(OrderRequest req) throws EmptyOrderException {
  if (req.isEmpty()) {
    throw new EmptyOrderException(); // Exceptions são caras!
  }
  return new Order(req);
}
```

### 2. Mantenha Kernel Framework-Free

```java
// ❌ NUNCA faça isso no kernel!
public class User extends Entity<UserId> {
  @Autowired private UserRepository repository; // ❌ Spring
  @NotNull private String email; // ❌ Bean Validation
}

// ✅ CORRETO
public class User extends Entity<UserId> {
  private final Email email; // ✅ Value Object próprio
  
  public User(..., Email email, ...) {
    Invariant.notNull(email, "email"); // ✅ Validação própria
    this.email = email;
  }
}
```

### 3. Use Value Objects para Conceitos de Domínio

```java
// ❌ Primitives obsession
public class Order {
  private String customerEmail;
  private BigDecimal totalAmount;
}

// ✅ Value Objects
public class Order {
  private Email customerEmail;
  private Money totalAmount;
}
```

### 4. Aggregates Controlam Invariantes

```java
// ✅ Invariante protegida pelo Aggregate
public class Order extends AggregateRoot<OrderId> {
  private List<OrderLine> lines;
  
  public void addLine(OrderLine line) {
    // Aggregate valida invariante
    Invariant.isTrue(lines.size() < 100, "Max 100 lines per order");
    lines.add(line);
  }
  
  // ❌ NUNCA exponha coleção mutável
  // public List<OrderLine> getLines() { return lines; }
  
  // ✅ Exponha cópia imutável
  public List<OrderLine> lines() {
    return List.copyOf(lines);
  }
}
```

### 5. Domain Events para Comunicação

```java
// ✅ Aggregate registra eventos
public void complete() {
  this.status = OrderStatus.COMPLETED;
  recordEvent(new OrderCompleted(id(), Instant.now()));
}

// Use case publica via outbox
public void completeOrder(OrderId orderId) {
  Order order = repository.findById(orderId).orElseThrow();
  order.complete();
  repository.save(order);
  
  // Pull e publicar eventos
  List<DomainEvent> events = order.pullDomainEvents();
  outboxPublisher.publishAll(events);
}
```

---

## Troubleshooting

### Problema: Eventos de Domínio Não São Publicados

**Causa**: Esqueceu de fazer `pullDomainEvents()` e publicar.

**Solução**:
```java
Order order = // ...
order.place();
repository.save(order);

// ✅ Não esquecer!
List<DomainEvent> events = order.pullDomainEvents();
outboxPublisher.publishAll(events);
```

### Problema: Transaction Rollback Mas Evento Foi Publicado

**Causa**: Publicou direto no broker, fora da transação.

**Solução**: Use Transactional Outbox:
```java
// ✅ Outbox salva evento na mesma transação
outboxPublisher.publish(event); // Salvo no DB
// Se rollback acontecer, evento também é revertido
```

### Problema: ArchUnit Rule Falha

**Causa**: Violação de dependência.

**Exemplo de erro**:
```
Rule 'kernel should be framework free' was violated:
  Class <Order> depends on <org.springframework.stereotype.Component>
```

**Solução**: Remova dependência de framework do kernel!

---

## Referências

- [Architecture Documentation](./architecture.md)
- [ADRs](./adr/README.md)
- [C4 Diagrams](./c4-diagrams.md)
- [Contribution Guide](../CONTRIBUTING.md)
