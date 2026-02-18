# API Reference: Saga Pattern

## Visão Geral

`commons-app-saga` implementa o padrão Saga para transações distribuídas, coordenando múltiplos serviços com compensação automática em caso de falha.

**Quando usar:**
- Transações que envolvem múltiplos microservices
- Operações de longa duração (long-running transactions)
- Garantir consistência eventual entre serviços
- Compensação automática de operações parciais
- Workflows de negócio complexos

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-saga</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### Saga

Representa uma transação distribuída.

```java
public class Saga<T> {
    
    private final SagaId id;
    private final String name;
    private final List<SagaStep<T>> steps;
    private final SagaState state;
    private final T context;
    
    /**
     * Executa saga completa.
     */
    public Result<T> execute();
    
    /**
     * Compensa saga até o último passo executado.
     */
    public Result<Void> compensate();
    
    /**
     * Adiciona step à saga.
     */
    public Saga<T> addStep(SagaStep<T> step);
}
```

### SagaStep

Define um passo da saga com compensação.

```java
public interface SagaStep<T> {
    
    /**
     * Nome do step.
     */
    String name();
    
    /**
     * Executa ação principal.
     */
    Result<T> execute(T context);
    
    /**
     * Compensa ação (rollback).
     */
    Result<Void> compensate(T context);
    
    /**
     * Determina se compensação é necessária.
     */
    default boolean requiresCompensation() {
        return true;
    }
}
```

### SagaOrchestrator

Coordena execução de sagas.

```java
public interface SagaOrchestrator {
    
    /**
     * Inicia execução de saga.
     */
    <T> Result<T> execute(Saga<T> saga);
    
    /**
     * Retoma saga após falha.
     */
    <T> Result<T> resume(SagaId sagaId);
    
    /**
     * Compensa saga manualmente.
     */
    Result<Void> compensate(SagaId sagaId);
    
    /**
     * Busca estado de saga.
     */
    Optional<SagaExecution> getExecution(SagaId sagaId);
}
```

---

## 💡 Orchestration-Based Saga

### Order Creation Saga

```java
public class CreateOrderSaga {
    
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    
    public Saga<OrderContext> build(CreateOrderCommand command) {
        return Saga.<OrderContext>builder()
            .name("create-order")
            .context(new OrderContext(command))
            
            // Step 1: Criar pedido
            .addStep(new SagaStep<>() {
                @Override
                public String name() {
                    return "create-order";
                }
                
                @Override
                public Result<OrderContext> execute(OrderContext ctx) {
                    return orderService.createOrder(ctx.command())
                        .map(orderId -> ctx.withOrderId(orderId));
                }
                
                @Override
                public Result<Void> compensate(OrderContext ctx) {
                    if (ctx.orderId() == null) {
                        return Result.ok();
                    }
                    return orderService.cancelOrder(ctx.orderId());
                }
            })
            
            // Step 2: Reservar inventário
            .addStep(new SagaStep<>() {
                @Override
                public String name() {
                    return "reserve-inventory";
                }
                
                @Override
                public Result<OrderContext> execute(OrderContext ctx) {
                    return inventoryService.reserve(
                        ctx.command().items()
                    ).map(reservationId -> 
                        ctx.withReservationId(reservationId)
                    );
                }
                
                @Override
                public Result<Void> compensate(OrderContext ctx) {
                    if (ctx.reservationId() == null) {
                        return Result.ok();
                    }
                    return inventoryService.cancelReservation(
                        ctx.reservationId()
                    );
                }
            })
            
            // Step 3: Processar pagamento
            .addStep(new SagaStep<>() {
                @Override
                public String name() {
                    return "process-payment";
                }
                
                @Override
                public Result<OrderContext> execute(OrderContext ctx) {
                    return paymentService.charge(
                        ctx.command().customerId(),
                        ctx.command().total()
                    ).map(paymentId -> 
                        ctx.withPaymentId(paymentId)
                    );
                }
                
                @Override
                public Result<Void> compensate(OrderContext ctx) {
                    if (ctx.paymentId() == null) {
                        return Result.ok();
                    }
                    return paymentService.refund(ctx.paymentId());
                }
            })
            
            // Step 4: Agendar envio
            .addStep(new SagaStep<>() {
                @Override
                public String name() {
                    return "schedule-shipping";
                }
                
                @Override
                public Result<OrderContext> execute(OrderContext ctx) {
                    return shippingService.schedule(
                        ctx.orderId(),
                        ctx.command().shippingAddress()
                    ).map(shippingId -> 
                        ctx.withShippingId(shippingId)
                    );
                }
                
                @Override
                public Result<Void> compensate(OrderContext ctx) {
                    if (ctx.shippingId() == null) {
                        return Result.ok();
                    }
                    return shippingService.cancel(ctx.shippingId());
                }
            })
            
            .build();
    }
}
```

