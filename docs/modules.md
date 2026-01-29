# Modules

## Foundation
- commons (root): Maven aggregator
- commons-parent: shared build configuration (plugins, enforcer, defaults)
- commons-bom: dependency versions lock (import as BOM)
- commons-platform: platform marker module

## Kernel (framework-free, domain-safe)
- commons-kernel-core: base utilities and primitives
- commons-kernel-time: ClockProvider and time helpers
- commons-kernel-result: Result/Either/Option primitives
- commons-kernel-errors: error model + exception hierarchy
- commons-kernel-ddd: DDD base types (Entity, AggregateRoot, ValueObject, DomainEvent)

## Ports (Hexagonal)
- commons-ports-persistence: paging/sorting/filtering and repository ports (framework-free)
- commons-ports-messaging: message envelope/headers and publisher/consumer ports
- commons-ports-http: http client port + request/response primitives
- commons-ports-cache: cache port primitives
- commons-ports-secrets: secret store port primitives
- commons-ports-files: file storage port primitives


## Application
- commons-app-observability: correlation/request context, logging facade, metrics/tracing facade

## Governance / Testing
- commons-testkit-core: test fixtures, ids, clocks, shared assertions
- commons-archunit: architectural rule sets (to be applied by services)
- commons-quality: naming/public-api/dependency guardrails

## Adapters
- commons-adapters-web: framework-agnostic API response primitives
- commons-adapters-web-spring: Spring Web filters, exception advice, versioning hooks

## Spring Starters
- commons-spring-starter-observability: auto-config for correlation/request context filters

## OpenTelemetry
- commons-adapters-otel: OpenTelemetry implementation of TracerFacade
- commons-spring-starter-otel: plugs OTel Spring Boot starter + exposes TracerFacade bean

## Resilience
- commons-app-resilience: framework-agnostic policies + ResilienceExecutor contract
- commons-adapters-resilience4j: Resilience4j implementation of ResilienceExecutor
- commons-spring-starter-resilience: auto-config exposing ResilienceExecutor bean (Resilience4j or noop)

## EDA / Reliability
- commons-app-outbox: outbox model + ports + default processor (framework-agnostic)
- commons-app-idempotency: idempotency model + store port + default executor (framework-agnostic)
