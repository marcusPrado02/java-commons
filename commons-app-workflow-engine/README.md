# Commons App Workflow Engine

A flexible workflow orchestration engine with state machine, saga pattern, and compensation support.

## Features

- **State Machine Execution**: Define workflows as state machines with transitions
- **Saga Pattern**: Distributed transaction coordination with automatic compensation
- **Compensation Logic**: Automatic rollback on failure with compensation actions
- **Event-Driven**: Trigger state transitions via events with optional conditions
- **Action Executors**: Pluggable action execution with Result pattern
- **In-Memory Storage**: Built-in in-memory instance storage (extend for persistence)
- **Workflow Context**: Thread-safe context management across states
- **Terminal States**: Proper handling of completion, failure, and cancellation
- **Execution History**: Track state transitions for compensation replay

## Installation

### Maven

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-workflow-engine</artifactId>
</dependency>
```

### Gradle

```gradle
implementation 'com.marcusprado02.commons:commons-app-workflow-engine'
```

## Quick Start

### 1. Create a Workflow Definition

```java
var workflow = WorkflowDefinition.builder()
    .id("order-processing")
    .name("Order Processing Workflow")
    .description("Processes customer orders with saga pattern")
    .initialState("start")

    // Define states
    .state(State.builder()
        .name("start")
        .type(StateType.START)
        .build())

    .state(State.builder()
        .name("reserve-inventory")
        .type(StateType.TASK)
        .action("reserveInventory")
        .compensation("releaseInventory")  // Compensation action
        .timeout(Duration.ofSeconds(30))
        .build())

    .state(State.builder()
        .name("charge-customer")
        .type(StateType.TASK)
        .action("chargeCustomer")
        .compensation("refundCustomer")
        .build())

    .state(State.builder()
        .name("ship-order")
        .type(StateType.TASK)
        .action("shipOrder")
        .build())

    .state(State.builder()
        .name("end")
        .type(StateType.END)
        .build())

    // Define transitions
    .transition(Transition.builder()
        .from("start")
        .to("reserve-inventory")
        .event("begin")
        .build())

    .transition(Transition.builder()
        .from("reserve-inventory")
        .to("charge-customer")
        .event("inventory-reserved")
        .build())

    .transition(Transition.builder()
        .from("charge-customer")
        .to("ship-order")
        .event("payment-charged")
        .build())

    .transition(Transition.builder()
        .from("ship-order")
        .to("end")
        .event("shipped")
        .build())

    .timeout(Duration.ofMinutes(10))
    .build();
```

### 2. Register Workflow and Action Executors

```java
var engine = DefaultWorkflowEngine.create();

// Register workflow definition
engine.registerDefinition(workflow);

// Register action executors
engine.registerActionExecutor("reserveInventory", (action, context) -> {
    var orderId = context.get("orderId");
    // Business logic to reserve inventory
    inventoryService.reserve(orderId);
    return Result.ok(Map.of("inventoryReserved", true));
});

engine.registerActionExecutor("releaseInventory", (action, context) -> {
    var orderId = context.get("orderId");
    // Compensation: release reserved inventory
    inventoryService.release(orderId);
    return Result.ok(Map.of("inventoryReleased", true));
});

engine.registerActionExecutor("chargeCustomer", (action, context) -> {
    var orderId = context.get("orderId");
    var amount = context.get("amount");

    // Simulate payment failure
    if (shouldFail()) {
        return Result.fail(Problem.of(
            ErrorCode.of("PAYMENT.FAILED"),
            ErrorCategory.BUSINESS,
            Severity.ERROR,
            "Payment declined"
        ));
    }

    paymentService.charge(orderId, amount);
    return Result.ok(Map.of("paymentId", "PAY-123"));
});

engine.registerActionExecutor("refundCustomer", (action, context) -> {
    var paymentId = context.get("paymentId");
    // Compensation: refund payment
    paymentService.refund(paymentId);
    return Result.ok(Map.of("refunded", true));
});

