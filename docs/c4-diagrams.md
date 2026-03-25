# Diagramas C4: Java Commons Platform

Este documento contém diagramas C4 (Context, Container, Component) da arquitetura do java-commons.

> **Sobre C4**: O modelo C4 (Context, Containers, Components, Code) é uma abordagem para visualização de arquitetura de software criada por Simon Brown.

## Nível 1: Context Diagram

**Visão de alto nível**: Como java-commons se relaciona com usuários e sistemas externos.

```mermaid
C4Context
    title System Context Diagram - Java Commons Platform
    
    Person(dev, "Developer", "Desenvolve microserviços usando java-commons")
    Person(architect, "Architect", "Define padrões e governança")
    
    System(javaCommons, "Java Commons Platform", "Plataforma compartilhada de<br/>componentes reutilizáveis<br/>para microserviços Java")
    
    System_Ext(springBoot, "Spring Boot", "Framework de aplicação")
    System_Ext(jpa, "JPA/Hibernate", "Framework de persistência")
    System_Ext(kafka, "Message Broker", "Kafka, RabbitMQ, etc.")
    System_Ext(otel, "OpenTelemetry", "Observability")
    System_Ext(github, "GitHub Packages", "Repositório de artefatos")
    
    Rel(dev, javaCommons, "Usa módulos", "Maven dependency")
    Rel(architect, javaCommons, "Define regras", "ArchUnit, ADRs")
    
    Rel(javaCommons, springBoot, "Integra via", "Starters")
    Rel(javaCommons, jpa, "Implementa ports via", "Adapters")
    Rel(javaCommons, kafka, "Publica eventos via", "Adapters")
    Rel(javaCommons, otel, "Envia telemetria via", "Adapters")
    Rel(javaCommons, github, "Publica artefatos", "Maven deploy")
```

## Nível 2: Container Diagram

**Módulos principais**: Containers lógicos (módulos Maven) que compõem a plataforma.

```mermaid
C4Container
    title Container Diagram - Java Commons Platform
    
    Container_Boundary(starters, "Spring Boot Starters") {
        Container(starter_obs, "Observability Starter", "Spring Boot AutoConfig", "Auto-configura correlation ID,<br/>logging, tracing")
        Container(starter_res, "Resilience Starter", "Spring Boot AutoConfig", "Auto-configura circuit breaker,<br/>retry policies")
        Container(starter_outbox, "Outbox Starter", "Spring Boot AutoConfig", "Auto-configura Transactional Outbox")
    }
    
    Container_Boundary(adapters, "Adapters") {
        Container(adapter_web, "Web Adapters", "Spring MVC/WebFlux", "Controllers REST, exception handlers")
        Container(adapter_jpa, "JPA Adapters", "Hibernate", "Repositórios concretos")
        Container(adapter_otel, "OTel Adapter", "OpenTelemetry SDK", "Tracing, metrics implementation")
    }
    
    Container_Boundary(ports, "Ports") {
        Container(port_persist, "Persistence Ports", "Interfaces", "Repository<E,ID>,<br/>PageableRepository")
        Container(port_msg, "Messaging Ports", "Interfaces", "MessagePublisher,<br/>MessageConsumer")
        Container(port_http, "HTTP Ports", "Interfaces", "HttpClient")
    }
    
    Container_Boundary(app, "Application") {
        Container(app_obs, "Observability App", "Use Cases", "Correlation context,<br/>request tracking")
        Container(app_outbox, "Outbox App", "Transactional Outbox", "Event persistence & polling")
        Container(app_idempotency, "Idempotency App", "Deduplication", "Idempotency handling")
    }
    
    Container_Boundary(kernel, "Kernel (Domain)") {
        Container(kernel_ddd, "DDD Kernel", "Pure Java", "Entity, AggregateRoot,<br/>ValueObject, DomainEvent")
        Container(kernel_result, "Result Kernel", "Pure Java", "Result<T,E>, Option<T>")
        Container(kernel_errors, "Errors Kernel", "Pure Java", "Error hierarchy")
    }
    
    Rel(starters, adapters, "Configura beans")
    Rel(adapters, ports, "Implementa")
    Rel(app, ports, "Usa")
    Rel(app, kernel, "Orquestra")
    Rel(ports, kernel, "Retorna tipos de")
```

## Nível 3: Component Diagram - Kernel DDD

**Componentes internos**: Classes principais dentro do módulo `commons-kernel-ddd`.

