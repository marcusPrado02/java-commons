# commons-kernel-result

Railway-oriented programming with `Result<T>` type for functional error handling.

## Overview

This module provides a robust `Result<T>` type that enables railway-oriented programming, allowing you to handle errors functionally without exceptions. It includes:

- **Result<T>**: Sealed interface for Ok/Fail states
- **AsyncResult<T>**: Asynchronous Result with CompletableFuture integration
- **ValidationResult<T>**: Multiple error aggregation for validation workflows
- **Results**: Utility methods for exception interop

## Core Features

### Result<T> - Railway-Oriented Programming

`Result<T>` is a sealed interface with two implementations:
- `Ok<T>`: Successful computation with value
- `Fail<T>`: Failed computation with Problem

```java
// Creating Results
Result<User> userResult = Result.ok(user);
Result<User> failureResult = Result.fail(problem);

// Basic operations
boolean isSuccess = result.isOk();
T value = result.getOrNull();
Problem error = result.problemOrNull();
```

### Mapping and Transformation

```java
// Map success value
Result<String> nameResult = userResult.map(User::getName);

// FlatMap for chaining Results
Result<Email> emailResult = userResult
    .flatMap(user -> validateEmail(user.getEmail()));

// Map error (Problem) to another Problem
Result<User> mappedError = userResult.mapError(problem ->
    Problem.of(
        ErrorCode.of("MAPPED_" + problem.code().value()),
        problem.category(),
        Severity.CRITICAL,
        "Critical: " + problem.message()
    )
);
```

### Recovery and Fallback

```java
// Recover from failure with value
Result<User> recovered = userResult.recover(problem -> defaultUser);

// Recover with another Result
Result<User> recoveredWith = userResult.recoverWith(problem ->
    findUserByEmail(fallbackEmail)
);

// Fallback to another Result
Result<User> withFallback = userResult.orElse(Result.ok(defaultUser));

// Lazy fallback evaluation
Result<User> lazyFallback = userResult.orElseGet(() ->
    loadDefaultUser()
);

// Get value with fallback
User user = userResult.getOrElse(defaultUser);
User user2 = userResult.getOrElseGet(() -> loadDefaultUser());
```

### Side Effects

```java
// Perform action on success (without changing Result)
userResult
    .peek(user -> log.info("User found: {}", user.getId()))
    .map(User::getName);

// Perform action on failure
userResult
    .peekError(problem -> log.error("Error: {}", problem.message()))
    .recover(problem -> defaultUser);
```

### Explicit Handling with Fold

```java
// Fold both cases into a single value
String message = userResult.fold(
    problem -> "Error: " + problem.message(),
    user -> "Success: " + user.getName()
);

// Use in HTTP responses
return userResult.fold(
    problem -> ResponseEntity.status(problem.status()).body(problem),
    user -> ResponseEntity.ok(user)
);
```

### Filtering

```java
// Filter with predicate
Result<User> adultUser = userResult.filter(
    user -> user.getAge() >= 18,
    () -> Problem.of(
        ErrorCode.of("AGE_RESTRICTION"),
        ErrorCategory.VALIDATION,
        Severity.ERROR,
        "User must be 18 or older"
    )
);
```

### Railway-Ori ented Pipeline

```java
Result<Order> processOrder(OrderRequest request) {
    return validateRequest(request)
        .flatMap(this::checkInventory)
        .flatMap(this::reserveItems)
        .flatMap(this::processPayment)
        .flatMap(this::createOrder)
        .peek(order -> publishOrderCreatedEvent(order))
        .peekError(problem -> log.error("Order failed: {}", problem.message()))
        .recoverWith(problem -> handleOrderFailure(problem, request));
}
```

## AsyncResult<T> - Asynchronous Operations

`AsyncResult<T>` wraps `CompletableFuture<Result<T>>` for async railway-oriented programming.

### Creating AsyncResults

```java
// From CompletableFuture<Result<T>>
CompletableFuture<Result<User>> future = CompletableFuture.supplyAsync(() ->
    findUser(userId)
);
AsyncResult<User> async = AsyncResult.of(future);

// From Result
AsyncResult<User> fromResult = AsyncResult.of(Result.ok(user));

// Direct creation
AsyncResult<User> success = AsyncResult.ok(user);
AsyncResult<User> failure = AsyncResult.fail(problem);

// From supplier (async execution)
AsyncResult<User> supplied = AsyncResult.supply(() -> findUser(userId));

// With custom executor
AsyncResult<User> customExecutor = AsyncResult.supply(
    () -> findUser(userId),
    executorService
);
```

### Async Mapping and Chaining

```java
AsyncResult<String> nameResult = asyncUser
    .map(User::getName)
    .map(String::toUpperCase);

AsyncResult<Email> emailResult = asyncUser
    .flatMap(user -> validateEmailSync(user.getEmail()));

// FlatMap with another AsyncResult
AsyncResult<Order> orderResult = asyncUser
    .flatMapAsync(user -> fetchOrderAsync(user.getId()));
```

### Async Recovery

