# Arquitetura Java Commons Platform

Este documento descreve a arquitetura completa do projeto java-commons, incluindo princípios, camadas, módulos e padrões de uso.

## Visão Geral

Java Commons Platform é uma coleção de módulos reutilizáveis para construção de microserviços

 Java de alta qualidade, baseados em:

- **Arquitetura Hexagonal (Ports & Adapters)**
- **Domain-Driven Design (DDD)**
- **Clean Architecture**
- **Kernel Framework-Agnostic**

## Princípios Fundamentais

### 1. Separação de Concerns

Cada camada tem responsabilidade bem definida:
- **Kernel**: Lógica de domínio pura
- **Ports**: Contratos de comunicação
- **Application**: Orquestração de casos de uso
- **Adapters**: Implementações técnicas
- **Starters**: Integrações facilitadas

### 2. Dependency Rule

**Regra de Ouro**: Dependências apontam para dentro. Camadas internas nunca dependem de externas.

```
Starters → Adapters → Ports → Application → Kernel
(outside) ←←←←←←←←←←←←←←←←←←←←←←←←←←←←← (inside)
```

### 3. Framework Independence

O kernel (domínio) é **100% livre de frameworks**:
- ✅ Apenas Java stdlib + SLF4J API
- ❌ Sem Spring, Jakarta EE, Hibernate, etc.
- ✅ Testável sem infraestrutura
- ✅ Portável entre frameworks

### 4. Domain-Centric

Lógica de negócio vive no domínio, não em services anêmicos:
- Entities contêm comportamento
- Aggregates protegem invariantes
- Domain Events comunicam mudanças
- Value Objects encapsulam conceitos

## Arquitetura em Camadas

### Diagrama de Camadas

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Starters                      │
│                                                               │
│  commons-spring-starter-{observability, otel, resilience,    │
│                          outbox, idempotency}                │
│                                                               │
│  • Auto-configurações Spring Boot                            │
│  • Beans e configurações prontas                             │
│  • Facilita integração                                       │
└─────────────────────────────────────────────────────────────┘
                             ↓ depends on
┌─────────────────────────────────────────────────────────────┐
│                         Adapters                             │
│                                                               │
│  commons-adapters-{web, web-spring, web-spring-webflux,     │
│                    persistence-jpa, persistence-inmemory,    │
│                    otel, resilience4j}                       │
│                                                               │
│  • Implementações concretas de Ports                         │
│  • Integrações com frameworks (JPA, Spring, etc.)            │
│  • Controllers, Repositories, Message Handlers               │
└─────────────────────────────────────────────────────────────┘
                             ↓ implements
┌─────────────────────────────────────────────────────────────┐
│                          Ports                               │
│                                                               │
│  commons-ports-{persistence, messaging, http, cache,         │
│                 secrets, files}                              │
│                                                               │
│  • Interfaces hexagonais (contratos)                         │
│  • Framework-agnostic                                        │
│  • Repository, Publisher, HttpClient, etc.                   │
└─────────────────────────────────────────────────────────────┘
                             ↓ used by / uses
┌─────────────────────────────────────────────────────────────┐
│                      Application Layer                       │
│                                                               │
│  commons-app-{observability, resilience, outbox,             │
│               idempotency}                                   │
│                                                               │
│  • Casos de uso e serviços de aplicação                     │
│  • Orquestração                                              │
│  • Cross-cutting concerns (correlation, retry, etc.)         │
└─────────────────────────────────────────────────────────────┘
                             ↓ uses
