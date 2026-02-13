# Commons App - Observability

APIs e utilitários framework-agnostic para observabilidade:

- `RequestContext`: contexto por thread (correlation/tenant/actor/etc)
- `StructuredLog`: helper para produzir mapas de log estruturado com contexto + sanitização
- `MetricsFacade` + `Metrics`: API simples para registrar SLIs/SLOs (counters/histograms/gauges)
- `HealthChecks`: registry/evaluator de readiness/liveness checks

## Structured logging

```java
RequestContext.put(ContextKeys.CORRELATION_ID, "c1");
RequestContext.put(ContextKeys.TENANT_ID, "t1");

Map<String, Object> log = StructuredLog.builder()
  .level("INFO")
  .message("order created")
  .field("orderId", "123")
  .build();

// logger.info("{}", log);
```

Sanitização padrão redige chaves sensíveis (`password`, `token`, `authorization`, etc.).

## Métricas (SLI/SLO)

```java
MetricsFacade metrics = MetricsFacade.noop(); // em produção, usar uma implementação real (OTel/Micrometer)

Metrics.recordRequest(metrics, "CreateOrder", Duration.ofMillis(42), true);
```

## Health checks + probes

```java
HealthChecks checks = new HealthChecks(List.of(
  new HealthCheck() {
    public String name() { return "db"; }
    public HealthCheckType type() { return HealthCheckType.READINESS; }
    public HealthCheckResult check() { return HealthCheckResult.up(name(), type()); }
  }
));

HealthReport readiness = checks.readiness();
HealthReport liveness = checks.liveness();
```

Adapters/starters (ex: Spring) podem expor `HealthReport` via endpoints `/health/readiness` e `/health/liveness`.
