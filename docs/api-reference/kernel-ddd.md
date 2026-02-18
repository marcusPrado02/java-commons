# API Reference: commons-kernel-ddd

## Visão Geral

O módulo `commons-kernel-ddd` fornece building blocks táticos para Domain-Driven Design, incluindo Entity, AggregateRoot, ValueObject, DomainEvent e outros padrões essenciais.

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-ddd</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🏗️ Building Blocks

### Entity

**Package:** `com.marcusprado02.commons.kernel.ddd.entity`

Classe base para entidades de domínio com identidade, versionamento, auditoria e multi-tenancy.

#### Estrutura

```java
public abstract class Entity<ID> {
    private final ID id;                    // Identidade única
    private final TenantId tenantId;        // Multi-tenancy
    private EntityVersion version;          // Versionamento otimista
    private final AuditTrail audit;         // Trilha de auditoria
    private DeletionStamp deletion;         // Soft delete (nullable)
    
    protected Entity(ID id, TenantId tenantId, AuditStamp created);
}
```

#### Métodos Principais

```java
public final ID id();
public final TenantId tenantId();
public final EntityVersion version();
public final AuditTrail audit();
public final boolean isDeleted();
public final Optional<DeletionStamp> deletion();

protected void markAsDeleted(AuditStamp deletedBy);
protected void markAsUpdated(AuditStamp updatedBy);
protected void incrementVersion();
```

#### Exemplo Completo

```java
package com.myapp.domain.user;

import com.marcusprado02.commons.kernel.ddd.entity.Entity;
import com.marcusprado02.commons.kernel.ddd.audit.*;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;

public class User extends Entity<UserId> {
    
    private UserName name;
    private Email email;
    private UserStatus status;
    
    // Construtor para criação
    public User(UserId id, TenantId tenantId, UserName name, Email email, 
                AuditStamp created) {
        super(id, tenantId, created);
        this.name = Objects.requireNonNull(name);
        this.email = Objects.requireNonNull(email);
        this.status = UserStatus.ACTIVE;
    }
    
    // Métodos de comportamento
    public void updateEmail(Email newEmail, AuditStamp updatedBy) {
        if (!this.email.equals(newEmail)) {
            this.email = newEmail;
            markAsUpdated(updatedBy);
        }
    }
    
    public void deactivate(AuditStamp deactivatedBy) {
        if (status != UserStatus.INACTIVE) {
            status = UserStatus.INACTIVE;
            markAsUpdated(deactivatedBy);
        }
    }
    
    public void delete(AuditStamp deletedBy) {
        markAsDeleted(deletedBy);
    }
    
    // Getters
    public UserName name() { return name; }
    public Email email() { return email; }
    public UserStatus status() { return status; }
}
```

#### Quando Usar

- ✅ Objetos com **identidade única** (dois usuários com mesmo nome são diferentes)
- ✅ Objetos que **mudam ao longo do tempo**
- ✅ Objetos que precisam de **auditoria** (quem criou, quando foi modificado)
- ✅ Aplicações **multi-tenant** (cada entidade pertence a um tenant)

---

### AggregateRoot

**Package:** `com.marcusprado02.commons.kernel.ddd.entity`

Raiz de agregado com suporte a Domain Events.

#### Estrutura

```java
public abstract class AggregateRoot<ID> extends Entity<ID> 
    implements DomainEventRecorder {
    
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    protected AggregateRoot(ID id, TenantId tenantId, AuditStamp created);
}
```

#### Métodos de Eventos

```java
// Registra evento de domínio
protected void recordEvent(DomainEvent event);

// Pull events para processamento (Transactional Outbox)
@Override
public List<DomainEvent> pullDomainEvents();

// Verifica se tem eventos pendentes
@Override
public boolean hasDomainEvents();
```

#### Exemplo Completo

