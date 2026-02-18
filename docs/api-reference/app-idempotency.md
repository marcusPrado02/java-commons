# API Reference: commons-app-idempotency

## Visão Geral

O módulo `commons-app-idempotency` fornece infraestrutura para garantir processamento idempotente de requisições e mensagens, evitando duplicações em sistemas distribuídos.

**Dependência Maven:**
```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-idempotency</artifactId>
</dependency>
```

---

## Core Concepts

### IdempotencyKey

Chave única que identifica uma operação.

```java
public class IdempotencyKey extends SingleValueObject<String> {
    
    private IdempotencyKey(String value) {
        super(value);
    }
    
    public static IdempotencyKey of(String value) {
        Preconditions.requireNonBlank(value, "Idempotency key cannot be blank");
        return new IdempotencyKey(value);
    }
    
    public static IdempotencyKey generate() {
        return new IdempotencyKey(UUID.randomUUID().toString());
    }
}
```

### IdempotencyRecord

Registro de uma operação processada.

```java
public class IdempotencyRecord {
    private IdempotencyKey key;
    private String operation;          // Nome da operação
    private String requestFingerprint; // Hash do request
    private String response;           // Resposta serializada
    private IdempotencyStatus status;  // PROCESSING, COMPLETED, FAILED
    private Instant createdAt;
    private Instant expiresAt;
}

public enum IdempotencyStatus {
    PROCESSING,   // Sendo processado
    COMPLETED,    // Completado com sucesso
    FAILED        // Falhou
}
```

---

## Core Interfaces

### IdempotencyService

```java
public interface IdempotencyService {
    
    /**
     * Tenta adquirir lock para processar operação
     * @return Optional.empty() se já está sendo processada
     */
    Optional<IdempotencyLock> tryAcquire(IdempotencyKey key, String operation);
    
    /**
     * Retorna resultado de operação já processada
     */
    Optional<IdempotencyRecord> findCompleted(IdempotencyKey key);
    
    /**
     * Marca operação como completada e armazena resultado
     */
    void markCompleted(IdempotencyKey key, Object result);
    
    /**
     * Marca operação como falhada
     */
    void markFailed(IdempotencyKey key, Throwable error);
    
    /**
     * Remove registros expirados
     */
    void cleanup();
}
```

---

## Exemplo Completo: Payment Service

### 1. REST Controller

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final IdempotencyService idempotencyService;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    
    @PostMapping
    public ResponseEntity<?> processPayment(
        @RequestHeader("Idempotency-Key") String idempotencyKeyValue,
        @RequestBody ProcessPaymentRequest request
    ) {
        IdempotencyKey key = IdempotencyKey.of(idempotencyKeyValue);
        
        // 1. Verifica se já foi processado
        Optional<IdempotencyRecord> existing = idempotencyService.findCompleted(key);
        if (existing.isPresent()) {
            // Retorna resposta cacheada
            return ResponseEntity
                .ok()
                .header("X-Idempotency-Cached", "true")
                .body(deserializeResponse(existing.get()));
        }
        
        // 2. Tenta adquirir lock
        Optional<IdempotencyLock> lock = idempotencyService.tryAcquire(
            key,
            "processPayment"
        );
        
        if (lock.isEmpty()) {
            // Operação já está sendo processada
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                    "error", "OPERATION_IN_PROGRESS",
                    "message", "Operation is already being processed"
                ));
        }
        
        try {
            // 3. Processa operação
            Result<PaymentResult> result = paymentService.processPayment(
                toCommand(request)
            );
            
            if (result.isFail()) {
                idempotencyService.markFailed(key, new RuntimeException(
                    result.problemOrNull().message()
                ));
                
                return ResponseEntity
                    .badRequest()
                    .body(result.problemOrNull());
            }
            
            // 4. Marca como completado
            PaymentResult paymentResult = result.getOrThrow();
            idempotencyService.markCompleted(key, paymentResult);
            
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentResult);
            
        } catch (Exception e) {
            idempotencyService.markFailed(key, e);
            throw e;
        }
    }
}
```

### 2. Application Service

```java
@Service
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    
    @Transactional
    public Result<PaymentResult> processPayment(ProcessPaymentCommand command) {
        // Lógica de negócio
        Payment payment = Payment.create(...);
        
        // Chama gateway de pagamento
        Result<PaymentGatewayResponse> gatewayResult = paymentGateway.charge(
            command.amount(),
            command.cardToken()
        );
        
        if (gatewayResult.isFail()) {
            return Result.fail(gatewayResult.problemOrNull());
        }
        
        payment.markAsCompleted(gatewayResult.getOrThrow());
        paymentRepository.save(payment);
        
        return Result.ok(PaymentResult.from(payment));
    }
}
```

### 3. Implementation

```java
@Service
public class DatabaseIdempotencyService implements IdempotencyService {
    
    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public Optional<IdempotencyLock> tryAcquire(IdempotencyKey key, String operation) {
        // Tenta inserir registro com status PROCESSING
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                .key(key)
                .operation(operation)
                .status(IdempotencyStatus.PROCESSING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofHours(24)))
                .build();
            