┌─────────────────────────────────────────────────────────────┐
│                       Kernel (Domain)                        │
│                                                               │
│  commons-kernel-{core, ddd, errors, result, time}           │
│                                                               │
│  • Domínio puro (100% framework-free)                        │
│  • Entities, Aggregates, Value Objects, Domain Events        │
│  • Invariantes e regras de negócio                           │
│  • Result types, primitivos                                  │
└─────────────────────────────────────────────────────────────┘
```

## Módulos por Camada

### Kernel (Domínio)

**Propósito**: Lógica de negócio pura, framework-free

| Módulo | Descrição | Principais Tipos |
|--------|-----------|------------------|
| `commons-kernel-core` | Primitivos base e utilitários | `Preconditions`, `StringUtils`, etc. |
| `commons-kernel-ddd` | Building blocks DDD | `Entity`, `AggregateRoot`, `ValueObject`, `DomainEvent` |
| `commons-kernel-errors` | Modelo de erro | `DomainError`, `ValidationError`, Exception hierarchy |
| `commons-kernel-result` | Functional error handling | `Result<T,E>`, `Option<T>`, `Either<L,R>` |
| `commons-kernel-time` | Abstrações de tempo | `ClockProvider`, utilidades de data/hora |

**Características**:
- ✅ Zero dependências de frameworks
- ✅ Apenas Java stdlib + SLF4J API
- ✅ Testes ultra-rápidos
- ✅ Reutilizável em qualquer contexto

### Ports (Contratos Hexagonais)

**Propósito**: Interfaces para comunicação com mundo externo

| Módulo | Descrição | Principais Interfaces |
|--------|-----------|----------------------|
| `commons-ports-persistence` | Persistência | `Repository<E,ID>`, `PageableRepository`, `Specification` |
| `commons-ports-messaging` | Mensageria | `MessagePublisher`, `MessageConsumer`, `MessageEnvelope` |
| `commons-ports-http` | Cliente HTTP | `HttpClient`, `HttpRequest`, `HttpResponse` |
| `commons-ports-cache` | Cache | `CacheProvider`, `CacheEntry` |
| `commons-ports-secrets` | Secrets | `SecretStore`, `Secret` |
| `commons-ports-files` | Armazenamento | `FileStorage`, `FileMetadata` |

**Características**:
- ✅ Apenas interfaces e DTOs
- ✅ Framework-agnostic
- ✅ Definem contratos, não implementações
- ✅ Usadas por Application e Kernel

### Application (Casos de Uso)

**Propósito**: Orquestração de lógica de negócio

| Módulo | Descrição | Funcionalidades |
|--------|-----------|-----------------|
| `commons-app-observability` | Observabilidade | Correlation ID, RequestContext, logging facade |
| `commons-app-resilience` | Resiliência | Circuit breaker, retry policies, timeout |
| `commons-app-outbox` | Transactional Outbox | Outbox pattern para eventos de domínio |
| `commons-app-idempotency` | Idempotência | Detecção e handling de requisições duplicadas |

**Características**:
- ✅ Orquestra domínio e ports
- ✅ Implementa casos de uso
- ✅ Cross-cutting concerns
- ✅ Framework-agnostic (pode usar ports)

### Adapters (Implementações)

**Propósito**: Implementações concretas de Ports com frameworks

| Módulo | Descrição | Tecnologias |
|--------|-----------|-------------|
| `commons-adapters-web` | REST base (framework-free) | Query parsing, pagination, DTOs |
| `commons-adapters-web-spring` | Spring MVC adapters | Controllers, exception handlers |
| `commons-adapters-web-spring-webflux` | Spring WebFlux adapters | Reactive controllers |
| `commons-adapters-persistence-jpa` | JPA repositories | Hibernate, Spring Data patterns |
| `commons-adapters-persistence-inmemory` | In-memory repos | HashMaps (para testes) |
| `commons-adapters-otel` | OpenTelemetry | Tracing, metrics, logging |
| `commons-adapters-resilience4j` | Resilience4j | Circuit breaker implementation |

**Características**:
- ✅ Implementam Ports
- ✅ Podem usar frameworks
- ✅ Controllers, Repositories concretos
- ✅ Integrações com bibliotecas externas

### Spring Boot Starters

**Propósito**: Autoconfigurações para produtividade

| Módulo | Descrição | Auto-configura |
|--------|-----------|----------------|
| `commons-spring-starter-observability` | Observability | Correlation ID, logging, request context |
| `commons-spring-starter-otel` | OpenTelemetry | Tracing, metrics, instrumentation |
| `commons-spring-starter-resilience` | Resilience | Circuit breaker, retry, timeout beans |
| `commons-spring-starter-outbox` | Transactional Outbox | Outbox polling, event publishing |
| `commons-spring-starter-idempotency` | Idempotency | Request deduplication |

**Características**:
- ✅ Auto-configurações Spring Boot
- ✅ Beans prontos para uso
- ✅ Convenções sobre configuração
- ✅ Facilita integração

### Governança e Qualidade

| Módulo | Descrição |
|--------|-----------|
| `commons-archunit` | ArchUnit rules para validar arquitetura |
| `commons-quality` | Regras de qualidade (SpotBugs, Checkstyle, PMD) |
| `commons-testkit-core` | Test fixtures, builders, assertions |

### Build e Gestão

| Módulo | Descrição |
|--------|-----------|
| `commons-bom` | Bill of Materials (gestão de versões) |
| `commons-parent` | Parent POM (plugins, configurações) |
| `commons-platform` | Aggregator (builds, relatórios) |

## Fluxo de Dados

### Request Flow (REST Example)

```
1. HTTP Request
   ↓
2. Spring Controller (commons-adapters-web-spring)
   ↓ parse request
3. Application Service (commons-app-*)
   ↓ validates, orchestrates
4. Domain Logic (commons-kernel-ddd)
   ↓ executes business rules, emits events
5. Repository Port (commons-ports-persistence)
   ↓ interface call
6. JPA Repository (commons-adapters-persistence-jpa)
   ↓ persists
7. Database
   ↓
8. Domain Events pulled
   ↓
