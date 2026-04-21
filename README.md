# java-commons

Biblioteca modular de commons para serviços Java 21+ com arquitetura hexagonal, DDD e tratamento de erros orientado a resultados.

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## Módulos

### Kernel (domínio puro — zero dependências de framework)

| Artefato | Descrição |
|----------|-----------|
| `commons-kernel-result` | `Result<T>`, `Either<L,R>`, `Option<T>` — railway-oriented programming |
| `commons-kernel-errors` | `Problem`, `ErrorCode`, `ErrorCategory`, `Severity` — RFC 7807 |
| `commons-kernel-ddd` | `AggregateRoot`, `Entity`, `ValueObject`, `DomainEvent`, `Repository` |
| `commons-kernel-time` | `Clock` port, implementações para prod e testes |

### Ports (interfaces hexagonais)

| Artefato | Interface principal |
|----------|---------------------|
| `commons-ports-cache` | `CachePort<String, V>` |
| `commons-ports-persistence` | `Repository<E, ID>`, `PageableRepository<E, ID>` |
| `commons-ports-messaging` | `MessagePublisher`, `MessageConsumer` |
| `commons-ports-email` | `EmailPort` |
| `commons-ports-http` | `HttpClientPort` |

### Adapters (implementações de infraestrutura)

| Artefato | Tecnologia |
|----------|-----------|
| `commons-adapters-cache-redis` | Spring Data Redis + Lettuce |
| `commons-adapters-cache-memcached` | Memcached |
| `commons-adapters-persistence-inmemory` | In-memory (testes) |
| `commons-adapters-persistence-jpa` | Spring Data JPA + Hibernate |
| `commons-adapters-messaging-kafka` | Apache Kafka |
| `commons-adapters-messaging-rabbitmq` | RabbitMQ |
| `commons-adapters-messaging-azure-servicebus` | Azure Service Bus |
| `commons-adapters-email-smtp` | Jakarta Mail |
| `commons-adapters-email-sendgrid` | SendGrid API |
| `commons-adapters-email-ses` | AWS SES |
| `commons-adapters-http-okhttp` | OkHttp |
| `commons-adapters-http-webclient` | Spring WebClient |
| `commons-adapters-otel` | OpenTelemetry |
| `commons-adapters-payment-stripe` | Stripe API |
| `commons-adapters-files-s3` | AWS S3 |
| `commons-adapters-files-gcs` | Google Cloud Storage |
| `commons-adapters-files-azure-blob` | Azure Blob Storage |
| `commons-adapters-grpc-client` | gRPC client |
| `commons-adapters-grpc-server` | gRPC server |
| `commons-adapters-graphql-server` | GraphQL |
| `commons-adapters-blockchain-web3j` | Ethereum / Web3j |
| `commons-adapters-compression` | Compressão de dados |
| `commons-adapters-excel-poi` | Apache POI (Excel) |
| `commons-adapters-pdf-itext` | iText (PDF) |

### Spring Boot Starters

| Artefato | Auto-configura |
|----------|----------------|
| `commons-spring-starter-cache` | `RedisCacheAdapter` via `@Bean` |
| `commons-spring-starter-persistence` | `JpaRepository` via `@Bean` |
| `commons-spring-starter-messaging` | `KafkaPublisher` via `@Bean` |

## Começando

### 1. Importe o BOM

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.marcusprado02.commons</groupId>
      <artifactId>commons-bom</artifactId>
      <version>0.1.2-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 2. Adicione apenas os módulos que precisa

```xml
<dependencies>
  <!-- Domínio puro: Result + erros + DDD -->
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-result</artifactId>
  </dependency>
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-errors</artifactId>
  </dependency>
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-ddd</artifactId>
  </dependency>

  <!-- Ports e adapters conforme necessário -->
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-cache-redis</artifactId>
  </dependency>
</dependencies>
```

## Exemplos de uso

### `Result<T>` — tratamento de erros sem exceções

```java
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

// Composição via sealed interface + pattern matching (Java 21)
Result<UserDto> dto = findById(id)
    .map(UserDto::from)
    .flatMap(this::validate);

switch (dto) {
    case Result.Ok<UserDto> ok -> respond(ok.value());
    case Result.Fail<UserDto> fail -> respondError(fail.problem());
}
```

### `AggregateRoot` com eventos de domínio

```java
public class Order extends AggregateRoot<OrderId> {

    private Order(OrderId id, List<LineItem> items) {
        super(id);
        registerEvent(new OrderCreatedEvent(id, items));
    }

    public static Result<Order> create(List<LineItem> items) {
        if (items.isEmpty()) {
            return Result.fail(Problem.of(
                ErrorCode.of("ORDER.EMPTY"),
                ErrorCategory.VALIDATION,
                Severity.WARNING,
                "Order must have at least one item"));
        }
        return Result.ok(new Order(OrderId.generate(), items));
    }
}
```

### `CachePort` com Redis

```java
// RedisCacheAdapter é String-keyed — Redis sempre usa String keys
RedisCacheAdapter<UserDto> cache = new RedisCacheAdapter<>(redisTemplate, "users:");

cache.put(userId, dto, Duration.ofMinutes(15));
Optional<UserDto> cached = cache.get(userId);
```

### Agregando ports com Spring Boot Starter

```java
@Configuration
public class CacheConfig {

    @Bean
    public CachePort<String, UserDto> userCache(RedisTemplate<String, UserDto> template) {
        return new RedisCacheAdapter<>(template, "users:");
    }
}
```

## Arquitetura

```
commons-kernel-*          ← domínio puro, zero dependências externas
       ↑
commons-ports-*           ← interfaces hexagonais (ports)
       ↑
commons-adapters-*        ← implementações de infra (adapters)
       ↑
commons-spring-starter-*  ← auto-configuração Spring Boot
```

A direção de dependências nunca inverte: adapters dependem de ports, não o contrário.

## Build

Requer Java 21+ e Maven 3.9+.

```bash
# Build completo com testes de integração (requer Docker)
./mvnw verify

# Apenas testes unitários
./mvnw test -Dgroups="unit"

# Gerar relatório de cobertura
./mvnw verify jacoco:report
```

## Qualidade

- **SpotBugs** — análise estática de bugs
- **Checkstyle** — estilo e convenções
- **PMD** — padrões de código
- **JaCoCo** — cobertura mínima 80% linhas / 75% branches
- **Mutation testing** — Pitest (semanal no CI)
- **Testcontainers** — testes de integração com infraestrutura real

## Requisitos

- Java 21+
- Maven 3.9+
- Docker (testes de integração via Testcontainers)

## Licença

MIT — veja [LICENSE](LICENSE).
