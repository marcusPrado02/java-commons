# ${artifactId}

Project generated using commons-platform-archetype.

## Overview

This is a Spring Boot application built with commons-platform following hexagonal architecture principles.

## Features

- ✅ Spring Boot 3.2.2
- ✅ Java 21
- ✅ Commons Platform integration
- ✅ Hexagonal architecture (Ports & Adapters)
- ✅ Domain-Driven Design support
#if (${includeDatabase} == "true")
- ✅ JPA/Hibernate with H2 database
#end
#if (${includeMessaging} == "true")
- ✅ Kafka messaging
#end
#if (${includeCache} == "true")
- ✅ Redis caching
#end
#if (${includeObservability} == "true")
- ✅ Observability (metrics, tracing, logging)
#end

## Prerequisites

- Java 21+
- Maven 3.8+
#if (${includeMessaging} == "true")
- Kafka (for messaging)
#end
#if (${includeCache} == "true")
- Redis (for caching)
#end

## Getting Started

### Build

```bash
mvn clean install
```

### Run

```bash
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar target/${artifactId}-${version}.jar
```

### Test

```bash
mvn test
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── ${package}/
│   │       ├── Application.java           # Main entry point
│   │       ├── domain/                    # Domain layer (entities, aggregates, domain services)
│   │       │   └── ExampleAggregate.java
│   │       ├── application/               # Application layer (use cases, services)
│   │       │   └── ExampleService.java
│   │       └── adapters/                  # Adapters layer (controllers, repositories, clients)
│   │           └── web/
│   │               └── ExampleController.java
│   └── resources/
│       └── application.yml                # Application configuration
└── test/
    └── java/
        └── ${package}/
            └── ApplicationTest.java
```

## Architecture

This project follows **Hexagonal Architecture** (Ports & Adapters):

### Domain Layer
- Contains business logic and domain models
- Independent of frameworks and external concerns
- Uses DDD patterns: Aggregates, Entities, Value Objects, Domain Events

### Application Layer
- Contains use cases and application services
- Orchestrates domain logic
- Coordinates transactions and domain events

### Adapters Layer
- Contains technical implementations
- Web controllers, repositories, message consumers, etc.
- Depends on domain and application layers

## API Endpoints

### Health Check
```
GET /actuator/health
```

### Example Resource
```
POST /api/examples
Content-Type: application/json

{
  "name": "Example Name",
  "description": "Example Description"
}
```

```
PUT /api/examples/{id}
Content-Type: application/json

{
  "name": "Updated Name"
}
```

#if (${includeDatabase} == "true")
## Database

H2 in-memory database is used by default.

Access H2 Console: http://localhost:8080/h2-console

- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)

#end
#if (${includeObservability} == "true")
## Observability

### Metrics
Prometheus metrics available at:
```
GET /actuator/prometheus
```

### Health
```
GET /actuator/health
```

#end
## Configuration

Configuration properties in `application.yml`:

- `spring.application.name`: Application name
#if (${includeDatabase} == "true")
- `spring.datasource.*`: Database configuration
#end
#if (${includeMessaging} == "true")
- `spring.kafka.*`: Kafka configuration
#end

## Commons Platform

This project uses [commons-platform](https://github.com/marcusPrado02/java-commons) version ${commonsVersion}.

For documentation on available commons modules, see the project README.

## Development

### Adding a New Feature

1. **Create Domain Model** in `domain/` package
2. **Create Application Service** in `application/` package
3. **Create Adapter** (controller, repository, etc.) in `adapters/` package
4. **Write Tests** for all layers

### Using Result Pattern

```java
public Result<MyEntity> createEntity(String name) {
    if (name == null || name.isBlank()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("INVALID_NAME"),
                ErrorCategory.VALIDATION,
                Severity.ERROR,
                "Name cannot be empty"
            )
        );
    }

    MyEntity entity = new MyEntity(name);
    return Result.ok(entity);
}
```

### Domain Events

```java
public class MyAggregate extends AggregateRoot<UUID> {
    public void doSomething() {
        // Business logic
        registerEvent(new SomethingHappenedEvent(id()));
    }
}
```

## License

[Add your license here]

## Support

For issues and questions, please create an issue in the repository.
