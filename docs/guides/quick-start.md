# Quick Start Guides

Guias rápidos para cenários comuns de uso da plataforma Commons.

---

## 🚀 Criar Novo Microsserviço REST

### 1. Adicione dependências

```xml
<dependencies>
    <!-- Kernel (domínio) -->
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-kernel-ddd</artifactId>
    </dependency>
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-kernel-result</artifactId>
    </dependency>
    
    <!-- Application layer -->
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-app-observability</artifactId>
    </dependency>
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-app-resilience</artifactId>
    </dependency>
    
    <!-- Spring Boot starters -->
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-spring-starter-observability</artifactId>
    </dependency>
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-adapters-persistence-jpa</artifactId>
    </dependency>
</dependencies>
```

### 2. Defina entidades de domínio

```java
// UserId.java
public record UserId(String value) implements Identifier<String> {
    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }
}

// Email.java
public record Email(String value) implements ValueObject {
    public static Result<Email> validate(String raw) {
        if (raw == null || !raw.contains("@")) {
            return Result.fail("EMAIL.INVALID", "Invalid email format");
        }
        return Result.ok(new Email(raw.trim().toLowerCase()));
    }
}

// User.java
public class User extends Entity<UserId> {
    private Email email;
    private String name;
    private UserStatus status;
    
    public User(UserId id, TenantId tenantId, Email email, 
                String name, AuditStamp created) {
        super(id, tenantId, created);
        this.email = email;
        this.name = name;
        this.status = UserStatus.ACTIVE;
    }
    
    public void updateEmail(Email newEmail, AuditStamp updatedBy) {
        this.email = newEmail;
        markAsUpdated(updatedBy);
    }
    
    // Getters
    public Email email() { return email; }
    public String name() { return name; }
    public UserStatus status() { return status; }
}
```

### 3. Crie Repository (Port)

```java
public interface UserRepository {
    Result<User> findById(UserId id);
    Optional<User> findByEmail(Email email);
    Result<Void> save(User user);
    Result<Void> delete(UserId id);
}
```

### 4. Application Service

```java
@Service
@Transactional
public class UserApplicationService {
    
    private final UserRepository userRepository;
    private final MetricsFacade metrics;
    
    public Result<UserId> createUser(CreateUserCommand cmd) {
        return Email.validate(cmd.email())
            .flatMap(email -> {
                if (userRepository.findByEmail(email).isPresent()) {
                    return Result.fail("USER.DUPLICATE_EMAIL", "Email already in use");
                }
                
                User user = new User(
                    UserId.generate(),
                    TenantId.current(),
                    email,
                    cmd.name(),
                    AuditStamp.now()
                );
                
                return userRepository.save(user)
                    .map(v -> user.id());
            })
            .onOk(userId -> metrics.incrementCounter("user.created"))
            .onFail(problem -> metrics.incrementCounter("user.creation.failed"));
    }
}
```

### 5. REST Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserApplicationService userService;
    
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        Result<UserId> result = userService.createUser(
            new CreateUserCommand(request.email(), request.name())
        );
        
        return result
            .map(userId -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("userId", userId.value())))
            .orElseGet(() -> ResponseEntity
                .badRequest()
                .body(result.problemOrNull()));
    }
}
```

**Pronto!** Você tem um microsserviço REST com DDD, Result pattern, observabilidade e persistência.

---

## 📨 Adicionar Mensageria (Kafka)

### 1. Adicione dependência

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-messaging-kafka</artifactId>
</dependency>
```

### 2. Configure Kafka

```java
@Configuration
public class KafkaConfig {
    
    @Bean
    public KafkaConfiguration kafkaConfiguration() {
        return KafkaConfiguration.builder()
            .bootstrapServers("localhost:9092")
            .groupId("user-service")
            .clientId("user-service-1")
            .build();
    }
    
    @Bean
    public MessagePublisher messagePublisher(KafkaConfiguration config) {
        return new KafkaMessagePublisher(config);
    }
}
```

### 3. Publique eventos

```java
@Service
public class UserEventPublisher {
    
    private final MessagePublisher publisher;
    
    public void publishUserCreated(User user) {
        UserCreatedEvent event = new UserCreatedEvent(
            user.id(),
            user.email(),
            Instant.now()
        );
        
        Message message = Message.builder()
            .topic("users.created")
            .key(user.id().value())
            .payload(serialize(event))
            .build();
        
        publisher.publish(message);
    }
}
```

### 4. Consuma eventos

```java
@Service
public class NotificationService implements MessageConsumer {
    
    private final EmailService emailService;
    
    @Override
    public void consume(Message message) {
        if ("users.created".equals(message.topic())) {
            UserCreatedEvent event = deserialize(message.payload());
            emailService.sendWelcomeEmail(event.email());
        }
    }
}
```

---

## 🔄 Adicionar Transactional Outbox

### 1. Adicione dependência

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-outbox</artifactId>
</dependency>
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-spring-starter-outbox</artifactId>
</dependency>
```

### 2. Configure Outbox

```yaml
# application.yml
commons:
  outbox:
    enabled: true
    polling-interval: 5s
    batch-size: 100
    max-retries: 3
