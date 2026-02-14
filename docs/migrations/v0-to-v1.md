# Migration Guide: v0.x to v1.0

This guide helps you migrate from version 0.x (SNAPSHOT) to version 1.0.0 of Java Commons.

## Overview

Version 1.0.0 is the first stable release of Java Commons. While we've strived to maintain compatibility with the 0.x snapshots, some breaking changes were necessary to improve the API design and align with best practices.

## Breaking Changes

### 1. Result API Changes

**What changed**: The `Result` type now uses `Optional` internally instead of nullable values.

**Before (v0.x)**:
```java
Result<User> result = userService.findUser(id);
if (result.isSuccess()) {
    User user = result.getValue(); // Could be null
}
```

**After (v1.0.0)**:
```java
Result<User> result = userService.findUser(id);
if (result.isSuccess()) {
    User user = result.get(); // Never null, throws if failure
    // Or use safely:
    result.ifSuccess(user -> { ... });
}
```

**Migration**:
- Replace `getValue()` with `get()` or `getOrNull()`
- Use `ifSuccess()`, `map()`, `flatMap()` for safer access
- Update null checks to handle `Optional` instead

**Automated migration**:
```bash
# Find all usages
grep -r "\.getValue()" src/

# Replace (use with caution)
find src/ -type f -name "*.java" -exec sed -i 's/\.getValue()/\.get()/g' {} +
```

---

### 2. DomainEvent.occurredOn() Returns Instant

**What changed**: `DomainEvent.occurredOn()` now returns `Instant` instead of `ZonedDateTime`.

**Before (v0.x)**:
```java
public interface DomainEvent {
    ZonedDateTime occurredOn();
}
```

**After (v1.0.0)**:
```java
public interface DomainEvent {
    Instant occurredOn();
}
```

**Migration**:
```java
// Before
ZonedDateTime timestamp = event.occurredOn();
String formatted = timestamp.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);

// After
Instant timestamp = event.occurredOn();
String formatted = timestamp.toString(); // ISO-8601 format
// Or if you need ZonedDateTime:
ZonedDateTime zdt = timestamp.atZone(ZoneId.systemDefault());
```

**Reason**: `Instant` is timezone-agnostic and better suited for event timestamps in distributed systems.

---

### 3. Repository Interface Signature Change

**What changed**: `Repository.save()` now returns `Result<T>` instead of throwing exceptions.

**Before (v0.x)**:
```java
public interface Repository<T, ID> {
    T save(T entity) throws PersistenceException;
}

// Usage
try {
    User saved = userRepository.save(user);
} catch (PersistenceException e) {
    log.error("Failed to save user", e);
}
```

**After (v1.0.0)**:
```java
public interface Repository<T, ID> {
    Result<T> save(T entity);
}

// Usage
Result<User> result = userRepository.save(user);
result.ifFailure(error -> log.error("Failed to save user: {}", error));
```

**Migration**:
1. Update repository implementations to return `Result<T>`
2. Replace try-catch blocks with `Result` handling:
   ```java
   // Pattern 1: Using ifSuccess/ifFailure
   userRepository.save(user)
       .ifSuccess(saved -> log.info("Saved user {}", saved.getId()))
       .ifFailure(error -> log.error("Save failed: {}", error));
   
   // Pattern 2: Using map/flatMap
   return userRepository.save(user)
       .map(User::getId)
       .flatMap(notificationService::notifyUserCreated);
   ```

---

### 4. ConfigurationProvider.get() Returns Optional

**What changed**: `ConfigurationProvider.get(String key)` now returns `Optional<String>` instead of `String`.

**Before (v0.x)**:
```java
String value = config.get("app.name"); // Could be null
if (value != null) {
    // Use value
}
```

**After (v1.0.0)**:
```java
Optional<String> value = config.get("app.name");
value.ifPresent(name -> {
    // Use name
});

// Or with default
String name = config.get("app.name").orElse("default-name");
```

**Migration**:
- Replace null checks with Optional methods
- Use `orElse()`, `orElseGet()`, or `orElseThrow()` for defaults

---

## New Features

### 1. Configuration Validation

Version 1.0.0 introduces configuration validation:

```java
ConfigurationValidator validator = ConfigurationValidator.builder()
    .required("app.name")
    .required("server.port")
    .pattern("app.email", "^[A-Za-z0-9+_.-]+@(.+)$")
    .range("server.port", 1024, 65535)
    .build();

ValidationResult result = validator.validate(configProvider);
if (result.hasErrors()) {
    throw new ConfigurationValidationException(result);
}
```

### 2. Feature Flags

New feature flags support with percentage rollout:

```java
FeatureFlags flags = InMemoryFeatureFlags.builder()
    .enable("new-ui")
    .enableForPercentage("beta-feature", 25.0)
    .build();

if (flags.isEnabled("new-ui", userId)) {
    // Show new UI
}
```

### 3. Enhanced Result Operations

New Result operators:

```java
Result<User> result = userService.findUser(id)
    .filter(user -> user.isActive(), "User is not active")
    .map(User::getEmail)
    .recover(error -> "unknown@example.com");
```

---

## Deprecated APIs

The following APIs are deprecated and will be removed in v2.0.0:

### 1. `Result.getOrThrow()`

**Deprecated**: Use `get()` or `getOrElse()` instead.

```java
// Deprecated
User user = result.getOrThrow(); // Throws RuntimeException

// Use instead
User user = result.get(); // Throws typed exception
User user = result.getOrElse(User.guest()); // Provides default
```

### 2. `DomainEventBus.publishAsync(DomainEvent)`

**Deprecated**: Use `publish(DomainEvent)` - all events are async by default.

```java
// Deprecated
eventBus.publishAsync(event);

// Use instead
eventBus.publish(event); // Already async
```

---

## Dependency Changes

### Updated Dependencies

| Dependency | v0.x | v1.0.0 | Notes |
|-----------|------|--------|-------|
| Spring Boot | 3.2.x | 3.3.x | Minor update |
| Jackson | 2.15.x | 2.17.x | JSON processing improvements |
| Resilience4j | 2.1.x | 2.2.x | New retry strategies |

### New Optional Dependencies

```xml
<!-- For configuration validation -->
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.0.2</version>
    <optional>true</optional>
</dependency>

<!-- For observability -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
    <version>1.36.0</version>
    <optional>true</optional>
</dependency>
```

---

## Testing Changes

### Test Dependencies

Update your test dependencies:

```xml
<!-- Before (v0.x) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-testkit</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>

<!-- After (v1.0.0) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-testkit-core</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

### Test Utilities Changes

**Before (v0.x)**:
```java
import com.marcusprado02.commons.testkit.TestData;
TestData.createUser();
```

**After (v1.0.0)**:
```java
import com.marcusprado02.commons.testkit.core.builders.UserBuilder;
User user = UserBuilder.aUser().build();
```

---

## Configuration Changes

### application.yml/properties

Some configuration property names have changed:

```yaml
# Before (v0.x)
commons:
  observability:
    tracing: true
    metrics: true

# After (v1.0.0)
commons:
  observability:
    tracing:
      enabled: true
    metrics:
      enabled: true
```

### Spring Boot Starters

Starter artifact IDs have been clarified:

```xml
<!-- Before (v0.x) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-spring-observability</artifactId>
</dependency>

<!-- After (v1.0.0) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-spring-starter-observability</artifactId>
</dependency>
```

---

## Step-by-Step Migration

### 1. Update Dependencies

In your `pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.marcusprado02.commons</groupId>
            <artifactId>commons-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. Run Build

```bash
./mvnw clean compile
```

Fix any compilation errors related to:
- `Result.getValue()` → `Result.get()`
- `DomainEvent.occurredOn()` → Returns `Instant`
- `Repository.save()` → Returns `Result<T>`

### 3. Update Tests

```bash
./mvnw test
```

Fix test failures, especially:
- Mock return types (e.g., repositories now return `Result<T>`)
- Assertions on timestamps (now `Instant` instead of `ZonedDateTime`)

### 4. Update Configuration

- Review and update `application.yml`/`application.properties`
- Add configuration validation if desired

### 5. Code Review

Search for deprecated API usage:

```bash
# Find deprecated usages (may show warnings)
./mvnw clean compile 2>&1 | grep -i deprecated
```

### 6. Run Integration Tests

```bash
./mvnw verify
```

### 7. Deploy to Staging

Test in a staging environment before production.

---

## Rollback Plan

If you encounter issues after upgrading:

1. **Revert dependency version**:
   ```xml
   <commons.version>0.9.0-SNAPSHOT</commons.version>
   ```

2. **Rebuild**:
   ```bash
   ./mvnw clean install
   ```

3. **Report issues**: [GitHub Issues](https://github.com/marcusPrado02/java-commons/issues)

---

## Getting Help

- **Migration Issues**: [GitHub Issues](https://github.com/marcusPrado02/java-commons/issues)
- **Questions**: [GitHub Discussions](https://github.com/marcusPrado02/java-commons/discussions)
- **Documentation**: [docs/](../)

---

## Estimated Migration Time

| Project Size | Estimated Time |
|-------------|----------------|
| Small (< 10k LOC) | 1-2 hours |
| Medium (10k-50k LOC) | 4-8 hours |
| Large (> 50k LOC) | 1-2 days |

---

**Last Updated**: 2026-02-14  
**Version**: 1.0.0
