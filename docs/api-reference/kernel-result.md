# API Reference: commons-kernel-result

## Visão Geral

O módulo `commons-kernel-result` implementa o **Result Pattern** (Railway-Oriented Programming) para tratamento funcional de erros, eliminando exceptions para controle de fluxo.

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-result</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🎯 Por Que Result Pattern?

### Problemas com Exceptions

```java
// ❌ RUIM - Exceptions para controle de fluxo
public class UserService {
    public User createUser(String email) throws ValidationException, 
                                                DuplicateEmailException, 
                                                DatabaseException {
        // Chamador precisa tratar todas as exceptions ou propagar
        // Erros não são visíveis na assinatura do método
    }
}
```

### Solução com Result

```java
// ✅ BOM - Resultado explícito na assinatura
public class UserService {
    public Result<User> createUser(String email) {
        // Erros são valores de retorno, não exceptions
        // Tipo força o chamador a lidar com erro
        // Composição funcional natural
    }
}
```

---

## Result<T>

**Package:** `com.marcusprado02.commons.kernel.result`

Container que representa sucesso (`Ok`) ou falha (`Fail`) de uma operação.

### Estrutura

```java
public sealed interface Result<T> 
    permits Result.Ok, Result.Fail {
    
    // Discriminadores
    boolean isOk();
    boolean isFail();
    
    // Extração de valores
    T getOrNull();
    Problem problemOrNull();
    
    // Transformações (functor)
    <U> Result<U> map(Function<T, U> mapper);
    <U> Result<U> flatMap(Function<T, Result<U>> mapper);
    
    // Recuperação de erros
    Result<T> recover(Function<Problem, Result<T>> recovery);
    T orElse(T defaultValue);
    T orElseGet(Supplier<T> supplier);
    T orElseThrow();
    
    // Side effects
    Result<T> onOk(Consumer<T> action);
    Result<T> onFail(Consumer<Problem> action);
    
    // Validação com predicado
    Result<T> filter(Predicate<T> predicate, Problem failProblem);
}
```

### Criação

```java
// Sucesso
Result<User> success = Result.ok(user);

// Falha com Problem
Result<User> failure = Result.fail(
    Problem.of("USER.NOT_FOUND", "User not found")
);

// Falha com código e mensagem
Result<User> failure = Result.fail("USER.INVALID_EMAIL", "Invalid email format");
```

### Exemplo Básico

```java
public class UserService {
    
    public Result<User> findUserByEmail(Email email) {
        return userRepository.findByEmail(email)
            .map(Result::ok)
            .orElse(Result.fail(
                "USER.NOT_FOUND", 
                "User with email " + email.value() + " not found"
            ));
    }
    
    public Result<User> createUser(String emailStr, String nameStr) {
        return Email.validate(emailStr)                    // Result<Email>
            .flatMap(email -> UserName.validate(nameStr)   // Result<UserName>
                .flatMap(name -> {
                    // Ambos validados com sucesso
                    if (userRepository.existsByEmail(email)) {
                        return Result.fail(
                            "USER.DUPLICATE_EMAIL",
                            "Email already in use"
                        );
                    }
                    
                    User user = new User(
                        UserId.generate(),
                        TenantId.current(),
                        name,
                        email,
                        AuditStamp.now()
                    );
                    
                    userRepository.save(user);
                    return Result.ok(user);
                })
            );
    }
}
```

---

## Transformações Funcionais

### map - Transformar valor de sucesso

```java
Result<User> userResult = findUser(userId);

// Transforma User -> UserDTO
Result<UserDTO> dtoResult = userResult.map(user -> 
    UserDTO.from(user)
);

// Chain múltiplos maps
Result<String> emailResult = userResult
    .map(User::email)           // Result<Email>
    .map(Email::value);         // Result<String>
```

### flatMap - Composição de operações que retornam Result

```java
Result<Order> orderResult = findUser(userId)              // Result<User>
    .flatMap(user -> findCart(user.id()))                 // Result<Cart>
    .flatMap(cart -> validateCart(cart))                  // Result<Cart>
    .flatMap(cart -> createOrder(cart));                  // Result<Order>
```

### filter - Validação condicional

```java
Result<User> adultUser = findUser(userId)
    .filter(
        user -> user.age() >= 18,
        Problem.of("USER.UNDERAGE", "User must be 18 or older")
    );
```

### recover - Recuperação de erros

```java
Result<User> userResult = findUser(userId)
    .recover(problem -> {
        if (problem.code().equals("USER.NOT_FOUND")) {
            // Retorna usuário padrão
            return Result.ok(User.anonymous());
        }
        // Mantém o erro original
        return Result.fail(problem);
    });
```

---

## Extração de Valores

### orElse - Valor padrão

```java
User user = findUser(userId)
    .orElse(User.anonymous());
```

### orElseGet - Valor padrão via supplier

```java
User user = findUser(userId)
    .orElseGet(() -> createGuestUser());
```

### orElseThrow - Lança exception

