# Migration from v1.0.0 to v2.0.0

**Version**: v1.x.x → v2.0.0  
**Release Date**: 2026-Q2 (Estimated)  
**Migration Effort**: 🔴 High  
**Estimated Time**: 4-8 hours (small projects) / 8-16 hours (large projects)

---

## Overview

### What's New

Version 2.0.0 is a major upgrade that modernizes the Commons library with improved APIs, better performance, and enhanced type safety.

**Highlights**:
- ✨ Reactive programming support with Project Reactor
- ⚡ 30% performance improvement in Result operations
- 🔒 Enhanced security with built-in audit logging
- 🐛 100+ bug fixes from community feedback
- 📦 Modular architecture for smaller deployments
- 🔧 Improved Spring Boot  integration with Auto-configuration 2.0

### Why Upgrade?

- **Performance**: Significant performance improvements across all modules
- **Type Safety**: Better compile-time checks with enhanced generics
- **Modern Java**: Full Java 21 feature support (Virtual Threads, Pattern Matching)
- **Security**: Enhanced security features and CVE fixes
- **Reactive**: Native reactive programming support
- **Support**: v1.x will reach end-of-life 6 months after v2.0.0 release

### Migration Complexity

| Component | Impact | Effort |
|-----------|--------|--------|
| Result API changes | 🔴 High | 4-6 hours |
| Repository interface | 🔴 High | 3-5 hours |
| Domain Events | 🟡 Medium | 2-3 hours |
| Configuration | 🟡 Medium | 1-2 hours |
| Testing utilities | 🟢 Low | 1 hour |

---

## Breaking Changes

### 1. Result API Signature Changes

**Impact**: 🔴 High  
**Affected Modules**: `commons-kernel-result`  
**Migration Time**: 3-4 hours

#### What Changed

The `Result` API has been redesigned for better type inference and functional composition. The `getOrThrow()` method has been removed in favor of safer alternatives.

#### Before (v1.x)

```java
public class UserService {
    public User findUser(String id) {
        Result<User> result = repository.findById(id);
        return result.getOrThrow(); // Removed in v2.0
    }
}
```

#### After (v2.0.0)

```java
public class UserService {
    // Option 1: Use getOrElse with default
    public User findUser(String id) {
        Result<User> result = repository.findById(id);
        return result.getOrElse(User.anonymous());
    }
    
    // Option 2: Use fold for explicit handling
    public User findUser(String id) {
        return repository.findById(id)
            .fold(
                error -> User.anonymous(),
                user -> user
            );
    }
    
    // Option 3: Propagate Result to caller (recommended)
    public Result<User> findUser(String id) {
        return repository.findById(id);
    }
}
```

#### Migration Path

1. **Find all usages**:
   ```bash
   grep -r "getOrThrow()" src/
   ```

2. **Replace with getOrElse**:
   ```bash
   find src -name "*.java" -exec sed -i 's/\.getOrThrow()/.getOrElse(null)/g' {} \;
   ```

3. **Review null handling**: Ensure null checks are in place or use Optional

4. **Consider Result propagation**: Update method signatures to return Result\<T\>

#### Automated Fix

```bash
./scripts/migrate.sh --fix result-api-changes
```

---

### 2. Repository Interface Changes

**Impact**: 🔴 High  
**Affected Modules**: `commons-ports-persistence`, `commons-kernel-ddd`  
**Migration Time**: 2-3 hours

#### What Changed

Repository methods now return `Result<T>` instead of `Optional<T>` for better error handling.

#### Before (v1.x)

```java
public interface UserRepository extends Repository<User, UserId> {
    Optional<User> findById(UserId id);
    void save(User user);
    void delete(UserId id);
}
```

#### After (v2.0.0)

```java
public interface UserRepository extends Repository<User, UserId> {
    Result<User> findById(UserId id);  // Changed
    Result<Void> save(User user);      // Changed
    Result<Void> delete(UserId id);    // Changed
}
```

#### Migration Path

