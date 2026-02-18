# API Reference: Data Validation

## Visão Geral

`commons-app-data-validation` fornece validação de dados robusta com suporte a Bean Validation (JSR 380), validações customizadas, validações assíncronas e composição de regras.

**Quando usar:**
- Validar entrada de APIs REST
- Validar comandos de aplicação
- Validar dados antes de persistência
- Validar regras de negócio complexas
- Validar dados de forma assíncrona

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-data-validation</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Bean Validation API -->
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>

<!-- Hibernate Validator (implementation) -->
<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
</dependency>
```

---

## 🔑 Core Components

### ValidationService

Serviço central para validação com Result pattern.

```java
public interface ValidationService {
    
    /**
     * Valida objeto e retorna Result com lista de erros.
     */
    <T> Result<T> validate(T object);
    
    /**
     * Valida objeto em grupo específico.
     */
    <T> Result<T> validate(T object, Class<?>... groups);
    
    /**
     * Valida propriedade específica.
     */
    <T> Result<Void> validateProperty(
        T object,
        String propertyName
    );
    
    /**
     * Valida valor para propriedade.
     */
    <T> Result<Void> validateValue(
        Class<T> beanType,
        String propertyName,
        Object value
    );
}
```

### ValidationFacade

API simplificada para validação rápida.

```java
public class ValidationFacade {
    
    /**
     * Valida e lança exceção se inválido.
     */
    public static <T> T validateOrThrow(T object) {
        Result<T> result = ValidationService.getInstance().validate(object);
        return result.getOrThrow();
    }
    
    /**
     * Valida e retorna lista de erros.
     */
    public static <T> List<ValidationError> validate(T object) {
        ValidationService service = ValidationService.getInstance();
        Result<T> result = service.validate(object);
        
        return result.isFail()
            ? extractErrors(result.problemOrNull())
            : List.of();
    }
}
```

---

## 💡 Uso Básico

### Bean Validation

```java
public class CreateOrderCommand {
    
    @NotNull(message = "Customer ID is required")
    private CustomerId customerId;
    
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemCommand> items;
    
    @NotNull
    @DecimalMin(value = "0.01", message = "Total must be positive")
    private BigDecimal total;
    
    @Email(message = "Invalid email format")
    private String customerEmail;
    
    @Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Invalid phone number"
    )
    private String customerPhone;
}

public class OrderItemCommand {
    
    @NotNull(message = "Product ID is required")
    private ProductId productId;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100, message = "Quantity cannot exceed 100")
    private Integer quantity;
    
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal unitPrice;
}
```

### Validação com Result Pattern

```java
@Service
public class OrderApplicationService {
    
    private final ValidationService validationService;
    private final OrderRepository orderRepository;
    
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        // Validar command
        Result<CreateOrderCommand> validationResult = 
            validationService.validate(command);
        
        if (validationResult.isFail()) {
            return Result.fail(validationResult.problemOrNull());
        }
        
        // Continuar com lógica de negócio
        Order order = Order.create(command);
        Result<Void> saveResult = orderRepository.save(order);
        
        return saveResult.map(() -> order.id());
    }
}
```

### Validação com Exception

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @PostMapping
    public ResponseEntity<?> createOrder(
        @RequestBody @Valid CreateOrderRequest request
    ) {
        // @Valid automaticamente valida e lança exception
        // ValidationExceptionHandler captura e retorna Problem Details
        
        CreateOrderCommand command = toCommand(request);
        Result<OrderId> result = orderService.createOrder(command);
        
        return result
            .map(orderId -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("orderId", orderId.value())))
            .recover(problem -> ResponseEntity
                .status(mapStatus(problem))
                .body(toProblemDetail(problem)));
    }
}
```

---

## 🎯 Custom Validations

### Custom Annotation

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CpfValidator.class)
@Documented
public @interface ValidCpf {
    
    String message() default "Invalid CPF";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
```

### Custom Validator

```java
public class CpfValidator implements ConstraintValidator<ValidCpf, String> {
    
    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null || cpf.isBlank()) {
            return true;  // Use @NotNull para obrigatoriedade
        }
        
        // Remove formatting
        String digits = cpf.replaceAll("[^0-9]", "");
        