```mermaid
C4Component
    title Component Diagram - commons-kernel-ddd
    
    Component(entity, "Entity", "Abstract Class", "Base para entidades com<br/>identidade, audit, multi-tenancy")
    Component(aggregate, "AggregateRoot", "Abstract Class", "Raiz de agregado com<br/>eventos de domínio")
    Component(valueObject, "ValueObject", "Abstract Class", "Objetos de valor imutáveis<br/>(igualdade por atributos)")
    Component(domainEvent, "DomainEvent", "Interface", "Eventos de domínio")
    Component(eventRecorder, "DomainEventRecorder", "Interface", "Buffer de eventos")
    Component(auditTrail, "AuditTrail", "Record", "Trilha de auditoria<br/>(created, updated, deleted)")
    Component(tenantId, "TenantId", "ValueObject", "Identificador de tenant")
    Component(invariant, "Invariant", "Utility", "Validações de invariantes")
    
    Rel(aggregate, entity, "Estende")
    Rel(aggregate, eventRecorder, "Implementa")
    Rel(aggregate, domainEvent, "Registra")
    Rel(entity, auditTrail, "Contém")
    Rel(entity, tenantId, "Contém")
    Rel(entity, valueObject, "Pode conter")
    Rel(entity, invariant, "Valida com")
```

## Nível 3: Component Diagram - Persistence

**Fluxo de persistência**: Como os componentes de persistência se relacionam.

```mermaid
C4Component
    title Component Diagram - Persistence Flow
    
    Component(controller, "OrderController", "REST Controller", "Endpoint HTTP")
    Component(useCase, "PlaceOrderUseCase", "Application Service", "Caso de uso")
    Component(aggregate, "Order", "Aggregate Root", "Aggregate de domínio")
    Component(portInterface, "OrderRepository", "Port Interface", "Contrato de repositório")
    Component(jpaImpl, "JpaOrderRepository", "JPA Adapter", "Implementação JPA")
    Component(entity, "OrderEntity", "JPA Entity", "@Entity")
    Component(mapper, "OrderMapper", "Mapper", "Domain <-> JPA")
    
    ComponentDb(database, "PostgreSQL", "Relational DB")
    
    Rel(controller, useCase, "Chama")
    Rel(useCase, aggregate, "Cria/manipula")
    Rel(useCase, portInterface, "Usa interface")
    Rel(jpaImpl, portInterface, "Implementa")
    Rel(jpaImpl, mapper, "Usa para converter")
    Rel(mapper, aggregate, "Converte de/para")
    Rel(mapper, entity, "Converte de/para")
    Rel(jpaImpl, entity, "Persiste")
    Rel(entity, database, "Salva em")
```

## Nível 3: Component Diagram - Result & Errors Kernel

**Tipos de retorno**: Componentes de `commons-kernel-result` e `commons-kernel-errors`.

```mermaid
C4Component
    title Component Diagram - commons-kernel-result / commons-kernel-errors

    Component(result, "Result<T>", "Interface/Record", "Tipo de retorno Railway-oriented.<br/>ok(T) / fail(Problem)")
    Component(asyncResult, "AsyncResult<T>", "Class", "Wrappers assíncronos:<br/>mapAsync, flatMapAsync, toFuture")
    Component(validationResult, "ValidationResult", "Class", "Agrega múltiplos erros;<br/>builder acumulativo")
    Component(option, "Option<T>", "Interface", "Alternativa type-safe ao Optional")
    Component(either, "Either<L,R>", "Interface", "Tipo soma para dois valores")
    Component(results, "Results", "Utility", "orThrow(), catchingDomain()")
    Component(resultCollectors, "ResultCollectors", "Utility", "Collectors para Stream<Result<T>>")

    Component(problem, "Problem", "Record", "Erro estruturado: code + category + message")
    Component(errorCode, "ErrorCode", "Value Object", "Código imutável (e.g. NOT_FOUND.ENTITY)")
    Component(problems, "Problems", "Factory", "Métodos de fábrica: notFound, validation, business")
    Component(standardCodes, "StandardErrorCodes", "Constants", "Códigos padrão da plataforma")
    Component(errorContext, "ErrorContext", "Builder", "Contexto rico: stack, breadcrumbs, i18n")
    Component(domainEx, "DomainException", "Exception hierarchy", "ValidationException, NotFoundException...")

    Rel(result, problem, "Transporta")
    Rel(asyncResult, result, "Envolve")
    Rel(validationResult, problem, "Agrega")
    Rel(results, result, "Processa")
    Rel(resultCollectors, result, "Coleta streams de")

    Rel(problems, problem, "Cria")
    Rel(problems, standardCodes, "Usa constantes de")
    Rel(problem, errorCode, "Contém")
    Rel(errorContext, problem, "Enriquece")
    Rel(domainEx, problem, "Carrega")
```

## Nível 3: Component Diagram - Outbox Pattern

**Transactional Outbox**: Componentes de `commons-app-outbox` e `commons-spring-starter-outbox`.

