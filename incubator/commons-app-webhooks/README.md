# Commons App Webhooks

Webhook delivery system with retry policies, signature verification, and delivery tracking.

## Features

- **Event Subscription**: Webhooks subscribe to specific event types
- **Automatic Retries**: Failed deliveries retry with configurable backoff
- **Signature Verification**: HMAC-SHA256 signatures prevent spoofing
- **Delivery Tracking**: Full audit trail of all delivery attempts
- **Status Management**: Track pending, in-progress, succeeded, failed, exhausted
- **Retry Policies**: Exponential backoff, fixed delay, or no retry
- **Idempotency Support**: Events can include idempotency keys

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-app-webhooks</artifactId>
</dependency>
```

## Quick Start

### 1. Register a Webhook

```java
Webhook webhook = Webhook.builder()
    .id("webhook-123")
    .url(URI.create("https://example.com/webhook"))
    .events(Set.of("order.created", "order.updated"))
    .secret("my-secret-key")
    .active(true)
    .description("Order notification webhook")
    .build();

webhookRepository.save(webhook);
```

### 2. Deliver an Event

```java
// Create event
WebhookEvent event = WebhookEvent.builder()
    .id("event-456")
    .type("order.created")
    .payload(Map.of(
        "orderId", "789",
        "amount", 100.0,
        "customer", "john@example.com"
    ))
    .idempotencyKey("order-789-created")
    .build();

// Setup delivery service
WebhookDeliveryService service = new WebhookDeliveryService(
    webhookRepository,
    deliveryRepository,
    httpClient,
    RetryPolicy.exponentialBackoff(5, Duration.ofSeconds(1))
);