            repository.save(record);
            
            return Optional.of(new IdempotencyLock(key));
            
        } catch (DataIntegrityViolationException e) {
            // Já existe - não conseguiu lock
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<IdempotencyRecord> findCompleted(IdempotencyKey key) {
        return repository.findByKeyAndStatus(key, IdempotencyStatus.COMPLETED);
    }
    
    @Override
    @Transactional
    public void markCompleted(IdempotencyKey key, Object result) {
        repository.findByKey(key).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setResponse(serialize(result));
            repository.save(record);
        });
    }
    
    @Override
    @Transactional
    public void markFailed(IdempotencyKey key, Throwable error) {
        repository.findByKey(key).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            record.setErrorMessage(error.getMessage());
            repository.save(record);
        });
    }
    
    @Scheduled(fixedRate = 3600000)  // 1 hora
    @Override
    public void cleanup() {
        Instant cutoff = Instant.now();
        int deleted = repository.deleteByExpiresAtBefore(cutoff);
        log.info("Cleaned up {} expired idempotency records", deleted);
    }
}
```

---

## Database Schema

```sql
CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    operation VARCHAR(255) NOT NULL,
    request_fingerprint VARCHAR(64),
    response TEXT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    
    INDEX idx_expires_at (expires_at)
);
```

---

## Message Processing

### Kafka Consumer com Idempotência

```java
@Component
public class OrderEventHandler {
    
    private final IdempotencyService idempotencyService;
    private final OrderService orderService;
    
    @KafkaListener(topics = "orders.created")
    @Transactional
    public void handleOrderCreated(
        @Payload OrderCreatedEvent event,
        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String messageKey
    ) {
        // Usa event ID como idempotency key
        IdempotencyKey key = IdempotencyKey.of(event.eventId());
        
        // Verifica se já foi processado
        Optional<IdempotencyRecord> existing = idempotencyService.findCompleted(key);
        if (existing.isPresent()) {
            log.debug("Event already processed: {}", event.eventId());
            return;  // Skip
        }
        
        // Tenta adquirir lock
        Optional<IdempotencyLock> lock = idempotencyService.tryAcquire(
            key,
            "handleOrderCreated"
        );
        
        if (lock.isEmpty()) {
            log.warn("Event is being processed by another consumer: {}", event.eventId());
            return;  // Skip
        }
        
        try {
            // Processa
            orderService.processOrderCreated(event);
            
            // Marca como completado
            idempotencyService.markCompleted(key, Map.of("processed", true));
            
        } catch (Exception e) {
            idempotencyService.markFailed(key, e);
            throw e;  // Requeue
        }
    }
}
```

---

## Advanced Patterns

### 1. Request Fingerprinting

```java
public class RequestFingerprint {
    
    public static String compute(Object request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(request);
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute fingerprint", e);
        }
    }
}

