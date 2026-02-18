# Guia: Spring Boot Integration

## Visão Geral

Guia completo para integrar Commons Platform com Spring Boot, aproveitando auto-configuration, dependency injection e ecossistema Spring.

---

## 🚀 Quick Start

### 1. Dependências Maven

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Commons Kernel (domain layer) -->
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-kernel-ddd</artifactId>
    </dependency>
    
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-kernel-result</artifactId>
    </dependency>
    
    <!-- Commons App (application layer) -->
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-app-observability</artifactId>
    </dependency>
    
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-app-resilience</artifactId>
    </dependency>
    
    <!-- Commons Spring Starter (auto-configuration) -->
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-spring-starter-observability</artifactId>
    </dependency>
</dependencies>
```

### 2. Application Properties

```yaml
# application.yml
spring:
  application:
    name: order-service
  
server:
  port: 8080

# Commons Configuration
commons:
  observability:
    enabled: true
    request-context:
      generate-correlation-id: true
    structured-logging:
      enabled: true
      format: json
  
  resilience:
    enabled: true
    defaults:
      circuit-breaker:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
      retry:
        max-attempts: 3
        wait-duration: 1s
```

### 3. Main Application

```java
@SpringBootApplication
@EnableCommonsObservability
@EnableCommonsResilience
public class OrderServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

---

## 🏗️ Architecture Setup

### Domain Layer (Framework-Free)

```java
// commons-kernel-ddd - sem dependências Spring
public class Order extends AggregateRoot<OrderId> {
    
    private CustomerId customerId;
    private OrderStatus status;
    private List<OrderItem> items;
    
    public Result<Void> submit() {
        if (items.isEmpty()) {
            return Result.fail(Problem.of(
                "ORDER.EMPTY_CART",
                "Cannot submit empty order"
            ));
        }
        
        this.status = OrderStatus.SUBMITTED;
        recordEvent(new OrderSubmitted(id(), Instant.now()));
        
        return Result.ok();
    }
}
```

### Application Layer (Spring Services)

```java
@Service
@Transactional
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final OutboxPublisher outboxPublisher;
    private final MetricsFacade metrics;
    
    public OrderApplicationService(
        OrderRepository orderRepository,
        OutboxPublisher outboxPublisher,
        MetricsFacade metrics
    ) {
        this.orderRepository = orderRepository;
        this.outboxPublisher = outboxPublisher;
        this.metrics = metrics;
    }
    
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        Instant start = Instant.now();
        
        try {
            // Domain logic
            Order order = Order.create(command);
            order.submit();
            
            // Persistence
            Result<Void> saveResult = orderRepository.save(order);
            if (saveResult.isFail()) {
                return Result.fail(saveResult.problemOrNull());
            }
            
            // Outbox
            order.pullDomainEvents().forEach(event ->
                outboxPublisher.publish("orders.events", order.id().value(), event)
            );
            
            // Metrics
            metrics.incrementCounter("order.created");
            metrics.recordTimer(
                "order.creation.duration",
                Duration.between(start, Instant.now())
            );
            
            return Result.ok(order.id());
            
        } catch (Exception e) {
            metrics.incrementCounter("order.creation.error");
            throw e;
        }
    }
}
```

### Infrastructure Layer (Adapters)

```java
@Repository
public class JpaOrderRepository implements OrderRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public Result<Void> save(Order order) {
        try {
            OrderEntity entity = OrderEntity.from(order);
            entityManager.merge(entity);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(Problem.of("ORDER.SAVE_FAILED", e.getMessage()));
        }
    }
    
    @Override
    public Result<Order> findById(OrderId id) {
        OrderEntity entity = entityManager.find(OrderEntity.class, id.value());
        
        if (entity == null) {
            return Result.fail(Problem.of("ORDER.NOT_FOUND", "Order not found"));
        }
        
        return Result.ok(entity.toDomain());
    }
}
```

### Presentation Layer (REST Controllers)

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderApplicationService orderService;
    
    public OrderController(OrderApplicationService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping
    public ResponseEntity<?> createOrder(
        @RequestBody @Valid CreateOrderRequest request
    ) {
        CreateOrderCommand command = toCommand(request);
        Result<OrderId> result = orderService.createOrder(command);
        
        return result
            .map(orderId -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("orderId", orderId.value()))
            )
            .recover(problem -> ResponseEntity
                .status(mapStatus(problem))
                .body(toProblemDetail(problem))
            );
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        Result<Order> result = orderService.findOrder(OrderId.of(orderId));
        
        return result
            .map(order -> ResponseEntity.ok(OrderResponse.from(order)))
            .recover(problem -> ResponseEntity
                .status(mapStatus(problem))
                .body(toProblemDetail(problem))
            );
    }
}
```

---

## 🔧 Configuration

### Auto-Configuration Classes

```java
@Configuration
@ConditionalOnProperty(prefix = "commons.observability", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ObservabilityProperties.class)
public class CommonsObservabilityAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public RequestContextFilter requestContextFilter(ObservabilityProperties properties) {
        return new RequestContextFilter(properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public StructuredLogFactory structuredLogFactory() {
        return new StructuredLogFactory();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MetricsFacade metricsFacade(MeterRegistry meterRegistry) {
        return new MicrometerMetricsFacade(meterRegistry);
    }
}
```

### Custom Configuration

```java
@Configuration
public class OrderServiceConfig {
    
    @Bean
    public OrderRepository orderRepository(EntityManager entityManager) {
        return new JpaOrderRepository(entityManager);
    }
    
    @Bean
    public MessagePublisher messagePublisher(KafkaTemplate<String, String> kafkaTemplate) {
        return new KafkaMessagePublisher(kafkaTemplate);
    }
    
    @Bean
    @ConditionalOnProperty("app.outbox.enabled")
    public OutboxProcessor outboxProcessor(
        OutboxRepository outboxRepository,
        MessagePublisher messagePublisher
    ) {
        return new DefaultOutboxProcessor(outboxRepository, messagePublisher);
    }
}
```

---

## 📊 Observability Integration

### Structured Logging

```java
@Component
public class OrderEventHandler {
    
    private final StructuredLog log;
    
    public OrderEventHandler(StructuredLogFactory logFactory) {
        this.log = logFactory.getLog(OrderEventHandler.class);
    }
    
    @KafkaListener(topics = "orders.events")
    public void handleOrderEvent(OrderEvent event) {
        log.info("Processing order event")
            .field("eventType", event.eventType())
            .field("orderId", event.orderId())
            .field("correlationId", RequestContext.getCorrelationId())
            .log();
        
        // Process...
    }
}
```

### Metrics with Micrometer

```java
@Service
public class OrderMetricsCollector {
    
    private final MetricsFacade metrics;
    
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        metrics.incrementCounter(
            "order.created",
            "status", event.status().name()
        );
        
        metrics.recordGauge(
            "order.amount",
            event.totalAmount().doubleValue(),
            "currency", event.currency()
        );
    }
}
```

### Health Checks

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            return Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("status", "connected")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

---

## 🔄 Transaction Management

### Declarative Transactions

```java
@Service
@Transactional
public class OrderService {
    
