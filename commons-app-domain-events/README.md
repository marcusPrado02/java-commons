# commons-app-domain-events

Infrastructure for domain events - in-memory event bus, dispatcher, and handlers registry.

## 📦 Maven Coordinates

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-domain-events</artifactId>
</dependency>
```

## 🎯 Overview

This module provides a simple, **in-memory** infrastructure for publishing and handling domain events:

- **DomainEventBus** - Simplified facade for publishing events and registering handlers
- **DomainEventHandler** - Interface for handling specific event types
- **DomainEventDispatcher** - Dispatches events to registered handlers
- **DomainEventHandlerRegistry** - Thread-safe registry of event handlers
- **DomainEventInterceptor** - Cross-cutting concerns (logging, metrics, tracing)

## 🚀 Quick Start

### Basic Usage

```java
// Create event bus
DomainEventBus eventBus = new DomainEventBus();

// Register handler
eventBus.register(new OrderCreatedHandler());

// Publish events from aggregate
Order order = orderRepository.findById(orderId).orElseThrow();
order.complete(AuditStamp.now(actorId));
orderRepository.save(order);

// Pull and publish events
eventBus.publishAll(order.pullDomainEvents());
```

### Defining Event Handlers

```java
@Component
public class OrderCreatedHandler implements DomainEventHandler<OrderCreatedEvent> {

    private final EmailService emailService;

    @Override
    public void handle(OrderCreatedEvent event) {
        // Send confirmation email
        emailService.sendOrderConfirmation(
            event.aggregate().id(),
            event.customerId()
        );
    }

    @Override
    public Class<OrderCreatedEvent> eventType() {
        return OrderCreatedEvent.class;
    }
}
```

### Multiple Handlers for Same Event

```java
// One event, multiple handlers
eventBus.register(new OrderCreatedEmailHandler());
eventBus.register(new OrderCreatedAnalyticsHandler());
eventBus.register(new OrderCreatedNotificationHandler());

// All handlers will be called when event is published
eventBus.publish(orderCreatedEvent);
```

## 🏗️ Architecture

### Event Bus Pattern

```
┌─────────────┐
│  Aggregate  │
│    Root     │
└──────┬──────┘
       │ pullDomainEvents()
       ▼
┌─────────────────┐
│ Domain Event    │
│ Bus             │
├─────────────────┤
│ - Registry      │
│ - Dispatcher    │
│ - Interceptors  │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
  Handler   Handler
```

### Components

**DomainEventBus**
- Facade combining registry, dispatcher, and interceptors
- Thread-safe for concurrent use
- Simple API: `register()`, `publish()`, `publishAll()`

**DomainEventHandler**
- Interface for handling specific event types
- Each handler declares its event type
- Exceptions in handlers are logged but don't stop other handlers

**DomainEventDispatcher**
- Retrieves handlers from registry
- Invokes handlers for each event
- Resilient to handler failures

**DomainEventHandlerRegistry**
- Thread-safe mapping of event types to handlers
- Supports multiple handlers per event type
- CopyOnWriteArrayList for concurrent reads

**DomainEventInterceptor**
- Cross-cutting concerns for all events
- `beforePublish()` and `afterPublish()` hooks
- Useful for logging, metrics, tracing

## 📚 Usage Examples

### Spring Integration

```java
@Configuration
public class DomainEventConfig {

    @Bean
    public DomainEventBus domainEventBus(List<DomainEventHandler<?>> handlers) {
        DomainEventBus eventBus = new DomainEventBus();

        // Register all handlers from Spring context
        handlers.forEach(eventBus::register);

        // Register interceptors
        eventBus.registerInterceptor(new LoggingInterceptor());
        eventBus.registerInterceptor(new MetricsInterceptor());

        return eventBus;
    }
}
```

### Application Service

```java
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final DomainEventBus eventBus;

    public void completeOrder(OrderId orderId) {
        // Load aggregate
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Execute domain logic
        order.complete(AuditStamp.now(getCurrentActorId()));

        // Save aggregate
        orderRepository.save(order);

        // Pull and publish events
        List<DomainEvent> events = order.pullDomainEvents();
        eventBus.publishAll(events);
    }
}
```

### Multiple Event Handlers

```java
// Email notification
@Component
public class OrderCreatedEmailHandler implements DomainEventHandler<OrderCreatedEvent> {

    @Override
    public void handle(OrderCreatedEvent event) {
        emailService.sendOrderConfirmation(event.customerId());
    }

    @Override
    public Class<OrderCreatedEvent> eventType() {
        return OrderCreatedEvent.class;
    }
}

// Analytics tracking
@Component
public class OrderCreatedAnalyticsHandler implements DomainEventHandler<OrderCreatedEvent> {

    @Override
    public void handle(OrderCreatedEvent event) {
        analyticsService.trackOrderCreated(event.aggregate().id());
    }

    @Override
    public Class<OrderCreatedEvent> eventType() {
        return OrderCreatedEvent.class;
    }
}

