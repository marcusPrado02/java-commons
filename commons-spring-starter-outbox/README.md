# Commons Spring Starter Outbox

Spring Boot starter for **Outbox Pattern** with comprehensive configuration, health monitoring, and metrics.

## Features

- ✅ **Auto-configuration** for JPA-based outbox repository
- ✅ **Configurable batch processing** with retry strategies
- ✅ **Health indicator** with customizable thresholds
- ✅ **Micrometer metrics** for monitoring
- ✅ **Scheduled processing** with cron-like intervals
- ✅ **Exponential backoff** retry strategy
- ✅ **Circuit breaker** support (optional)

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-spring-starter-outbox</artifactId>
</dependency>
```

## Configuration

### Processing Configuration

Controls batch size and circuit breaker:

```yaml
commons:
  outbox:
    processing:
      batch-size: 10               # Number of messages processed per batch (default: 10)
      use-circuit-breaker: false   # Enable circuit breaker wrapper (default: false)
```

### Scheduling Configuration

Controls automatic polling:

```yaml
commons:
  outbox:
    scheduling:
      enabled: true                # Enable scheduled processing (default: true)
      fixed-delay: 60000           # Interval between polls in milliseconds (default: 60000 = 1min)
```

### Retry Configuration

Controls exponential backoff strategy:

```yaml
commons:
  outbox:
    retry:
      max-attempts: 5              # Maximum retry attempts before dead letter (default: 5)
      initial-backoff: 100         # Initial backoff in milliseconds (default: 100ms)
      max-backoff: 30000           # Maximum backoff cap in milliseconds (default: 30s)
      backoff-multiplier: 2.0      # Backoff multiplier for exponential growth (default: 2.0)
```

**Backoff calculation**: `delay = min(initialBackoff × multiplier^attempts, maxBackoff)`

### Health Indicator Configuration

Reports outbox health based on pending/failed message counts:

```yaml
commons:
  outbox:
    health:
      enabled: true                # Enable health indicator (default: true)
      warning-threshold: 100       # Messages before DEGRADED status (default: 100)
      error-threshold: 500         # Messages before DOWN/OUT_OF_SERVICE (default: 500)
```

**Health statuses**:
- **UP**: pending < warningThreshold AND failed < warningThreshold
- **DEGRADED**: pending > warningThreshold OR failed > warningThreshold
- **OUT_OF_SERVICE**: pending > errorThreshold
- **DOWN**: failed > errorThreshold

## Metrics

When Micrometer is available, the starter automatically registers:

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `outbox.published` | Counter | `topic` | Successful publications |
| `outbox.failed` | Counter | `topic`, `reason` | Failed publications |
| `outbox.dead` | Counter | `topic` | Messages moved to dead letter queue |
| `outbox.publish.latency` | Timer | `topic` | Publication latency |
| `outbox.batch.processing` | Timer | `size` | Batch processing duration |

## Usage

### 1. Implement OutboundPublisher

Provide an integration with your message broker:

```java
@Component
public class KafkaOutboundPublisher implements OutboundPublisher {

  private final KafkaTemplate<String, String> kafka;

  @Override
  public void publish(String topic, String body, Map<String, String> headers) {
    kafka.send(topic, body);
  }
}
```

### 2. Store Messages via OutboxSupport

Inject and use:

```java
@Service
public class OrderService {

  private final OutboxSupport outbox;

  public void createOrder(Order order) {
    // ... save order to database ...

    outbox.store("orders.created", order, Map.of(
      "order-id", order.id().toString()
    ));
  }
}
```

### 3. Monitor Health

Access health endpoint:

```bash
GET /actuator/health/outbox
```

Response:

```json
{
  "status": "UP",
  "details": {
    "pending": 5,
    "processing": 1,
    "published": 1234,
    "failed": 2,
    "dead": 0,
    "total": 1242
  }
}
```

### 4. Monitor Metrics

Prometheus scrape endpoint:

```
GET /actuator/prometheus
```

Example metrics:

```
outbox_published_total{topic="orders.created"} 1234
outbox_failed_total{topic="orders.created",reason="Connection timeout"} 2
outbox_publish_latency_seconds_sum{topic="orders.created"} 12.5
```

## Advanced Configuration Example

```yaml
commons:
  outbox:
    processing:
      batch-size: 20
      use-circuit-breaker: true
    scheduling:
      enabled: true
      fixed-delay: 30000  # Poll every 30 seconds
    retry:
      max-attempts: 3
      initial-backoff: 200
      max-backoff: 60000
      backoff-multiplier: 3.0
    health:
      enabled: true
      warning-threshold: 50
      error-threshold: 200
```

## Architecture

This starter bridges:

1. **commons-app-outbox**: Framework-agnostic outbox pattern implementation
2. **commons-adapters-persistence-jpa**: JPA repository adapter
3. **Spring Boot**: Auto-configuration and actuator integration

## Dependencies

### Required

- `spring-boot-starter-data-jpa`
- `commons-app-outbox`

### Optional

- `spring-boot-starter-actuator` (for health indicator)
- `micrometer-core` (for metrics)

## License

See [LICENSE](../LICENSE)
