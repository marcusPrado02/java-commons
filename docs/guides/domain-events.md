# Guia: Domain Events

## Visão Geral

Domain Events representam fatos importantes que aconteceram no domínio. São fundamentais para comunicação assíncrona entre bounded contexts e para implementar padrões como Event Sourcing e CQRS.

---

## 🎯 Por Que Domain Events?

### Acoplamento Reduzido

```java
// ❌ Sem Events: Acoplamento direto
@Service
public class OrderService {
    private final EmailService emailService;          // ❌ Acoplado
    private final InventoryService inventoryService;  // ❌ Acoplado
    private final AnalyticsService analyticsService;  // ❌ Acoplado
    
    public void createOrder(Order order) {
        orderRepository.save(order);
        emailService.sendConfirmation(order);     // ❌ Síncrono
        inventoryService.reserveStock(order);     // ❌ Síncrono
        analyticsService.trackOrder(order);       // ❌ Síncrono
    }
}

// ✅ Com Events: Desacoplado
@Service
public class OrderService {
    private final EventPublisher eventPublisher;  // ✅ Único acoplamento
    
    public void createOrder(Order order) {
        orderRepository.save(order);
        eventPublisher.publish(new OrderCreated(order));  // ✅ Assíncrono
        // Outros serviços reagem ao evento independentemente
    }
}
```

### Auditoria Completa

```java
// Todos os eventos ficam registrados
OrderCreated → OrderItemAdded → OrderSubmitted → PaymentProcessed → OrderShipped
```

### Comunicação Entre Bounded Contexts

```
┌─────────────┐                  ┌─────────────┐
│   Orders    │                  │  Inventory  │
│             │ OrderCreated     │             │
│             ├─────────────────>│             │
│  (context)  │                  │  (context)  │
└─────────────┘                  └─────────────┘
```

---

## 📦 Estrutura de um Domain Event

### Interface Base

```java
public interface DomainEvent {
    
    /**
     * ID único do evento
     */
    String eventId();
    
    /**
     * Tipo do evento (ex: "OrderCreated")
     */
    String eventType();
    
    /**
     * Quando o evento ocorreu
     */
    Instant occurredAt();
    
    /**
     * ID do aggregate que gerou o evento
     */
    String aggregateId();
    
    /**
     * Versão do aggregate (para Event Sourcing)
     */
    default Long aggregateVersion() {
        return null;
    }
}
```

### Implementação Concreta

```java
public record OrderCreated(
    String eventId,
    String orderId,
    String customerId,
    BigDecimal totalAmount,
    List<OrderItem> items,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public String eventType() {
        return "OrderCreated";
    }
    
    @Override
    public String aggregateId() {
        return orderId;
    }
    
    // Factory method
    public static OrderCreated from(Order order) {
        return new OrderCreated(
            UUID.randomUUID().toString(),
            order.id().value(),
            order.customerId().value(),
            order.totalAmount().value(),
            order.items().stream()
                .map(OrderItemDTO::from)
                .toList(),
            Instant.now()
        );
    }
}
```

---

## 🏗️ Aggregate com Events

### Aggregate Root

