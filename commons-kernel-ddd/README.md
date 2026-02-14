# commons-kernel-ddd

Core DDD (Domain-Driven Design) tactical patterns and building blocks for domain modeling.

## 📦 Maven Coordinates

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-ddd</artifactId>
</dependency>
```

## 🎯 Overview

This module provides **DDD tactical patterns** to help you build rich domain models:

- **Entity** and **AggregateRoot** base classes with identity, versioning, audit, soft delete
- **ValueObject** helpers for immutable value types
- **Repository** marker interfaces following DDD repository pattern
- **Specification** pattern for composable business rules
- **Domain Events** infrastructure for event-driven architectures
- **Multi-tenancy** support built into entities
- **Audit Trail** with creation/update/deletion tracking
- **Entity Factory** helpers for context-aware entity creation

## 🧱 Core Building Blocks

### Entity

Base class for domain entities with identity, versioning, audit trail, and soft delete support.

```java
public class Order extends Entity<OrderId> {
    private final CustomerId customerId;
    private Money total;
    private OrderStatus status;

    public Order(OrderId id, TenantId tenantId, CustomerId customerId, AuditStamp created) {
        super(id, tenantId, created);
        this.customerId = customerId;
        this.total = Money.ZERO;
        this.status = OrderStatus.PENDING;
    }

    public void addItem(OrderItem item, AuditStamp updated) {
        this.total = this.total.add(item.price());
        touch(updated);  // Updates audit trail and increments version
    }

    public void cancel(AuditStamp updated, DeletionStamp deleted) {
        this.status = OrderStatus.CANCELLED;
        softDelete(deleted, updated);  // Soft delete with audit
    }
}
```

**Features:**
- Identity-based equality (by ID)
- Optimistic locking via `EntityVersion`
- Audit trail with `createdAt`, `createdBy`, `updatedAt`, `updatedBy`
- Soft delete with `deletedAt`, `deletedBy`
- Multi-tenant support with `TenantId`
- Protected mutation methods (`touch()`, `softDelete()`, `restore()`)

### AggregateRoot

Extends Entity and adds domain event recording.

```java
public class Order extends AggregateRoot<OrderId> {
    private OrderStatus status;

    public Order(OrderId id, TenantId tenantId, CustomerId customerId, AuditStamp created) {
        super(id, tenantId, created);
        recordChange(snapshot ->
            new OrderCreatedEvent(snapshot, customerId)
        );
    }

    public void complete(AuditStamp updated) {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be completed");
        }
        this.status = OrderStatus.COMPLETED;
        touch(updated);
        recordChange(snapshot ->
            new OrderCompletedEvent(snapshot)
        );
    }

    public void cancel(AuditStamp updated, DeletionStamp deleted) {
        this.status = OrderStatus.CANCELLED;
        softDelete(deleted, updated);
        recordSoftDelete(snapshot ->
            new OrderCancelledEvent(snapshot)
        );
    }
}
```

**Event recording:**
```java
// In your application service
public void completeOrder(OrderId orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));

    order.complete(AuditStamp.now(currentActorId()));

    orderRepository.save(order);

    // Pull and publish events
    List<DomainEvent> events = order.pullDomainEvents();
    domainEventPublisher.publishAll(events);
}
```

### ValueObject

Marker interface and base classes for value objects.

#### Using Records (Recommended)

```java
public record Email(String value) implements ValueObject {
    public Email {
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        value = value.trim().toLowerCase();  // Normalization
    }
}

public record Address(
    String street,
    String city,
    String zipCode,
    String country
) implements ValueObject {
    public Address {
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(zipCode, "zipCode");
        Objects.requireNonNull(country, "country");
    }
}
```

#### Using SingleValueObject

```java
public final class Email extends SingleValueObject<String> {
    public Email(String value) {
        super(validate(value));
    }

    private static String validate(String value) {
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        return value.trim().toLowerCase();
    }
}
```

#### Using CompositeValueObject

```java
public final class Address extends CompositeValueObject {
    private final String street;
    private final String city;
    private final String zipCode;

    public Address(String street, String city, String zipCode) {
        this.street = Objects.requireNonNull(street);
        this.city = Objects.requireNonNull(city);
        this.zipCode = Objects.requireNonNull(zipCode);
    }

