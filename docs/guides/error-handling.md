# Guia: Error Handling

## Visão Geral

Guia completo de tratamento de erros em aplicações construídas com Commons Platform, desde o domínio até a API REST.

---

## 🎯 Estratégias de Error Handling

### 1. Result Pattern (Recomendado)

Para falhas **esperadas** do domínio.

```java
@Service
public class OrderService {
    
    public Result<Order> createOrder(CreateOrderCommand command) {
        // Validação
        if (command.items().isEmpty()) {
            return Result.fail(Problem.of(
                "ORDER.EMPTY_CART",
                "Cannot create order with no items"
            ));
        }
        
        // Regra de negócio
        if (customer.creditLimit().lessThan(command.totalAmount())) {
            return Result.fail(Problem.builder()
                .code("ORDER.INSUFFICIENT_CREDIT")
                .message("Customer credit limit exceeded")
                .detail("creditLimit", customer.creditLimit())
                .detail("requestedAmount", command.totalAmount())
                .severity(ProblemSeverity.MAJOR)
                .build()
            );
        }
        
        // Sucesso
        Order order = Order.create(command);
        return Result.ok(order);
    }
}
```

### 2. Exceptions

Para falhas **inesperadas** ou **críticas**.

```java
@Service
public class PaymentService {
    
    public Payment processPayment(ProcessPaymentCommand command) {
        // Validação de pré-condição (exception)
        Objects.requireNonNull(command, "Command cannot be null");
        
        // Invariante crítica (exception)
        if (command.amount().isNegative()) {
            throw new IllegalArgumentException("Payment amount cannot be negative");
        }
        
        try {
            // Chama gateway externo
            PaymentGatewayResponse response = paymentGateway.charge(command);
            return Payment.from(response);
            
        } catch (PaymentGatewayException e) {
            // Falha externa inesperada
            throw new InfrastructureException("Payment gateway failed", e);
        }
    }
}
```

---

## 📦 Domain Layer

### Value Objects com Validação

```java
public class Email extends SingleValueObject<String> {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    
    private Email(String value) {
        super(value);
    }
    
    public static Result<Email> validate(String value) {
        // Validação null/blank
        if (value == null || value.isBlank()) {
            return Result.fail(Problem.of(
                "EMAIL.REQUIRED",
                "Email is required"
            ));
        }
        
        // Normalização
        String normalized = value.trim().toLowerCase();
        
        // Validação de formato
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            return Result.fail(Problem.of(
                "EMAIL.INVALID_FORMAT",
                "Invalid email format: " + value
            ));
        }
        
        return Result.ok(new Email(normalized));
    }
    
    // Factory que lança exception (use com cuidado)
    public static Email of(String value) {
        return validate(value)
            .getOrThrow(() -> new IllegalArgumentException("Invalid email: " + value));
    }
}
```

### Entities com Invariantes

```java
public class Order extends AggregateRoot<OrderId> {
    
    private OrderStatus status;
    private List<OrderItem> items;
    private Money totalAmount;
    
    public Result<Void> submit() {
        // Invariante: não pode submeter order vazio
        if (items.isEmpty()) {
            return Result.fail(Problem.of(
                "ORDER.EMPTY_CART",
                "Cannot submit order with no items"
            ));
        }
        
        // Invariante: só pode submeter se status = DRAFT
        if (status != OrderStatus.DRAFT) {
            return Result.fail(Problem.of(
                "ORDER.INVALID_STATUS",
                "Cannot submit order in status: " + status
            ));
        }
        
        // Transição de estado
        this.status = OrderStatus.SUBMITTED;
        recordEvent(new OrderSubmitted(id(), totalAmount, Instant.now()));
        
        return Result.ok();
    }
    
    public void cancel() {
        // Invariante crítica - usa exception
        if (status == OrderStatus.DELIVERED) {
            throw new OrderAlreadyDeliveredException(
                id(),
                "Cannot cancel order that has been delivered"
            );
        }
        
        this.status = OrderStatus.CANCELLED;
        recordEvent(new OrderCancelled(id(), Instant.now()));
    }
}
```

---

## 🔧 Application Layer

### Composição de Results

