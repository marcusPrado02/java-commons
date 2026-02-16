# Commons Platform Archetype

Maven archetype for creating new projects using commons-platform.

## Features

- **Pre-configured Project Templates**: Ready-to-use Spring Boot project structure
- **Hexagonal Architecture**: Follows Ports & Adapters pattern
- **Commons Platform Integration**: Includes commons modules out of the box
- **Configurable Modules**: Choose which features to include (database, messaging, cache, observability)
- **CI/CD Ready**: Includes GitHub Actions workflow
- **Best Practices**: Structure follows DDD and clean architecture principles

## Installation

This archetype is part of the commons-platform project. Build and install it locally:

```bash
cd commons-platform-archetype
mvn clean install
```

## Usage

### Basic Project Creation

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.marcusprado02.commons \
  -DarchetypeArtifactId=commons-platform-archetype \
  -DarchetypeVersion=0.1.0-SNAPSHOT \
  -DgroupId=com.example \
  -DartifactId=my-service \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=com.example.myservice
```

### Interactive Mode

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.marcusprado02.commons \
  -DarchetypeArtifactId=commons-platform-archetype \
  -DarchetypeVersion=0.1.0-SNAPSHOT
```

You'll be prompted for:
- `groupId`: Your project group ID
- `artifactId`: Your project artifact ID
- `version`: Your project version
- `package`: Base package name
- `commonsVersion`: Commons platform version to use
- `projectType`: Type of project (web, batch, etc.)
- `includeDatabase`: Include database support (true/false)
- `includeMessaging`: Include messaging support (true/false)
- `includeCache`: Include cache support (true/false)
- `includeObservability`: Include observability support (true/false)

### Advanced Configuration

Create a project with all features enabled:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.marcusprado02.commons \
  -DarchetypeArtifactId=commons-platform-archetype \
  -DarchetypeVersion=0.1.0-SNAPSHOT \
  -DgroupId=com.example \
  -DartifactId=my-service \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=com.example.myservice \
  -DcommonsVersion=0.1.0-SNAPSHOT \
  -DprojectType=web \
  -DincludeDatabase=true \
  -DincludeMessaging=true \
  -DincludeCache=true \
  -DincludeObservability=true
```

Create a minimal project:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.marcusprado02.commons \
  -DarchetypeArtifactId=commons-platform-archetype \
  -DarchetypeVersion=0.1.0-SNAPSHOT \
  -DgroupId=com.example \
  -DartifactId=simple-service \
  -Dpackage=com.example.simple \
  -DincludeDatabase=false \
  -DincludeMessaging=false \
  -DincludeCache=false \
  -DincludeObservability=false
```

## Generated Project Structure

The archetype generates a complete Spring Boot application with:

```
my-service/
├── .github/
│   └── workflows/
│       └── ci.yml                    # GitHub Actions CI pipeline
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/myservice/
│   │   │       ├── Application.java              # Main Spring Boot class
│   │   │       ├── domain/                       # Domain layer
│   │   │       │   └── ExampleAggregate.java     # Example aggregate
│   │   │       ├── application/                  # Application layer
│   │   │       │   └── ExampleService.java       # Example service
│   │   │       └── adapters/                     # Adapters layer
│   │   │           └── web/
│   │   │               └── ExampleController.java # REST controller
│   │   └── resources/
│   │       └── application.yml                    # Application config
│   └── test/
│       └── java/
│           └── com/example/myservice/
│               └── ApplicationTest.java           # Integration test
├── .gitignore
├── README.md
└── pom.xml
```

## Architecture

Projects generated from this archetype follow **Hexagonal Architecture**:

### Domain Layer (`domain/`)
- Contains business logic and domain models
- Independent of frameworks and infrastructure
- Uses DDD patterns: Aggregates, Entities, Value Objects, Domain Events

### Application Layer (`application/`)
- Contains use cases and application services
- Orchestrates domain logic
- Manages transactions and event publishing

### Adapters Layer (`adapters/`)
- Contains infrastructure implementations
- REST controllers, repositories, message consumers, etc.
- Adapts external interfaces to domain needs

## Included Commons Modules

All generated projects include:

- `commons-platform`: Base platform with transitive dependencies
- `commons-kernel-core`: Core kernel utilities
- `commons-kernel-errors`: Error handling with Problem/ErrorCode
- `commons-kernel-result`: Result pattern for error handling
- `commons-kernel-ddd`: DDD building blocks

### Optional Modules (based on configuration)

**Database (`includeDatabase=true`)**:
- `commons-adapters-persistence-jpa`
- Spring Data JPA
- H2 database (for development)

**Messaging (`includeMessaging=true`)**:
- `commons-ports-messaging`
- `commons-adapters-messaging-kafka`

**Cache (`includeCache=true`)**:
- `commons-ports-cache`
- `commons-adapters-cache-redis`