```java
// Para APIs legadas que requerem exceptions
try {
    User user = findUser(userId).orElseThrow();
} catch (DomainException ex) {
    // ex.problem() contém o Problem
}
```

### getOrNull - Extração manual

```java
Result<User> result = findUser(userId);

if (result.isOk()) {
    User user = result.getOrNull();
    // ... usa user
} else {
    Problem problem = result.problemOrNull();
    // ... trata erro
}
```

---

## Side Effects

### onOk - Ação em caso de sucesso

```java
findUser(userId)
    .onOk(user -> logger.info("Found user: {}", user.id()))
    .onFail(problem -> logger.error("User not found: {}", problem));
```

### onFail - Ação em caso de falha

```java
createOrder(cart)
    .onOk(order -> emailService.sendConfirmation(order))
    .onFail(problem -> metrics.incrementFailure("order.creation"));
```

---

## Results Utility Class

**Package:** `com.marcusprado02.commons.kernel.result`

Funções auxiliares para trabalhar com Result.

### catchingDomain - Captura DomainException

```java
public class UserService {
    
    public Result<User> createUser(String email) {
        return Results.catchingDomainValue(() -> {
            Email validEmail = Email.of(email);  // pode lançar DomainException
            User user = new User(..., validEmail, ...);
            return user;
        });
    }
}
```

### fromException - Converte DomainException para Result

```java
try {
    Email email = Email.of(rawEmail);
    return Result.ok(email);
} catch (DomainException ex) {
    return Results.fromException(ex);
}
```

### orThrow - Unwrap ou lança exception

```java
// Útil em camada interna que não quer lidar com Result
User user = Results.orThrow(findUser(userId));
// Se Result.isFail(), lança DomainException
```

---

## Padrões de Uso

### Validação em Cadeia

```java
public record Email(String value) {
    
    public static Result<Email> validate(String value) {
        if (value == null || value.isBlank()) {
            return Result.fail("EMAIL.BLANK", "Email cannot be blank");
        }
        
        String normalized = value.trim().toLowerCase();
        
        if (!normalized.contains("@")) {
            return Result.fail("EMAIL.INVALID_FORMAT", "Invalid email format");
        }
        
        if (normalized.length() > 255) {
            return Result.fail("EMAIL.TOO_LONG", "Email too long (max 255)");
        }
        
        return Result.ok(new Email(normalized));
    }
}

public record UserName(String value) {
    
    public static Result<UserName> validate(String value) {
        if (value == null || value.isBlank()) {
            return Result.fail("NAME.BLANK", "Name cannot be blank");
        }
        
        String trimmed = value.trim();
        
        if (trimmed.length() < 2) {
            return Result.fail("NAME.TOO_SHORT", "Name too short (min 2 chars)");
        }
        
        if (trimmed.length() > 100) {
            return Result.fail("NAME.TOO_LONG", "Name too long (max 100 chars)");
        }
        
        return Result.ok(new UserName(trimmed));
    }
}

// Composição
public Result<User> createUser(String emailStr, String nameStr) {
    return Email.validate(emailStr)
        .flatMap(email -> UserName.validate(nameStr)
            .map(name -> new User(
                UserId.generate(),
                TenantId.current(),
                name,
                email,
                AuditStamp.now()
            ))
        );
}
```

### Application Service com Result

```java
@Service
@Transactional
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    
    public Result<OrderId> placeOrder(PlaceOrderCommand cmd) {
        return validateUser(cmd.userId())
            .flatMap(user -> validateItems(cmd.items()))
            .flatMap(items -> createOrder(cmd.userId(), items))
            .flatMap(order -> processPayment(order, cmd.paymentMethod()))
            .map(Order::id);
    }
    
    private Result<User> validateUser(UserId userId) {
        return userRepository.findById(userId)
            .map(Result::ok)
            .orElse(Result.fail(
                "USER.NOT_FOUND",
                "User " + userId.value() + " not found"
            ));
    }
    
    private Result<List<OrderItem>> validateItems(List<ItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return Result.fail("ORDER.EMPTY", "Order must have at least one item");
        }
        
        List<OrderItem> orderItems = items.stream()
            .map(this::toOrderItem)
            .toList();
            
        return Result.ok(orderItems);
    }
    
    private Result<Order> createOrder(UserId userId, List<OrderItem> items) {
        try {
            Order order = Order.create(
                OrderId.generate(),
                TenantId.current(),
                userId,
                AuditStamp.now()
            );
            
            for (OrderItem item : items) {
                order.addItem(item.productId(), item.quantity(), AuditStamp.now());
            }
            
            orderRepository.save(order);
            return Result.ok(order);
            
        } catch (Exception ex) {
            return Result.fail(
                "ORDER.CREATION_FAILED",
                "Failed to create order: " + ex.getMessage()
            );
        }
    }
    
    private Result<Order> processPayment(Order order, PaymentMethod method) {
        return paymentService.charge(order.totalAmount(), method)
            .map(payment -> {
                order.markAsPaid(payment.id(), AuditStamp.now());
                orderRepository.save(order);
                return order;
            });
    }
}
```