```java
public class Order extends AggregateRoot<OrderId> {
    
    private CustomerId customerId;
    private OrderStatus status;
    private List<OrderItem> items;
    private Money totalAmount;
    
    // Construtor privado
    private Order(OrderId id, CustomerId customerId, TenantId tenantId, AuditStamp audit) {
        super(id, tenantId, audit);
        this.customerId = customerId;
        this.status = OrderStatus.DRAFT;
        this.items = new ArrayList<>();
        this.totalAmount = Money.zero();
        
        // ✅ Registra evento de criação
        recordEvent(OrderCreated.from(this));
    }
    
    // Factory method
    public static Order create(OrderId id, CustomerId customerId, TenantId tenantId, AuditStamp audit) {
        return new Order(id, customerId, tenantId, audit);
    }
    
    // Comportamento que gera evento
    public Result<Void> addItem(Product product, int quantity, AuditStamp audit) {
        if (status != OrderStatus.DRAFT) {
            return Result.fail(Problem.of(
                "ORDER.CANNOT_MODIFY",
                "Cannot add items to order in status: " + status
            ));
        }
        
        OrderItem item = new OrderItem(product, quantity);
        items.add(item);
        recalculateTotal();
        
        // ✅ Registra evento
        recordEvent(new OrderItemAdded(
            UUID.randomUUID().toString(),
            id().value(),
            product.id().value(),
            quantity,
            Instant.now()
        ));
        
        return Result.ok();
    }
    
    public Result<Void> submit(AuditStamp audit) {
        if (items.isEmpty()) {
            return Result.fail(Problem.of(
                "ORDER.EMPTY_CART",
                "Cannot submit empty order"
            ));
        }
        
        this.status = OrderStatus.SUBMITTED;
        this.audit = audit;
        
        // ✅ Registra evento importante
        recordEvent(new OrderSubmitted(
            UUID.randomUUID().toString(),
            id().value(),
            customerId.value(),
            totalAmount.value(),
            items.size(),
            Instant.now()
        ));
        
        return Result.ok();
    }
}
```

### Base Class (AggregateRoot)

```java
public abstract class AggregateRoot<ID extends Identifier> extends Entity<ID> {
    
    // Lista interna de eventos
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    /**
     * Registra evento (não publica ainda)
     */
    protected void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    /**
     * Retorna e limpa eventos (publish-and-clear)
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
    
    /**
     * Apenas visualiza eventos (não limpa)
     */
    public List<DomainEvent> peekDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
}
```

---

## 📤 Publishing Events

### Application Service (Transactional Outbox)

```java
@Service
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final OutboxPublisher outboxPublisher;
    
    @Transactional  // ⚠️ Transação engloba tudo
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        // 1. Cria aggregate (gera eventos internos)
        Order order = Order.create(...);
        order.addItem(...);
        order.submit();
        
        // 2. Persiste aggregate
        Result<Void> saveResult = orderRepository.save(order);
        if (saveResult.isFail()) {
            return Result.fail(saveResult.problemOrNull());
        }
        
        // 3. Publica eventos no outbox (mesma transação!)
        List<DomainEvent> events = order.pullDomainEvents();
        for (DomainEvent event : events) {
            outboxPublisher.publish(
                "orders.events",         // topic
                order.id().value(),      // aggregateId (garante ordem)
                event                    // payload
            );
        }
        
        return Result.ok(order.id());
        
        // ✅ Commit atomicamente: Order + Outbox Messages
        // ❌ Rollback: Nada é persistido
    }
}
```

### Direct Publishing (Sem Outbox)

```java
@Service
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    
    @Transactional
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        Order order = Order.create(...);
        order.submit();
        
        // Persiste
        orderRepository.save(order);
        
        // Registra eventos para publicação APÓS commit
        List<DomainEvent> events = order.pullDomainEvents();
        events.forEach(eventPublisher::register);
        
        return Result.ok(order.id());
    }
}

// Transaction listener publica após commit
@Component
public class DomainEventPublisherListener {
    
    private final MessagePublisher messagePublisher;
    private final DomainEventPublisher eventPublisher;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvents(TransactionCommittedEvent event) {
        List<DomainEvent> events = eventPublisher.pullRegisteredEvents();
        
        events.forEach(domainEvent -> {
            messagePublisher.publish(Message.builder()
                .topic("domain.events")
                .key(domainEvent.aggregateId())
                .payload(domainEvent)
                .build()
            );
        });
    }
}
```

---

## 📥 Consuming Events

### Event Handler