    // Getters...
}
```

### Repository

Marker interfaces following DDD repository pattern.

```java
public interface OrderRepository extends Repository<Order, OrderId> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByCustomerId(CustomerId customerId);
    List<Order> findMatching(Specification<Order> spec);
}
```

**With pagination:**

```java
public interface CustomerRepository extends PageableRepository<Customer, CustomerId> {
    Page<Customer> findByCountry(String country, Pageable pageable);
}

// Usage
Pageable pageable = Pageable.of(0, 20, "name");
Page<Customer> page = customerRepository.findByCountry("USA", pageable);
```

**Key principles:**
- Operate on aggregate roots only
- Use ubiquitous language in method names
- Return domain objects, not DTOs
- Keep interface simple - use Specifications for complex queries

### Specification

Composable business rules pattern.

```java
// Define specifications
Specification<Order> isPending = order -> order.status() == OrderStatus.PENDING;
Specification<Order> isExpensive = order -> order.total().compareTo(Money.of(1000)) > 0;
Specification<Order> isOlderThan = days ->
    order -> order.createdAt().isBefore(Instant.now().minus(Duration.ofDays(days)));

// Combine them
Specification<Order> oldExpensivePendingOrders =
    isPending.and(isExpensive).and(isOlderThan(30));

// Use in repository
List<Order> orders = orderRepository.findMatching(oldExpensivePendingOrders);

// Use for validation
if (!customerCanPlaceOrder.isSatisfiedBy(customer)) {
    throw new CustomerCannotPlaceOrderException();
}

// Use in streams
List<Order> filtered = orders.stream()
    .filter(isPending.and(isExpensive))
    .toList();
```

**Class-based specifications:**

```java
public class OrderSpecifications {
    public static Specification<Order> isPending() {
        return order -> order.status() == OrderStatus.PENDING;
    }

    public static Specification<Order> totalGreaterThan(Money amount) {
        return order -> order.total().compareTo(amount) > 0;
    }

    public static Specification<Order> createdAfter(Instant date) {
        return order -> order.audit().created().timestamp().isAfter(date);
    }
}

// Usage
var spec = OrderSpecifications.isPending()
    .and(OrderSpecifications.totalGreaterThan(Money.of(1000)))
    .and(OrderSpecifications.createdAfter(Instant.now().minus(Duration.ofDays(30))));
```

**Abstract specifications:**

```java
public class CustomerHasActiveOrders extends AbstractSpecification<Customer> {
    @Override
    public boolean isSatisfiedBy(Customer customer) {
        return customer.orders().stream()
            .anyMatch(Order::isActive);
    }
}
```

### EntityFactory

Helper for creating entities with proper audit context.

```java
public class OrderFactory {
    private final EntityFactory entityFactory;

    public OrderFactory(EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    public Order createOrder(OrderId id, CustomerId customerId) {
        return entityFactory.create((tenantId, auditStamp) ->
            new Order(id, tenantId, customerId, auditStamp)
        );
    }
}

// Or without context providers
TenantId tenantId = TenantId.of("tenant-1");
String actorId = "user-123";
Instant now = Instant.now();

Order order = EntityFactory.create(tenantId, actorId, now,
    (tid, stamp) -> new Order(OrderId.generate(), tid, customerId, stamp));
```

## 🔧 Domain Events

### Defining Events

```java
public record OrderCreatedEvent(
    AggregateSnapshot<OrderId> aggregate,
    CustomerId customerId
) implements DomainEvent {
    @Override
    public String eventType() {
        return "OrderCreated";
    }
}
```

### Recording Events in Aggregates

```java
public class Order extends AggregateRoot<OrderId> {
    public void complete(AuditStamp updated) {
        this.status = OrderStatus.COMPLETED;
        touch(updated);
        recordChange(snapshot -> new OrderCompletedEvent(snapshot));
    }
}
```

### Publishing Events

```java
// In application service
Order order = orderRepository.findById(orderId).orElseThrow();
order.complete(AuditStamp.now(actorId));
orderRepository.save(order);

// Pull and publish
List<DomainEvent> events = order.pullDomainEvents();
domainEventPublisher.publishAll(events);
```

## 🏗️ Architecture Patterns

### Multi-Tenancy

All entities are multi-tenant by default via `TenantId`:

```java
public class Order extends Entity<OrderId> {
    public Order(OrderId id, TenantId tenantId, ...) {
        super(id, tenantId, ...);
    }
}

// Tenant is always set
TenantId tenantId = order.tenantId();
```

### Optimistic Locking

Entities include version for optimistic locking:

```java
public class Order extends Entity<OrderId> {
    // Version is automatically managed
    public void update(AuditStamp updated) {
        // ...
        touch(updated);  // Increments version automatically
    }
}

// In JPA repository
@Version
private Long version;
```

### Soft Delete

Entities support soft delete out of the box:

```java
public class Order extends Entity<OrderId> {
    public void cancel(AuditStamp updated, DeletionStamp deleted) {
        softDelete(deleted, updated);
    }

