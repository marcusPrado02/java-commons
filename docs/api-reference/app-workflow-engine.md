# API Reference: Workflow Engine

## Visão Geral

`commons-app-workflow-engine` fornece orquestração de workflows para processos de negócio complexos com state machines e compensação.

**Quando usar:**
- Processos de negócio com múltiplos steps
- State machines complexas (aprovação, pedido, onboarding)
- Processos de longa duração com persistência
- Workflows com decisões condicionais
- Compensação automática de erros

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-workflow-engine</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### Workflow

Representa um processo de negócio.

```java
public class Workflow<T> {
    
    private final WorkflowId id;
    private final String name;
    private final WorkflowState state;
    private final List<WorkflowStep<T>> steps;
    private final T context;
    
    /**
     * Executa workflow até completar ou falhar.
     */
    public Result<T> execute();
    
    /**
     * Retoma workflow do último checkpoint.
     */
    public Result<T> resume();
    
    /**
     * Pausa workflow (checkpointing).
     */
    public Result<Void> pause();
    
    /**
     * Cancela e compensa workflow.
     */
    public Result<Void> cancel();
}
```

### WorkflowStep

Define um passo do workflow.

```java
public interface WorkflowStep<T> {
    
    /**
     * Nome único do step.
     */
    String name();
    
    /**
     * Executa step.
     */
    Result<StepResult<T>> execute(T context);
    
    /**
     * Compensa step (rollback).
     */
    Result<Void> compensate(T context);
    
    /**
     * Timeout do step (opcional).
     */
    default Optional<Duration> timeout() {
        return Optional.empty();
    }
    
    /**
     * Retry policy (opcional).
     */
    default Optional<RetryPolicy> retryPolicy() {
        return Optional.empty();
    }
}
```

### StepResult

Resultado da execução de um step.

```java
public sealed interface StepResult<T> {
    
    /**
     * Step completou - continuar para próximo.
     */
    record Success<T>(T context) implements StepResult<T> {}
    
    /**
     * Step aguardando evento externo - pausar.
     */
    record Waiting<T>(
        T context,
        String eventType,
        Duration timeout
    ) implements StepResult<T> {}
    
    /**
     * Pular para step específico.
     */
    record Jump<T>(T context, String targetStep) implements StepResult<T> {}
    
    /**
     * Terminar workflow com sucesso.
     */
    record Complete<T>(T context) implements StepResult<T> {}
}
```

---

## 🔄 Order Approval Workflow

### Workflow Definition

```java
@Component
public class OrderApprovalWorkflow {
    
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    
    public Workflow<OrderContext> create(OrderId orderId) {
        return Workflow.<OrderContext>builder()
            .name("order-approval")
            .context(new OrderContext(orderId))
            
            // Step 1: Load order
            .addStep(new LoadOrderStep(orderRepository))
            
            // Step 2: Check inventory
            .addStep(new CheckInventoryStep(inventoryService))
            
            // Step 3: Validate payment method
            .addStep(new ValidatePaymentStep(paymentService))
            
            // Step 4: Wait for manager approval (if > $1000)
            .addStep(new ManagerApprovalStep(notificationService))
            
            // Step 5: Reserve inventory
            .addStep(new ReserveInventoryStep(inventoryService))
            
            // Step 6: Process payment
            .addStep(new ProcessPaymentStep(paymentService))
            
            // Step 7: Update order status
            .addStep(new UpdateOrderStatusStep(orderRepository))
            
            .build();
    }
}
```

### Step Implementations