```java
@Service
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        // 1. Valida customer
        Result<Customer> customerResult = customerRepository.findById(command.customerId());
        if (customerResult.isFail()) {
            return Result.fail(customerResult.problemOrNull());
        }
        Customer customer = customerResult.getOrThrow();
        
        // 2. Cria order
        Order order = Order.create(
            OrderId.generate(),
            command.tenantId(),
            customer.id(),
            AuditStamp.now()
        );
        
        // 3. Adiciona items (com validação)
        for (OrderItemRequest itemRequest : command.items()) {
            Result<Product> productResult = productRepository.findById(itemRequest.productId());
            
            if (productResult.isFail()) {
                return Result.fail(productResult.problemOrNull());
            }
            
            Result<Void> addResult = order.addItem(
                productResult.getOrThrow(),
                itemRequest.quantity()
            );
            
            if (addResult.isFail()) {
                return Result.fail(addResult.problemOrNull());
            }
        }
        
        // 4. Submit
        Result<Void> submitResult = order.submit();
        if (submitResult.isFail()) {
            return Result.fail(submitResult.problemOrNull());
        }
        
        // 5. Persiste
        return orderRepository.save(order)
            .map(saved -> order.id());
    }
}
```

### Railway-Oriented Programming

```java
@Service
public class UserRegistrationService {
    
    public Result<UserId> registerUser(RegisterUserCommand command) {
        return Email.validate(command.email())
            .flatMap(email -> createUser(email, command.name()))
            .flatMap(user -> validateBusinessRules(user))
            .flatMap(user -> saveUser(user))
            .flatMap(user -> sendWelcomeEmail(user))
            .map(User::id);
        
        // Se qualquer passo falhar, propaga o Problem
        // Se todos passos OK, retorna UserId
    }
    
    private Result<User> createUser(Email email, String name) {
        // Lógica
    }
    
    private Result<User> validateBusinessRules(User user) {
        // Validações
    }
}
```

---

## 🌐 REST API Layer

### Controller com Result

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderApplicationService orderService;
    
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        Result<OrderId> result = orderService.createOrder(toCommand(request));
        
        return result
            .map(orderId -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("orderId", orderId.value()))
            )
            .recover(problem -> ResponseEntity
                .status(mapToHttpStatus(problem))
                .body(toProblemResponse(problem))
            );
    }
    
    private HttpStatus mapToHttpStatus(Problem problem) {
        String category = problem.code().split("\\.")[0];
        
        return switch (category) {
            case "VALIDATION" -> HttpStatus.BAD_REQUEST;
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "CONFLICT" -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    private Map<String, Object> toProblemResponse(Problem problem) {
        return Map.of(
            "error", problem.code(),
            "message", problem.message(),
            "details", problem.details(),
            "severity", problem.severity().name()
        );
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private final MetricsFacade metrics;
    
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex) {
        Problem problem = ex.getProblem();
        
        metrics.incrementCounter(
            "api.error",
            "type", "domain",
            "code", problem.code()
        );
        
        return ResponseEntity
            .status(mapToHttpStatus(problem))
            .body(ProblemDetail.builder()
                .type("about:blank")
                .title(problem.code())
                .status(mapToHttpStatus(problem).value())
                .detail(problem.message())
                .instance("/api/orders")
                .build()
            );
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationProblemDetail> handleValidationException(
        ValidationException ex
    ) {
        metrics.incrementCounter("api.error", "type", "validation");
        
        return ResponseEntity
            .badRequest()
            .body(ValidationProblemDetail.builder()
                .type("validation-error")
                .title("VALIDATION.FAILED")
                .status(400)
                .errors(ex.getFieldErrors())
                .build()
            );
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception ex) {
        log.error("Unexpected exception", ex);
        
        metrics.incrementCounter(
            "api.error",
            "type", "unexpected",
            "exception", ex.getClass().getSimpleName()
        );
        
        // NÃO exponha detalhes internos
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ProblemDetail.builder()
                .type("internal-error")
                .title("INTERNAL_SERVER_ERROR")
                .status(500)
                .detail("An unexpected error occurred")
                .build()
            );
    }
}
```

---

## 📋 RFC 7807 Problem Details

### Standard Format

```json
{
  "type": "https://api.example.com/errors/order-empty-cart",
  "title": "ORDER.EMPTY_CART",
  "status": 400,
  "detail": "Cannot submit order with no items",
  "instance": "/api/orders/123",
  "customerId": "customer-456"
}
```

### Implementation

```java
public record ProblemDetail(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    Map<String, Object> extensions
) {
    
    public static ProblemDetail from(Problem problem, String instance) {
        return new ProblemDetail(
            "https://api.example.com/errors/" + problem.code().toLowerCase(),
            problem.code(),
            mapToStatusCode(problem),
            problem.message(),
            instance,
            problem.details()
        );
    }
}
```

---

## 🔍 Validation Patterns

### Bean Validation (JSR 380)

```java
public record CreateOrderRequest(
    @NotNull(message = "Customer ID is required")
    String customerId,
    
    @NotEmpty(message = "Items cannot be empty")
    @Valid
    List<OrderItemRequest> items,
    
    @NotNull
    @Positive(message = "Total amount must be positive")
    BigDecimal totalAmount
) {
    
    @AssertTrue(message = "Total amount must match items sum")
    public boolean isTotalAmountValid() {
        BigDecimal sum = items.stream()
            .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalAmount.compareTo(sum) == 0;
    }
}

// Controller
@PostMapping
public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    // Validações automáticas
}
```

### Custom Validator

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailValidator.class)
public @interface ValidEmail {
    String message() default "Invalid email format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;  // @NotNull handle isso
        
        return Email.validate(value).isOk();
    }
}
```