9. Outbox (commons-app-outbox)
   ↓ stores events
10. Async Publisher
   ↓
11. Message Broker (Kafka, RabbitMQ, etc.)
```

### Dependency Flow

```
Controller → UseCase → Domain ← Repository Interface
   ↓          ↓          ↓              ↑
   └──────────┴──────────┴──────────────┘
              (implements)
           JPA Repository
```

## Padrões Arquiteturais Aplicados

### 1. Dependency Inversion Principle (DIP)

```java
// ❌ ERRADO: Domínio depende de implementação
public class OrderService {
  private JpaOrderRepository repository; // Acoplamento!
}

// ✅ CORRETO: Domínio depende de abstração (Port)
public class OrderService {
  private OrderRepository repository; // Interface!
}
```

### 2. Interface Segregation

Ports são granulares e específicas:

```java
// ✅ BOM: Interfaces focadas
public interface Repository<E, ID> {
  void save(E entity);
  Option<E> findById(ID id);
}

public interface PageableRepository<E, ID> extends Repository<E, ID> {
  PageResult<E> findAll(PageRequest pageRequest);
}

// ❌ RUIM: Interface inchada
public interface MegaRepository<E, ID> {
  // 50 métodos misturados
}
```

### 3. Single Responsibility Principle

Cada módulo tem uma responsabilidade clara:

- `commons-kernel-ddd`: DDD building blocks
- `commons-kernel-result`: Result types
- `commons-app-outbox`: Apenas Transactional Outbox
- Etc.

## Casos de Uso Comuns

### Criar Novo Aggregate

```java
// 1. Criar no kernel
// commons-kernel-ddd/../domain/Order.java
public class Order extends AggregateRoot<OrderId> {
  private List<OrderLine> lines;
  private Money total;
  
  public void addLine(Product product, Quantity quantity) {
    // Validações
    Invariant.notNull(product, "product");
    
    // Lógica de domínio
    lines.add(new OrderLine(product, quantity));
    recalculateTotal();
    
    // Evento
    recordEvent(new OrderLineAdded(id(), product.id()));
  }
}

// 2. Criar Port
// commons-ports-persistence/.../OrderRepository.java
public interface OrderRepository extends Repository<Order, OrderId> {
  List<Order> findByCustomerId(CustomerId customerId);
}

// 3. Criar Adapter
// commons-adapters-persistence-jpa/.../JpaOrderRepository.java
@Repository
public class JpaOrderRepository implements OrderRepository {
  // Implementação JPA
}

// 4. Criar Use Case
// commons-app-sales/.../PlaceOrderUseCase.java
public class PlaceOrderUseCase {
  private final OrderRepository repository;
  
  public Result<Order, OrderError> execute(PlaceOrderCommand cmd) {
    // Orquestração
  }
}

// 5. Criar Controller
// commons-adapters-web-spring/.../OrderController.java
@RestController
public class OrderController {
  private final PlaceOrderUseCase useCase;
  
  @PostMapping("/orders")
  public ResponseEntity<?> placeOrder(@RequestBody OrderRequest req) {
    return useCase.execute(req.toCommand())
      .map(ResponseEntity::ok)
      .getOrElse(error -> ResponseEntity.badRequest().body(error));
  }
}
```

## Regras de Conformidade

### ArchUnit Rules

Ver `commons-archunit` para regras automáticas:

```java
// Kernel não pode depender de frameworks
@ArchTest
static final ArchRule kernel_framework_free =
  classes().that().resideInAPackage("..kernel..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("java..", "..kernel..", "org.slf4j..");

// Hexagonal: Domain não depende de Adapters
@ArchTest
static final ArchRule domain_independent =
  classes().that().resideInAPackage("..kernel..")
    .should().onlyBeAccessed().byClassesThat()
    .resideInAnyPackage("..kernel..", "..ports..", "..app..");
```

## Diagramas

Ver [C4 Diagrams](./c4-diagrams.md) para diagramas detalhados de:
- Context Diagram (nível 1)
- Container Diagram (nível 2)
- Component Diagram (nível 3)

## Referências

- [ADR-0001: Hexagonal Architecture](./adr/0001-hexagonal-architecture.md)
- [ADR-0002: Domain-Driven Design](./adr/0002-domain-driven-design.md)
- [ADR-0003: Result Type Pattern](./adr/0003-result-type-pattern.md)
- [ADR-0004: Framework-Agnostic Kernel](./adr/0004-framework-agnostic-kernel.md)
- [ADR-0005: Module Structure](./adr/0005-module-structure.md)

## Próximos Passos

- Ver [Padrões de Uso](./usage-patterns.md) para exemplos práticos
- Ver [Guia de Contribuição](../CONTRIBUTING.md) para colaborar
- Ver [Módulos](./modules.md) para lista completa de módulos