```java
package com.myapp.domain.order;

import com.marcusprado02.commons.kernel.ddd.entity.AggregateRoot;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.audit.AuditStamp;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

public class Order extends AggregateRoot<OrderId> {
    
    private CustomerId customerId;
    private List<OrderItem> items;
    private Money totalAmount;
    private OrderStatus status;
    
    // Construtor privado - usa factory method
    private Order(OrderId id, TenantId tenantId, CustomerId customerId, 
                  AuditStamp created) {
        super(id, tenantId, created);
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.totalAmount = Money.zero();
        this.status = OrderStatus.DRAFT;
    }
    
    // Factory method
    public static Order create(OrderId id, TenantId tenantId, 
                              CustomerId customerId, AuditStamp created) {
        Order order = new Order(id, tenantId, customerId, created);
        order.recordEvent(new OrderCreated(
            id, 
            customerId, 
            Instant.now()
        ));
        return order;
    }
    
    // Comportamentos que geram eventos
    public void addItem(Product product, int quantity, AuditStamp updatedBy) {
        Invariant.notNull(product, "product");
        Invariant.isTrue(quantity > 0, "quantity must be positive");
        Invariant.isTrue(status == OrderStatus.DRAFT, 
            "Cannot add items to non-draft order");
        
        OrderItem item = new OrderItem(product.id(), product.price(), quantity);
        items.add(item);
        recalculateTotal();
        markAsUpdated(updatedBy);
        
        recordEvent(new OrderItemAdded(
            id(), 
            product.id(), 
            quantity, 
            Instant.now()
        ));
    }
    
    public void submit(AuditStamp submittedBy) {
        Invariant.isTrue(status == OrderStatus.DRAFT, 
            "Only draft orders can be submitted");
        Invariant.isTrue(!items.isEmpty(), 
            "Cannot submit empty order");
        
        status = OrderStatus.SUBMITTED;
        markAsUpdated(submittedBy);
        
        recordEvent(new OrderSubmitted(
            id(), 
            customerId, 
            totalAmount, 
            Instant.now()
        ));
    }
    
    public void cancel(String reason, AuditStamp cancelledBy) {
        Invariant.isTrue(status.isCancellable(), 
            "Order cannot be cancelled in current status");
        
        status = OrderStatus.CANCELLED;
        markAsUpdated(cancelledBy);
        
        recordEvent(new OrderCancelled(
            id(), 
            reason, 
            Instant.now()
        ));
    }
    
    private void recalculateTotal() {
        totalAmount = items.stream()
            .map(OrderItem::subtotal)
            .reduce(Money.zero(), Money::add);
    }
    
    // Getters
    public CustomerId customerId() { return customerId; }
    public List<OrderItem> items() { return List.copyOf(items); }
    public Money totalAmount() { return totalAmount; }
    public OrderStatus status() { return status; }
}
```

#### Domain Events

```java
// OrderCreated.java
public record OrderCreated(
    EventId eventId,
    OrderId orderId,
    CustomerId customerId,
    Instant occurredAt
) implements DomainEvent {
    
    public OrderCreated(OrderId orderId, CustomerId customerId, Instant occurredAt) {
        this(EventId.generate(), orderId, customerId, occurredAt);
    }
    
    @Override
    public String aggregateType() { return "Order"; }
    
    @Override
    public String aggregateId() { return orderId.value(); }
    
    @Override
    public long aggregateVersion() { return 0; }
    
    @Override
    public EventMetadata metadata() { 
        return EventMetadata.empty(); 
    }
}

// OrderSubmitted.java
public record OrderSubmitted(
    EventId eventId,
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    Instant occurredAt
) implements DomainEvent {
    // ... similar ao OrderCreated
}
```

#### Processamento de Eventos (Transactional Outbox)

```java
@Service
@Transactional
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    
    public void submitOrder(OrderId orderId, ActorId actorId) {
        // 1. Carrega aggregate
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // 2. Executa comportamento de negócio
        order.submit(AuditStamp.now(actorId));
        
        // 3. Salva aggregate (mesma transação)
        orderRepository.save(order);
        
        // 4. Pull events e salva no outbox (mesma transação)
        List<DomainEvent> events = order.pullDomainEvents();
        for (DomainEvent event : events) {
            outboxRepository.save(toOutboxMessage(event));
        }
        
        // Commit da transação garante atomicidade
    }
}
```

#### Quando Usar

- ✅ **Raiz de agregado** que controla invariantes de outras entidades
- ✅ Precisa publicar **Domain Events** (Transactional Outbox)
- ✅ **Consistência transacional** dentro do agregado
- ✅ Operações que precisam de **auditoria completa** com eventos

---

### ValueObject

**Package:** `com.marcusprado02.commons.kernel.ddd.vo`

Objetos imutáveis identificados por seus atributos, não por identidade.

#### Interface Base

```java
public interface ValueObject {
    // Marker interface - implementações devem ser imutáveis
}
```

#### SingleValueObject (Helper)

Para value objects com um único atributo:

```java
public abstract class SingleValueObject<T> implements ValueObject {
    private final T value;
    
    protected SingleValueObject(T value);
    public final T value();
    
    // equals/hashCode/toString baseados no value
}
```

#### Exemplos Completos

##### Email (Single Value)