// Deliver to all subscribed webhooks
Result<Void> result = service.deliver(event);
```

### 3. Process Pending Deliveries

```java
// Run periodically (e.g., every 30 seconds)
@Scheduled(fixedDelay = 30000)
public void processWebhooks() {
    service.processPendingDeliveries().ifOk(count ->
        log.info("Processed {} deliveries", count)
    );
}
```

### 4. Verify Signatures (Receiver Side)

```java
@PostMapping("/webhook")
public ResponseEntity<Void> receiveWebhook(
        @RequestBody String payload,
        @RequestHeader("X-Webhook-Signature") String signature) {

    if (!WebhookSignature.verify(payload, secret, signature)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Process webhook...
    return ResponseEntity.ok().build();
}
```

## Core Components

### Webhook

Represents a registered webhook endpoint:

```java
Webhook webhook = Webhook.builder()
    .id("webhook-123")
    .url(URI.create("https://api.example.com/webhooks"))
    .events(Set.of("order.created", "order.updated", "order.cancelled"))
    .secret("webhook-secret-key-xyz")
    .active(true)
    .description("Order processing webhook")
    .build();

// Check subscription
boolean subscribed = webhook.isSubscribedTo("order.created"); // true

// Wildcard subscription (all events)
Webhook allEvents = Webhook.builder()
    .events(Set.of("*"))
    .build();
```

### WebhookEvent

Event to be delivered:

```java
WebhookEvent event = WebhookEvent.builder()
    .id(UUID.randomUUID().toString())
    .type("order.created")
    .payload(Map.of(
        "orderId", "123",
        "amount", 99.99,
        "status", "confirmed"
    ))
    .occurredAt(Instant.now())
    .idempotencyKey("order-123-created")
    .build();
```

### WebhookDelivery

Tracks delivery attempts:

```java
WebhookDelivery delivery = WebhookDelivery.builder()
    .id("delivery-789")
    .webhookId("webhook-123")
    .eventId("event-456")
    .status(WebhookDeliveryStatus.SUCCEEDED)
    .attemptNumber(2) // Second attempt
    .httpStatusCode(200)
    .responseBody("{\"received\":true}")
    .scheduledAt(Instant.now().minus(Duration.ofMinutes(5)))
    .attemptedAt(Instant.now().minus(Duration.ofMinutes(2)))
    .completedAt(Instant.now())
    .responseTime(Duration.ofMillis(350))
    .build();

// Check status
boolean canRetry = delivery.isRetryable();
boolean isDone = delivery.isTerminal();
```

### Delivery Status

```java
public enum WebhookDeliveryStatus {
    PENDING,      // Waiting to be sent
    IN_PROGRESS,  // Currently sending
    SUCCEEDED,    // Delivered successfully (HTTP 2xx)
    FAILED,       // Failed but will retry
    EXHAUSTED,    // Failed and no more retries
    CANCELLED     // Manually cancelled
}
```

## Retry Policies

### Exponential Backoff

Doubles delay between retries:

```java
// 1s, 2s, 4s, 8s, 16s
RetryPolicy policy = RetryPolicy.exponentialBackoff(
    5,                          // max retries
    Duration.ofSeconds(1)       // initial delay
);

// Custom multiplier and max delay
RetryPolicy policy = new ExponentialBackoffRetryPolicy(
    10,                         // max retries
    Duration.ofSeconds(1),      // initial delay
    3.0,                        // multiplier (triple each time)
    Duration.ofHours(1)         // max delay cap
);
```

### Fixed Delay

Same delay between retries:

```java
// 30s between each attempt
RetryPolicy policy = RetryPolicy.fixedDelay(
    3,                          // max retries
    Duration.ofSeconds(30)      // delay
);
```

### No Retry

Fail immediately:

```java
RetryPolicy policy = RetryPolicy.noRetry();
```

## Signature Verification

### Generating Signatures (Sender)

```java
String payload = "{\"orderId\":\"123\"}";
String secret = "webhook-secret-key";

String signature = WebhookSignature.generate(payload, secret);
// Returns: "sha256=a1b2c3d4..."

// Add to request header
headers.put("X-Webhook-Signature", signature);
```

### Verifying Signatures (Receiver)

```java
@PostMapping("/webhook")
public ResponseEntity<Void> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("X-Webhook-Signature") String signature) {

    String secret = getWebhookSecret(); // From configuration

    if (!WebhookSignature.verify(payload, secret, signature)) {
        log.warn("Invalid webhook signature");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    processWebhook(payload);
    return ResponseEntity.ok().build();
}
```

**Security Notes:**
- Uses HMAC-SHA256 for cryptographic security
- Constant-time comparison prevents timing attacks
- Signature format: `sha256=<hex-hash>`

## Delivery Service

### Basic Usage

```java
WebhookDeliveryService service = new WebhookDeliveryService(
    webhookRepository,
    deliveryRepository,
    httpClient,
    RetryPolicy.exponentialBackoff(5, Duration.ofSeconds(1))
);

// Deliver event to all subscribed webhooks
Result<Void> result = service.deliver(event);

// Process pending/retry deliveries
Result<Integer> processed = service.processPendingDeliveries();

// Manually retry a failed delivery
Result<Void> retry = service.retry("delivery-id");
```

### HTTP Headers Sent

```
POST /webhook HTTP/1.1
Host: example.com
Content-Type: application/json
X-Webhook-Signature: sha256=abc123...
X-Webhook-Event-Id: event-456
X-Webhook-Delivery-Id: delivery-789

{"orderId": "123", "amount": 99.99}
```

### Success Criteria

A delivery is considered successful if:
- HTTP status code is 2xx (200-299)
- Response received within timeout

A delivery will be retried if:
- HTTP status code is 5xx (server error)
- Network timeout or connection error
- Any exception during delivery

A delivery will NOT be retried if:
- HTTP status code is 4xx (client error, except 429)
- Maximum retries exhausted
- Webhook is inactive or deleted

## Repository Interfaces

### WebhookRepository

```java
public interface WebhookRepository {
    Result<Webhook> save(Webhook webhook);
    Result<Optional<Webhook>> findById(String id);
    Result<List<Webhook>> findByEventType(String eventType);
    Result<List<Webhook>> findAll();
    Result<Void> deleteById(String id);
}
```

### WebhookDeliveryRepository

```java
public interface WebhookDeliveryRepository {
    Result<WebhookDelivery> save(WebhookDelivery delivery);
    Result<Optional<WebhookDelivery>> findById(String id);
    Result<List<WebhookDelivery>> findByEventId(String eventId);
    Result<List<WebhookDelivery>> findByWebhookId(String webhookId);
    Result<List<WebhookDelivery>> findScheduledBefore(
        Instant before,
        WebhookDeliveryStatus status
    );
    Result<Integer> deleteOlderThan(Instant before);
}
```

## Complete Example

```java
public class OrderService {

    private final WebhookDeliveryService webhookService;

    public void createOrder(Order order) {
        // Save order to database
        orderRepository.save(order);

        // Create webhook event
        WebhookEvent event = WebhookEvent.builder()
            .id(UUID.randomUUID().toString())
            .type("order.created")
            .payload(Map.of(
                "orderId", order.getId(),
                "customerId", order.getCustomerId(),
                "amount", order.getTotal(),
                "items", order.getItems().size(),
                "status", "confirmed"
            ))
            .occurredAt(Instant.now())
            .idempotencyKey("order-" + order.getId() + "-created")
            .build();

        // Deliver to all subscribed webhooks
        webhookService.deliver(event)
            .ifError(problem ->
                log.error("Failed to queue webhook delivery: {}", problem)
            );
    }
}

@Configuration
public class WebhookConfig {

    @Bean
    public WebhookDeliveryService webhookDeliveryService(
            WebhookRepository webhookRepository,
            WebhookDeliveryRepository deliveryRepository,
            WebhookHttpClient httpClient) {

        RetryPolicy retryPolicy = RetryPolicy.exponentialBackoff(
            5,                          // 5 retries
            Duration.ofSeconds(1)       // Starting at 1s
        );

        return new WebhookDeliveryService(
            webhookRepository,
            deliveryRepository,
            httpClient,
            retryPolicy
        );
    }

    @Bean
    public WebhookHttpClient webhookHttpClient() {
        return new ApacheHttpClient(); // Your implementation
    }
}

@Component
public class WebhookScheduler {

    private final WebhookDeliveryService service;

    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void processWebhooks() {
        service.processPendingDeliveries()
            .ifOk(count -> log.debug("Processed {} webhook deliveries", count))
            .ifError(problem -> log.error("Webhook processing error: {}", problem));
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldDeliveries() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(30));
        deliveryRepository.deleteOlderThan(cutoff)
            .ifOk(count -> log.info("Deleted {} old deliveries", count));
    }
}
```

## Best Practices

### 1. Security

- **Always verify signatures** on the receiver side
- **Use HTTPS** for webhook URLs
- **Rotate secrets** periodically
- **Validate payload** before processing
- **Rate limit** webhook processing

### 2. Reliability

- **Exponential backoff** prevents overwhelming failed endpoints
- **Set reasonable retry limits** (5-10 attempts)
- **Use idempotency keys** for duplicate detection
- **Archive old deliveries** to prevent database bloat
- **Monitor delivery rates** and alert on failures

### 3. Performance

- **Process deliveries asynchronously** (background jobs)
- **Batch database operations** when possible
- **Use connection pooling** for HTTP client
- **Implement timeouts** (10-30 seconds per request)
- **Consider queueing** for high-volume events

### 4. User Experience

- **Provide webhook management UI** for users
- **Support webhook testing** endpoints
- **Show delivery history** and logs
- **Allow manual retries** for failed deliveries
- **Send alerts** for consistently failing webhooks

### 5. Event Design

- **Use specific event types** (order.created vs order.*)
- **Include necessary data** in payload
- **Keep payloads small** (< 1MB)
- **Add timestamp** and version info
- **Document event schemas**

## Testing

```java
@Test
void shouldDeliverWebhookSuccessfully() {
    // Given
    Webhook webhook = Webhook.builder()
        .id("webhook-1")
        .url(URI.create("https://example.com/webhook"))
        .events(Set.of("test.event"))
        .secret("secret")
        .active(true)
        .build();

    WebhookEvent event = WebhookEvent.builder()
        .id("event-1")
        .type("test.event")
        .payload(Map.of("key", "value"))
        .build();

    when(webhookRepository.findByEventType("test.event"))
        .thenReturn(Result.ok(List.of(webhook)));

    when(httpClient.send(any()))
        .thenReturn(Result.ok(
            WebhookHttpResponse.builder()
                .statusCode(200)
                .responseTime(Duration.ofMillis(100))
                .build()
        ));

    // When
    Result<Void> result = service.deliver(event);

    // Then
    assertThat(result.isOk()).isTrue();
    verify(httpClient).send(argThat(req ->
        req.getUrl().equals(webhook.getUrl()) &&
        req.getHeaders().containsKey("X-Webhook-Signature")
    ));
}
```

## License

Copyright © 2026 Marcus Prado. All rights reserved.