---

## 📊 Error Monitoring

### Metrics

```java
@Component
public class ErrorMetricsCollector {
    
    private final MetricsFacade metrics;
    
    @EventListener
    public void onDomainError(DomainErrorEvent event) {
        Problem problem = event.problem();
        
        metrics.incrementCounter(
            "domain.error",
            "code", problem.code(),
            "severity", problem.severity().name()
        );
    }
    
    @EventListener
    public void onApiError(ApiErrorEvent event) {
        metrics.incrementCounter(
            "api.error",
            "endpoint", event.endpoint(),
            "status", event.statusCode(),
            "errorCode", event.errorCode()
        );
    }
}
```

### Structured Logging

```java
@Aspect
@Component
public class ErrorLoggingAspect {
    
    private final StructuredLog log;
    
    @AfterThrowing(
        pointcut = "@within(org.springframework.web.bind.annotation.RestController)",
        throwing = "ex"
    )
    public void logApiError(JoinPoint joinPoint, Exception ex) {
        log.error("API error")
            .field("endpoint", joinPoint.getSignature().toShortString())
            .field("exception", ex.getClass().getSimpleName())
            .field("message", ex.getMessage())
            .field("correlationId", RequestContext.getCorrelationId())
            .log();
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use Result para falhas esperadas
public Result<Order> findOrder(OrderId id);

// ✅ Use Exception para falhas inesperadas/críticas
if (config == null) throw new IllegalStateException("Config not initialized");

// ✅ Códigos de erro estruturados
Problem.of("ORDER.NOT_FOUND", "Order not found")

// ✅ Contexto relevante nos erros
.detail("orderId", orderId)
.detail("customerId", customerId)

// ✅ Monitore erros
metrics.incrementCounter("error", "code", problem.code());

// ✅ Logs estruturados
log.error("Order creation failed")
    .field("orderId", orderId)
    .field("error", problem.code())
    .log();
```

### ❌ DON'T

```java
// ❌ NÃO use exceptions para controle de fluxo
try {
    return userRepository.findById(id);
} catch (UserNotFoundException e) {
    return null;  // Use Result!
}

// ❌ NÃO exponha stack traces na API
return ResponseEntity.status(500).body(ex.printStackTrace());  // ❌

// ❌ NÃO use strings genéricas
throw new RuntimeException("Error");  // ❌

// ❌ NÃO ignore erros
try { ... } catch (Exception e) { /* ignored */ }  // ❌
```

---

## Ver Também

- [Result Pattern](../api-reference/kernel-result.md)
- [Problem & Exceptions](../api-reference/kernel-errors.md)
- [Observability](observability.md)
- [Testing Strategies](testing.md)