```java
// Step 1: Load Order
public class LoadOrderStep implements WorkflowStep<OrderContext> {
    
    private final OrderRepository orderRepository;
    
    @Override
    public String name() {
        return "load-order";
    }
    
    @Override
    public Result<StepResult<OrderContext>> execute(OrderContext ctx) {
        return orderRepository.findById(ctx.orderId())
            .map(order -> {
                OrderContext updated = ctx.withOrder(order);
                return Result.ok(new StepResult.Success<>(updated));
            })
            .orElse(Result.fail(Problem.of(
                "ORDER.NOT_FOUND",
                "Order not found: " + ctx.orderId()
            )));
    }
    
    @Override
    public Result<Void> compensate(OrderContext ctx) {
        // No compensation needed
        return Result.ok();
    }
}

// Step 2: Check Inventory
public class CheckInventoryStep implements WorkflowStep<OrderContext> {
    
    private final InventoryService inventoryService;
    
    @Override
    public String name() {
        return "check-inventory";
    }
    
    @Override
    public Result<StepResult<OrderContext>> execute(OrderContext ctx) {
        Order order = ctx.order();
        
        return inventoryService.checkAvailability(order.items())
            .map(available -> {
                if (available) {
                    return new StepResult.Success<>(ctx);
                } else {
                    // Cancel workflow - out of stock
                    return new StepResult.Complete<>(
                        ctx.withCancellationReason("out_of_stock")
                    );
                }
            });
    }
    
    @Override
    public Result<Void> compensate(OrderContext ctx) {
        return Result.ok();
    }
}

// Step 4: Manager Approval (conditional wait)
public class ManagerApprovalStep implements WorkflowStep<OrderContext> {
    
    private final NotificationService notificationService;
    
    @Override
    public String name() {
        return "manager-approval";
    }
    
    @Override
    public Result<StepResult<OrderContext>> execute(OrderContext ctx) {
        Order order = ctx.order();
        
        // If order > $1000, requires manager approval
        if (order.total().amount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            
            // Check if already approved
            if (ctx.managerApproval() != null) {
                if (ctx.managerApproval().approved()) {
                    return Result.ok(new StepResult.Success<>(ctx));
                } else {
                    // Rejected - cancel workflow
                    return Result.ok(new StepResult.Complete<>(
                        ctx.withCancellationReason("manager_rejected")
                    ));
                }
            }
            
            // Send approval request
            notificationService.sendApprovalRequest(
                order.id(),
                order.total()
            );
            
            // Wait for approval event
            return Result.ok(new StepResult.Waiting<>(
                ctx,
                "manager-approval-received",
                Duration.ofDays(2)
            ));
        }
        
        // Order < $1000 - skip approval
        return Result.ok(new StepResult.Success<>(ctx));
    }
    
    @Override
    public Result<Void> compensate(OrderContext ctx) {
        return Result.ok();
    }
}

// Step 5: Reserve Inventory
public class ReserveInventoryStep implements WorkflowStep<OrderContext> {
    
    private final InventoryService inventoryService;
    
    @Override
    public String name() {
        return "reserve-inventory";
    }
    
    @Override
    public Result<StepResult<OrderContext>> execute(OrderContext ctx) {
        return inventoryService.reserve(ctx.order().items())
            .map(reservationId -> {
                OrderContext updated = ctx.withReservationId(reservationId);
                return new StepResult.Success<>(updated);
            });
    }
    
    @Override
    public Result<Void> compensate(OrderContext ctx) {
        if (ctx.reservationId() == null) {
            return Result.ok();
        }
        return inventoryService.cancelReservation(ctx.reservationId());
    }
    
    @Override
    public Optional<Duration> timeout() {
        return Optional.of(Duration.ofSeconds(30));
    }
    
    @Override
    public Optional<RetryPolicy> retryPolicy() {
        return Optional.of(RetryPolicy.exponentialBackoff(3));
    }
}
```

### Workflow Context

```java
public record OrderContext(
    OrderId orderId,
    Order order,
    ManagerApproval managerApproval,
    ReservationId reservationId,
    PaymentId paymentId,
    String cancellationReason
) {
    public OrderContext(OrderId orderId) {
        this(orderId, null, null, null, null, null);
    }
    
    public OrderContext withOrder(Order order) {
        return new OrderContext(orderId, order, managerApproval, 
            reservationId, paymentId, cancellationReason);
    }
    
    public OrderContext withManagerApproval(ManagerApproval approval) {
        return new OrderContext(orderId, order, approval, 
            reservationId, paymentId, cancellationReason);
    }
    
    public OrderContext withReservationId(ReservationId reservationId) {
        return new OrderContext(orderId, order, managerApproval, 
            reservationId, paymentId, cancellationReason);
    }
    
    public OrderContext withPaymentId(PaymentId paymentId) {
        return new OrderContext(orderId, order, managerApproval, 
            reservationId, paymentId, cancellationReason);
    }
    
    public OrderContext withCancellationReason(String reason) {
        return new OrderContext(orderId, order, managerApproval, 
            reservationId, paymentId, reason);
    }
}
```