engine.registerActionExecutor("shipOrder", (action, context) -> {
    var orderId = context.get("orderId");
    shippingService.ship(orderId);
    return Result.ok(Map.of("trackingNumber", "TRACK-456"));
});
```

### 3. Start and Execute Workflow

```java
// Start workflow
var startResult = engine.startWorkflow("order-processing", Map.of(
    "orderId", "ORDER-123",
    "amount", 99.99
));

if (startResult.isFail()) {
    logger.error("Failed to start workflow: {}", startResult.problemOrNull());
    return;
}

var instance = startResult.getOrNull();
String workflowId = instance.id();

// Send events to trigger transitions
engine.sendEvent(workflowId, "begin", Map.of());
engine.sendEvent(workflowId, "inventory-reserved", Map.of());
engine.sendEvent(workflowId, "payment-charged", Map.of());
engine.sendEvent(workflowId, "shipped", Map.of());

// Check workflow status
var statusResult = engine.getWorkflow(workflowId);
var currentInstance = statusResult.getOrNull();

if (currentInstance.status() == WorkflowStatus.COMPLETED) {
    logger.info("Order processed successfully!");
} else if (currentInstance.status() == WorkflowStatus.COMPENSATED) {
    logger.warn("Order failed and was compensated");
}
```

## Saga Pattern and Compensation

The workflow engine automatically handles failures with the saga pattern:

1. **Action Failure**: If any action fails, the engine triggers compensation
2. **Reverse Order**: Compensations execute in reverse order of execution
3. **State Tracking**: Execution history tracks all visited states
4. **Context Preservation**: Compensation actions access the full workflow context

### Automatic Compensation Example

```java
// This workflow will automatically compensate if chargeCustomer fails
var result = engine.startWorkflow("order-processing", Map.of("orderId", "ORDER-123"));
var workflowId = result.getOrNull().id();

// Start processing
engine.sendEvent(workflowId, "begin", Map.of());
engine.sendEvent(workflowId, "inventory-reserved", Map.of());

// chargeCustomer fails → automatic compensation triggered
// 1. releaseInventory compensation executes
// 2. Workflow status set to COMPENSATED
```

### Manual Compensation

You can manually trigger compensation:

```java
var compensateResult = engine.compensate(workflowId);

if (compensateResult.isOk()) {
    logger.info("Workflow compensated successfully");
}
```

## State Types

The workflow engine supports multiple state types:

| State Type | Description | Example Use Case |
|------------|-------------|------------------|
| **START** | Initial entry point | Workflow initialization |
| **TASK** | Execute an action | Business logic step |
| **CHOICE** | Conditional branching | Route based on data |
| **PARALLEL** | Concurrent execution | Parallel tasks (future) |
| **END** | Successful completion | Terminal state |
| **FAIL** | Failure state with compensation | Error handling |

### State Configuration

```java
State.builder()
    .name("process-order")
    .type(StateType.TASK)
    .action("processOrder")              // Action to execute
    .compensation("undoProcessOrder")    // Compensation action
    .timeout(Duration.ofSeconds(30))     // State timeout
    .build()
```

## Workflow Instance Status

Workflows progress through various statuses:

| Status | Description | Terminal? |
|--------|-------------|-----------|
| **PENDING** | Created but not started | No |
| **RUNNING** | Active execution | No |
| **COMPLETED** | Successfully finished | Yes |
| **FAILED** | Failed without compensation | Yes |
| **COMPENSATING** | Running compensation actions | No |
| **COMPENSATED** | Compensation completed | Yes |
| **TIMED_OUT** | Exceeded timeout | Yes |

### Checking Status

```java
var instance = engine.getWorkflow(workflowId).getOrNull();

if (instance.isActive()) {
    logger.info("Workflow is running");
}

if (instance.isTerminal()) {
    logger.info("Workflow has completed: {}", instance.status());
}
```

## Context Management

Workflow context is a thread-safe `Map<String, Object>` that carries data across states:

```java
// Initial context
var startResult = engine.startWorkflow("my-workflow", Map.of(
    "orderId", "ORDER-123",
    "customerId", "CUST-456"
));

// Action adds to context
engine.registerActionExecutor("action1", (action, context) -> {
    var orderId = context.get("orderId");
    return Result.ok(Map.of("inventoryId", "INV-789"));
});