```

### 3. Use no Application Service

```java
@Service
@Transactional
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final OutboxPublisher outboxPublisher;
    
    public Result<OrderId> createOrder(CreateOrderCommand cmd) {
        // 1. Cria Order aggregate
        Order order = Order.create(...);
        order.submit(AuditStamp.now());
        
        // 2. Salva aggregate
        orderRepository.save(order);
        
        // 3. Pull domain events e salva no outbox (MESMA TRANSAÇÃO)
        List<DomainEvent> events = order.pullDomainEvents();
        for (DomainEvent event : events) {
            outboxPublisher.publish(
                "orders.created",
                order.id().value(),
                event
            );
        }
        
        // COMMIT garante atomicidade: Order + Outbox
        return Result.ok(order.id());
    }
}
```

**Pronto!** Eventos serão publicados garantidamente (at-least-once).

---

## 🛡️ Adicionar Resiliência

### 1. Configure políticas

```java
@Configuration
public class ResilienceConfig {
    
    @Bean
    public ResiliencePolicySet defaultPolicies() {
        return new ResiliencePolicySet(
            new CachePolicy(Duration.ofMinutes(5), 1000),
            new TimeoutPolicy(Duration.ofSeconds(10)),
            new CircuitBreakerPolicy(50.0f, 20),
            BulkheadPolicy.of(50, Duration.ofMillis(100)),
            new RateLimiterPolicy(1000, Duration.ofMinutes(1), Duration.ZERO),
            new RetryPolicy(3, Duration.ofMillis(500), 2.0f)
        );
    }
}
```

### 2. Use nas chamadas externas

```java
@Service
public class PaymentService {
    
    private final ResilienceExecutor resilience;
    private final ResiliencePolicySet policies;
    private final PaymentGatewayClient gatewayClient;
    
    public PaymentResult processPayment(Payment payment) {
        return resilience.supply(
            "payment.process",
            policies,
            () -> gatewayClient.charge(payment),
            FallbackStrategy.value(PaymentResult.pending())
        );
    }
}
```

**Pronto!** Sua aplicação tem circuit breaker, retry, timeout, etc.

---

## 📊 Adicionar Observabilidade

### 1. Configure RequestContext filter

```java
@Component
public class RequestContextFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response,
                                   FilterChain chain) throws ServletException, IOException {
        try {
            RequestContext.put(ContextKeys.CORRELATION_ID, 
                getOrGenerateCorrelationId(request));
            RequestContext.put(ContextKeys.TENANT_ID, 
                extractTenantId(request));
            
            chain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }
}
```

### 2. Use logs estruturados

```java
@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    public Order createOrder(CreateOrderCommand cmd) {
        logger.info("{}", StructuredLog.builder()
            .message("Creating order")
            .field("userId", cmd.userId())
            .field("itemCount", cmd.items().size())
            .build());
        
        // ... lógica
        
        logger.info("{}", StructuredLog.builder()
            .message("Order created")
            .field("orderId", order.id().value())
            .field("amount", order.total().toString())
            .build());
        
        return order;
    }
}
```

### 3. Registre métricas

```java
@Service
public class OrderMetrics {
    
    private final MetricsFacade metrics;
    
    public void recordOrderCreated(Order order) {
        metrics.incrementCounter("order.created", 
            Map.of("status", order.status().name()));
    }
    
    public void recordOrderDuration(Duration duration, boolean success) {
        metrics.recordHistogram("order.duration", 
            duration.toMillis(),
            Map.of("success", String.valueOf(success)));
    }
}
```

### 4. Configure Health Checks

```java
@Configuration
public class HealthConfig {
    
    @Bean
    public HealthChecks healthChecks(
            DataSource dataSource,
            RedisClient redis) {
        
        return new HealthChecks(List.of(
            new DatabaseHealthCheck(dataSource),
            new RedisHealthCheck(redis)
        ));
    }
}

@RestController
public class HealthController {
    
    @GetMapping("/health/liveness")
    public ResponseEntity<HealthReport> liveness() {
        HealthReport report = healthChecks.liveness();
        return ResponseEntity
            .status(report.status() == HealthStatus.UP ? 200 : 503)
            .body(report);
    }
}
```

**Pronto!** Observabilidade completa: logs, métricas, health checks.

---

## 🔐 Adicionar Multi-Tenancy

### 1. Adicione dependência

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-multi-tenancy</artifactId>
</dependency>
```

### 2. Configure resolução de tenant

```java
@Bean
public TenantResolver tenantResolver() {
    return new HeaderTenantResolver("X-Tenant-ID");
}

@Component
public class TenantFilter extends OncePerRequestFilter {
    
    private final TenantResolver resolver;
    
    @Override
    protected void doFilterInternal(...) {
        try {
            TenantContext context = resolver.resolve(request);
            TenantContextHolder.setContext(context);
            
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
```

### 3. Use TenantId nas entidades

```java
public class Order extends Entity<OrderId> {
    
    public Order(OrderId id, TenantId tenantId, ...) {
        super(id, tenantId, ...);  // Tenant passado para Entity
    }
}

// Verificação automática em queries
String tenantId = TenantContextHolder.getCurrentTenantId();
List<Order> orders = orderRepository.findByTenant(TenantId.of(tenantId));
```

**Pronto!** Multi-tenancy com isolamento automático.

---

## Ver Também

- [Guias Completos](README.md#guides)
- [API Reference](README.md#api-reference)
- [Exemplos](../../examples/)