```java
public final class Email extends SingleValueObject<String> {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    
    private Email(String value) {
        super(value);
    }
    
    public static Email of(String value) {
        Invariant.notBlank(value, "email");
        
        String normalized = value.trim().toLowerCase();
        
        Invariant.isTrue(
            EMAIL_PATTERN.matcher(normalized).matches(),
            "Invalid email format"
        );
        Invariant.isTrue(
            normalized.length() <= 255,
            "Email too long (max 255 characters)"
        );
        
        return new Email(normalized);
    }
    
    public String domain() {
        return value().substring(value().indexOf('@') + 1);
    }
    
    public boolean isFromDomain(String domain) {
        return domain().equalsIgnoreCase(domain);
    }
}
```

##### Money (Multi Value)

```java
public final class Money implements ValueObject {
    
    private final BigDecimal amount;
    private final Currency currency;
    
    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }
    
    public static Money of(BigDecimal amount, Currency currency) {
        Invariant.notNull(amount, "amount");
        Invariant.notNull(currency, "currency");
        Invariant.isTrue(
            amount.compareTo(BigDecimal.ZERO) >= 0,
            "Amount cannot be negative"
        );
        
        return new Money(
            amount.setScale(currency.getDefaultFractionDigits(), 
                           RoundingMode.HALF_UP),
            currency
        );
    }
    
    public static Money zero() {
        return of(BigDecimal.ZERO, Currency.getInstance("USD"));
    }
    
    // Operações imutáveis
    public Money add(Money other) {
        assertSameCurrency(other);
        return of(amount.add(other.amount), currency);
    }
    
    public Money subtract(Money other) {
        assertSameCurrency(other);
        return of(amount.subtract(other.amount), currency);
    }
    
    public Money multiply(BigDecimal multiplier) {
        return of(amount.multiply(multiplier), currency);
    }
    
    public boolean greaterThan(Money other) {
        assertSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }
    
    private void assertSameCurrency(Money other) {
        Invariant.isTrue(
            currency.equals(other.currency),
            "Cannot operate on different currencies"
        );
    }
    
    // Getters
    public BigDecimal amount() { return amount; }
    public Currency currency() { return currency; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Money other)) return false;
        return amount.equals(other.amount) && 
               currency.equals(other.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s", 
            amount.toPlainString(), 
            currency.getCurrencyCode());
    }
}
```

##### Address (Complex Value)

```java
public final class Address implements ValueObject {
    
    private final String street;
    private final String city;
    private final String state;
    private final PostalCode postalCode;
    private final Country country;
    
    private Address(String street, String city, String state, 
                   PostalCode postalCode, Country country) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }
    
    public static Address of(String street, String city, String state,
                            PostalCode postalCode, Country country) {
        Invariant.notBlank(street, "street");
        Invariant.notBlank(city, "city");
        Invariant.notBlank(state, "state");
        Invariant.notNull(postalCode, "postalCode");
        Invariant.notNull(country, "country);
        
        return new Address(
            street.trim(),
            city.trim(),
            state.trim().toUpperCase(),
            postalCode,
            country
        );
    }
    
    public String fullAddress() {
        return String.format("%s, %s - %s, %s, %s",
            street, city, state, postalCode.value(), country.code());
    }
    
    // Getters
    public String street() { return street; }
    public String city() { return city; }
    public String state() { return state; }
    public PostalCode postalCode() { return postalCode; }
    public Country country() { return country; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Address other)) return false;
        return Objects.equals(street, other.street) &&
               Objects.equals(city, other.city) &&
               Objects.equals(state, other.state) &&
               Objects.equals(postalCode, other.postalCode) &&
               Objects.equals(country, other.country);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, postalCode, country);
    }
}
```

#### Quando Usar

- ✅ Conceitos de domínio **sem identidade própria**
- ✅ **Igualdade por valor** (dois CPFs "12345678900" são o mesmo)
- ✅ Objetos **imutáveis**
- ✅ **Validações concentradas** no momento da criação
- ✅ **Comportamentos** relacionados ao conceito (Money.add, Email.domain)

---

### Identifier

**Package:** `com.marcusprado02.commons.kernel.ddd.id`

Interface base para identificadores tipados.

#### Interface

```java
public interface Identifier<T> extends Serializable {
    T value();
    
    default String asString() {
        return String.valueOf(value());
    }
}
```

#### Exemplos