// Event adds to context
engine.sendEvent(workflowId, "next", Map.of("status", "processed"));

// Access merged context
var instance = engine.getWorkflow(workflowId).getOrNull();
var fullContext = instance.context();
// Contains: orderId, customerId, inventoryId, status
```

## Error Handling

All methods return `Result<T>` for consistent error handling:

```java
var result = engine.startWorkflow("my-workflow", Map.of());

if (result.isFail()) {
    var problem = result.problemOrNull();
    logger.error("Error: {} - {}",
        problem.errorCode().code(),
        problem.message());

    // Handle specific error categories
    switch (problem.errorCategory()) {
        case NOT_FOUND -> handleNotFound();
        case BUSINESS -> handleBusinessError();
        case TECHNICAL -> handleTechnicalError();
    }
}
```

### Common Error Codes

| Error Code | Category | Description |
|------------|----------|-------------|
| `WORKFLOW.DEFINITION_NOT_FOUND` | NOT_FOUND | Workflow definition not registered |
| `WORKFLOW.INSTANCE_NOT_FOUND` | NOT_FOUND | Workflow instance not found |
| `WORKFLOW.STATE_NOT_FOUND` | NOT_FOUND | State not defined in workflow |
| `WORKFLOW.NO_TRANSITION` | BUSINESS | No matching transition for event |
| `WORKFLOW.ALREADY_TERMINAL` | BUSINESS | Cannot send events to terminal state |

## Advanced Features

### Conditional Transitions

Define transitions with conditions (future enhancement):

```java
Transition.builder()
    .from("check-inventory")
    .to("process-order")
    .event("inventory-checked")
    .condition("inventoryAvailable == true")
    .build()
```

### Timeout Handling

Configure timeouts at workflow and state level:

```java
WorkflowDefinition.builder()
    .timeout(Duration.ofMinutes(10))  // Global workflow timeout
    .state(State.builder()
        .name("task1")
        .timeout(Duration.ofSeconds(30))  // State-level timeout
        .build())
    .build()
```

### Workflow Cancellation

Cancel a workflow with a reason:

```java
engine.cancel(workflowId, "User requested cancellation");
```

## Integration Patterns

### Spring Boot Integration

```java
@Configuration
public class WorkflowConfig {

    @Bean
    public WorkflowEngine workflowEngine() {
        var engine = DefaultWorkflowEngine.create();

        // Register workflows
        engine.registerDefinition(orderProcessingWorkflow());
        engine.registerDefinition(shipmentWorkflow());

        return engine;
    }

    @Bean
    public WorkflowDefinition orderProcessingWorkflow() {
        return WorkflowDefinition.builder()
            .id("order-processing")
            .name("Order Processing")
            .initialState("start")
            // ... state and transition definitions
            .build();
    }
}

@Service
public class OrderService {

    private final WorkflowEngine workflowEngine;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    public OrderService(WorkflowEngine workflowEngine,
                       InventoryService inventoryService,
                       PaymentService paymentService) {
        this.workflowEngine = workflowEngine;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;

        // Register action executors
        registerActions();
    }

    private void registerActions() {
        workflowEngine.registerActionExecutor("reserveInventory",
            (action, ctx) -> {
                var orderId = ctx.get("orderId");
                return inventoryService.reserve(orderId);
            });

        // ... register other executors
    }

    public Result<String> processOrder(Order order) {
        var result = workflowEngine.startWorkflow("order-processing", Map.of(
            "orderId", order.id(),
            "customerId", order.customerId(),
            "amount", order.totalAmount()
        ));

        return result.map(WorkflowInstance::id);
    }
}
```

### Persistent Storage

Extend for persistent storage:

```java
public class PersistentWorkflowEngine implements WorkflowEngine {

    private final WorkflowInstanceRepository repository;
    private final DefaultWorkflowEngine delegate;

    @Override
    public Result<WorkflowInstance> startWorkflow(String definitionId,
                                                  Map<String, Object> context) {
        return delegate.startWorkflow(definitionId, context)
            .map(instance -> {
                repository.save(instance);
                return instance;
            });
    }

