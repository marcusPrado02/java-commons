# Commons App Data Validation

Flexible data validation framework with integration support for Jakarta Bean Validation.

## Features

- **Type-safe validation** - Generic validators for any type
- **Composable validators** - Combine multiple validators easily
- **Field validators** - Validate individual fields with predicates
- **Contextual validation** - Pass metadata during validation
- **Severity levels** - ERROR, WARNING, INFO violations
- **Common predicates** - Email, length, range, regex, and more
- **Jakarta integration** - Optional adapter for Bean Validation
- **Zero runtime dependencies** - Core functionality has no required dependencies

## Installation

```xml
<dependency>
    <groupId>com.marcusprado02</groupId>
    <artifactId>commons-app-data-validation</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Simple Field Validation

```java
import com.marcusprado02.commons.app.validation.*;

public class User {
    private String email;
    private String name;
    private int age;
    // getters/setters
}

// Create field validators
FieldValidator<User, String> emailValidator = FieldValidator.of(
    "email",
    User::getEmail,
    Validators.isEmail(),
    "Email must be valid"
);

FieldValidator<User, String> nameValidator = FieldValidator.of(
    "name",
    User::getName,
    Validators.notBlank(),
    "Name is required"
);

FieldValidator<User, Integer> ageValidator = FieldValidator.of(
    "age",
    User::getAge,
    Validators.isPositive(),
    "Age must be positive"
);

// Validate
ValidationResult result = emailValidator.validate(user);
if (result.hasViolations()) {
    result.getViolations().forEach(violation ->
        System.out.println(violation.getField() + ": " + violation.getMessage())
    );
}
```

### Composite Validation

```java
// Combine multiple validators
Validator<User> userValidator = CompositeValidator.of(
    FieldValidator.of("email", User::getEmail, Validators.isEmail(), "Invalid email"),
    FieldValidator.of("name", User::getName, Validators.notBlank(), "Name is required"),
    FieldValidator.of("age", User::getAge, Validators.range(18, 120), "Age must be 18-120")
);

ValidationResult result = userValidator.validate(user);
if (result.hasViolations()) {
    System.out.println("Found " + result.getViolations().size() + " violations");
    result.getErrors().forEach(error ->
        System.out.println(" - " + error.getMessage())
    );
}
```

### Using Common Validators

```java
import static com.marcusprado02.commons.app.validation.Validators.*;

// String validators
Predicate<String> notBlank = notBlank();
Predicate<String> email = isEmail();
Predicate<String> minLen = minLength(5);
Predicate<String> maxLen = maxLength(100);
Predicate<String> lenRange = lengthBetween(5, 100);
Predicate<String> pattern = matches("^[A-Z][a-z]+$");

// Number validators
Predicate<Integer> positive = isPositive();
Predicate<Integer> nonNegative = isNonNegative();
Predicate<Integer> range = range(1, 100);

// General validators
Predicate<Object> notNull = notNull();
```

### Custom Validators

```java
public class UserValidator implements Validator<User> {

    @Override
    public ValidationResult validate(User user) {
        if (user == null) {
            return ValidationResult.valid();
        }

        List<ValidationViolation> violations = new ArrayList<>();

        // Email validation
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            violations.add(
                ValidationViolation.builder()
                    .field("email")
                    .value(user.getEmail())
                    .message("Email must be valid")
                    .code("INVALID_EMAIL")
                    .build()
            );
        }

        // Name validation
        if (user.getName() == null || user.getName().isBlank()) {
            violations.add(
                ValidationViolation.builder()
                    .field("name")
                    .message("Name is required")
                    .code("NAME_REQUIRED")
                    .build()
            );
        }

        // Age validation
        if (user.getAge() < 18) {
            violations.add(
                ValidationViolation.builder()
                    .field("age")
                    .value(user.getAge())
                    .message("Age must be at least 18")
                    .code("AGE_TOO_LOW")
                    .severity(ViolationSeverity.WARNING)
                    .build()
            );
        }