        if (digits.length() != 11) {
            return false;
        }
        
        // Check for repeated digits
        if (digits.matches("(\\d)\\1{10}")) {
            return false;
        }
        
        // Validate check digits
        return validateCheckDigits(digits);
    }
    
    private boolean validateCheckDigits(String cpf) {
        int[] weights1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] weights2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
        
        int sum1 = 0, sum2 = 0;
        
        for (int i = 0; i < 9; i++) {
            int digit = Character.getNumericValue(cpf.charAt(i));
            sum1 += digit * weights1[i];
            sum2 += digit * weights2[i];
        }
        
        int checkDigit1 = Character.getNumericValue(cpf.charAt(9));
        int checkDigit2 = Character.getNumericValue(cpf.charAt(10));
        
        sum2 += checkDigit1 * weights2[9];
        
        int remainder1 = sum1 % 11;
        int remainder2 = sum2 % 11;
        
        int expectedCheckDigit1 = (remainder1 < 2) ? 0 : 11 - remainder1;
        int expectedCheckDigit2 = (remainder2 < 2) ? 0 : 11 - remainder2;
        
        return checkDigit1 == expectedCheckDigit1 && checkDigit2 == expectedCheckDigit2;
    }
}
```

### Uso da Custom Annotation

```java
public class CreateUserCommand {
    
    @NotBlank
    private String name;
    
    @Email
    private String email;
    
    @ValidCpf(message = "CPF inválido")
    private String cpf;
}
```

---

## 📊 Validation Groups

Validação condicional baseada em grupos.

```java
public interface ValidationGroups {
    interface Create {}
    interface Update {}
    interface Complete {}
}

public class UserCommand {
    
    @Null(groups = Create.class, message = "ID must be null for creation")
    @NotNull(groups = Update.class, message = "ID is required for update")
    private UserId id;
    
    @NotBlank(groups = {Create.class, Update.class})
    @Size(min = 3, max = 100)
    private String name;
    
    @NotBlank(groups = Create.class, message = "Email required for creation")
    @Email
    private String email;
    
    @NotBlank(groups = Complete.class)
    @ValidCpf
    private String cpf;  // Obrigatório apenas para usuário "completo"
}
```

### Uso de Groups

```java
@Service
public class UserService {
    
    private final ValidationService validationService;
    
    public Result<UserId> createUser(UserCommand command) {
        // Valida com grupo Create
        Result<UserCommand> validation = 
            validationService.validate(command, ValidationGroups.Create.class);
        
        if (validation.isFail()) {
            return Result.fail(validation.problemOrNull());
        }
        
        // ...
    }
    
    public Result<Void> completeUserProfile(UserCommand command) {
        // Valida com grupo Complete
        Result<UserCommand> validation = 
            validationService.validate(command, ValidationGroups.Complete.class);
        
        if (validation.isFail()) {
            return Result.fail(validation.problemOrNull());
        }
        
        // ...
    }
}
```

---

## 🔗 Cross-Field Validation

Validação entre múltiplos campos.

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
    
    String message() default "End date must be after start date";
    
    String startField();
    
    String endField();
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
```

```java
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {
    
    private String startField;
    private String endField;
    
    @Override
    public void initialize(ValidDateRange annotation) {
        this.startField = annotation.startField();
        this.endField = annotation.endField();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            LocalDate startDate = (LocalDate) getFieldValue(value, startField);
            LocalDate endDate = (LocalDate) getFieldValue(value, endField);
            
            if (startDate == null || endDate == null) {
                return true;  // Use @NotNull nos campos
            }
            
            return !endDate.isBefore(startDate);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private Object getFieldValue(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}
```

### Uso de Cross-Field Validation

```java
@ValidDateRange(
    startField = "startDate",
    endField = "endDate",
    message = "Check-out must be after check-in"
)
public class BookingCommand {
    
    @NotNull
    @FutureOrPresent
    private LocalDate startDate;
    
    @NotNull
    @Future
    private LocalDate endDate;
}
```

---

## ⚡ Async Validation

Validação assíncrona para regras que exigem I/O.

```java
public interface AsyncValidator<T> {
    CompletableFuture<Result<T>> validateAsync(T value);
}
```