**Observability (`includeObservability=true`)**:
- `commons-spring-starter-observability`
- Prometheus metrics
- Distributed tracing

## Quick Start After Generation

1. **Navigate to project**:
   ```bash
   cd my-service
   ```

2. **Build**:
   ```bash
   mvn clean install
   ```

3. **Run**:
   ```bash
   mvn spring-boot:run
   ```

4. **Test**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

5. **Access H2 Console** (if database enabled):
   ```
   http://localhost:8080/h2-console
   ```

## Customization

### Adding More Dependencies

Edit `pom.xml` and add commons modules you need:

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-outbox</artifactId>
</dependency>
```

### Changing Project Type

The archetype currently supports:
- `web`: REST API with Spring Web
- Future: `batch`, `streaming`, `grpc`

### Environment-Specific Configuration

Create environment-specific YAML files:

- `application-dev.yml`
- `application-prod.yml`

Run with profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## CI/CD

Generated projects include a GitHub Actions workflow (`.github/workflows/ci.yml`) that:

- Runs on push to `main` and `develop`
- Runs on pull requests
- Builds with Maven
- Runs tests
- Uploads test results
- Uploads code coverage to Codecov

### Customizing CI/CD

Edit `.github/workflows/ci.yml` to add:
- Docker image building
- Deployment steps
- Security scanning
- Additional quality gates

## Example Usage Scenarios

### Microservice with Database

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.marcusprado02.commons \
  -DarchetypeArtifactId=commons-platform-archetype \
  -DarchetypeVersion=0.1.0-SNAPSHOT \
  -DgroupId=com.company \
  -DartifactId=user-service \
  -Dpackage=com.company.user \
  -DincludeDatabase=true \
  -DincludeObservability=true
```

### Event-Driven Service

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.marcusprado02.commons \
  -DarchetypeArtifactId=commons-platform-archetype \
  -DarchetypeVersion=0.1.0-SNAPSHOT \
  -DgroupId=com.company \
  -DartifactId=order-service \
  -Dpackage=com.company.order \
  -DincludeDatabase=true \
  -DincludeMessaging=true \
  -DincludeObservability=true
```

### Simple REST API

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.marcusprado02.commons \
  -DarchetypeArtifactId=commons-platform-archetype \
  -DarchetypeVersion=0.1.0-SNAPSHOT \
  -DgroupId=com.company \
  -DartifactId=api-gateway \
  -Dpackage=com.company.gateway \
  -DincludeDatabase=false \
  -DincludeCache=true \
  -DincludeObservability=true
```

## Best Practices

### Domain-Driven Design

- Keep domain models in `domain/` package
- Domain should not depend on infrastructure
- Use domain events for important state changes
- Aggregates enforce consistency boundaries

### Result Pattern

Always use `Result<T>` for operations that can fail:

```java
public Result<Order> createOrder(CreateOrderRequest request) {
    if (request.items().isEmpty()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("EMPTY_ORDER"),
                ErrorCategory.VALIDATION,
                Severity.ERROR,
                "Order must have at least one item"
            )
        );
    }

    Order order = new Order(request.items());
    return Result.ok(order);
}
```

### Testing

- Write unit tests for domain logic
- Write integration tests for adapters
- Use `commons-testkit-core` for test utilities
- Use `@SpringBootTest` for integration tests

### Configuration

- Keep sensitive data in environment variables
- Use Spring profiles for different environments
- Never commit credentials to version control

## Troubleshooting

### Archetype Not Found

Make sure the archetype is installed in your local repository:
```bash
cd commons-platform-archetype
mvn clean install
```

### Generation Fails

Check Maven version (requires 3.8+):
```bash
mvn --version
```

### Project Doesn't Build

Ensure commons-platform is installed:
```bash
cd java-commons
mvn clean install -DskipTests
```

## Contributing

To modify this archetype:

1. Edit templates in `src/main/resources/archetype-resources/`
2. Update `archetype-metadata.xml` if adding new files
3. Rebuild and reinstall:
   ```bash
   mvn clean install
   ```
4. Test the generation:
   ```bash
   mvn archetype:generate -DarchetypeGroupId=com.marcusprado02.commons \
     -DarchetypeArtifactId=commons-platform-archetype \
     -DarchetypeVersion=0.1.0-SNAPSHOT
   ```

## Version Compatibility

| Archetype Version | Commons Version | Spring Boot | Java |
|-------------------|----------------|-------------|------|
| 0.1.0-SNAPSHOT    | 0.1.0-SNAPSHOT | 3.2.2       | 21   |

## Resources

- [Maven Archetype Plugin](https://maven.apache.org/archetype/maven-archetype-plugin/)
- [Commons Platform Documentation](../README.md)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://martinfowler.com/tags/domain%20driven%20design.html)

## License

This archetype is part of commons-platform and shares the same license.