        if (violations.isEmpty()) {
            return ValidationResult.valid();
        }

        return ValidationResult.invalid(violations);
    }
}
```

### Contextual Validation

For validation that depends on external state (e.g., database checks):

```java
public class UniqueEmailValidator implements ContextualValidator<User> {

    private final UserRepository userRepository;

    public UniqueEmailValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public ValidationResult validate(User user, ValidationContext context) {
        if (user.getEmail() == null) {
            return ValidationResult.valid();
        }

        // Get current user ID from context (for updates)
        String userId = context.get("userId", String.class);

        // Check if email exists for other users
        boolean exists = userRepository.existsByEmailAndIdNot(user.getEmail(), userId);

        if (exists) {
            return ValidationResult.invalid(
                ValidationViolation.builder()
                    .field("email")
                    .value(user.getEmail())
                    .message("Email already exists")
                    .code("EMAIL_NOT_UNIQUE")
                    .build()
            );
        }

        return ValidationResult.valid();
    }
}

// Usage
ValidationContext context = ValidationContext.builder()
    .put("userId", "123")
    .put("action", "update")
    .build();

ContextualValidator<User> validator = new UniqueEmailValidator(userRepository);
ValidationResult result = validator.validate(user, context);
```

### Violation Severity

```java
ValidationViolation error = ValidationViolation.builder()
    .field("email")
    .message("Email is required")
    .severity(ViolationSeverity.ERROR)
    .build();

ValidationViolation warning = ValidationViolation.builder()
    .field("age")
    .message("Age below 21 may have restrictions")
    .severity(ViolationSeverity.WARNING)
    .build();

ValidationResult result = ValidationResult.invalid(List.of(error, warning));

// Filter by severity
List<ValidationViolation> errors = result.getErrors();    // Only ERROR
List<ValidationViolation> warnings = result.getWarnings(); // Only WARNING

boolean hasErrors = result.hasErrors(); // true if any ERROR exists
```

## Jakarta Bean Validation Integration

Optional integration with Jakarta Bean Validation:

```xml
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.0.2</version>
</dependency>
<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>8.0.0.Final</version>
</dependency>
```

```java
import com.marcusprado02.commons.app.validation.jakarta.JakartaValidatorAdapter;
import jakarta.validation.constraints.*;

public class User {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank
    @Size(min = 3, max = 50)
    private String name;

    @Min(18)
    @Max(120)
    private int age;

    // getters/setters
}

// Create adapter
Validator<User> validator = JakartaValidatorAdapter.create();

// Validate
ValidationResult result = validator.validate(user);

// Validate specific property
ValidationResult emailResult =
    ((JakartaValidatorAdapter<User>) validator).validateProperty(user, "email");
```

## Spring Integration

```java
@Configuration
public class ValidationConfig {

    @Bean
    public Validator<UserDto> userValidator() {
        return CompositeValidator.of(
            FieldValidator.of("email", UserDto::getEmail,
                Validators.isEmail(), "Invalid email"),
            FieldValidator.of("name", UserDto::getName,
                Validators.lengthBetween(3, 50), "Name must be 3-50 characters"),
            FieldValidator.of("age", UserDto::getAge,
                Validators.range(18, 120), "Age must be 18-120")
        );
    }

    @Bean
    public ContextualValidator<UserDto> uniqueEmailValidator(UserRepository repository) {
        return new UniqueEmailValidator(repository);
    }
}

@Service
public class UserService {

    private final Validator<UserDto> validator;
    private final ContextualValidator<UserDto> uniqueEmailValidator;

    public UserService(
            Validator<UserDto> validator,
            ContextualValidator<UserDto> uniqueEmailValidator) {
        this.validator = validator;
        this.uniqueEmailValidator = uniqueEmailValidator;
    }

