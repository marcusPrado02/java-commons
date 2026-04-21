# commons-adapters-resilience4j

Implementação de `ResilienceExecutor` usando Resilience4j. Combina circuit breaker, retry, bulkhead, rate limiter, time limiter e cache local em uma única API.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-resilience4j</artifactId>
</dependency>
```

## Setup básico

```java
ResilienceExecutor resilience = new Resilience4jExecutor();
// ou com métricas
ResilienceExecutor resilience = new Resilience4jExecutor(metricsFacade);
```

## Políticas disponíveis

```java
ResiliencePolicySet policies = ResiliencePolicySet.builder()
    .circuitBreaker(CircuitBreakerPolicy.builder()
        .failureRateThreshold(50)           // abre com 50% de falhas
        .waitDurationInOpenState(Duration.ofSeconds(10))
        .slidingWindowSize(10)
        .build())
    .retry(RetryPolicy.builder()
        .maxAttempts(3)
        .waitDuration(Duration.ofMillis(200))
        .build())
    .timeLimiter(TimeLimiterPolicy.of(Duration.ofSeconds(5)))
    .build();
```

## Executar com resiliência

```java
// Operação síncrona com circuit breaker + retry
Result<PaymentResponse> result = resilience.execute(
    "payment-gateway",
    policies,
    () -> paymentGateway.charge(request),
    failure -> Result.fail(Problems.technical(
        ErrorCode.of("PAYMENT.UNAVAILABLE"),
        "Payment gateway unavailable: " + failure.getMessage()))
);

// Operação assíncrona com timeout
CompletableFuture<Result<UserDto>> async = resilience.executeAsync(
    "user-service",
    policies,
    () -> userServiceClient.getUser(userId),
    failure -> Result.fail(Problems.technical(ErrorCode.of("USER_SERVICE.TIMEOUT"), failure.getMessage()))
);
```

## Rate Limiter

```java
ResiliencePolicySet withRateLimit = ResiliencePolicySet.builder()
    .rateLimiter(RateLimiterPolicy.builder()
        .limitForPeriod(100)                 // 100 req/s
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .timeoutDuration(Duration.ofMillis(50))
        .build())
    .build();

resilience.execute("api-endpoint", withRateLimit, () -> handle(request), e -> fallback());
```

## Bulkhead (isolamento de thread pool)

```java
ResiliencePolicySet withBulkhead = ResiliencePolicySet.builder()
    .bulkhead(BulkheadPolicy.builder()
        .maxConcurrentCalls(25)
        .maxWaitDuration(Duration.ofMillis(100))
        .build())
    .build();
```

## Cache local (Caffeine)

```java
ResiliencePolicySet withCache = ResiliencePolicySet.builder()
    .cache(CachePolicy.builder()
        .maximumSize(500)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build())
    .build();

// Segunda chamada com mesma chave serve do cache
resilience.execute("product-catalog", withCache, () -> catalog.findAll(), e -> List.of());
```

## Monitorar estado dos circuit breakers

```java
Resilience4jExecutor r4j = (Resilience4jExecutor) resilience;

r4j.circuitBreakerStatuses().forEach(status -> {
    log.info("CB {} state={} failureRate={}%",
        status.name(), status.state(), status.failureRate());
});
```
