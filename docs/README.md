# Commons Library Documentation

Welcome to the Java Commons library documentation. This directory contains comprehensive guides for using, extending, and migrating the Commons library.

---

## 🚀 Quick Start

### New Users
1. Start with [Architecture Overview](00-overview.md)
2. Read [Module Structure](modules.md)
3. Follow a [Quick Start Guide](#quick-start-guides) for your use case

### Existing Users
- [API Reference](#api-reference) - Detailed class documentation
- [Guides](#guides) - Pattern implementations and best practices
- [Migration Guides](#migration-guides) - Upgrade between versions

---

## 📖 API Reference

Complete documentation for all core modules with examples:

### Kernel (Domain Layer)
- [**commons-kernel-core**](api-reference/kernel-core.md) ⭐
  - `Preconditions` - Validação fail-fast
  - `StringUtils` - Utilitários null-safe para strings
  - `Dates` - Manipulação de datas
  - `Collections` - Helpers para coleções

- [**commons-kernel-ddd**](api-reference/kernel-ddd.md) ⭐⭐⭐
  - `Entity`, `AggregateRoot` - Entidades DDD
  - `ValueObject`, `SingleValueObject` - Objetos de valor imutáveis
  - `DomainEvent` - Eventos de domínio
  - `Identifier` - IDs tipados
  - `Invariant` - Validações de domínio

- [**commons-kernel-result**](api-reference/kernel-result.md) ⭐⭐⭐
  - `Result<T>` - Railway-Oriented Programming
  - `map`, `flatMap`, `filter` - Transformações funcionais
  - `Results` - Helpers e conversões
  - Pattern completo para tratamento de erros

- [**commons-kernel-errors**](api-reference/kernel-errors.md) ⭐⭐
  - `Problem` - Modelo de erro estruturado
  - `DomainException` - Exceptions de domínio
  - `ValidationException` - Validações com múltiplos erros
  - Integração com Result pattern

- [**commons-kernel-time**](api-reference/kernel-time.md)
  - `ClockProvider` - Abstração de relógio para testes
  - `TimeRange` - Intervalos de tempo
  - Utilitários temporais

### Application Layer
- [**commons-app-configuration**](api-reference/app-configuration.md)
  - `ConfigurationProvider` - Configuração externalizada
  - `FeatureFlags` - Feature toggles
  - Providers: Environment, Spring Cloud, Consul, etc.

- [**commons-app-observability**](api-reference/app-observability.md)
  - `RequestContext` - Contexto de request (correlation-id, tenant-id)
  - `StructuredLog` - Logs estruturados JSON
  - `MetricsFacade` - Métricas SLI/SLO
  - `HealthChecks` - Liveness/Readiness probes

- [**commons-app-resilience**](api-reference/app-resilience.md)
  - `CircuitBreakerPolicy` - Circuit breaker
  - `RetryPolicy` - Retries com backoff
  - `TimeoutPolicy`, `BulkheadPolicy`, `RateLimiterPolicy`
  - `ResilienceExecutor` - Execução com políticas

- [**commons-app-outbox**](api-reference/app-outbox.md) ⭐⭐⭐
  - `OutboxPublisher` - Publicação transacional
  - `OutboxProcessor` - Processamento assíncrono
  - `OutboxMessage` - Modelo de mensagem
  - Pattern completo Transactional Outbox com exemplos

- [**commons-app-idempotency**](api-reference/app-idempotency.md) ⭐⭐
  - `IdempotencyKey` - Chave de idempotência
  - `IdempotencyService` - Deduplicação automática
  - `IdempotencyRecord` - Registro de operações processadas
  - Integration patterns e Redis backend

- [**commons-app-multi-tenancy**](api-reference/app-multi-tenancy.md) ⭐⭐
  - `TenantId`, `TenantContext` - Contexto de tenant
  - `TenantResolver` - Estratégias de resolução (header, subdomain, JWT)
  - Isolation strategies (database, schema, row-level)
  - Web e messaging integration

- [**commons-app-feature-flags**](api-reference/app-feature-flags.md) ⭐⭐
  - `FeatureFlagService` - Feature toggles e rollout gradual
  - `FeatureContext` - Contexto para targeting
  - A/B testing e multivariate experiments
  - Estratégias: boolean, percentage, user-targeting, attribute-based

- [**commons-app-health-checks**](api-reference/app-health-checks.md) ⭐⭐
  - `HealthCheck` interface - Kubernetes-ready
  - Liveness vs Readiness probes
  - Built-in checks: database, messaging, redis, disk space
  - `HealthAggregator` - Múltiplos checks

- [**commons-app-data-validation**](api-reference/app-data-validation.md) ⭐⭐⭐
  - `ValidationService` - Validação com Result pattern
  - Bean Validation (JSR 380)
  - Custom validators e cross-field validation
  - Validation groups e async validation

- [**commons-app-scheduler**](api-reference/app-scheduler.md) ⭐⭐⭐
  - `TaskScheduler` - Agendamento de tarefas
  - Cron expressions e fixed rate/delay
  - `DistributedLock` - Coordenação em cluster
  - Batch processing e retry policies

- [**commons-app-api-versioning**](api-reference/app-api-versioning.md) ⭐⭐⭐
  - `ApiVersion` - Semantic versioning
  - URI, header, content negotiation strategies
  - `DeprecationService` - API lifecycle
  - Sunset headers e migration guides

- [**commons-app-i18n**](api-reference/app-i18n.md) ⭐⭐
  - `I18nService` - Internacionalização
  - `LocaleContext` - Locale management
  - Number, currency, date formatting
  - Localized emails e validation messages

- [**commons-app-batch**](api-reference/app-batch.md) ⭐⭐⭐
  - `BatchJob` - Read-process-write pattern
  - `BatchExecutor` - Execução com particionamento
  - Checkpoint e restart capabilities
  - Parallel processing e error handling

- [**commons-app-webhooks**](api-reference/app-webhooks.md) ⭐⭐⭐
  - `WebhookService` - Sistema completo de webhooks
  - `WebhookSubscription` - Gerenciamento de inscrições
  - HMAC signature validation para segurança
  - Retry com exponential backoff

- [**commons-app-audit-log**](api-reference/app-audit-log.md) ⭐⭐⭐
  - `AuditLogger` - Logs de auditoria
  - `@Audited` annotation para automatic auditing
  - GDPR compliance (export, anonymize)
  - Audit trails e reports

- [**commons-app-api-gateway**](api-reference/app-api-gateway.md) ⭐⭐⭐
  - `Route`, `GatewayFilter` - Gateway patterns
  - Authentication, rate limiting filters
  - Request aggregation pattern
  - Service discovery integration

### Ports (Interfaces)
- [**Ports Index**](api-reference/ports/README.md) ⭐⭐⭐
  - Complete catalog of all port interfaces
  - Hexagonal architecture contracts
- [**commons-ports-persistence**](api-reference/ports/persistence.md)
  - `Repository<T, ID>` - CRUD genérico
  - `SpecificationRepository` - Queries dinâmicas
  - `EventStore` - Event Sourcing
- [**commons-ports-messaging**](api-reference/ports/messaging.md)
  - `MessagePublisher`, `MessageConsumer`
  - Pub/Sub patterns

### Adapters (Implementations)
See [Adapters Index](api-reference/adapters/README.md) for complete list.

---

## 📚 Guides

Comprehensive guides for implementing patterns and solving common problems:

### Core Patterns
- [**Spring Boot Integration**](guides/spring-boot.md) ⭐⭐⭐
  - Auto-configuration setup
  - Architecture layers (domain, application, infrastructure, presentation)
  - Transaction management e event publishing
  - Testing with MockMvc e Testcontainers
  - Profiles & environments

- [**Observability**](guides/observability.md) ⭐⭐⭐
  - Logs estruturados com RequestContext
  - Métricas e SLIs
  - Tracing distribuído
  - Health checks completos

- [**Resilience**](guides/resilience.md) ⭐⭐⭐
  - Circuit Breaker
  - Retry with exponential backoff
  - Timeout, Bulkhead, Rate Limiter
  - Combinando políticas

- [**Configuration Management**](guides/configuration.md) ⭐⭐
  - Configuração externalizada (12-factor)
  - Feature flags e rollout gradual
  - Multi-source configuration
  - Dynamic refresh

- [**Error Handling**](guides/error-handling.md) ⭐⭐⭐
  - Result pattern vs Exceptions (quando usar cada um)
  - Domain errors com Problem/DomainException
  - REST API error responses (RFC 7807)
  - Validation patterns (Bean Validation, custom validators)
  - Global exception handling e monitoring

### Messaging & Events
- [**Messaging Patterns**](guides/messaging.md) ⭐⭐⭐
  - Kafka, RabbitMQ, Azure Service Bus patterns
  - Fire-and-forget, Request-reply, Pub/Sub
  - At-most-once, At-least-once, Exactly-once
  - Partitioning, compaction, dead letter queues
  - Performance: batching, parallel processing

- [**Domain Events**](guides/domain-events.md) ⭐⭐⭐
  - DomainEvent interface e implementações
  - Eventos com AggregateRoot
  - Event handlers e idempotência
  - Publicação via Outbox
  - Event Sourcing patterns e Event Store

### Testing
- [**Testing Strategies**](guides/testing.md) ⭐⭐⭐
  - Pirâmide de testes
  - Unit tests com DDD
  - Integration tests com Testcontainers
  - Contract tests e Architecture tests
  - Test fixtures e best practices

### Integration Guides
- [**Adapters Catalog**](api-reference/adapters/README.md) ⭐⭐
  - Complete list of all 40+ infrastructure adapters
  - Web, Persistence, Messaging, Cloud, etc.
  - When to use each adapter

---

## 🏗️ Architecture

### Architecture & Design
- [**Architecture Overview**](architecture.md) - Hexagonal architecture, layers
- [**Module Overview**](modules.md) - Complete list of all modules
- [**Dependency Rules**](01-dependency-rules.md) - Architectural constraints
- [**C4 Diagrams**](c4-diagrams.md) - Visual architecture

### ADRs (Architecture Decision Records)
- [ADR-0001: Hexagonal Architecture](adr/0001-hexagonal-architecture.md)
- [ADR-0002: Domain-Driven Design](adr/0002-domain-driven-design.md)
- [ADR-0003: Result Pattern](adr/0003-result-pattern.md)
- [ADR-0004: Framework-Agnostic Kernel](adr/0004-framework-agnostic-kernel.md)
- [ADR-0005: Module Structure](adr/0005-module-structure.md)

---

## 🔄 Migration Guides

Detailed step-by-step guides for migrating between major versions:

### Available Guides
- [**v0 → v1**](migrations/v0-to-v1.md) - Migration from v0.x to v1.0.0
- [**v1 → v2**](migrations/v1-to-v2.md) - Migration from v1.x to v2.0.0
- [**Template**](migrations/TEMPLATE.md) - Template for creating new migration guides

### Versioning & Releases
- [**Versioning Strategy**](versioning-and-releases.md) ⭐
  - Semantic Versioning 2.0.0
  - Conventional Commits
  - Release automation
  - Backward compatibility policies

- [**Upgrade Process**](upgrade-process.md) ⭐
  - Upgrade strategies
  - Step-by-step process
  - Risk assessment
  - Rollback procedures
- ✅ Rollback plan
- ✅ FAQ and troubleshooting

---

## 🛠️ Automation Scripts

Located in [`../scripts/`](../scripts/README.md):

### Available Scripts

| Script | Purpose | Usage |
|--------|---------|-------|
| [migrate.sh](../scripts/migrate.sh) | Automate migration process | `./migrate.sh 1.0.0 2.0.0 /project` |
| [validate-pre-migration.sh](../scripts/validate-pre-migration.sh) | Validate project readiness | `./validate-pre-migration.sh /project 2.0.0` |
| [detect-deprecations.sh](../scripts/detect-deprecations.sh) | Find deprecated API usage | `./detect-deprecations.sh /project` |

**See**: [Scripts README](../scripts/README.md) for detailed usage and examples.

---

## 📖 Quick Start

### New to Commons?

1. Start with [Module Overview](modules.md) to understand the structure
2. Read [Dependency Rules](01-dependency-rules.md) for architectural constraints
3. See [Versioning Strategy](versioning-and-releases.md) for release information

### Planning an Upgrade?

1. Read [Upgrade Process](upgrade-process.md) for the complete workflow
2. Find your version in [Migration Guides](migrations/) (e.g., v1 → v2)
3. Run [Pre-Migration Validation](../scripts/README.md#2-validate-pre-migrationsh)
4. Use [Migration Scripts](../scripts/README.md) to automate the upgrade

### Creating a Release?

1. Review [Versioning Strategy](versioning-and-releases.md#release-process)
2. Follow [Conventional Commits](versioning-and-releases.md#conventional-commits)
3. Use [Release Workflow](../.github/workflows/release.yml)
4. Generate [CHANGELOG](versioning-and-releases.md#changelog) automatically

---

## 📋 Documentation Index

### By Topic

#### Architecture
- [00-overview.md](00-overview.md) - Architecture overview
- [01-dependency-rules.md](01-dependency-rules.md) - Dependency constraints
- [modules.md](modules.md) - Module catalog

#### Versioning
- [versioning-and-releases.md](versioning-and-releases.md) - Versioning strategy
- [02-versioning.md](02-versioning.md) - Version numbering

#### Migration
- [upgrade-process.md](upgrade-process.md) - Upgrade workflow
- [migrations/TEMPLATE.md](migrations/TEMPLATE.md) - Migration guide template
- [migrations/v0-to-v1.md](migrations/v0-to-v1.md) - v0 to v1 migration
- [migrations/v1-to-v2.md](migrations/v1-to-v2.md) - v1 to v2 migration

### By Audience

#### For Library Users
- [Upgrade Process](upgrade-process.md) - How to upgrade
- [Migration Guides](migrations/) - Version-specific instructions
- [Versioning Strategy](versioning-and-releases.md) - When to upgrade

#### For Contributors
- [Dependency Rules](01-dependency-rules.md) - Architectural constraints
- [Versioning Strategy](versioning-and-releases.md#conventional-commits) - Commit message format
- [Migration Template](migrations/TEMPLATE.md) - Creating migration guides

#### For Maintainers
- [Versioning Strategy](versioning-and-releases.md#release-process) - Release process
- [Backward Compatibility](versioning-and-releases.md#backward-compatibility) - API compatibility
- [Deprecation Policy](versioning-and-releases.md#deprecation-policy) - Deprecation workflow

---

## 🔗 External Resources

### Related Projects
- [GitHub Repository](https://github.com/marcusPrado02/java-commons)
- [CHANGELOG](../CHANGELOG.md)
- [Contributing Guide](../CONTRIBUTING.md)
- [License](../LICENSE)

### Standards & References
- [Semantic Versioning 2.0.0](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

---

## 🤝 Contributing to Documentation

### Improving Existing Docs

Found an error or want to improve documentation?

1. Fork the repository
2. Edit the relevant `.md` file
3. Submit a pull request with:
   - Clear description of changes
   - Reason for the change
   - Any related issues

### Creating New Migration Guides

When creating a new major version release:

1. Copy [migrations/TEMPLATE.md](migrations/TEMPLATE.md)
2. Rename to `vX-to-vY.md` (e.g., `v2-to-v3.md`)
3. Fill in all sections with actual changes
4. Create corresponding [migration rules](../scripts/migrations/)
5. Update this README to include the new guide

### Documentation Standards

- **Format**: Markdown with proper headings
- **Code Examples**: Always include before/after
- **Links**: Use relative paths for internal docs
- **Tone**: Clear, concise, and helpful
- **Audience**: Assume intermediate Java knowledge

---

## 📞 Support

### Getting Help

- **Questions**: [GitHub Discussions](https://github.com/marcusPrado02/java-commons/discussions)
- **Bug Reports**: [GitHub Issues](https://github.com/marcusPrado02/java-commons/issues)
- **Documentation Issues**: [GitHub Issues](https://github.com/marcusPrado02/java-commons/issues) (label: documentation)

### Frequently Asked Questions

See the FAQ sections in:
- [Upgrade Process FAQ](upgrade-process.md#faq)
- [Versioning FAQ](versioning-and-releases.md#faq)
- Migration guide FAQs (e.g., [v1→v2 FAQ](migrations/v1-to-v2.md#faq))

---

## 📝 Document Status

| Document | Last Updated | Status |
|----------|--------------|--------|
| versioning-and-releases.md | 2026-02-14 | ✅ Current |
| upgrade-process.md | 2026-02-14 | ✅ Current |
| migrations/TEMPLATE.md | 2026-02-14 | ✅ Current |
| migrations/v0-to-v1.md | 2026-02-14 | ✅ Current |
| migrations/v1-to-v2.md | 2026-02-14 | ✅ Current |
| modules.md | 2025-xx-xx | 🟡 Review needed |
| 00-overview.md | 2025-xx-xx | 🟡 Review needed |
| 01-dependency-rules.md | 2025-xx-xx | 🟡 Review needed |

---

**Documentation Version**: 1.0.0  
**Last Updated**: 2026-02-14  
**Maintained by**: Commons Library Team
