# commons-spring-starter-resilience

Auto-configuração Spring Boot para `ResilienceExecutor` usando Resilience4j. Configura circuit breakers, retry, bulkhead e rate limiter via `application.yml`.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-spring-starter-resilience</artifactId>
</dependency>
```

## Configuração mínima (`application.yml`)

```yaml
commons:
  resilience:
    circuit-breaker:
      failure-rate-threshold: 50        # % de falhas para abrir
      wait-duration-open: 10s
      sliding-window-size: 10
    retry:
      max-attempts: 3
      wait-duration: 200ms
    time-limiter:
      timeout: 5s
```

## Configuração completa

```yaml
commons:
  resilience:
    circuit-breaker:
      enabled: true
      failure-rate-threshold: 50
      slow-call-rate-threshold: 80
      slow-call-duration-threshold: 3s
      wait-duration-open: 15s
      sliding-window-size: 20
      permitted-calls-in-half-open: 5
    retry:
      enabled: true
      max-attempts: 3
      wait-duration: 200ms
      exponential-backoff-multiplier: 2.0
      retry-on: IOException, TimeoutException
    bulkhead:
      enabled: true
      max-concurrent-calls: 25
      max-wait-duration: 100ms
    rate-limiter:
      enabled: false
    time-limiter:
      enabled: true
      timeout: 5s
    cache:
      enabled: true
      maximum-size: 500
      expire-after-write: 5m
```

## Injeção automática

```java
@Service
public class PaymentService {

    private final ResilienceExecutor resilience;
    private final ResiliencePolicySet policies;  // injetado do yml

    public Result<PaymentResponse> charge(PaymentRequest request) {
        return resilience.execute(
            "payment-gateway",
            policies,
            () -> gateway.charge(request),
            ex -> Result.fail(Problems.technical(
                ErrorCode.of("PAYMENT.UNAVAILABLE"), ex.getMessage()))
        );
    }
}
```

## Health indicator

O starter registra automaticamente um `HealthIndicator` que expõe o estado dos circuit breakers:

```
GET /actuator/health
{
  "components": {
    "resilience": {
      "status": "UP",
      "details": {
        "payment-gateway": "CLOSED",
        "user-service": "HALF_OPEN"
      }
    }
  }
}
```

## Sem Spring (manual)

```java
// Use commons-adapters-resilience4j diretamente
ResilienceExecutor executor = new Resilience4jExecutor(metricsFacade);
```