### Saga Context

```java
public record OrderContext(
    CreateOrderCommand command,
    OrderId orderId,
    ReservationId reservationId,
    PaymentId paymentId,
    ShippingId shippingId
) {
    public OrderContext(CreateOrderCommand command) {
        this(command, null, null, null, null);
    }
    
    public OrderContext withOrderId(OrderId orderId) {
        return new OrderContext(command, orderId, reservationId, paymentId, shippingId);
    }
    
    public OrderContext withReservationId(ReservationId reservationId) {
        return new OrderContext(command, orderId, reservationId, paymentId, shippingId);
    }
    
    public OrderContext withPaymentId(PaymentId paymentId) {
        return new OrderContext(command, orderId, reservationId, paymentId, shippingId);
    }
    
    public OrderContext withShippingId(ShippingId shippingId) {
        return new OrderContext(command, orderId, reservationId, paymentId, shippingId);
    }
}
```

### Saga Execution

```java
@Service
public class OrderApplicationService {
    
    private final SagaOrchestrator sagaOrchestrator;
    private final CreateOrderSaga createOrderSaga;
    
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        // Construir saga
        Saga<OrderContext> saga = createOrderSaga.build(command);
        
        // Executar
        Result<OrderContext> result = sagaOrchestrator.execute(saga);
        
        // Retornar order ID
        return result.map(OrderContext::orderId);
    }
}
```

---

## 🌐 Choreography-Based Saga

### Event-Driven Saga

```java
// Event 1: Order Created
@Service
public class OrderCreatedEventHandler {
    
    private final InventoryService inventoryService;
    private final EventPublisher eventPublisher;
    
    @EventListener
    public void handle(OrderCreatedEvent event) {
        // Reservar inventário
        Result<ReservationId> result = inventoryService.reserve(
            event.items()
        );
        
        if (result.isOk()) {
            // Sucesso - publicar próximo evento
            eventPublisher.publish(new InventoryReservedEvent(
                event.orderId(),
                result.getOrThrow()
            ));
        } else {
            // Falha - publicar evento de compensação
            eventPublisher.publish(new OrderCreationFailedEvent(
                event.orderId(),
                "inventory-reservation-failed",
                result.problemOrNull().detail()
            ));
        }
    }
}

// Event 2: Inventory Reserved
@Service
public class InventoryReservedEventHandler {
    
    private final PaymentService paymentService;
    private final EventPublisher eventPublisher;
    
    @EventListener
    public void handle(InventoryReservedEvent event) {
        // Processar pagamento
        Result<PaymentId> result = paymentService.charge(
            event.customerId(),
            event.total()
        );
        
        if (result.isOk()) {
            // Sucesso
            eventPublisher.publish(new PaymentProcessedEvent(
                event.orderId(),
                result.getOrThrow()
            ));
        } else {
            // Falha - compensar inventário
            eventPublisher.publish(new InventoryCompensationRequestedEvent(
                event.orderId(),
                event.reservationId()
            ));
        }
    }
}

// Compensação: Cancel Inventory Reservation
@Service
public class InventoryCompensationHandler {
    
    private final InventoryService inventoryService;
    private final EventPublisher eventPublisher;
    
    @EventListener
    public void handle(InventoryCompensationRequestedEvent event) {
        Result<Void> result = inventoryService.cancelReservation(
            event.reservationId()
        );
        
        if (result.isOk()) {
            eventPublisher.publish(new InventoryCompensatedEvent(
                event.orderId()
            ));
        } else {
            // Log erro crítico - compensação falhou!
            log.error("Inventory compensation failed")
                .field("orderId", event.orderId())
                .field("reservationId", event.reservationId())
                .log();
        }
    }
}
```

---

## 📊 Saga State Management

### Saga Execution State

```java
public class SagaExecution {
    
    private final SagaId sagaId;
    private final String sagaName;
    private final SagaStatus status;
    private final int currentStep;
    private final Map<String, StepExecution> stepExecutions;
    private final Instant startedAt;
    private final Instant completedAt;
    
    public boolean isCompleted() {
        return status == SagaStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == SagaStatus.FAILED;
    }
    
    public boolean isCompensating() {
        return status == SagaStatus.COMPENSATING;
    }
    
    public List<StepExecution> getCompletedSteps() {
        return stepExecutions.values().stream()
            .filter(step -> step.status() == StepStatus.COMPLETED)
            .sorted(Comparator.comparing(StepExecution::executedAt))
            .toList();
    }
}

public enum SagaStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED
}
```

### Saga Repository