---

## 🎯 Workflow Engine

### Workflow Executor

```java
@Component
public class WorkflowEngine {
    
    private final WorkflowRepository workflowRepository;
    private final EventPublisher eventPublisher;
    private final StructuredLog log;
    private final MetricsFacade metrics;
    
    public <T> Result<T> execute(Workflow<T> workflow) {
        Instant start = Instant.now();
        
        log.info("Starting workflow")
            .field("workflowId", workflow.id().value())
            .field("workflowName", workflow.name())
            .log();
        
        // Persist workflow
        WorkflowExecution execution = WorkflowExecution.create(workflow);
        workflowRepository.save(execution);
        
        T context = workflow.context();
        
        for (WorkflowStep<T> step : workflow.steps()) {
            log.info("Executing workflow step")
                .field("workflowId", workflow.id().value())
                .field("stepName", step.name())
                .log();
            
            Instant stepStart = Instant.now();
            
            // Execute with timeout
            Result<StepResult<T>> result = executeWithTimeout(
                step,
                context,
                step.timeout().orElse(Duration.ofMinutes(5))
            );
            
            Duration stepDuration = Duration.between(stepStart, Instant.now());
            
            if (result.isFail()) {
                // Step failed - retry if policy exists
                result = retryWithPolicy(step, context, result);
            }
            
            if (result.isFail()) {
                // Still failed after retries - compensate
                log.error("Workflow step failed")
                    .field("workflowId", workflow.id().value())
                    .field("stepName", step.name())
                    .field("error", result.problemOrNull().detail())
                    .log();
                
                compensate(workflow, execution);
                
                metrics.recordTimer(
                    "workflow.duration",
                    Duration.between(start, Instant.now()),
                    "workflowName", workflow.name(),
                    "status", "failed"
                );
                
                return Result.fail(result.problemOrNull());
            }
            
            // Handle step result
            StepResult<T> stepResult = result.getOrThrow();
            
            switch (stepResult) {
                case StepResult.Success<T> success -> {
                    context = success.context();
                    execution = execution.completeStep(step.name(), stepDuration);
                    workflowRepository.save(execution);
                }
                
                case StepResult.Waiting<T> waiting -> {
                    // Pause workflow - wait for external event
                    context = waiting.context();
                    execution = execution.waitForEvent(
                        step.name(),
                        waiting.eventType(),
                        waiting.timeout()
                    );
                    workflowRepository.save(execution);
                    
                    log.info("Workflow waiting for event")
                        .field("workflowId", workflow.id().value())
                        .field("eventType", waiting.eventType())
                        .log();
                    
                    metrics.recordTimer(
                        "workflow.duration",
                        Duration.between(start, Instant.now()),
                        "workflowName", workflow.name(),
                        "status", "waiting"
                    );
                    
                    return Result.ok(context);
                }
                
                case StepResult.Jump<T> jump -> {
                    // Jump to specific step
                    context = jump.context();
                    // Find target step and continue from there
                    // (implementation omitted for brevity)
                }
                
                case StepResult.Complete<T> complete -> {
                    // Early completion
                    context = complete.context();
                    execution = execution.complete();
                    workflowRepository.save(execution);
                    
                    metrics.recordTimer(
                        "workflow.duration",
                        Duration.between(start, Instant.now()),
                        "workflowName", workflow.name(),
                        "status", "completed"
                    );
                    
                    return Result.ok(context);
                }
            }
            
            metrics.recordTimer(
                "workflow.step.duration",
                stepDuration,
                "workflowName", workflow.name(),
                "stepName", step.name()
            );
        }
        
        // Workflow completed
        execution = execution.complete();
        workflowRepository.save(execution);
        
        log.info("Workflow completed")
            .field("workflowId", workflow.id().value())
            .field("duration", Duration.between(start, Instant.now()))
            .log();
        
        metrics.recordTimer(
            "workflow.duration",
            Duration.between(start, Instant.now()),
            "workflowName", workflow.name(),
            "status", "completed"
        );
        
        return Result.ok(context);
    }
    
    private <T> Result<StepResult<T>> executeWithTimeout(
        WorkflowStep<T> step,
        T context,
        Duration timeout
    ) {
        return Mono.fromCallable(() -> step.execute(context))
            .timeout(timeout)
            .onErrorResume(TimeoutException.class, e ->
                Mono.just(Result.fail(Problem.of(
                    "WORKFLOW.STEP_TIMEOUT",
                    "Step timed out: " + step.name()
                )))
            )
            .block();
    }
    
    private <T> Result<StepResult<T>> retryWithPolicy(
        WorkflowStep<T> step,
        T context,
        Result<StepResult<T>> failedResult
    ) {
        Optional<RetryPolicy> policyOpt = step.retryPolicy();
        
        if (policyOpt.isEmpty()) {
            return failedResult;
        }
        
        RetryPolicy policy = policyOpt.get();
        
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            log.info("Retrying workflow step")
                .field("stepName", step.name())
                .field("attempt", attempt)
                .log();
            
            try {
                Thread.sleep(policy.backoff().toMillis() * attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return failedResult;
            }
            
            Result<StepResult<T>> result = step.execute(context);
            
            if (result.isOk()) {
                return result;
            }
        }
        
        return failedResult;
    }
}
```