    public void reactivate(AuditStamp updated) {
        restore(updated);
    }
}

// Check if deleted
if (order.isDeleted()) {
    // Handle deleted entity
}

// Get deletion info
order.deletion().ifPresent(del -> {
    System.out.println("Deleted by: " + del.actorId());
    System.out.println("Deleted at: " + del.timestamp());
});
```

## 📚 Best Practices

### 1. Keep Aggregates Small

```java
// ❌ BAD: Large aggregate with many entities
public class Order extends AggregateRoot<OrderId> {
    private List<OrderItem> items;
    private List<Payment> payments;
    private List<Shipment> shipments;
    // Too much!
}

// ✅ GOOD: Small aggregates with references
public class Order extends AggregateRoot<OrderId> {
    private List<OrderItem> items;  // Owned entities only
    private PaymentId paymentId;    // Reference to another aggregate
    private ShipmentId shipmentId;  // Reference to another aggregate
}
```

### 2. Protect Invariants

```java
public class Order extends AggregateRoot<OrderId> {
    // ❌ Public setters break encapsulation
    public void setStatus(OrderStatus status) { ... }

    // ✅ Intent-revealing methods that protect invariants
    public void complete(AuditStamp updated) {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be completed");
        }
        if (this.items.isEmpty()) {
            throw new IllegalStateException("Cannot complete order with no items");
        }
        this.status = OrderStatus.COMPLETED;
        touch(updated);
        recordChange(snapshot -> new OrderCompletedEvent(snapshot));
    }
}
```

### 3. Use Value Objects

```java
// ❌ Primitive obsession
public class Customer {
    private String email;
    private String street;
    private String city;
}

// ✅ Rich domain model with value objects
public class Customer {
    private Email email;
    private Address address;
}
```

### 4. Repository Naming

```java
// ✅ Use domain language
List<Order> findActiveOrders();
List<Customer> findPremiumCustomers();
Optional<Order> findByOrderNumber(String orderNumber);

// ❌ Avoid technical language
List<Order> findByStatusEquals(String status);
List<Customer> findByCreatedAtGreaterThan(Instant date);
```

### 5. Specifications for Complex Queries

```java
// ✅ Composable, testable, reusable
Specification<Order> spec = OrderSpecifications.isPending()
    .and(OrderSpecifications.totalGreaterThan(Money.of(1000)))
    .and(OrderSpecifications.forCustomer(customerId));

List<Order> orders = orderRepository.findMatching(spec);

// ❌ Hard-coded query in repository
List<Order> findPendingExpensiveOrdersForCustomer(CustomerId id, Money minTotal);
```

## 🧪 Testing

```java
@Test
void shouldCompleteOrder() {
    // Given
    OrderId id = OrderId.generate();
    TenantId tenantId = TenantId.of("tenant-1");
    CustomerId customerId = CustomerId.generate();
    AuditStamp created = AuditStamp.of("user-1", Instant.now());

    Order order = new Order(id, tenantId, customerId, created);
    order.addItem(new OrderItem(...), AuditStamp.now("user-1"));

    // When
    order.complete(AuditStamp.now("user-1"));

    // Then
    assertEquals(OrderStatus.COMPLETED, order.status());
    List<DomainEvent> events = order.pullDomainEvents();
    assertEquals(2, events.size());  // OrderCreated + OrderCompleted
    assertInstanceOf(OrderCompletedEvent.class, events.get(1));
}
```

## 🔗 Related Modules

- **commons-kernel-errors** - Error handling and domain exceptions
- **commons-adapters-persistence-jpa** - JPA implementations for repositories
- **commons-app-outbox** - Transactional outbox pattern for events

## 📖 References

- **Domain-Driven Design** by Eric Evans
- **Implementing Domain-Driven Design** by Vaughn Vernon
- **Domain-Driven Design Distilled** by Vaughn Vernon