    @Transactional(readOnly = true)
    public Optional<Order> findOrder(OrderId id) {
        return orderRepository.findById(id);
    }
    
    @Transactional
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        // Tudo na mesma transação
        Order order = Order.create(command);
        orderRepository.save(order);
        outboxPublisher.publish(...);  // Mesmo transaction
        
        return Result.ok(order.id());
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOutboxBatch() {
        // Nova transação independente
    }
}
```

### Event Publishing After Commit

```java
@Component
public class DomainEventPublisher {
    
    private final MessagePublisher messagePublisher;
    private final ThreadLocal<List<DomainEvent>> events = new ThreadLocal<>();
    
    public void register(DomainEvent event) {
        if (events.get() == null) {
            events.set(new ArrayList<>());
        }
        events.get().add(event);
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvents(ContextRefreshedEvent event) {
        List<DomainEvent> toPublish = events.get();
        if (toPublish != null) {
            toPublish.forEach(messagePublisher::publish);
            events.remove();
        }
    }
}
```

---

## 🧪 Testing

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Autowired
    private OrderApplicationService orderService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        CreateOrderCommand command = new CreateOrderCommand(...);
        
        // When
        Result<OrderId> result = orderService.createOrder(command);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        OrderId orderId = result.getOrThrow();
        Result<Order> found = orderRepository.findById(orderId);
        assertThat(found.isOk()).isTrue();
    }
}
```

### Mock MVC Tests

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OrderApplicationService orderService;
    
    @Test
    void shouldCreateOrder() throws Exception {
        // Given
        OrderId orderId = OrderId.generate();
        when(orderService.createOrder(any()))
            .thenReturn(Result.ok(orderId));
        
        // When/Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "customerId": "customer-123",
                        "items": [
                            {"productId": "product-1", "quantity": 2}
                        ]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(orderId.value()));
    }
}
```

---

## 🔌 Profiles & Environments

### Profile-Based Configuration

```yaml
# application-dev.yml
commons:
  observability:
    structured-logging:
      format: pretty  # Logs legíveis em dev

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orders_dev

# application-prod.yml
commons:
  observability:
    structured-logging:
      format: json  # Logs JSON em prod

spring:
  datasource:
    url: ${DATABASE_URL}
```

### Profile-Specific Beans

```java
@Configuration
public class MessagingConfig {
    
    @Bean
    @Profile("dev")
    public MessagePublisher inMemoryPublisher() {
        return new InMemoryMessagePublisher();
    }
    
    @Bean
    @Profile("prod")
    public MessagePublisher kafkaPublisher(KafkaTemplate<String, String> kafka) {
        return new KafkaMessagePublisher(kafka);
    }
}
```

---

## 📦 Starters Recomendados

```xml
<!-- Observability (logs, metrics, tracing) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-spring-starter-observability</artifactId>
</dependency>

<!-- Resilience (circuit breaker, retry, etc.) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-spring-starter-resilience</artifactId>
</dependency>

<!-- Web (REST controllers, exception handling) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-spring-starter-web</artifactId>
</dependency>

<!-- Persistence (JPA integration) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-spring-starter-persistence</artifactId>
</dependency>

<!-- Messaging (Kafka/RabbitMQ integration) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-spring-starter-messaging</artifactId>
</dependency>
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use constructor injection
@Service
public class OrderService {
    private final OrderRepository repository;
    
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}

// ✅ Mantenha domain layer framework-free
// ✅ Use @Transactional em application services
// ✅ Configure profiles para ambientes diferentes
// ✅ Use Testcontainers para integration tests
```

### ❌ DON'T

```java
// ❌ NÃO use field injection
@Autowired private OrderRepository repository;  // ❌

// ❌ NÃO coloque Spring annotations no domain
@Service  // ❌ No domain layer!
public class Order extends AggregateRoot<OrderId> {}

// ❌ NÃO esqueça @Transactional
public void createOrder() {  // ❌ Sem transação!
    orderRepository.save(order);
    outboxPublisher.publish(event);
}
```

---

## Ver Também

- [Quick Start Guide](quick-start.md)
- [Observability Guide](observability.md)
- [Testing Strategies](testing.md)
- [Configuration Management](configuration.md)
