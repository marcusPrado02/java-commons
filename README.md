# Commons Platform

Supreme shared commons platform for high-quality Java microservices.

## Quick Start (5 minutes)

### 1. Add the BOM to your project

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.marcusprado02.commons</groupId>
      <artifactId>commons-bom</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 2. Add the modules you need

```xml
<dependencies>
  <!-- Functional error handling (no exceptions) -->
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-result</artifactId>
  </dependency>

  <!-- Rich error model (Problem, ErrorCode, Severity) -->
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-errors</artifactId>
  </dependency>
</dependencies>
```

### 3. Use Result<T> for error handling

```java
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.kernel.errors.*;

public class UserService {

    public Result<User> findById(String id) {
        return userRepository.findById(id)
            .map(Result::ok)
            .orElseGet(() -> Result.fail(
                Problem.of(
                    ErrorCode.of("USER.NOT_FOUND"),
                    ErrorCategory.NOT_FOUND,
                    Severity.WARNING,
                    "User not found: " + id)));
    }
}

// Consume without exceptions
userService.findById("42")
    .peek(user -> System.out.println("Found: " + user.name()))
    .peekFail(problem -> System.err.println("Error: " + problem.message()))
    .map(User::email)
    .getOrElse("unknown@example.com");

// Async pipeline
userService.findById("42")
    .mapAsync(user -> loadProfileAsync(user.id()))
    .thenCompose(r -> r.flatMapAsync(profile -> enrichAsync(profile)));
```

### 4. Optionally add a Spring Boot Starter

```xml
<!-- Auto-configures outbox, idempotency, caching, secrets -->
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-spring-starter-outbox</artifactId>
</dependency>
```

See [docs/guides/quick-start.md](docs/guides/quick-start.md) for the full guide.

---

## Goals
- Clean Architecture / Hexagonal
- DDD-first
- Framework-agnostic core
- Optional adapters and Spring Boot starters
- Strong governance (architecture rules, quality gates)

## GroupId
com.marcusprado02.commons

## Java
- Java 21+

## Structure
- `commons-kernel-*` → domain-safe, framework-free
- `commons-app-*` → application layer helpers
- `commons-ports-*` → hexagonal ports
- `commons-adapters-*` → infrastructure implementations
- `commons-spring-starter-*` → productivity starters
