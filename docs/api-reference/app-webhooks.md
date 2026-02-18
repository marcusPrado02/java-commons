# API Reference: Webhooks

## Visão Geral

`commons-app-webhooks` fornece sistema completo de webhooks para notificar sistemas externos sobre eventos, com retry automático, assinaturas, validação de signatures e delivery tracking.

**Quando usar:**
- Notificar clientes sobre eventos (pedido criado, pagamento processado)
- Integração assíncrona entre sistemas
- Event-driven APIs
- Real-time notifications para sistemas externos
- Callback após processamento assíncrono

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-webhooks</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### WebhookSubscription

Define uma inscrição de webhook.

```java
public class WebhookSubscription {
    
    private final SubscriptionId id;
    private final String url;
    private final List<String> events;  // order.created, payment.completed
    private final String secret;        // Para HMAC signature
    private final SubscriptionStatus status;
    private final Map<String, String> metadata;
    
    public boolean subscribesTo(String eventType) {
        return events.contains(eventType) || events.contains("*");
    }
    
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }
}
```

### WebhookDelivery

Representa uma tentativa de entrega de webhook.

```java
public class WebhookDelivery {
    
    private final DeliveryId id;
    private final SubscriptionId subscriptionId;
    private final String eventType;
    private final String payload;
    private final DeliveryStatus status;
    private final int attemptCount;
    private final Instant nextRetryAt;
    private final List<DeliveryAttempt> attempts;
    
    public boolean shouldRetry() {
        return status == DeliveryStatus.FAILED 
            && attemptCount < maxRetries
            && Instant.now().isAfter(nextRetryAt);
    }
}
```

### WebhookService

Serviço principal para gerenciar webhooks.

```java
public interface WebhookService {
    
    /**
     * Registra nova subscription.
     */
    Result<SubscriptionId> subscribe(
        String url,
        List<String> events,
        Map<String, String> metadata
    );
    
    /**
     * Remove subscription.
     */
    Result<Void> unsubscribe(SubscriptionId id);
    
    /**
     * Dispara webhook para evento.
     */
    Result<Void> trigger(String eventType, Object payload);
    
    /**
     * Lista deliveries com falha.
     */
    List<WebhookDelivery> getFailedDeliveries();
    
    /**
     * Retenta delivery manualmente.
     */
    Result<Void> retryDelivery(DeliveryId id);
}
```

---

## 💡 Uso Básico

### Registrar Subscription

```java
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    
    private final WebhookService webhookService;
    
    @PostMapping("/subscriptions")
    public ResponseEntity<?> createSubscription(
        @RequestBody SubscriptionRequest request
    ) {
        Result<SubscriptionId> result = webhookService.subscribe(
            request.url(),
            request.events(),
            request.metadata()
        );
        
        return result
            .map(id -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("subscriptionId", id.value())))
            .recover(problem -> ResponseEntity
                .badRequest()
                .body(problem));
    }
    
    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<?> deleteSubscription(@PathVariable String id) {
        Result<Void> result = webhookService.unsubscribe(
            SubscriptionId.of(id)
        );
        
        return result
            .map(v -> ResponseEntity.noContent().build())
            .recover(problem -> ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problem));
    }
}
```

### Disparar Webhook

```java
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final WebhookService webhookService;
    
    @Transactional
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        // Criar pedido
        Order order = Order.create(command);
        Result<Void> saveResult = orderRepository.save(order);
        
        if (saveResult.isFail()) {
            return Result.fail(saveResult.problemOrNull());
        }
        
        // Disparar webhook
        webhookService.trigger(
            "order.created",
            OrderWebhookPayload.from(order)
        );
        
        return Result.ok(order.id());
    }
}
```

---

## 🔐 Signature Validation

### HMAC Signature

```java
@Component
public class WebhookSignatureGenerator {
    
    public String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(keySpec);
            
            byte[] signatureBytes = mac.doFinal(
                payload.getBytes(StandardCharsets.UTF_8)
            );
            
            return "sha256=" + bytesToHex(signatureBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
    
    public boolean validateSignature(
        String payload,
        String signature,
        String secret
    ) {
        String expected = generateSignature(payload, secret);
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8)
        );
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
```

### Webhook Delivery com Signature

