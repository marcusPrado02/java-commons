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

## Application
- commons-app-observability: correlation/request context, logging facade, metrics/tracing facade

## Governance / Testing
- commons-testkit-core: test fixtures, ids, clocks, shared assertions
- commons-archunit: architectural rule sets (to be applied by services)
- commons-quality: naming/public-api/dependency guardrails