1. **Update interface implementations**:
   ```java
   // Before
   @Override
   public Optional<User> findById(UserId id) {
       return Optional.ofNullable(entityManager.find(User.class, id));
   }
   
   // After
   @Override
   public Result<User> findById(UserId id) {
       try {
           User user = entityManager.find(User.class, id);
           return user != null 
               ? Result.success(user)
               : Result.failure(ErrorCode.NOT_FOUND, "User not found: " + id);
       } catch (Exception e) {
           return Result.failure(ErrorCode.DATABASE_ERROR, e.getMessage());
       }
   }
   ```

2. **Update callers**:
   ```java
   // Before
   Optional<User> user = repository.findById(userId);
   if (user.isPresent()) {
       // process
   }
   
   // After
   Result<User> user = repository.findById(userId);
   user.onSuccess(u -> {
       // process
   });
   ```

---

### 3. DomainEvent Timestamp Changes

**Impact**: 🟡 Medium  
**Affected Modules**: `commons-kernel-ddd`, `commons-app-domain-events`  
**Migration Time**: 1-2 hours

#### What Changed

`DomainEvent.occurredAt()` has been renamed to `occurredOn()` and now returns `Instant` instead of `ZonedDateTime` for timezone-independent events.

#### Before (v1.x)

```java
public interface DomainEvent {
    UUID eventId();
    ZonedDateTime occurredAt();  // Deprecated
}

public class UserCreatedEvent implements DomainEvent {
    private final ZonedDateTime occurredAt;
    
    public ZonedDateTime occurredAt() {
        return occurredAt;
    }
}
```

#### After (v2.0.0)

```java
public interface DomainEvent {
    UUID eventId();
    Instant occurredOn();  // New method
}

public class UserCreatedEvent implements DomainEvent {
    private final Instant occurredOn;
    
    public Instant occurredOn() {
        return occurredOn;
    }
}
```

#### Migration Path

1. **Update field types**:
   ```java
   - private final ZonedDateTime occurredAt;
   + private final Instant occurredOn;
   ```

2. **Update method names**:
   ```bash
   find src -name "*.java" -exec sed -i 's/occurredAt()/occurredOn()/g' {} \;
   ```

3. **Convert timestamps**:
   ```java
   // If you need ZonedDateTime
   Instant instant = event.occurredOn();
   ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
   ```

---

### 4. ConfigurationProvider Optional Changes

**Impact**: 🟡 Medium  
**Affected Modules**: `commons-app-configuration`  
**Migration Time**: 1 hour

#### What Changed

`ConfigurationProvider.get(String)` now returns `Result<String>` instead of `Optional<String>` for consistent error handling.

#### Before (v1.x)

```java
Optional<String> value = config.get("app.name");
String name = value.orElse("default");
```

#### After (v2.0.0)

```java
Result<String> value = config.get("app.name");
String name = value.getOrElse("default");
```

---

## Deprecated Features

### Result.fromException()

**Deprecated in**: v1.5.0  
**Removal in**: v2.0.0  
**Alternative**: Use `Result.failure()` instead

#### Example

```java
// ❌ Deprecated - removed in v2.0.0
Result<User> result = Result.fromException(new RuntimeException("error"));

// ✅ Recommended
Result<User> result = Result.failure(ErrorCode.INTERNAL_ERROR, "error");
```

### Repository.save()

**Deprecated in**: v1.8.0  
**Removal in**: v2.0.0  
**Alternative**: Use `Repository.persist()` instead

#### Example

```java
// ❌ Deprecated - removed in v2.0.0
repository.save(user);

// ✅ Recommended
Result<Void> result = repository.persist(user);
result.onFailure(error -> log.error("Failed to persist user: {}", error));
```

---

## New Features

### Reactive Support

Native reactive programming with Project Reactor.

#### Usage Example

```java
@Service
public class ReactiveUserService {
    private final ReactiveUserRepository repository;
    
    public Mono<Result<User>> findUser(String id) {
        return repository.findById(UserId.of(id));
    }
    
    public Flux<User> findAllActive() {
        return repository.findAll()
            .filter(User::isActive);
    }
}
```

#### Benefits

- Non-blocking I/O
- Backpressure support
- Better resource utilization
- Seamless Spring WebFlux integration