---

## 📨 Event-Based Resume

### Event Handler

```java
@Service
public class WorkflowEventHandler {
    
    private final WorkflowRepository workflowRepository;
    private final WorkflowEngine engine;
    
    @EventListener
    public void handleManagerApproval(ManagerApprovalReceivedEvent event) {
        // Find waiting workflows
        List<WorkflowExecution> waiting = workflowRepository
            .findWaitingForEvent("manager-approval-received");
        
        for (WorkflowExecution execution : waiting) {
            // Update context with approval
            OrderContext context = loadContext(execution);
            OrderContext updated = context.withManagerApproval(
                new ManagerApproval(
                    event.approved(),
                    event.reason()
                )
            );
            
            // Resume workflow
            Workflow<OrderContext> workflow = reconstruct(execution, updated);
            engine.execute(workflow);
        }
    }
}
```

### Resume API

```java
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    
    private final WorkflowEngine engine;
    private final WorkflowRepository workflowRepository;
    
    @PostMapping("/{workflowId}/resume")
    public ResponseEntity<?> resume(
        @PathVariable WorkflowId workflowId,
        @RequestBody Map<String, Object> event
    ) {
        // Find workflow
        Optional<WorkflowExecution> executionOpt = 
            workflowRepository.findById(workflowId);
        
        if (executionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        WorkflowExecution execution = executionOpt.get();
        
        // Reconstruct workflow with event data
        Workflow<?> workflow = reconstructWorkflow(execution, event);
        
        // Resume execution
        Result<?> result = engine.execute(workflow);
        
        return result
            .map(ctx -> ResponseEntity.ok().build())
            .getOrElse(problem -> ResponseEntity.badRequest().body(problem));
    }
}
```

---

## 🧪 Testing

### Workflow Tests