// Push notification
@Component
public class OrderCreatedNotificationHandler implements DomainEventHandler<OrderCreatedEvent> {

    @Override
    public void handle(OrderCreatedEvent event) {
        notificationService.sendPushNotification(event.customerId());
    }

    @Override
    public Class<OrderCreatedEvent> eventType() {
        return OrderCreatedEvent.class;
    }
}
```

### Event Interceptor

```java
public class LoggingInterceptor implements DomainEventInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public void beforePublish(DomainEvent event) {
        log.info("Publishing event: {} for aggregate: {}",
            event.eventType(),
            event.aggregate().id());
    }

    @Override
    public void afterPublish(DomainEvent event) {
        log.debug("Published event: {}", event.eventType());
    }
}

// Register interceptor
eventBus.registerInterceptor(new LoggingInterceptor());
```

### Metrics Interceptor

```java
public class MetricsInterceptor implements DomainEventInterceptor {

    private final MeterRegistry meterRegistry;

    @Override
    public void beforePublish(DomainEvent event) {
        meterRegistry.counter("domain.events.published",
            "type", event.eventType())
            .increment();
    }
}
```

## ⚠️ Important Considerations

### In-Memory Only

This implementation is **in-memory only**:
- Events are not persisted
- Events are lost if the application crashes before handlers complete
- No delivery guarantees across service restarts

For guaranteed delivery, use:
- **commons-app-outbox** - Transactional outbox pattern
- **Message brokers** - Kafka, RabbitMQ, etc.

### Synchronous Execution

Handlers execute **synchronously** in the same thread:
- Blocking operations in handlers will block the publisher
- For async execution, use ExecutorService inside handlers
- Consider using messaging infrastructure for async processing

### Error Handling

- Exceptions in handlers are **logged** and **don't stop** other handlers
- No retries or dead-letter queues
- For resilient processing, use messaging infrastructure

### Thread Safety

All components are **thread-safe**:
- DomainEventBus can be shared across threads
- ConcurrentHashMap + CopyOnWriteArrayList for thread safety
- Safe for concurrent event publishing

## 🔄 Comparison with Messaging Systems

| Feature | DomainEventBus | Kafka/RabbitMQ |
|---------|----------------|----------------|
| Delivery Guarantee | ❌ In-memory only | ✅ Persistent |
| Async Processing | ❌ Synchronous | ✅ Asynchronous |
| Distributed | ❌ Single JVM | ✅ Multi-service |
| Complexity | ✅ Simple | ⚠️ Complex |
| Latency | ✅ Very low | ⚠️ Higher |
| Use Case | Within bounded context | Across services |

**When to use DomainEventBus:**
- Events within a single bounded context
- Simple in-process notification
- Development and testing
- Low latency requirements

**When to use Messaging:**
- Events across multiple services
- Guaranteed delivery required
- Async processing needed
- Scalability and resilience critical

## 🧪 Testing

```java
@Test
void shouldHandleOrderCreatedEvent() {
    // Given
    DomainEventBus eventBus = new DomainEventBus();
    AtomicInteger callCount = new AtomicInteger(0);

    DomainEventHandler<OrderCreatedEvent> handler = new DomainEventHandler<>() {
        @Override
        public void handle(OrderCreatedEvent event) {
            callCount.incrementAndGet();
            assertEquals(customerId, event.customerId());
        }

        @Override
        public Class<OrderCreatedEvent> eventType() {
            return OrderCreatedEvent.class;
        }
    };

    eventBus.register(handler);

    // When
    OrderCreatedEvent event = new OrderCreatedEvent(snapshot, customerId);
    eventBus.publish(event);

    // Then
    assertEquals(1, callCount.get());
}
```

### Test with Interceptors

```java
@Test
void shouldCallInterceptors() {
    DomainEventBus eventBus = new DomainEventBus();
    AtomicInteger beforeCount = new AtomicInteger(0);
    AtomicInteger afterCount = new AtomicInteger(0);

    eventBus.registerInterceptor(new DomainEventInterceptor() {
        @Override
        public void beforePublish(DomainEvent event) {
            beforeCount.incrementAndGet();
        }

        @Override
        public void afterPublish(DomainEvent event) {
            afterCount.incrementAndGet();
        }
    });

    eventBus.publish(testEvent);

    assertEquals(1, beforeCount.get());
    assertEquals(1, afterCount.get());
}
```

## 🔗 Related Modules

- **commons-kernel-ddd** - Domain event interfaces (DomainEvent, DomainEventPublisher)
- **commons-app-outbox** - Transactional outbox pattern for reliable delivery
- **commons-adapters-messaging** - Integration with Kafka, RabbitMQ

## 📖 References

- **Domain-Driven Design** by Eric Evans (Chapter: Domain Events)
- **Implementing Domain-Driven Design** by Vaughn Vernon (Chapter 8: Domain Events)
- **Event-Driven Architecture** patterns and practices