```mermaid
C4Component
    title Component Diagram - commons-app-outbox

    Component(processor, "DefaultOutboxProcessor", "Application Service", "Polling loop: busca PENDING,<br/>publica, atualiza status")
    Component(processorPort, "OutboxProcessor", "Interface (Port)", "processAll() / processBatch(int)")
    Component(publisher, "OutboundPublisher", "Interface (Port)", "publish(topic, body, headers)")
    Component(repoPort, "OutboxRepositoryPort", "Interface (Port)", "fetchBatch, markProcessing,<br/>markPublished, markFailed, markDead")
    Component(message, "OutboxMessage", "Record (Domain)", "ID, aggregateType, topic,<br/>payload, status, attempts")
    Component(messageId, "OutboxMessageId", "Value Object", "Identidade imutável da mensagem")
    Component(status, "OutboxStatus", "Enum", "PENDING → PROCESSING →<br/>PUBLISHED | FAILED | DEAD")
    Component(config, "OutboxProcessorConfig", "Value Object", "batchSize, maxAttempts,<br/>backoffMultiplier, retentionPeriod")
    Component(metrics, "OutboxMetrics", "Interface", "recordPublished, recordFailed,<br/>recordDead, recordLatency")
    Component(jpaRepo, "JpaOutboxRepositoryAdapter", "JPA Adapter", "Implementa port com Hibernate<br/>+ lock otimista")
    Component(starter, "OutboxAutoConfiguration", "Spring Boot Starter", "Auto-wiring de processor,<br/>scheduler, jpa adapter")

    Rel(starter, processor, "Cria bean de")
    Rel(processor, processorPort, "Implementa")
    Rel(processor, repoPort, "Usa")
    Rel(processor, publisher, "Delega publish a")
    Rel(processor, metrics, "Reporta para")
    Rel(processor, config, "Configurado por")
    Rel(processor, message, "Processa")
    Rel(message, messageId, "Identificado por")
    Rel(message, status, "Tem")
    Rel(jpaRepo, repoPort, "Implementa")
```

## Nível 3: Component Diagram - JPA Adapter

**Persistência concreta**: Componentes de `commons-adapters-persistence-jpa`.

```mermaid
C4Component
    title Component Diagram - commons-adapters-persistence-jpa

    Component(factory, "JpaRepositoryFactory", "Factory", "Cria repositórios concretos<br/>a partir de EntityManager")
    Component(baseRepo, "BaseGenericRepository", "Abstract Class", "CRUD: save, findById,<br/>findAll, delete, count")
    Component(pageableRepo, "PageableJpaRepository", "Class", "findAll(PageRequest, SearchCriteria)<br/>search(PageRequest, Spec, Sort)")
    Component(specBuilder, "SpecificationBuilder", "Class", "Converte SearchCriteria em<br/>JPA Predicate")
    Component(jpaQueries, "JpaQueries", "Utility", "Helpers de query JPQL")
    Component(outboxAdapter, "JpaOutboxRepositoryAdapter", "Adapter", "Implementa OutboxRepositoryPort<br/>com JPA + lock otimista")
    Component(idempAdapter, "JpaIdempotencyStoreAdapter", "Adapter", "Implementa IdempotencyStore<br/>com JPA")
    Component(outboxEntity, "OutboxMessageEntity", "JPA Entity", "@Entity para OutboxMessage")
    Component(mapper, "OutboxJpaMapper", "Mapper", "Domain ↔ JPA Entity")
    Component(txSupport, "Transactional", "Utility", "Wrappers de transação JPA")

    ComponentDb(db, "RDBMS", "PostgreSQL / MySQL / H2")

    Rel(factory, baseRepo, "Instancia")
    Rel(factory, pageableRepo, "Instancia")
    Rel(pageableRepo, baseRepo, "Estende")
    Rel(pageableRepo, specBuilder, "Usa para filtros")
    Rel(pageableRepo, jpaQueries, "Usa")
    Rel(outboxAdapter, outboxEntity, "Persiste")
    Rel(outboxAdapter, mapper, "Usa")
    Rel(mapper, outboxEntity, "Converte de/para")
    Rel(baseRepo, db, "Persiste em")
    Rel(outboxAdapter, db, "Persiste em")
    Rel(idempAdapter, db, "Persiste em")
```

## Diagrama de Dependências entre Módulos

