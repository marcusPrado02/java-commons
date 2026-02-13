# commons-app-resilience

Módulo **framework-agnostic** com APIs para aplicar padrões de resiliência em chamadas a I/O (HTTP, mensageria, DB, etc.) via `ResilienceExecutor`.

## Conceitos

- `ResiliencePolicySet`: conjunto de políticas opcionais para uma operação.
- `ResilienceExecutor`: executa um `Supplier<T>` aplicando as políticas.
- `FallbackStrategy<T>`: estratégia de fallback executada quando a chamada falha.

## Policies

- `CircuitBreakerPolicy`: abre o circuito baseado em taxa de falha.
- `BulkheadPolicy`: limita concorrência (e opcionalmente tempo de espera).
- `RateLimiterPolicy`: limita requisições por janela de tempo.
- `TimeoutPolicy`: timeout por operação.
- `RetryPolicy`: retries com backoff.
- `CachePolicy`: cache de resultados (quando usado via `supplyCached`).

## Exemplos

### Execução simples

```java
ResiliencePolicySet policies = new ResiliencePolicySet(
    null,
    new TimeoutPolicy(Duration.ofSeconds(2)),
    new CircuitBreakerPolicy(50.0f, 20),
    BulkheadPolicy.of(10),
    null,
    null
);

String body = resilienceExecutor.supply("users.get", policies, () -> httpClient.get("/users"));
```

### Com fallback

```java
String body = resilienceExecutor.supply(
    "users.get",
    policies,
    () -> httpClient.get("/users"),
    FallbackStrategy.value("[]")
);
```

### Cache (requer `cacheKey`)

```java
ResiliencePolicySet cachedPolicies = new ResiliencePolicySet(
    null, null, null, null, null,
    new CachePolicy(1_000, Duration.ofMinutes(5))
);

User user = resilienceExecutor.supplyCached(
    "users.byId",
    cachedPolicies,
    userId,
    () -> userClient.fetch(userId)
);
```