```java
@Component
public class WebhookDeliveryService {
    
    private final WebClient webClient;
    private final WebhookSignatureGenerator signatureGenerator;
    private final StructuredLog log;
    
    public Result<Void> deliver(
        WebhookSubscription subscription,
        String eventType,
        String payload
    ) {
        // Gerar signature
        String signature = signatureGenerator.generateSignature(
            payload,
            subscription.secret()
        );
        
        // Headers
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");
        headers.add("X-Webhook-Event", eventType);
        headers.add("X-Webhook-Signature", signature);
        headers.add("X-Webhook-Delivery-Id", UUID.randomUUID().toString());
        headers.add("X-Webhook-Timestamp", Instant.now().toString());
        
        try {
            // Fazer POST
            WebClient.ResponseSpec response = webClient
                .post()
                .uri(subscription.url())
                .headers(h -> h.addAll(headers))
                .bodyValue(payload)
                .retrieve();
            
            // Aguardar resposta (timeout 30s)
            response.toBodilessEntity()
                .timeout(Duration.ofSeconds(30))
                .block();
            
            log.info("Webhook delivered successfully")
                .field("subscriptionId", subscription.id().value())
                .field("eventType", eventType)
                .field("url", subscription.url())
                .log();
            
            return Result.ok();
            
        } catch (WebClientResponseException e) {
            log.warn("Webhook delivery failed")
                .field("subscriptionId", subscription.id().value())
                .field("statusCode", e.getStatusCode().value())
                .field("responseBody", e.getResponseBodyAsString())
                .log();
            
            return Result.fail(Problem.of(
                "WEBHOOK.DELIVERY_FAILED",
                "HTTP " + e.getStatusCode().value()
            ));
            
        } catch (Exception e) {
            log.error("Webhook delivery error", e).log();
            
            return Result.fail(Problem.of(
                "WEBHOOK.DELIVERY_ERROR",
                e.getMessage()
            ));
        }
    }
}
```

---

## 🔄 Retry Strategy

### Exponential Backoff

```java
@Component
public class WebhookRetryProcessor {
    
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryService deliveryService;
    
    /**
     * Processa deliveries pendentes de retry.
     */
    @Scheduled(fixedDelay = 60000)  // A cada 1 minuto
    public void processRetries() {
        List<WebhookDelivery> pending = deliveryRepository
            .findPendingRetries(100);
        
        for (WebhookDelivery delivery : pending) {
            processRetry(delivery);
        }
    }
    
    private void processRetry(WebhookDelivery delivery) {
        // Buscar subscription
        Optional<WebhookSubscription> subscription = 
            subscriptionRepository.findById(delivery.subscriptionId());
        
        if (subscription.isEmpty() || !subscription.get().isActive()) {
            deliveryRepository.markAsAbandoned(delivery.id());
            return;
        }
        
        // Tentar entregar
        Result<Void> result = deliveryService.deliver(
            subscription.get(),
            delivery.eventType(),
            delivery.payload()
        );
        
        if (result.isOk()) {
            // Sucesso
            deliveryRepository.markAsDelivered(
                delivery.id(),
                Instant.now()
            );
        } else {
            // Falha - agendar próximo retry
            int newAttemptCount = delivery.attemptCount() + 1;
            Instant nextRetry = calculateNextRetry(newAttemptCount);
            
            deliveryRepository.recordFailedAttempt(
                delivery.id(),
                newAttemptCount,
                nextRetry,
                result.problemOrNull().detail()
            );
            
            // Se excedeu max attempts, marcar como failed
            if (newAttemptCount >= 5) {
                deliveryRepository.markAsFailed(delivery.id());
            }
        }
    }
    
    private Instant calculateNextRetry(int attemptCount) {
        // Exponential backoff: 1min, 5min, 15min, 1h, 3h
        long[] delays = {60, 300, 900, 3600, 10800};  // seconds
        
        int index = Math.min(attemptCount - 1, delays.length - 1);
        return Instant.now().plusSeconds(delays[index]);
    }
}
```

---

## 📊 Webhook Events

### Event Types

```java
public enum WebhookEventType {
    
    // Order events
    ORDER_CREATED("order.created"),
    ORDER_UPDATED("order.updated"),
    ORDER_CANCELLED("order.cancelled"),
    ORDER_COMPLETED("order.completed"),
    
    // Payment events
    PAYMENT_INITIATED("payment.initiated"),
    PAYMENT_COMPLETED("payment.completed"),
    PAYMENT_FAILED("payment.failed"),
    PAYMENT_REFUNDED("payment.refunded"),
    
    // User events
    USER_CREATED("user.created"),
    USER_UPDATED("user.updated"),
    USER_DELETED("user.deleted"),
    
    // Wildcard
    ALL("*");
    
    private final String value;
    
    public static WebhookEventType from(String value) {
        return Arrays.stream(values())
            .filter(type -> type.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown event type: " + value
            ));
    }
}
```