```java
public interface SagaExecutionRepository {
    
    /**
     * Salva estado da saga.
     */
    Result<Void> save(SagaExecution execution);
    
    /**
     * Atualiza step execution.
     */
    Result<Void> updateStep(
        SagaId sagaId,
        String stepName,
        StepExecution execution
    );
    
    /**
     * Busca saga por ID.
     */
    Optional<SagaExecution> findById(SagaId sagaId);
    
    /**
     * Lista sagas em execução.
     */
    List<SagaExecution> findRunning();
    
    /**
     * Lista sagas falhadas.
     */
    List<SagaExecution> findFailed(Duration olderThan);
}
```

---

## 🔄 Saga Orchestrator Implementation

### Default Orchestrator

```java
@Component
public class DefaultSagaOrchestrator implements SagaOrchestrator {
    
    private final SagaExecutionRepository executionRepository;
    private final StructuredLog log;
    private final MetricsFacade metrics;
    
    @Override
    public <T> Result<T> execute(Saga<T> saga) {
        Instant start = Instant.now();
        
        // Criar execution
        SagaExecution execution = SagaExecution.create(saga);
        executionRepository.save(execution);
        
        log.info("Starting saga")
            .field("sagaId", saga.id().value())
            .field("sagaName", saga.name())
            .field("steps", saga.steps().size())
            .log();
        
        T context = saga.context();
        int completedSteps = 0;
        
        try {
            // Executar steps sequencialmente
            for (SagaStep<T> step : saga.steps()) {
                log.info("Executing saga step")
                    .field("sagaId", saga.id().value())
                    .field("stepName", step.name())
                    .log();
                
                Instant stepStart = Instant.now();
                Result<T> stepResult = step.execute(context);
                Duration stepDuration = Duration.between(stepStart, Instant.now());
                
                if (stepResult.isFail()) {
                    // Step falhou - compensar
                    log.warn("Saga step failed")
                        .field("sagaId", saga.id().value())
                        .field("stepName", step.name())
                        .field("error", stepResult.problemOrNull().detail())
                        .log();
                    
                    // Atualizar estado
                    executionRepository.updateStep(
                        saga.id(),
                        step.name(),
                        StepExecution.failed(step.name(), stepResult.problemOrNull())
                    );
                    
                    // Compensar steps completados
                    Result<Void> compensationResult = compensateSteps(
                        saga,
                        completedSteps,
                        context
                    );
                    
                    metrics.recordTimer(
                        "saga.duration",
                        Duration.between(start, Instant.now()),
                        "sagaName", saga.name(),
                        "status", "failed"
                    );
                    
                    return Result.fail(stepResult.problemOrNull());
                }
                
                // Step sucesso
                context = stepResult.getOrThrow();
                completedSteps++;
                
                executionRepository.updateStep(
                    saga.id(),
                    step.name(),
                    StepExecution.completed(step.name(), stepDuration)
                );
                
                metrics.recordTimer(
                    "saga.step.duration",
                    stepDuration,
                    "sagaName", saga.name(),
                    "stepName", step.name()
                );
            }
            
            // Saga completa
            execution = execution.complete();
            executionRepository.save(execution);
            
            log.info("Saga completed")
                .field("sagaId", saga.id().value())
                .field("duration", Duration.between(start, Instant.now()))
                .log();
            
            metrics.recordTimer(
                "saga.duration",
                Duration.between(start, Instant.now()),
                "sagaName", saga.name(),
                "status", "completed"
            );
            
            return Result.ok(context);
            
        } catch (Exception e) {
            log.error("Saga execution failed", e).log();
            
            compensateSteps(saga, completedSteps, context);
            
            return Result.fail(Problem.of(
                "SAGA.EXECUTION_FAILED",
                e.getMessage()
            ));
        }
    }
    
    private <T> Result<Void> compensateSteps(
        Saga<T> saga,
        int completedSteps,
        T context
    ) {
        log.info("Compensating saga")
            .field("sagaId", saga.id().value())
            .field("stepsToCompensate", completedSteps)
            .log();
        
        SagaExecution execution = executionRepository
            .findById(saga.id())
            .orElseThrow();
        
        execution = execution.startCompensation();
        executionRepository.save(execution);
        
        // Compensar em ordem reversa
        List<SagaStep<T>> steps = saga.steps();
        for (int i = completedSteps - 1; i >= 0; i--) {
            SagaStep<T> step = steps.get(i);
            
            if (!step.requiresCompensation()) {
                continue;
            }
            
            log.info("Compensating saga step")
                .field("sagaId", saga.id().value())
                .field("stepName", step.name())
                .log();
            
            Result<Void> compensationResult = step.compensate(context);
            
            if (compensationResult.isFail()) {
                log.error("Step compensation failed")
                    .field("sagaId", saga.id().value())
                    .field("stepName", step.name())
                    .field("error", compensationResult.problemOrNull().detail())
                    .log();
                
                execution = execution.failCompensation();
                executionRepository.save(execution);
                
                return compensationResult;
            }
        }
        
        execution = execution.completeCompensation();
        executionRepository.save(execution);
        
        log.info("Saga compensation completed")
            .field("sagaId", saga.id().value())
            .log();
        
        return Result.ok();
    }
}
```

