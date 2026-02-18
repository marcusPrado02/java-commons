# API Reference: commons-kernel-errors

## Visão Geral

O módulo `commons-kernel-errors` fornece abstrações para tratamento de erros de forma estruturada e tipo-segura, seguindo os princípios de Domain-Driven Design.

**Dependência Maven:**
```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-errors</artifactId>
</dependency>
```

---

## Core Classes

### Problem

Representa um problema/erro estruturado com código, mensagem e metadados.

```java
public interface Problem {
    String code();               // Código único do erro (ex: "USER.NOT_FOUND")
    String message();            // Mensagem legível humana
    Map<String, Object> details();  // Detalhes adicionais
    ProblemSeverity severity();  // MINOR, MAJOR, CRITICAL
}
```

#### Criando Problems

```java
// Problem simples
Problem problem = Problem.of(
    "USER.NOT_FOUND",
    "User not found"
);

// Problem com detalhes
Problem problem = Problem.builder()
    .code("ORDER.INVALID_AMOUNT")
    .message("Order amount must be positive")
    .detail("requestedAmount", -100.0)
    .detail("minimumAmount", 0.01)
    .severity(ProblemSeverity.MAJOR)
    .build();

// Problem de validação
Problem problem = ValidationProblem.of(
    "VALIDATION.FAILED",
    Map.of(
        "email", "Invalid email format",
        "age", "Must be at least 18"
    )
);
```

#### Usando com Result

```java
public Result<User> findUser(UserId userId) {
    return userRepository.findById(userId)
        .map(Result::ok)
        .orElseGet(() -> Result.fail(
            Problem.of("USER.NOT_FOUND", "User not found with id: " + userId)
        ));
}
```

---

### DomainException

Exception base para erros de domínio.

```java
public abstract class DomainException extends RuntimeException {
    private final Problem problem;
    
    protected DomainException(Problem problem) {
        super(problem.message());
        this.problem = problem;
    }
    
    public Problem getProblem() {
        return problem;
    }
}
```

#### Criando Domain Exceptions

```java
// Exception específica do domínio
public class InsufficientFundsException extends DomainException {
    
    public InsufficientFundsException(Money balance, Money required) {
        super(Problem.builder()
            .code("ACCOUNT.INSUFFICIENT_FUNDS")
            .message("Insufficient funds for transaction")
            .detail("balance", balance.value())
            .detail("required", required.value())
            .severity(ProblemSeverity.MAJOR)
            .build()
        );
    }
}

// Uso
public void withdraw(Money amount) {
    if (balance.lessThan(amount)) {
        throw new InsufficientFundsException(balance, amount);
    }
    this.balance = balance.subtract(amount);
}
```

---

### ValidationException

Exception para erros de validação (múltiplos erros).

```java
public class ValidationException extends DomainException {
    
    private final Map<String, String> fieldErrors;
    
    public ValidationException(Map<String, String> fieldErrors) {
        super(ValidationProblem.of("VALIDATION.FAILED", fieldErrors));
        this.fieldErrors = fieldErrors;
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
```

#### Exemplo

```java
public Result<User> validateAndCreate(CreateUserCommand command) {
    Map<String, String> errors = new HashMap<>();
    
    // Valida múltiplos campos
    if (command.email() == null || !command.email().contains("@")) {
        errors.put("email", "Invalid email format");
    }
    
    if (command.age() < 18) {
        errors.put("age", "Must be at least 18 years old");
    }
    
    if (!errors.isEmpty()) {
        return Result.fail(ValidationProblem.of("VALIDATION.FAILED", errors));
    }
    
    // Cria usuário
    return Result.ok(createUser(command));
}
```

---

## Exemplo Completo: Order Domain

### Domain Model