### Webhook Payload

```java
public record WebhookPayload(
    String id,
    String eventType,
    Instant timestamp,
    Object data,
    Map<String, String> metadata
) {
    public static WebhookPayload create(
        String eventType,
        Object data
    ) {
        return new WebhookPayload(
            UUID.randomUUID().toString(),
            eventType,
            Instant.now(),
            data,
            Map.of()
        );
    }
}

// Order event payload
public record OrderWebhookPayload(
    String orderId,
    String customerId,
    BigDecimal total,
    String status,
    Instant createdAt
) {
    public static OrderWebhookPayload from(Order order) {
        return new OrderWebhookPayload(
            order.id().value(),
            order.customerId().value(),
            order.total().amount(),
            order.status().name(),
            order.createdAt()
        );
    }
}
```

---

## 🎯 Complete Example

### Order Service with Webhooks

```java
@Service
@Transactional
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final WebhookService webhookService;
    private final StructuredLog log;
    
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        try {
            // Criar pedido
            Order order = Order.create(command);
            Result<Void> saveResult = orderRepository.save(order);
            
            if (saveResult.isFail()) {
                return Result.fail(saveResult.problemOrNull());
            }
            
            // Disparar webhook (assíncrono)
            CompletableFuture.runAsync(() -> {
                WebhookPayload payload = WebhookPayload.create(
                    "order.created",
                    OrderWebhookPayload.from(order)
                );
                
                webhookService.trigger("order.created", payload);
            });
            
            return Result.ok(order.id());
            
        } catch (Exception e) {
            log.error("Failed to create order", e).log();
            return Result.fail(Problem.of(
                "ORDER.CREATION_FAILED",
                e.getMessage()
            ));
        }
    }
    
    public Result<Void> cancelOrder(OrderId orderId, String reason) {
        return orderRepository.findById(orderId)
            .flatMap(order -> {
                Result<Void> cancelResult = order.cancel(reason);
                if (cancelResult.isFail()) {
                    return cancelResult;
                }
                
                Result<Void> saveResult = orderRepository.save(order);
                if (saveResult.isFail()) {
                    return saveResult;
                }
                
                // Webhook
                CompletableFuture.runAsync(() -> {
                    webhookService.trigger(
                        "order.cancelled",
                        WebhookPayload.create(
                            "order.cancelled",
                            Map.of(
                                "orderId", orderId.value(),
                                "reason", reason,
                                "cancelledAt", Instant.now()
                            )
                        )
                    );
                });
                
                return Result.ok();
            });
    }
}
```

---

## 📈 Monitoring

### Webhook Metrics

```java
@Component
public class WebhookMetricsCollector {
    
    private final MetricsFacade metrics;
    
    public void recordDeliveryAttempt(
        String eventType,
        DeliveryStatus status,
        Duration duration
    ) {
        metrics.incrementCounter(
            "webhook.delivery.attempts",
            "eventType", eventType,
            "status", status.name()
        );
        
        metrics.recordTimer(
            "webhook.delivery.duration",
            duration,
            "eventType", eventType
        );
    }
    
    public void recordRetry(String eventType, int attemptNumber) {
        metrics.incrementCounter(
            "webhook.delivery.retries",
            "eventType", eventType,
            "attempt", String.valueOf(attemptNumber)
        );
    }
    
    public void recordSubscription(String eventType) {
        metrics.incrementCounter(
            "webhook.subscriptions.created",
            "eventType", eventType
        );
    }
}
```

### Health Check

```java
@Component
public class WebhookHealthCheck implements HealthCheck {
    
    private final WebhookDeliveryRepository deliveryRepository;
    
    @Override
    public String name() {
        return "webhooks";
    }
    
    @Override
    public HealthStatus check() {
        try {
            // Verificar deliveries falhados
            long failedCount = deliveryRepository.countFailed();
            
            if (failedCount > 1000) {
                return HealthStatus.down()
                    .withDetail("failedDeliveries", failedCount)
                    .withMessage("Too many failed webhook deliveries");
            }
            
            if (failedCount > 100) {
                return HealthStatus.degraded()
                    .withDetail("failedDeliveries", failedCount)
                    .withMessage("High number of failed deliveries");
            }
            
            return HealthStatus.up()
                .withDetail("failedDeliveries", failedCount);
                
        } catch (Exception e) {
            return HealthStatus.down()
                .withException(e);
        }
    }
}
```