```java
@Component
public class InventoryEventHandler {
    
    private final InventoryService inventoryService;
    
    @KafkaListener(topics = "orders.events", groupId = "inventory-service")
    public void handleOrderEvent(String eventJson) {
        DomainEvent event = deserialize(eventJson);
        
        switch (event) {
            case OrderSubmitted e -> reserveStock(e);
            case OrderCancelled e -> releaseStock(e);
            default -> log.debug("Ignoring event: {}", event.eventType());
        }
    }
    
    private void reserveStock(OrderSubmitted event) {
        try {
            inventoryService.reserveForOrder(
                event.orderId(),
                event.items()
            );
            
            log.info("Stock reserved for order: {}", event.orderId());
            
        } catch (Exception e) {
            log.error("Failed to reserve stock for order: {}", event.orderId(), e);
            // Implementar compensação ou retry
        }
    }
}
```

### Event Handler com Idempotência

```java
@Component
public class IdempotentEmailEventHandler {
    
    private final EmailService emailService;
    private final ProcessedEventRepository processedEventRepository;
    
    @KafkaListener(topics = "orders.events")
    @Transactional
    public void handleOrderEvent(DomainEvent event) {
        // 1. Verifica se já foi processado
        if (processedEventRepository.exists(event.eventId())) {
            log.debug("Event already processed: {}", event.eventId());
            return;  // Skip
        }
        
        // 2. Processa
        if (event instanceof OrderSubmitted orderSubmitted) {
            emailService.sendOrderConfirmation(orderSubmitted);
        }
        
        // 3. Marca como processado (mesma transação)
        processedEventRepository.save(new ProcessedEvent(
            event.eventId(),
            event.eventType(),
            Instant.now()
        ));
    }
}
```

---

## 🔄 Event-Driven Sagas

### Saga Orchestration

```java
@Component
public class OrderFulfillmentSaga {
    
    private final SagaRepository sagaRepository;
    private final CommandBus commandBus;
    
    @KafkaListener(topics = "orders.events")
    public void handleOrderEvent(DomainEvent event) {
        switch (event) {
            case OrderSubmitted e -> startSaga(e);
            case PaymentCompleted e -> continueAfterPayment(e);
            case PaymentFailed e -> compensateOrder(e);
            default -> {}
        }
    }
    
    private void startSaga(OrderSubmitted event) {
        // 1. Cria saga
        OrderFulfillmentSaga saga = new OrderFulfillmentSaga(
            event.orderId(),
            SagaStatus.STARTED
        );
        sagaRepository.save(saga);
        
        // 2. Envia comando para Payment Service
        commandBus.send(new ProcessPayment(
            event.orderId(),
            event.totalAmount()
        ));
    }
    
    private void continueAfterPayment(PaymentCompleted event) {
        // 3. Atualiza saga
        OrderFulfillmentSaga saga = sagaRepository.findByOrderId(event.orderId());
        saga.markPaymentCompleted();
        sagaRepository.save(saga);
        
        // 4. Próximo passo: Reserve Inventory
        commandBus.send(new ReserveInventory(
            event.orderId()
        ));
    }
    
    private void compensateOrder(PaymentFailed event) {
        // Compensação: Cancela order
        commandBus.send(new CancelOrder(
            event.orderId(),
            "Payment failed"
        ));
    }
}
```

---

## 🔍 Event Sourcing (Básico)

### Event-Sourced Aggregate