```java
public class Order extends AggregateRoot<OrderId> {
    
    private OrderStatus status;
    private List<OrderItem> items;
    private Money totalAmount;
    
    // Regras de negócio que podem falhar
    
    public Result<Void> addItem(Product product, int quantity) {
        // Validação
        if (quantity <= 0) {
            return Result.fail(Problem.of(
                "ORDER.INVALID_QUANTITY",
                "Quantity must be positive"
            ));
        }
        
        if (status != OrderStatus.DRAFT) {
            return Result.fail(Problem.of(
                "ORDER.CANNOT_MODIFY",
                "Cannot add items to order in status: " + status
            ));
        }
        
        // Lógica
        OrderItem item = new OrderItem(product, quantity);
        items.add(item);
        recalculateTotal();
        
        recordEvent(new ItemAddedToOrder(id(), product.id(), quantity));
        
        return Result.ok();
    }
    
    public Result<Void> submit() {
        // Invariantes
        if (items.isEmpty()) {
            return Result.fail(Problem.of(
                "ORDER.EMPTY_CART",
                "Cannot submit order with no items"
            ));
        }
        
        if (status != OrderStatus.DRAFT) {
            return Result.fail(Problem.of(
                "ORDER.INVALID_STATUS_TRANSITION",
                "Cannot submit order in status: " + status
            ));
        }
        
        // Transição de estado
        this.status = OrderStatus.SUBMITTED;
        recordEvent(new OrderSubmitted(id(), totalAmount, Instant.now()));
        
        return Result.ok();
    }
    
    public void cancel(String reason) {
        // Exception para invariante crítica
        if (status == OrderStatus.DELIVERED) {
            throw new OrderAlreadyDeliveredException(id());
        }
        
        this.status = OrderStatus.CANCELLED;
        recordEvent(new OrderCancelled(id(), reason, Instant.now()));
    }
}
```

### Application Service

```java
@Service
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        // Cria aggregate
        Order order = Order.create(
            OrderId.generate(),
            command.tenantId(),
            command.customerId(),
            AuditStamp.now(command.actorId())
        );
        
        // Adiciona itens com validação
        for (OrderItemRequest item : command.items()) {
            Result<Product> productResult = productRepository.findById(item.productId());
            
            if (productResult.isFail()) {
                return Result.fail(productResult.problemOrNull());
            }
            
            Result<Void> addResult = order.addItem(
                productResult.getOrThrow(),
                item.quantity()
            );
            
            if (addResult.isFail()) {
                return Result.fail(addResult.problemOrNull());
            }
        }
        
        // Submit
        Result<Void> submitResult = order.submit();
        if (submitResult.isFail()) {
            return Result.fail(submitResult.problemOrNull());
        }
        
        // Persiste
        return orderRepository.save(order)
            .map(saved -> order.id());
    }
}
```