---

## 🧪 Testing

### Unit Tests

```java
class WebhookServiceTest {
    
    private WebhookService webhookService;
    private WebhookSubscriptionRepository subscriptionRepository;
    private WebhookDeliveryService deliveryService;
    
    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(WebhookSubscriptionRepository.class);
        deliveryService = mock(WebhookDeliveryService.class);
        webhookService = new DefaultWebhookService(
            subscriptionRepository,
            deliveryService
        );
    }
    
    @Test
    void shouldCreateSubscription() {
        // When
        Result<SubscriptionId> result = webhookService.subscribe(
            "https://example.com/webhook",
            List.of("order.created", "order.updated"),
            Map.of()
        );
        
        // Then
        assertThat(result.isOk()).isTrue();
        verify(subscriptionRepository).save(any(WebhookSubscription.class));
    }
    
    @Test
    void shouldTriggerWebhookForSubscribers() {
        // Given
        WebhookSubscription sub = createSubscription(
            "https://example.com/webhook",
            List.of("order.created")
        );
        
        when(subscriptionRepository.findActiveByEventType("order.created"))
            .thenReturn(List.of(sub));
        
        when(deliveryService.deliver(any(), any(), any()))
            .thenReturn(Result.ok());
        
        // When
        Result<Void> result = webhookService.trigger(
            "order.created",
            Map.of("orderId", "order-123")
        );
        
        // Then
        assertThat(result.isOk()).isTrue();
        verify(deliveryService).deliver(eq(sub), eq("order.created"), any());
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class WebhookIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15");
    
    @Autowired
    private WebhookService webhookService;
    
    private MockWebServer mockWebServer;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void shouldDeliverWebhook() throws Exception {
        // Given: Subscribe
        String url = mockWebServer.url("/webhook").toString();
        Result<SubscriptionId> subResult = webhookService.subscribe(
            url,
            List.of("test.event"),
            Map.of()
        );
        assertThat(subResult.isOk()).isTrue();
        
        // Mock server response
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("OK"));
        
        // When: Trigger
        Result<Void> triggerResult = webhookService.trigger(
            "test.event",
            Map.of("data", "test")
        );
        
        // Then
        assertThat(triggerResult.isOk()).isTrue();
        
        await().atMost(Duration.ofSeconds(5))
            .until(() -> mockWebServer.getRequestCount() > 0);
        
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getHeader("X-Webhook-Event")).isEqualTo("test.event");
        assertThat(request.getHeader("X-Webhook-Signature")).isNotNull();
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use HMAC signature para segurança
String signature = signatureGenerator.generateSignature(payload, secret);
headers.add("X-Webhook-Signature", signature);

// ✅ Implemente retry com exponential backoff
Instant nextRetry = calculateNextRetry(attemptCount);

// ✅ Envie webhook de forma assíncrona
CompletableFuture.runAsync(() -> webhookService.trigger(...));

// ✅ Valide URL da subscription
if (!isValidUrl(url)) {
    return Result.fail(Problem.of("INVALID_URL", "..."));
}

// ✅ Limite timeout de delivery
response.timeout(Duration.ofSeconds(30)).block();

// ✅ Monitore deliveries falhados
metrics.incrementCounter("webhook.delivery.failed");
```

### ❌ DON'T

```java
// ❌ NÃO bloqueie request aguardando webhook
webhookService.trigger(...).block();  // ❌ Síncrono!

// ❌ NÃO envie dados sensíveis em webhooks
payload.put("password", user.getPassword());  // ❌

// ❌ NÃO ignore falhas de validação de signature
if (!validateSignature(...)) {
    // ❌ Processar mesmo assim é inseguro!
}

// ❌ NÃO retry indefinidamente
while (failed) {
    retry();  // ❌ Vai loop infinito!
}

// ❌ NÃO exponha subscription secrets
@GetMapping("/subscriptions/{id}")
public Subscription get(@PathVariable String id) {
    return repository.findById(id);  // ❌ Expõe secret!
}
```

---

## Ver Também

- [Event-Driven Architecture](../guides/domain-events.md)
- [Outbox Pattern](app-outbox.md) - Event publishing
- [API Gateway](app-api-gateway.md) - Gateway patterns
- [Resilience Guide](../guides/resilience.md) - Retry policies