```java
public class Account extends AggregateRoot<AccountId> {
    
    private Money balance;
    private List<Transaction> transactions;
    
    // Construtor vazio para reconstitution
    protected Account() {
        super();
    }
    
    // Factory que gera evento
    public static Account open(AccountId id, Money initialDeposit, AuditStamp audit) {
        Account account = new Account();
        account.apply(new AccountOpened(
            UUID.randomUUID().toString(),
            id.value(),
            initialDeposit.value(),
            Instant.now()
        ));
        return account;
    }
    
    // Comando que gera evento
    public Result<Void> deposit(Money amount, AuditStamp audit) {
        if (amount.isNegative()) {
            return Result.fail(Problem.of(
                "ACCOUNT.INVALID_AMOUNT",
                "Deposit amount must be positive"
            ));
        }
        
        apply(new MoneyDeposited(
            UUID.randomUUID().toString(),
            id().value(),
            amount.value(),
            Instant.now()
        ));
        
        return Result.ok();
    }
    
    // Apply event (muta estado)
    private void apply(DomainEvent event) {
        switch (event) {
            case AccountOpened e -> {
                this.id = AccountId.of(e.accountId());
                this.balance = Money.of(e.initialDeposit());
                this.transactions = new ArrayList<>();
            }
            case MoneyDeposited e -> {
                this.balance = this.balance.add(Money.of(e.amount()));
                this.transactions.add(new Transaction(TransactionType.DEPOSIT, Money.of(e.amount())));
            }
            case MoneyWithdrawn e -> {
                this.balance = this.balance.subtract(Money.of(e.amount()));
                this.transactions.add(new Transaction(TransactionType.WITHDRAWAL, Money.of(e.amount())));
            }
        }
        
        // Registra evento
        recordEvent(event);
    }
    
    // Reconstitui estado a partir de eventos
    public static Account fromEvents(List<DomainEvent> events) {
        Account account = new Account();
        events.forEach(account::apply);
        return account;
    }
}
```

---

## 📊 Event Store

```java
@Repository
public class EventStoreRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    public void saveEvents(String aggregateId, List<DomainEvent> events, long expectedVersion) {
        events.forEach(event -> {
            String eventData = serialize(event);
            
            jdbcTemplate.update(
                """
                INSERT INTO event_store (
                    event_id, aggregate_id, event_type, event_data, 
                    version, occurred_at
                ) VALUES (?, ?, ?, ?::jsonb, ?, ?)
                """,
                event.eventId(),
                aggregateId,
                event.eventType(),
                eventData,
                expectedVersion + 1,
                event.occurredAt()
            );
        });
    }
    
    public List<DomainEvent> loadEvents(String aggregateId) {
        return jdbcTemplate.query(
            """
            SELECT event_id, event_type, event_data, occurred_at
            FROM event_store
            WHERE aggregate_id = ?
            ORDER BY version ASC
            """,
            (rs, rowNum) -> deserialize(
                rs.getString("event_type"),
                rs.getString("event_data")
            ),
            aggregateId
        );
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Nomeie eventos no passado
OrderCreated, OrderSubmitted, PaymentCompleted

// ✅ Eventos são imutáveis
public record OrderCreated(...) implements DomainEvent {}  // ✅ Record

// ✅ Eventos contêm dados suficientes
new OrderCreated(
    eventId,
    orderId,
    customerId,
    totalAmount,    // ✅ Dados relevantes
    itemCount       // ✅ Evita consultas futuras
)

// ✅ Use Outbox Pattern para garantias de entrega
@Transactional
public void createOrder() {
    orderRepository.save(order);
    outboxPublisher.publish(event);  // ✅ Atômico
}

// ✅ Implemente idempotência nos handlers
if (processedEvents.contains(event.eventId())) return;
```

### ❌ DON'T

```java
// ❌ NÃO nomeie no imperativo
CreateOrder, SubmitOrder  // ❌ Comandos, não eventos

// ❌ NÃO mute eventos
event.setOccurredAt(Instant.now());  // ❌ Imutável!

// ❌ NÃO publique direto sem garantias
kafkaTemplate.send(event);  // ❌ Sem transação!

// ❌ NÃO tenha lógica de negócio no handler
@KafkaListener
public void handle(OrderCreated event) {
    order.calculateTotal();  // ❌ Isso é do aggregate!
}
```

---

## Ver Também

- [Transactional Outbox](../api-reference/app-outbox.md)
- [Messaging Patterns](messaging.md)
- [DDD Building Blocks](../api-reference/kernel-ddd.md)
- [Observability](observability.md)