```java
// UserId.java
public record UserId(String value) implements Identifier<String> {
    
    public UserId {
        Invariant.notBlank(value, "userId");
    }
    
    public static UserId of(String value) {
        return new UserId(value);
    }
    
    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }
}

// OrderId.java (usando UUID)
public record OrderId(UUID value) implements Identifier<UUID> {
    
    public OrderId {
        Objects.requireNonNull(value, "orderId cannot be null");
    }
    
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }
    
    public static OrderId of(UUID value) {
        return new OrderId(value);
    }
    
    public static OrderId parse(String uuidString) {
        return new OrderId(UUID.fromString(uuidString));
    }
}

// CustomerId.java (usando Long)
public record CustomerId(Long value) implements Identifier<Long> {
    
    public CustomerId {
        Invariant.notNull(value, "customerId");
        Invariant.isTrue(value > 0, "customerId must be positive");
    }
    
    public static CustomerId of(Long value) {
        return new CustomerId(value);
    }
}
```

#### Benefícios

- ✅ **Type safety**: `transfer(OrderId, UserId)` vs `transfer(String, String)`
- ✅ **Validação centralizada**: Sempre válido se instanciado
- ✅ **Semântica clara**: `CustomerId` é mais expressivo que `Long`
- ✅ **Refactoring seguro**: Mudar de UUID para Long é localizado

---

### Invariant

**Package:** `com.marcusprado02.commons.kernel.ddd.invariant`

Validações de invariantes de domínio.

#### Métodos

```java
public final class Invariant {
    
    public static void notNull(Object value, String fieldName);
    public static void notBlank(String value, String fieldName);
    public static void notEmpty(Collection<?> collection, String fieldName);
    
    public static void isTrue(boolean condition, String message);
    public static void isFalse(boolean condition, String message);
    
    public static void in(Object value, Collection<?> allowed, String fieldName);
    public static void notIn(Object value, Collection<?> forbidden, String fieldName);
    
    public static void greaterThan(Comparable value, Comparable min, String fieldName);
    public static void lessThan(Comparable value, Comparable max, String fieldName);
    public static void between(Comparable value, Comparable min, Comparable max, String fieldName);
}
```

#### Exemplo

```java
public final class Age extends SingleValueObject<Integer> {
    
    private Age(Integer value) {
        super(value);
    }
    
    public static Age of(int value) {
        Invariant.isTrue(value >= 0, "Age cannot be negative");
        Invariant.isTrue(value <= 150, "Age too high (unrealistic)");
        
        return new Age(value);
    }
    
    public boolean isAdult() {
        return value() >= 18;
    }
    
    public boolean isMinor() {
        return value() < 18;
    }
}
```

---

## Padrões de Uso

### Aggregate Design

#### Regras de Ouro

1. **Um aggregate root por transação**
2. **Referências entre aggregates por ID** (não por objeto)
3. **Invariantes protegidas dentro do aggregate**
4. **Eventos para comunicação entre aggregates**

#### Exemplo

```java
// ❌ ERRADO - Aggregate muito grande
public class Order extends AggregateRoot<OrderId> {
    private Customer customer;  // ❌ Outro aggregate embutido
    private List<Product> products;  // ❌ Outro aggregate embutido
}

// ✅ CORRETO - Referências por ID
public class Order extends AggregateRoot<OrderId> {
    private CustomerId customerId;  // ✅ Apenas o ID
    private List<OrderItem> items;  // ✅ Entidades internas
    
    record OrderItem(ProductId productId, int quantity, Money price) {}
}
```

---

## Testing

```java
@Test
void shouldCreateUserWithValidData() {
    // Given
    UserId id = UserId.generate();
    TenantId tenantId = TenantId.of("tenant-1");
    UserName name = UserName.of("John Doe");
    Email email = Email.of("john@example.com");
    AuditStamp created = AuditStamp.now(ActorId.of("admin"));
    
    // When
    User user = new User(id, tenantId, name, email, created);
    
    // Then
    assertThat(user.id()).isEqualTo(id);
    assertThat(user.name()).isEqualTo(name);
    assertThat(user.email()).isEqualTo(email);
    assertThat(user.isDeleted()).isFalse();
}

@Test
void shouldRecordEventWhenOrderSubmitted() {
    // Given
    Order order = createDraftOrder();
    order.addItem(product, 2, auditStamp);
    
    // When
    order.submit(auditStamp);
    
    // Then
    List<DomainEvent> events = order.pullDomainEvents();
    assertThat(events).hasSize(3);  // OrderCreated, ItemAdded, OrderSubmitted
    assertThat(events.get(2)).isInstanceOf(OrderSubmitted.class);
}
```

---

## Ver Também

- [commons-kernel-result](kernel-result.md) - Result pattern para erros de domínio
- [commons-app-domain-events](../guides/domain-events.md) - Publicação de eventos
- [commons-app-outbox](../guides/transactional-outbox.md) - Consistência eventual