---

## 🧪 Testing

### Saga Tests

```java
class CreateOrderSagaTest {
    
    private OrderService orderService;
    private PaymentService paymentService;
    private InventoryService inventoryService;
    private ShippingService shippingService;
    private SagaOrchestrator orchestrator;
    private CreateOrderSaga createOrderSaga;
    
    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        paymentService = mock(PaymentService.class);
        inventoryService = mock(InventoryService.class);
        shippingService = mock(ShippingService.class);
        
        orchestrator = new DefaultSagaOrchestrator(
            mock(SagaExecutionRepository.class),
            StructuredLog.noop(),
            MetricsFacade.noop()
        );
        
        createOrderSaga = new CreateOrderSaga(
            orderService,
            paymentService,
            inventoryService,
            shippingService
        );
    }
    
    @Test
    void shouldCompleteSuccessfully() {
        // Given: All services succeed
        when(orderService.createOrder(any()))
            .thenReturn(Result.ok(OrderId.generate()));
        when(inventoryService.reserve(any()))
            .thenReturn(Result.ok(ReservationId.generate()));
        when(paymentService.charge(any(), any()))
            .thenReturn(Result.ok(PaymentId.generate()));
        when(shippingService.schedule(any(), any()))
            .thenReturn(Result.ok(ShippingId.generate()));
        
        // When
        Saga<OrderContext> saga = createOrderSaga.build(createCommand());
        Result<OrderContext> result = orchestrator.execute(saga);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        OrderContext context = result.getOrThrow();
        assertThat(context.orderId()).isNotNull();
        assertThat(context.paymentId()).isNotNull();
        assertThat(context.reservationId()).isNotNull();
        assertThat(context.shippingId()).isNotNull();
    }
    
    @Test
    void shouldCompensateWhenPaymentFails() {
        // Given: Payment fails
        when(orderService.createOrder(any()))
            .thenReturn(Result.ok(OrderId.generate()));
        when(inventoryService.reserve(any()))
            .thenReturn(Result.ok(ReservationId.generate()));
        when(paymentService.charge(any(), any()))
            .thenReturn(Result.fail(Problem.of("PAYMENT_FAILED", "Insufficient funds")));
        
        when(orderService.cancelOrder(any()))
            .thenReturn(Result.ok());
        when(inventoryService.cancelReservation(any()))
            .thenReturn(Result.ok());
        
        // When
        Saga<OrderContext> saga = createOrderSaga.build(createCommand());
        Result<OrderContext> result = orchestrator.execute(saga);
        
        // Then: Saga fails
        assertThat(result.isFail()).isTrue();
        
        // Compensations called in reverse order
        verify(inventoryService).cancelReservation(any());
        verify(orderService).cancelOrder(any());
        verify(shippingService, never()).schedule(any(), any());
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Torne compensações idempotentes
@Override
public Result<Void> compensate(OrderContext ctx) {
    if (ctx.paymentId() == null) {
        return Result.ok();  // Já compensado ou não executado
    }
    return paymentService.refund(ctx.paymentId());
}

// ✅ Persista estado da saga
executionRepository.save(execution);

// ✅ Use timeouts em steps
@Override
public Result<T> execute(T context) {
    return Mono.fromCallable(() -> doWork(context))
        .timeout(Duration.ofSeconds(30))
        .block();
}

// ✅ Monitore sagas falhadas
metrics.incrementCounter("saga.failed", "sagaName", name);

// ✅ Implemente retry em compensações
@Retry(maxAttempts = 3)
public Result<Void> compensate(T context) { ... }
```

### ❌ DON'T

```java
// ❌ NÃO use sagas para transações simples
// Use transações locais ACID quando possível

// ❌ NÃO ignore falhas de compensação
if (compensationResult.isFail()) {
    // ❌ NUNCA ignorar! Requer investigação manual
}

// ❌ NÃO faça compensações não-idempotentes
public Result<Void> compensate(T ctx) {
    payment.refund();  // ❌ Vai duplicar refund!
}

// ❌ NÃO use para operações leves
Saga saga = Saga.builder()
    .addStep(updateUserName)  // ❌ Transação local seria melhor
    .build();

// ❌ NÃO deixe sagas órfãs
// Monitore e resolva sagas em estado COMPENSATING por muito tempo
```

---

## Ver Também

- [Domain Events Guide](../guides/domain-events.md) - Event-driven sagas
- [Outbox Pattern](app-outbox.md) - Reliable event publishing
- [Resilience Guide](../guides/resilience.md) - Retry policies
- [Observability Guide](../guides/observability.md) - Saga monitoring