@PostMapping
public ResponseEntity<?> processPayment(
    @RequestHeader("Idempotency-Key") String keyValue,
    @RequestBody ProcessPaymentRequest request
) {
    IdempotencyKey key = IdempotencyKey.of(keyValue);
    
    // Verifica fingerprint
    String fingerprint = RequestFingerprint.compute(request);
    Optional<IdempotencyRecord> existing = idempotencyService.findCompleted(key);
    
    if (existing.isPresent()) {
        if (!existing.get().requestFingerprint().equals(fingerprint)) {
            return ResponseEntity
                .badRequest()
                .body(Map.of(
                    "error", "IDEMPOTENCY_KEY_MISMATCH",
                    "message", "Idempotency key reused with different request body"
                ));
        }
        
        return ResponseEntity.ok(existing.get().response());
    }
    
    // Processa...
}
```

### 2. TTL Automático

```java
@Configuration
public class IdempotencyConfig {
    
    @Bean
    public IdempotencyProperties idempotencyProperties() {
        return IdempotencyProperties.builder()
            .ttl(Duration.ofHours(24))  // 24h de retenção
            .cleanupInterval(Duration.ofHours(1))  // Limpa a cada 1h
            .build();
    }
}
```

### 3. Redis Backend (Alta Performance)

```java
@Service
public class RedisIdempotencyService implements IdempotencyService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final Duration ttl = Duration.ofHours(24);
    
    @Override
    public Optional<IdempotencyLock> tryAcquire(IdempotencyKey key, String operation) {
        String redisKey = "idempotency:" + key.value();
        
        // SETNX (set if not exists)
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(redisKey, "PROCESSING", ttl);
        
        return acquired != null && acquired
            ? Optional.of(new IdempotencyLock(key))
            : Optional.empty();
    }
    
    @Override
    public Optional<IdempotencyRecord> findCompleted(IdempotencyKey key) {
        String redisKey = "idempotency:" + key.value();
        String value = redisTemplate.opsForValue().get(redisKey);
        
        if (value != null && !value.equals("PROCESSING")) {
            return Optional.of(deserialize(value));
        }
        
        return Optional.empty();
    }
    
    @Override
    public void markCompleted(IdempotencyKey key, Object result) {
        String redisKey = "idempotency:" + key.value();
        redisTemplate.opsForValue().set(redisKey, serialize(result), ttl);
    }
}
```

---

## Monitoring

```java
@Component
public class IdempotencyMetrics {
    
    private final MetricsFacade metrics;
    private final IdempotencyRepository repository;
    
    @Scheduled(fixedRate = 60000)  // 1 minuto
    public void reportMetrics() {
        // Total de registros
        long total = repository.count();
        metrics.recordGauge("idempotency.records.total", total);
        
        // Por status
        long processing = repository.countByStatus(IdempotencyStatus.PROCESSING);
        metrics.recordGauge("idempotency.processing", processing);
        
        long completed = repository.countByStatus(IdempotencyStatus.COMPLETED);
        metrics.recordGauge("idempotency.completed", completed);
        
        // Cache hits
        // (track via interceptor)
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use UUID v4 para idempotency keys
IdempotencyKey key = IdempotencyKey.generate();

// ✅ Valide fingerprint em caso de retry
if (!fingerprint.equals(existing.fingerprint())) {
    throw new IdempotencyMismatchException();
}

// ✅ Configure TTL apropriado
.ttl(Duration.ofHours(24))  // 24h geralmente é suficiente

// ✅ Retorne resposta cacheada com header
response.header("X-Idempotency-Cached", "true")

// ✅ Implemente cleanup periódico
@Scheduled(fixedRate = 3600000)
public void cleanup() { ... }
```

### ❌ DON'T

```java
// ❌ NÃO reutilize idempotency keys
// ❌ NÃO mantenha registros forever (use TTL)
// ❌ NÃO esqueça de marcar como completed/failed
// ❌ NÃO ignore conflitos (409)
```

---

## Ver Também

- [Transactional Outbox](app-outbox.md)
- [Messaging Patterns](../guides/messaging.md)
- [Observability](../guides/observability.md)