```mermaid
graph TD
    %% Starters
    starter_obs[commons-spring-starter-observability]
    starter_otel[commons-spring-starter-otel]
    starter_res[commons-spring-starter-resilience]
    starter_outbox[commons-spring-starter-outbox]
    starter_idemp[commons-spring-starter-idempotency]
    
    %% Adapters
    adapter_web[commons-adapters-web]
    adapter_web_spring[commons-adapters-web-spring]
    adapter_jpa[commons-adapters-persistence-jpa]
    adapter_inmem[commons-adapters-persistence-inmemory]
    adapter_otel[commons-adapters-otel]
    adapter_r4j[commons-adapters-resilience4j]
    
    %% Ports
    port_persist[commons-ports-persistence]
    port_msg[commons-ports-messaging]
    port_http[commons-ports-http]
    port_cache[commons-ports-cache]
    
    %% Application
    app_obs[commons-app-observability]
    app_res[commons-app-resilience]
    app_outbox[commons-app-outbox]
    app_idemp[commons-app-idempotency]
    
    %% Kernel
    kernel_core[commons-kernel-core]
    kernel_ddd[commons-kernel-ddd]
    kernel_errors[commons-kernel-errors]
    kernel_result[commons-kernel-result]
    kernel_time[commons-kernel-time]
    
    %% Dependencies (outside -> inside)
    starter_obs --> adapter_otel
    starter_obs --> app_obs
    
    starter_otel --> adapter_otel
    
    starter_res --> adapter_r4j
    starter_res --> app_res
    
    starter_outbox --> app_outbox
    starter_outbox --> adapter_jpa
    
    starter_idemp --> app_idemp
    
    adapter_web_spring --> adapter_web
    adapter_web --> port_http
    
    adapter_jpa --> port_persist
    adapter_inmem --> port_persist
    
    adapter_otel --> app_obs
    adapter_r4j --> app_res
    
    port_persist --> kernel_ddd
    port_msg --> kernel_ddd
    port_http --> kernel_result
    port_cache --> kernel_core
    
    app_obs --> kernel_core
    app_res --> kernel_errors
    app_outbox --> port_persist
    app_outbox --> port_msg
    app_outbox --> kernel_ddd
    app_idemp --> kernel_core
    
    kernel_ddd --> kernel_core
    kernel_ddd --> kernel_errors
    kernel_ddd --> kernel_time
    
    kernel_errors --> kernel_core
    kernel_result --> kernel_errors
    
    style kernel_core fill:#e1f5ff
    style kernel_ddd fill:#e1f5ff
    style kernel_errors fill:#e1f5ff
    style kernel_result fill:#e1f5ff
    style kernel_time fill:#e1f5ff
    
    style port_persist fill:#fff4e1
    style port_msg fill:#fff4e1
    style port_http fill:#fff4e1
    style port_cache fill:#fff4e1
    
    style app_obs fill:#f0fff4
    style app_res fill:#f0fff4
    style app_outbox fill:#f0fff4
    style app_idemp fill:#f0fff4
    
    style adapter_web fill:#ffe1f5
    style adapter_web_spring fill:#ffe1f5
    style adapter_jpa fill:#ffe1f5
    style adapter_inmem fill:#ffe1f5
    style adapter_otel fill:#ffe1f5
    style adapter_r4j fill:#ffe1f5
    
    style starter_obs fill:#f5e1ff
    style starter_otel fill:#f5e1ff
    style starter_res fill:#f5e1ff
    style starter_outbox fill:#f5e1ff
    style starter_idemp fill:#f5e1ff
```

## Legendas

### Camadas

- 🔵 **Kernel** (Azul): Domínio puro, framework-free
- 🟡 **Ports** (Amarelo): Interfaces hexagonais
- 🟢 **Application** (Verde): Casos de uso e serviços
- 🔴 **Adapters** (Rosa): Implementações concretas
- 🟣 **Starters** (Roxo): Auto-configurações Spring Boot

### Direção de Dependências

```
Starters → Adapters → Ports → Application → Kernel
```

**Regra**: Dependências sempre apontam para dentro (para camadas mais internas).

## Ferramentas de Visualização

### Mermaid Live Editor

Para editar os diagramas:
- [Mermaid Live Editor](https://mermaid.live)
- Copie o código Mermaid e cole no editor

### VS Code

Extensões recomendadas:
- **Markdown Preview Mermaid Support**: Visualiza diagramas no preview
- **Mermaid Markdown Syntax Highlighting**: Syntax highlight

### Exportação

Diagramas podem ser exportados para:
- PNG/SVG: Via Mermaid Live Editor
- PDF: Via pandoc com suporte Mermaid
- Draw.io: Importando Mermaid

## Referências

- [C4 Model](https://c4model.com/) - Simon Brown
- [Mermaid Documentation](https://mermaid.js.org/)
- [Structurizr](https://structurizr.com/) - Ferramentas C4

## Atualização dos Diagramas

Ao adicionar novos módulos ou mudar dependências:

1. Atualize os diagramas Mermaid neste arquivo
2. Gere novas imagens se necessário
3. Valide que diagramas refletem realidade (`mvn dependency:tree`)
4. Atualize [architecture.md](./architecture.md) se necessário