```java
@Component
public class UniqueEmailValidator implements AsyncValidator<String> {
    
    private final UserRepository userRepository;
    
    @Override
    public CompletableFuture<Result<String>> validateAsync(String email) {
        return userRepository.existsByEmail(email)
            .thenApply(exists -> {
                if (exists) {
                    return Result.fail(Problem.of(
                        "USER.EMAIL_ALREADY_EXISTS",
                        "Email already in use"
                    ));
                }
                return Result.ok(email);
            });
    }
}
```

```java
@Service
public class UserRegistrationService {
    
    private final ValidationService validationService;
    private final UniqueEmailValidator emailValidator;
    
    public CompletableFuture<Result<UserId>> registerUser(
        RegisterUserCommand command
    ) {
        // Validação síncrona primeiro
        Result<RegisterUserCommand> syncValidation = 
            validationService.validate(command);
        
        if (syncValidation.isFail()) {
            return CompletableFuture.completedFuture(
                Result.fail(syncValidation.problemOrNull())
            );
        }
        
        // Validação assíncrona
        return emailValidator.validateAsync(command.email())
            .thenCompose(emailResult -> {
                if (emailResult.isFail()) {
                    return CompletableFuture.completedFuture(
                        Result.fail(emailResult.problemOrNull())
                    );
                }
                
                // Criar usuário
                User user = User.create(command);
                return userRepository.saveAsync(user)
                    .thenApply(saveResult -> saveResult.map(() -> user.id()));
            });
    }
}
```

---

## 🧩 Validation Composition

Compose validações complexas com combinadores.

```java
public class ValidationComposer {
    
    public static <T> Result<T> composeAll(T value, List<Validator<T>> validators) {
        List<Problem> errors = new ArrayList<>();
        
        for (Validator<T> validator : validators) {
            Result<T> result = validator.validate(value);
            if (result.isFail()) {
                errors.add(result.problemOrNull());
            }
        }
        
        if (!errors.isEmpty()) {
            return Result.fail(Problem.aggregate(errors));
        }
        
        return Result.ok(value);
    }
    
    public static <T> Result<T> composeAny(T value, List<Validator<T>> validators) {
        for (Validator<T> validator : validators) {
            Result<T> result = validator.validate(value);
            if (result.isOk()) {
                return result;
            }
        }
        
        return Result.fail(Problem.of(
            "VALIDATION.ALL_FAILED",
            "All validations failed"
        ));
    }
}
```

### Exemplo de Composição

```java
@Service
public class PasswordValidator {
    
    private final List<Validator<String>> rules = List.of(
        new MinLengthValidator(8),
        new MaxLengthValidator(100),
        new ContainsUppercaseValidator(),
        new ContainsLowercaseValidator(),
        new ContainsDigitValidator(),
        new ContainsSpecialCharValidator(),
        new NotCommonPasswordValidator()
    );
    
    public Result<String> validate(String password) {
        return ValidationComposer.composeAll(password, rules);
    }
}
```

---

## 🛡️ Domain Validation

Validação integrada com domain layer.

```java
public class Email extends ValueObject {
    
    private final String value;
    
    private Email(String value) {
        this.value = value;
    }
    
    public static Result<Email> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.fail(Problem.of(
                "EMAIL.BLANK",
                "Email cannot be blank"
            ));
        }
        
        if (!isValid(value)) {
            return Result.fail(Problem.of(
                "EMAIL.INVALID_FORMAT",
                "Invalid email format"
            ));
        }
        
        return Result.ok(new Email(value.toLowerCase().trim()));
    }
    
    private static boolean isValid(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(regex);
    }
    
    public String value() {
        return value;
    }
}
```