    @Override
    public Result<WorkflowInstance> getWorkflow(String workflowId) {
        return repository.findById(workflowId)
            .map(Result::ok)
            .orElse(Result.fail(notFoundProblem(workflowId)));
    }

    // ... implement other methods with persistence
}
```

## Best Practices

### 1. Design Idempotent Actions

Actions should be idempotent to handle retries:

```java
engine.registerActionExecutor("processPayment", (action, context) -> {
    var paymentId = context.get("paymentId");

    // Check if already processed
    if (paymentService.isProcessed(paymentId)) {
        return Result.ok(Map.of("alreadyProcessed", true));
    }

    return paymentService.process(paymentId);
});
```

### 2. Keep State Names Meaningful

Use descriptive state names:

```java
// Good
.state("validate-customer-credit")
.state("reserve-inventory")
.state("charge-payment")

// Avoid
.state("step1")
.state("step2")
.state("process")
```

### 3. Design Effective Compensations

Ensure compensation actions properly undo changes:

```java
// Action
engine.registerActionExecutor("createUser", (action, ctx) -> {
    var user = userService.create(ctx.get("email"));
    return Result.ok(Map.of("userId", user.id()));
});

// Compensation
engine.registerActionExecutor("deleteUser", (action, ctx) -> {
    var userId = ctx.get("userId");
    userService.delete(userId);
    return Result.ok(Map.of());
});
```

### 4. Use Context for Data Flow

Pass data between states via context:

```java
// Action 1 produces data
engine.registerActionExecutor("action1", (action, ctx) -> {
    return Result.ok(Map.of("data", computedValue));
});

// Action 2 consumes data
engine.registerActionExecutor("action2", (action, ctx) -> {
    var data = ctx.get("data");
    return Result.ok(Map.of("result", process(data)));
});
```

### 5. Handle Timeouts Appropriately

Set realistic timeouts based on business requirements:

```java
WorkflowDefinition.builder()
    .timeout(Duration.ofMinutes(30))  // Long-running workflow
    .state(State.builder()
        .name("external-api-call")
        .timeout(Duration.ofSeconds(10))  // Quick timeout for external calls
        .build())
    .build()
```

## Architecture

The workflow engine follows hexagonal architecture:

```
commons-app-workflow-engine/
├── WorkflowEngine              # Port interface
├── WorkflowInstance            # Domain model
├── WorkflowDefinition          # Domain model
├── ActionExecutor              # Port interface
└── DefaultWorkflowEngine       # Adapter implementation
```

## Dependencies

- **commons-kernel-result**: Result pattern
- **commons-kernel-errors**: Error handling
- **commons-kernel-core**: Core utilities
- **SLF4J**: Logging

## Performance Considerations

- **In-Memory Storage**: Default implementation uses ConcurrentHashMap
- **Thread Safety**: All operations are thread-safe
- **Scalability**: For high-scale scenarios, use persistent storage and distributed locks
- **History Tracking**: Execution history grows linearly with state count

## Migration from Other Workflow Engines

### From Camunda

```java
// Camunda
runtimeService.startProcessInstanceByKey("order-process", variables);

// Commons Workflow Engine
engine.startWorkflow("order-process", variables);
```

### From AWS Step Functions

```java
// Step Functions uses JSON state machines
// Commons uses fluent Java API

WorkflowDefinition.builder()
    .id("state-machine")
    .state(...)
    .transition(...)
    .build();
```

## Testing

Test workflows with in-memory engine:

```java
@Test
void shouldProcessOrderWithSaga() {
    var engine = DefaultWorkflowEngine.create();
    engine.registerDefinition(orderWorkflow);

    // Register mock executors
    engine.registerActionExecutor("charge",
        (action, ctx) -> Result.fail(...));  // Simulate failure
    engine.registerActionExecutor("refund",
        (action, ctx) -> Result.ok(Map.of()));

    var result = engine.startWorkflow("order", Map.of());

    assertThat(result.getOrNull().status())
        .isEqualTo(WorkflowStatus.COMPENSATED);
}
```

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution guidelines.

## License

This project is licensed under the MIT License - see [LICENSE](../LICENSE) for details.