### REST Controller com Result

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserApplicationService userService;
    
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        Result<UserId> result = userService.createUser(
            request.email(),
            request.name()
        );
        
        return result
            .map(userId -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("userId", userId.value()))
            )
            .orElseGet(() -> {
                Problem problem = result.problemOrNull();
                return ResponseEntity
                    .status(mapToHttpStatus(problem))
                    .body(problem);
            });
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        return userService.findUser(UserId.of(userId))
            .map(user -> ResponseEntity.ok(UserDTO.from(user)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    private HttpStatus mapToHttpStatus(Problem problem) {
        return switch (problem.code()) {
            case String code when code.endsWith(".NOT_FOUND") -> 
                HttpStatus.NOT_FOUND;
            case String code when code.endsWith(".DUPLICATE") -> 
                HttpStatus.CONFLICT;
            case String code when code.contains(".VALIDATION") -> 
                HttpStatus.BAD_REQUEST;
            default -> 
                HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
```

---

## Integração com Spring

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // Captura DomainException (quando orElseThrow() é usado)
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Problem> handleDomainException(DomainException ex) {
        Problem problem = ex.problem();
        HttpStatus status = mapToHttpStatus(problem);
        return ResponseEntity.status(status).body(problem);
    }
    
    private HttpStatus mapToHttpStatus(Problem problem) {
        // ... mesmo mapping do exemplo anterior
    }
}
```

---

## Testing

```java
@Test
void shouldReturnOkWhenEmailIsValid() {
    Result<Email> result = Email.validate("john@example.com");
    
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().value()).isEqualTo("john@example.com");
}

@Test
void shouldReturnFailWhenEmailIsInvalid() {
    Result<Email> result = Email.validate("invalid-email");
    
    assertThat(result.isFail()).isTrue();
    
    Problem problem = result.problemOrNull();
    assertThat(problem.code()).isEqualTo("EMAIL.INVALID_FORMAT");
    assertThat(problem.message()).contains("Invalid email");
}

@Test
void shouldChainValidationsWithFlatMap() {
    Result<User> result = Email.validate("john@example.com")
        .flatMap(email -> UserName.validate("John Doe")
            .map(name -> new User(
                UserId.generate(),
                TenantId.of("tenant-1"),
                name,
                email,
                AuditStamp.now()
            ))
        );
    
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().email().value()).isEqualTo("john@example.com");
}

@Test
void shouldShortCircuitOnFirstFailure() {
    AtomicBoolean nameValidationCalled = new AtomicBoolean(false);
    
    Result<User> result = Email.validate("invalid")  // ❌ Falha aqui
        .flatMap(email -> {
            nameValidationCalled.set(true);  // Não é executado
            return UserName.validate("John")
                .map(name -> createUser(email, name));
        });
    
    assertThat(result.isFail()).isTrue();
    assertThat(nameValidationCalled.get()).isFalse();  // Short-circuit!
}
```

---

## Best Practices

### ✅ DO

```java
// Use Result para erros de negócio esperados
public Result<Order> placeOrder(Cart cart) {
    if (cart.isEmpty()) {
        return Result.fail("ORDER.EMPTY_CART", "Cart is empty");
    }
    // ...
    return Result.ok(order);
}

// Componha com flatMap
Result<Invoice> invoice = findOrder(orderId)
    .flatMap(order -> validateOrder(order))
    .flatMap(order -> generateInvoice(order));

// Use onOk/onFail para side effects
result
    .onOk(order -> logger.info("Order created: {}", order.id()))
    .onFail(problem -> metrics.increment("order.failed"));
```

### ❌ DON'T

```java
// ❌ NÃO use Result para erros de programação
public Result<User> findUser(UserId userId) {
    if (userId == null) {
        return Result.fail("USER_ID_NULL", "userId is null");
        // ❌ ERRADO - use Preconditions ou fail-fast
    }
}

// ❌ NÃO extraia imediatamente com getOrNull em toda chamada
Result<User> result = findUser(userId);
if (result.isOk()) {
    User user = result.getOrNull();  // ❌ Perde composição
}
// ✅ Melhor: use map/flatMap

// ❌ NÃO use exceptions para controle de fluxo quando tem Result
public Result<User> createUser(String email) {
    try {
        // ... lógica
        return Result.ok(user);
    } catch (ValidationException ex) {  // ❌ Use Result ao invés
        return Result.fail("VALIDATION", ex.getMessage());
    }
}
```

---

## Performance

- **Zero overhead** - Result é sealed interface, JIT otimiza
- **No stack traces** - Não lança exceptions (muito mais rápido)
- **Composição lazy** - map/flatMap só executa se Ok

---

## Ver Também

- [commons-kernel-errors](kernel-errors.md) - Problem e DomainException
- [commons-kernel-ddd](kernel-ddd.md) - Integração com entities
- [Guia: Error Handling](../guides/error-handling.md) - Estratégias completas