    public Result<User> createUser(UserDto dto) {
        // Basic validation
        ValidationResult validation = validator.validate(dto);
        if (validation.hasViolations()) {
            return Result.fail(
                ProblemBuilder.of("VALIDATION_FAILED")
                    .message("User validation failed")
                    .detail("violations", validation.getViolations())
                    .build()
            );
        }

        // Contextual validation
        ValidationContext context = ValidationContext.builder()
            .put("action", "create")
            .build();

        ValidationResult uniqueCheck = uniqueEmailValidator.validate(dto, context);
        if (uniqueCheck.hasViolations()) {
            return Result.fail(
                ProblemBuilder.of("EMAIL_EXISTS")
                    .message("Email already exists")
                    .build()
            );
        }

        // Create user
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());

        return Result.ok(user);
    }
}
```

## REST Controller Integration

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final Validator<CreateUserRequest> validator;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        ValidationResult validation = validator.validate(request);

        if (validation.hasViolations()) {
            Map<String, List<String>> errors = validation.getViolations().stream()
                .collect(Collectors.groupingBy(
                    ValidationViolation::getField,
                    Collectors.mapping(ValidationViolation::getMessage, Collectors.toList())
                ));

            return ResponseEntity.badRequest().body(Map.of(
                "status", "VALIDATION_FAILED",
                "errors", errors
            ));
        }

        // Process request...
        return ResponseEntity.ok().build();
    }
}
```

## Advanced Patterns

### Chaining Validators

```java
Validator<User> emailValidator =
    FieldValidator.of("email", User::getEmail, Validators.isEmail(), "Invalid email");

Validator<User> nameValidator =
    FieldValidator.of("name", User::getName, Validators.notBlank(), "Name required");

// Chain with and()
Validator<User> combined = emailValidator.and(nameValidator);

ValidationResult result = combined.validate(user);
```

### Conditional Validation

```java
public class ConditionalValidator<T> implements Validator<T> {

    private final Predicate<T> condition;
    private final Validator<T> validator;

    public ConditionalValidator(Predicate<T> condition, Validator<T> validator) {
        this.condition = condition;
        this.validator = validator;
    }

    @Override
    public ValidationResult validate(T value) {
        if (condition.test(value)) {
            return validator.validate(value);
        }
        return ValidationResult.valid();
    }
}

// Usage
Validator<Order> rushOrderValidator = new ConditionalValidator<>(
    order -> order.isRush(),
    FieldValidator.of("deliveryAddress", Order::getDeliveryAddress,
        Validators.notBlank(), "Rush orders require delivery address")
);
```

### Cross-Field Validation

```java
public class PasswordConfirmationValidator implements Validator<RegistrationForm> {

    @Override
    public ValidationResult validate(RegistrationForm form) {
        if (form.getPassword() == null || form.getPasswordConfirmation() == null) {
            return ValidationResult.valid();
        }

        if (!form.getPassword().equals(form.getPasswordConfirmation())) {
            return ValidationResult.invalid(
                ValidationViolation.builder()
                    .field("passwordConfirmation")
                    .message("Passwords do not match")
                    .code("PASSWORD_MISMATCH")
                    .build()
            );
        }

        return ValidationResult.valid();
    }
}
```

## Best Practices

1. **Fail Fast** - Validate early and return meaningful errors
2. **Separate Concerns** - Use field validators for single fields, custom validators for complex rules
3. **Meaningful Messages** - Provide clear, actionable error messages
4. **Use Codes** - Add error codes for internationalization
5. **Choose Severity** - Use WARNING for non-blocking issues
6. **Context When Needed** - Use contextual validation only when necessary
7. **Compose Validators** - Build complex validators from simple ones
8. **Test Thoroughly** - Validate both valid and invalid cases

## Thread Safety

All validators in this module are thread-safe and can be safely shared across threads. Validator instances should be created once and reused.

## License

This module is part of the commons library and follows the same license.