```java
public class User extends AggregateRoot<UserId> {
    
    private Email email;
    private UserProfile profile;
    
    public Result<Void> changeEmail(Email newEmail) {
        // Validação de invariante
        if (this.email.equals(newEmail)) {
            return Result.fail(Problem.of(
                "USER.EMAIL_UNCHANGED",
                "New email must be different"
            ));
        }
        
        this.email = newEmail;
        recordEvent(new UserEmailChanged(id(), newEmail, Instant.now()));
        
        return Result.ok();
    }
    
    public Result<Void> completeProfile(UserProfile profile) {
        // Validação condicional
        if (this.profile != null && this.profile.isComplete()) {
            return Result.fail(Problem.of(
                "USER.PROFILE_ALREADY_COMPLETE",
                "Profile is already complete"
            ));
        }
        
        Result<Void> validation = profile.validate();
        if (validation.isFail()) {
            return validation;
        }
        
        this.profile = profile;
        recordEvent(new UserProfileCompleted(id(), Instant.now()));
        
        return Result.ok();
    }
}
```

---

## 🧪 Testing

### Unit Tests

```java
class CreateOrderCommandTest {
    
    private final ValidationService validationService = 
        new DefaultValidationService();
    
    @Test
    void shouldFailWhenCustomerIdIsNull() {
        CreateOrderCommand command = new CreateOrderCommand(
            null,  // customerId
            List.of(new OrderItemCommand(...)),
            BigDecimal.valueOf(100)
        );
        
        Result<CreateOrderCommand> result = validationService.validate(command);
        
        assertThat(result.isFail()).isTrue();
        Problem problem = result.problemOrNull();
        assertThat(problem.code()).isEqualTo("VALIDATION.FAILED");
        assertThat(problem.detail()).contains("Customer ID is required");
    }
    
    @Test
    void shouldFailWhenItemsIsEmpty() {
        CreateOrderCommand command = new CreateOrderCommand(
            CustomerId.generate(),
            List.of(),  // Empty items
            BigDecimal.ZERO
        );
        
        Result<CreateOrderCommand> result = validationService.validate(command);
        
        assertThat(result.isFail()).isTrue();
        assertThat(result.problemOrNull().detail())
            .contains("Order must have at least one item");
    }
}
```

### Custom Validator Tests

```java
class CpfValidatorTest {
    
    private CpfValidator validator;
    private ConstraintValidatorContext context;
    
    @BeforeEach
    void setUp() {
        validator = new CpfValidator();
        context = mock(ConstraintValidatorContext.class);
    }
    
    @Test
    void shouldAcceptValidCpf() {
        assertThat(validator.isValid("123.456.789-09", context)).isTrue();
    }
    
    @Test
    void shouldRejectInvalidCpf() {
        assertThat(validator.isValid("123.456.789-00", context)).isFalse();
    }
    
    @Test
    void shouldRejectRepeatedDigits() {
        assertThat(validator.isValid("111.111.111-11", context)).isFalse();
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use Bean Validation para validações simples
@NotNull
@Size(min = 3, max = 100)
private String name;

// ✅ Use custom validators para lógica complexa
@ValidCpf
private String cpf;

// ✅ Use validation groups para diferentes contextos
validationService.validate(command, Create.class);

// ✅ Valide invariantes no domain layer
public Result<Void> changeStatus(OrderStatus newStatus) {
    if (!canTransitionTo(newStatus)) {
        return Result.fail(Problem.of(...));
    }
    // ...
}

// ✅ Use Result pattern para composição
return validationResult
    .flatMap(cmd -> orderService.createOrder(cmd));
```

### ❌ DON'T

```java
// ❌ NÃO valide apenas no controller
@PostMapping
public ResponseEntity<?> create(@RequestBody CreateOrderRequest request) {
    // ❌ Sem validação! Controller pode ser bypassed
    return orderService.create(request);
}

// ❌ NÃO lance exceptions genéricas
if (name.isBlank()) {
    throw new RuntimeException("Name is blank");  // ❌
}

// ❌ NÃO retorne null em validações
public String validate(String value) {
    if (invalid) return null;  // ❌ Use Result ou throw
}

// ❌ NÃO misture validação de formato e regras de negócio
@AssertTrue  // ❌ Regra de negócio, não validação de formato
public boolean isEligibleForDiscount() {
    return totalAmount.compareTo(BigDecimal.valueOf(100)) > 0;
}
```

---

## Ver Também

- [Kernel Errors](kernel-errors.md) - Problem e DomainException
- [Error Handling Guide](../guides/error-handling.md) - Estratégias de tratamento de erros
- [Testing Guide](../guides/testing.md) - Testes de validação