### REST Controller

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderApplicationService orderService;
    
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        Result<OrderId> result = orderService.createOrder(
            toCommand(request)
        );
        
        // Converte Result em resposta HTTP
        return result
            .map(orderId -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("orderId", orderId.value()))
            )
            .recover(problem -> ResponseEntity
                .status(toProblemStatus(problem))
                .body(Map.of(
                    "error", problem.code(),
                    "message", problem.message(),
                    "details", problem.details()
                ))
            );
    }
    
    private HttpStatus toHttpStatus(Problem problem) {
        return switch (problem.code().split("\\.")[0]) {
            case "VALIDATION" -> HttpStatus.BAD_REQUEST;
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
```

---

## Problem Categories

### Convenções de Códigos

```java
// Formato: ENTITY.ERROR_TYPE
"USER.NOT_FOUND"
"ORDER.INVALID_STATUS"
"PAYMENT.INSUFFICIENT_FUNDS"
"VALIDATION.FAILED"
```

### Severidade

```java
public enum ProblemSeverity {
    MINOR,      // Erros recuperáveis, não críticos
    MAJOR,      // Erros significativos, requer atenção
    CRITICAL    // Falhas críticas, requer ação imediata
}
```

### Exemplo de Hierarquia

```java
public class OrderProblems {
    
    public static Problem notFound(OrderId orderId) {
        return Problem.of(
            "ORDER.NOT_FOUND",
            "Order not found: " + orderId
        );
    }
    
    public static Problem emptyCart() {
        return Problem.builder()
            .code("ORDER.EMPTY_CART")
            .message("Cannot submit order with no items")
            .severity(ProblemSeverity.MAJOR)
            .build();
    }
    
    public static Problem invalidStatusTransition(
        OrderStatus from, 
        OrderStatus to
    ) {
        return Problem.builder()
            .code("ORDER.INVALID_STATUS_TRANSITION")
            .message("Cannot transition from " + from + " to " + to)
            .detail("currentStatus", from)
            .detail("requestedStatus", to)
            .severity(ProblemSeverity.MAJOR)
            .build();
    }
}
```

---

## Integration with Result Pattern

### Composition

```java
public Result<OrderSummary> getOrderSummary(OrderId orderId) {
    return orderRepository.findById(orderId)
        .flatMap(order -> customerRepository.findById(order.customerId())
            .map(customer -> new OrderSummary(order, customer))
        );
    // Se qualquer operação falhar, propaga o Problem
}
```

### Error Recovery

```java
public Result<User> findUserWithFallback(UserId userId) {
    return userRepository.findById(userId)
        .orElseRecover(problem -> {
            // Log do problema original
            log.warn("User not found, creating default: {}", problem);
            
            // Cria usuário default
            return Result.ok(createDefaultUser());
        });
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use códigos estruturados
Problem.of("USER.NOT_FOUND", "User not found")

// ✅ Adicione contexto útil
Problem.builder()
    .code("ORDER.INSUFFICIENT_STOCK")
    .message("Product out of stock")
    .detail("productId", productId)
    .detail("requestedQuantity", 5)
    .detail("availableQuantity", 2)
    .build()

// ✅ Use Result para falhas esperadas
public Result<User> findUser(UserId id)

// ✅ Use Exception para falhas inesperadas/críticas
if (order == null) throw new IllegalStateException("Order cannot be null");

// ✅ Propague erros de forma funcional
return userRepository.findById(userId)
    .flatMap(user -> orderRepository.findByUserId(user.id()))
```

### ❌ DON'T

```java
// ❌ NÃO use strings genéricas
Problem.of("ERROR", "Something went wrong")

// ❌ NÃO use exceptions para controle de fluxo normal
try {
    return userRepository.findById(id);
} catch (UserNotFoundException e) {
    return null;  // Use Result!
}

// ❌ NÃO ignore erros
result.getOrThrow();  // ⚠️ Pode lançar exception

// ✅ Trate explicitamente
result
    .map(value -> process(value))
    .recover(problem -> handleError(problem))
```

---

## Testing

```java
class OrderTest {
    
    @Test
    void shouldFailWhenAddingToSubmittedOrder() {
        // Given
        Order order = createOrder();
        order.submit();
        
        // When
        Result<Void> result = order.addItem(product, 1);
        
        // Then
        assertThat(result.isFail()).isTrue();
        
        Problem problem = result.problemOrNull();
        assertThat(problem.code()).isEqualTo("ORDER.CANNOT_MODIFY");
        assertThat(problem.message()).contains("Cannot add items");
    }
    
    @Test
    void shouldThrowWhenCancellingDeliveredOrder() {
        // Given
        Order order = createDeliveredOrder();
        
        // Then
        assertThatThrownBy(() -> order.cancel("Customer request"))
            .isInstanceOf(OrderAlreadyDeliveredException.class)
            .satisfies(ex -> {
                DomainException domainEx = (DomainException) ex;
                assertThat(domainEx.getProblem().code())
                    .isEqualTo("ORDER.ALREADY_DELIVERED");
            });
    }
}
```

---

## Ver Também

- [Result Pattern](kernel-result.md)
- [DDD Building Blocks](kernel-ddd.md)
- [Observability Guide](../guides/observability.md)