```java
// Recover asynchronously
AsyncResult<User> recovered = asyncUser
    .recover(problem -> defaultUser);

// Recover with Result
AsyncResult<User> recoveredWith = asyncUser
    .recoverWith(problem -> findUserByEmail(fallbackEmail));

// Recover with another AsyncResult
AsyncResult<User> recoveredAsync = asyncUser
    .recoverWithAsync(problem -> loadDefaultUserAsync());
```

### Async Side Effects

```java
asyncUser
    .peek(user -> log.info("User loaded: {}", user.getId()))
    .peekError(problem -> log.error("Failed: {}", problem.message()))
    .map(User::getName);
```

### Conversion

```java
// Get underlying CompletableFuture<Result<T>>
CompletableFuture<Result<User>> future = asyncUser.toCompletableFuture();

// Block and get Result
Result<User> result = asyncUser.join();

// Unwrap to CompletableFuture<T> (throws exception on Fail)
CompletableFuture<User> futureValue = asyncUser.toCompletableFutureOfValue();
```

### Async Pipeline Example

```java
AsyncResult<Invoice> generateInvoice(OrderId orderId) {
    return AsyncResult.supply(() -> findOrder(orderId))
        .flatMapAsync(order -> validateOrderAsync(order))
        .flatMapAsync(order -> calculateTotalAsync(order))
        .flatMapAsync(total -> generateInvoiceAsync(orderId, total))
        .peek(invoice -> sendEmail(invoice))
        .peekError(problem -> notifyAdmin(problem))
        .recoverWithAsync(problem -> createDraftInvoice(orderId));
}
```

## ValidationResult<T> - Error Aggregation

`ValidationResult<T>` collects multiple errors instead of short-circuiting on the first failure.

### Creating ValidationResults

```java
// Valid result
ValidationResult<User> valid = ValidationResult.valid(user);

// Invalid with single error
ValidationResult<User> invalid = ValidationResult.invalid(problem);

// Invalid with multiple errors
ValidationResult<User> multipleErrors = ValidationResult.invalid(
    List.of(problem1, problem2, problem3)
);

// From Result
ValidationResult<User> fromResult = ValidationResult.of(result);
```

### Validation Builder Pattern

```java
// Validate multiple fields
ValidationResult<User> validation = ValidationResult.builder()
    .add(validateEmail(user.getEmail()))
    .add(validatePassword(user.getPassword()))
    .add(validateAge(user.getAge()))
    .add(validateName(user.getName()))
    .build(user);

// Only compute value if all validations pass
ValidationResult<User> lazyValidation = ValidationResult.builder()
    .add(validateEmail(email))
    .add(validatePassword(password))
    .build(() -> createUser(email, password));
```

### Combining Validations

```java
ValidationResult<String> emailValidation = validateEmail(email);
ValidationResult<String> passwordValidation = validatePassword(password);

// Combine validations (collects all errors)
ValidationResult<User> combined = emailValidation.combine(
    passwordValidation,
    email -> password -> new User(email, password)
);
```

### Mapping and Chaining

```java
// Map valid value
ValidationResult<String> nameValidation = userValidation
    .map(User::getName);

// Chain validations
ValidationResult<Account> accountValidation = userValidation
    .andThen(user -> validateAccount(user.getAccountId()));
```

### Converting to Result

```java
// Convert to Result (combines multiple Problems)
Result<User> result = validation.toResult();

// Custom Problem combiner
Result<User> customResult = validation.toResult(problems ->
    Problem.of(
        ErrorCode.of("VALIDATION_FAILED"),
        ErrorCategory.VALIDATION,
        Severity.ERROR,
        "Multiple validation errors: " + problems.size()
    )
);
```

### Validation Example

```java
Result<User> registerUser(RegistrationRequest request) {
    Result<String> emailValidation = validateEmail(request.getEmail());
    Result<String> passwordValidation = validatePassword(request.getPassword());
    Result<Integer> ageValidation = validateAge(request.getAge());
    Result<String> nameValidation = validateName(request.getName());

    return ValidationResult.builder()
        .add(emailValidation)
        .add(passwordValidation)
        .add(ageValidation)
        .add(nameValidation)
        .build(() -> createUser(request))
        .toResult();
}

// If any validation fails, all errors are collected:
// Problem: "Validation failed:
//   - Email must be valid
//   - Password must be at least 8 characters
//   - Age must be 18 or older"
```

## Results - Exception Interop

Utility methods for converting between exceptions and Results.

```java
// Convert DomainException to Result.fail
Result<User> result = Results.fromException(() -> {
    if (user == null) throw new NotFoundException("User not found");
    return user;
});

// Unwrap Result or throw DomainException
User user = Results.orThrow(userResult);

// Unwrap Result or throw custom exception
User user = Results.orThrow(userResult, problem ->
    new IllegalStateException(problem.message())
);

// Catch DomainException and return Result
Result<User> result = Results.catchingDomain(() -> findUser(id));

// Catch any exception and wrap value in Result
Result<User> result = Results.catchingDomainValue(() -> {
    return repository.findById(userId);
});
```

## Best Practices

### ✅ DO: Use Result for Expected Failures