```java
class OrderApprovalWorkflowTest {
    
    private OrderRepository orderRepository;
    private InventoryService inventoryService;
    private PaymentService paymentService;
    private WorkflowEngine engine;
    
    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        inventoryService = mock(InventoryService.class);
        paymentService = mock(PaymentService.class);
        
        engine = new WorkflowEngine(
            mock(WorkflowRepository.class),
            mock(EventPublisher.class),
            StructuredLog.noop(),
            MetricsFacade.noop()
        );
    }
    
    @Test
    void shouldCompleteWorkflowWhenOrderUnder1000() {
        // Given: Order < $1000
        Order order = Order.create(
            CustomerId.generate(),
            BigDecimal.valueOf(500)
        );
        
        when(orderRepository.findById(any()))
            .thenReturn(Optional.of(order));
        when(inventoryService.checkAvailability(any()))
            .thenReturn(Result.ok(true));
        when(inventoryService.reserve(any()))
            .thenReturn(Result.ok(ReservationId.generate()));
        when(paymentService.charge(any(), any()))
            .thenReturn(Result.ok(PaymentId.generate()));
        
        // When
        OrderApprovalWorkflow workflowDef = new OrderApprovalWorkflow(
            orderRepository,
            paymentService,
            inventoryService,
            mock(NotificationService.class)
        );
        
        Workflow<OrderContext> workflow = workflowDef.create(order.id());
        Result<OrderContext> result = engine.execute(workflow);
        
        // Then: Completed without waiting for approval
        assertThat(result.isOk()).isTrue();
        
        OrderContext context = result.getOrThrow();
        assertThat(context.reservationId()).isNotNull();
        assertThat(context.paymentId()).isNotNull();
        assertThat(context.managerApproval()).isNull();
    }
    
    @Test
    void shouldWaitForApprovalWhenOrderOver1000() {
        // Given: Order > $1000
        Order order = Order.create(
            CustomerId.generate(),
            BigDecimal.valueOf(1500)
        );
        
        when(orderRepository.findById(any()))
            .thenReturn(Optional.of(order));
        when(inventoryService.checkAvailability(any()))
            .thenReturn(Result.ok(true));
        
        // When
        OrderApprovalWorkflow workflowDef = new OrderApprovalWorkflow(
            orderRepository,
            paymentService,
            inventoryService,
            mock(NotificationService.class)
        );
        
        Workflow<OrderContext> workflow = workflowDef.create(order.id());
        Result<OrderContext> result = engine.execute(workflow);
        
        // Then: Workflow paused waiting for approval
        assertThat(result.isOk()).isTrue();
        
        OrderContext context = result.getOrThrow();
        assertThat(context.managerApproval()).isNull();
        assertThat(context.reservationId()).isNull();  // Not reserved yet
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Persista estado do workflow
workflowRepository.save(execution);

// ✅ Implemente timeouts em steps
@Override
public Optional<Duration> timeout() {
    return Optional.of(Duration.ofSeconds(30));
}

// ✅ Use retry policies para steps transientes
@Override
public Optional<RetryPolicy> retryPolicy() {
    return Optional.of(RetryPolicy.exponentialBackoff(3));
}

// ✅ Torne compensações idempotentes
@Override
public Result<Void> compensate(OrderContext ctx) {
    if (ctx.paymentId() == null) {
        return Result.ok();  // Já compensado
    }
    return paymentService.refund(ctx.paymentId());
}

// ✅ Monitore workflows em waiting state
List<WorkflowExecution> stuck = workflowRepository
    .findWaitingLongerThan(Duration.ofDays(7));
```

### ❌ DON'T

```java
// ❌ NÃO armazene estado mutável no step
public class BadStep implements WorkflowStep<Context> {
    private int counter = 0;  // ❌ Perdido em restart!
}

// ❌ NÃO ignore falhas de compensação
@Override
public Result<Void> compensate(Context ctx) {
    try {
        cleanup();
        return Result.ok();
    } catch (Exception e) {
        return Result.ok();  // ❌ NUNCA ignorar!
    }
}

// ❌ NÃO faça chamadas síncronas sem timeout
@Override
public Result<StepResult<T>> execute(T ctx) {
    externalService.call();  // ❌ Pode travar forever!
}

// ❌ NÃO use para processos síncronos simples
// Use workflows apenas para processos complexos e/ou assíncronos
```

---

## Ver Também

- [Saga Pattern](app-saga.md) - Distributed transactions
- [Domain Events](../guides/domain-events.md) - Event-driven workflows
- [Scheduler](app-scheduler.md) - Scheduled workflows
- [Observability](../guides/observability.md) - Workflow monitoring