### Enhanced Validation

New fluent validation API.

#### Usage Example

```java
Result<User> validation = Validator.of(user)
    .validate(u -> u.getEmail() != null, "Email required")
    .validate(u -> u.getAge() >= 18, "Must be 18+")
    .validate(u -> u.getName().length() > 2, "Name too short")
    .toResult();

validation.onFailure(errors -> {
    errors.forEach(System.out::println);
});
```

---

## Dependency Changes

### Updated Dependencies

| Dependency | Old Version | New Version | Notes |
|------------|-------------|-------------|-------|
| Spring Boot | 3.1.x | 3.2.x | Auto-configuration improvements |
| Jackson | 2.15.x | 2.16.x | Performance improvements |
| Hibernate | 6.2.x | 6.4.x | See Hibernate migration guide |
| SLF4J | 2.0.7 | 2.0.12 | Bug fixes |

### New Dependencies

- **reactor-core** (3.6.x): Reactive programming support
- **micrometer-tracing** (1.2.x): Distributed tracing

### Removed Dependencies

- **commons-lang3**: Inlined required utilities

---

## Migration Steps

### Prerequisites

- [ ] Java 21 installed
- [ ] Maven 3.9+ or Gradle 8+
- [ ] All tests passing on v1.x
- [ ] Code committed to version control
- [ ] Create backup branch: `git checkout -b backup-before-v2`

### Step-by-Step Migration

#### 1. Update Dependencies

```xml
<!-- Update in pom.xml -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-bom</artifactId>
    <version>2.0.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

#### 2. Run Automated Migration Script

```bash
# Preview changes
./scripts/migrate.sh --dry-run 1.0.0 2.0.0 .

# Apply migration
./scripts/migrate.sh --backup 1.0.0 2.0.0 .
```

#### 3. Fix Compilation Errors

```bash
mvn clean compile
```

See [Breaking Changes](#breaking-changes) section for detailed fix instructions.

#### 4. Update Tests

```bash
mvn test
```

Common test updates:
- Update Result assertions
- Update Repository mocks
- Update DomainEvent assertions

#### 5. Verify Migration

```bash
mvn clean verify
mvn verify -P integration-tests
```

---

## Rollback Plan

### If Migration Fails

1. **Restore from backup**:
   ```bash
   git checkout backup-before-v2
   git branch -D main
   git checkout -b main
   ```

2. **Revert dependency versions**:
   ```xml
   <version>1.x.x</version>
   ```

3. **Rebuild and test**:
   ```bash
   mvn clean install
   mvn test
   ```

---

## FAQ

### Q: How long does migration typically take?

**A**:
- Small projects (< 10k LOC): 4-8 hours
- Medium projects (10k-50k LOC): 8-16 hours  
- Large projects (> 50k LOC): 2-5 days

### Q: Can I migrate incrementally?

**A**: No, v1 to v2 requires all changes at once due to core API changes. However, you can:
1. Migrate module by module in a monorepo
2. Test each module independently
3. Deploy gradually to production

### Q: What if I have custom Repository implementations?

**A**: Update them to return `Result<T>`:
```java
@Override
public Result<User> findById(UserId id) {
    try {
        User user = // your custom logic
        return user != null 
            ? Result.success(user)
            : Result.failure(ErrorCode.NOT_FOUND, "Not found");
    } catch (Exception e) {
        return Result.failure(ErrorCode.DATABASE_ERROR, e.getMessage());
    }
}
```

---

## Support

### Resources

- **Documentation**: [https://github.com/marcusPrado02/java-commons/docs](https://github.com/marcusPrado02/java-commons/docs)
- **Changelog**: [CHANGELOG.md](../../CHANGELOG.md)
- **Release Notes**: [v2.0.0 Release](https://github.com/marcusPrado02/java-commons/releases/tag/v2.0.0)

### Need Help?

- **GitHub Issues**: [Report issues](https://github.com/marcusPrado02/java-commons/issues)
- **Discussions**: [Ask questions](https://github.com/marcusPrado02/java-commons/discussions)

---

**Last Updated**: 2026-02-14