```java
// Good: Business logic failures are expected
Result<Order> placeOrder(Cart cart) {
    if (cart.isEmpty()) {
        return Result.fail(Problem.of(
            ErrorCode.of("EMPTY_CART"),
            ErrorCategory.BUSINESS,
            Severity.ERROR,
            "Cannot place order with empty cart"
        ));
    }
    return Result.ok(createOrder(cart));
}
```

### ❌ DON'T: Use Result for Programming Errors

```java
// Bad: NullPointerException is a programming error
Result<String> getName(User user) {
    return Result.ok(user.getName()); // Let NPE propagate if user is null
}

// Good: Null check with validation
Result<String> getName(User user) {
    if (user == null) {
        return Result.fail(Problem.of(/*...*/));
    }
    return Result.ok(user.getName());
}
```

### ✅ DO: Chain Operations for Readability

```java
Result<Receipt> processPayment(OrderId orderId, PaymentInfo payment) {
    return findOrder(orderId)
        .flatMap(order -> validatePaymentAmount(order, payment))
        .flatMap(order -> chargePayment(order, payment))
        .flatMap(transaction -> generateReceipt(transaction))
        .peek(receipt -> sendConfirmationEmail(receipt))
        .peekError(problem -> log.error("Payment failed: {}", problem.message()));
}
```

### ✅ DO: Use ValidationResult for Multiple Independent Checks

```java
// Good: Collect all validation errors at once
Result<User> validateUser(UserRequest request) {
    return ValidationResult.builder()
        .add(validateEmail(request.getEmail()))
        .add(validatePassword(request.getPassword()))
        .add(validateAge(request.getAge()))
        .build(() -> new User(request))
        .toResult();
}
```

### ❌ DON'T: Use flatMap for Independent Validations

```java
// Bad: Stops at first error, user doesn't see all validation issues
Result<User> validateUser(UserRequest request) {
    return validateEmail(request.getEmail())
        .flatMap(email -> validatePassword(request.getPassword()))
        .flatMap(password -> validateAge(request.getAge()))
        .map(age -> new User(request));
}
```

### ✅ DO: Use AsyncResult for I/O Operations

```java
AsyncResult<OrderSummary> getOrderSummary(OrderId id) {
    return AsyncResult.supply(() -> loadOrder(id), executorService)
        .flatMapAsync(order -> loadCustomerAsync(order.getCustomerId()))
        .flatMapAsync(customer -> enrichWithPreferencesAsync(customer))
        .map(customer -> createSummary(customer));
}
```

### ✅ DO: Use fold for Explicit Error Handling

```java
// Good: Both cases handled explicitly
return userResult.fold(
    problem -> ResponseEntity.status(mapStatusCode(problem)).body(problem),
    user -> ResponseEntity.ok(toDTO(user))
);
```

### ❌ DON'T: Ignore Errors

```java
// Bad: Error case ignored
userResult.map(User::getName).getOrNull();

// Good: Explicit handling
String name = userResult.fold(
    problem -> "Unknown",
    User::getName
);
```

## Comparison with Exceptions

### Traditional Exception-Based Code

```java
public User findUser(UserId id) throws UserNotFoundException, DatabaseException {
    User user = repository.findById(id);
    if (user == null) {
        throw new UserNotFoundException(id);
    }
    return user;
}

// Caller must handle exceptions
try {
    User user = findUser(userId);
    String email = user.getEmail();
    sendWelcomeEmail(email);
} catch (UserNotFoundException e) {
    log.error("User not found", e);
} catch (DatabaseException e) {
    log.error("Database error", e);
}
```

### Result-Based Code

```java
public Result<User> findUser(UserId id) {
    return repository.findById(id)
        .map(Result::ok)
        .orElseGet(() -> Result.fail(Problem.of(
            ErrorCode.of("USER_NOT_FOUND"),
            ErrorCategory.NOT_FOUND,
            Severity.ERROR,
            "User not found: " + id
        )));
}

// Caller uses railway-oriented programming
findUser(userId)
    .map(User::getEmail)
    .flatMap(this::sendWelcomeEmail)
    .peekError(problem -> log.error("Failed: {}", problem.message()));
```

## Integration with DDD

```java
// Domain service using Result
@Service
public class OrderService {

    public Result<Order> placeOrder(PlaceOrderCommand command) {
        return ValidationResult.builder()
            .add(validateItems(command.getItems()))
            .add(validateShippingAddress(command.getAddress()))
            .add(validatePaymentMethod(command.getPayment()))
            .build(() -> command)
            .toResult()
            .flatMap(this::checkInventory)
            .flatMap(this::calculateTotal)
            .flatMap(this::processPayment)
            .flatMap(this ::createOrder)
            .peek(order -> eventBus.publish(new OrderPlacedEvent(order)));
    }
}
```

## References

- **Railway-Oriented Programming**: https://fsharpforfunandprofit.com/rop/
- **Functional Error Handling**: https://www.youtube.com/watch?v=srQt1NAHYC0
- **Result Type Pattern**: https://thoughtbot.com/blog/railway-oriented-programming-in-elixir

## License

See parent project LICENSE.
