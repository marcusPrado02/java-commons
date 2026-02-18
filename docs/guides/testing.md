# Guia: Testing Strategies

## Visão Geral

Estratégias e práticas para testar aplicações construídas com Commons Platform.

---

## 🎯 Pirâmide de Testes

```
        /\
       /  \      E2E Tests (poucos)
      /____\     - Testa fluxo completo
     /      \    - Browser, HTTP real
    /________\   
   /          \  Integration Tests (alguns)
  /____________\ - Testa com dependências reais
 /              \
/________________\ Unit Tests (muitos)
                   - Testa lógica isolada
                   - Rápido, determinístico
```

---

## 🧪 Unit Tests

### Domain Layer (Kernel)

Testes de entidades, value objects e lógica de domínio.

#### Testando Value Objects

```java
class EmailTest {
    
    @Test
    void shouldCreateValidEmail() {
        // When
        Result<Email> result = Email.validate("john@example.com");
        
        // Then
        assertThat(result.isOk()).isTrue();
        assertThat(result.getOrNull().value()).isEqualTo("john@example.com");
    }
    
    @Test
    void shouldRejectInvalidEmail() {
        // When
        Result<Email> result = Email.validate("invalid-email");
        
        // Then
        assertThat(result.isFail()).isTrue();
        
        Problem problem = result.problemOrNull();
        assertThat(problem.code()).isEqualTo("EMAIL.INVALID_FORMAT");
        assertThat(problem.message()).contains("Invalid email");
    }
    
    @Test
    void shouldNormalizeEmail() {
        // When
        Result<Email> result = Email.validate("  JOHN@EXAMPLE.COM  ");
        
        // Then
        assertThat(result.getOrNull().value()).isEqualTo("john@example.com");
    }
}
```

#### Testando Entities

```java
class UserTest {
    
    private UserId userId;
    private TenantId tenantId;
    private Email email;
    private AuditStamp auditStamp;
    
    @BeforeEach
    void setUp() {
        userId = UserId.generate();
        tenantId = TenantId.of("tenant-1");
        email = Email.of("john@example.com");
        auditStamp = AuditStamp.now(ActorId.of("admin"));
    }
    
    @Test
    void shouldCreateUserWithValidData() {
        // When
        User user = new User(userId, tenantId, email, "John Doe", auditStamp);
        
        // Then
        assertThat(user.id()).isEqualTo(userId);
        assertThat(user.email()).isEqualTo(email);
        assertThat(user.name()).isEqualTo("John Doe");
        assertThat(user.isDeleted()).isFalse();
    }
    
    @Test
    void shouldUpdateEmail() {
        // Given
        User user = new User(userId, tenantId, email, "John Doe", auditStamp);
        Email newEmail = Email.of("newemail@example.com");
        AuditStamp updateStamp = AuditStamp.now(ActorId.of("user"));
        
        // When
        user.updateEmail(newEmail, updateStamp);
        
        // Then
        assertThat(user.email()).isEqualTo(newEmail);
        assertThat(user.audit().updated()).isPresent();
    }
    
    @Test
    void shouldMarkAsDeleted() {
        // Given
        User user = new User(userId, tenantId, email, "John Doe", auditStamp);
        
        // When
        user.delete(AuditStamp.now(ActorId.of("admin")));
        
        // Then
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.deletion()).isPresent();
    }
}
```

#### Testando Aggregates com Domain Events

```java
class OrderTest {
    
    @Test
    void shouldRecordEventWhenOrderSubmitted() {
        // Given
        Order order = createDraftOrder();
        order.addItem(product, 2, auditStamp);
        
        // When
        order.submit(auditStamp);
        
        // Then
        List<DomainEvent> events = order.pullDomainEvents();
        assertThat(events).hasSize(3);  // Created, ItemAdded, Submitted
        assertThat(events.get(2)).isInstanceOf(OrderSubmitted.class);
        
        OrderSubmitted event = (OrderSubmitted) events.get(2);
        assertThat(event.orderId()).isEqualTo(order.id());
        assertThat(event.totalAmount()).isEqualTo(order.totalAmount());
    }
    
    @Test
    void shouldClearEventsAfterPull() {
        // Given
        Order order = createDraftOrder();
        order.submit(auditStamp);
        
        // When
        order.pullDomainEvents();  // Primeiro pull
        List<DomainEvent> secondPull = order.pullDomainEvents();
        
        // Then
        assertThat(secondPull).isEmpty();  // Eventos foram limpos
    }
}
```

### Application Layer

Testes de application services com mocks.

```java
@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private MetricsFacade metrics;
    
    @InjectMocks
    private UserApplicationService userService;
    
    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        String email = "john@example.com";
        String name = "John Doe";
        
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(Result.ok());
        
        // When
        Result<UserId> result = userService.createUser(
            new CreateUserCommand(email, name)
        );
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        verify(userRepository).save(argThat(user -> 
            user.email().value().equals(email) &&
            user.name().equals(name)
        ));
        
        verify(metrics).incrementCounter("user.created");
    }
    
    @Test
    void shouldRejectDuplicateEmail() {
        // Given
        String email = "existing@example.com";
        when(userRepository.findByEmail(any()))
            .thenReturn(Optional.of(createUser()));
        
        // When
        Result<UserId> result = userService.createUser(
            new CreateUserCommand(email, "John")
        );
        
        // Then
        assertThat(result.isFail()).isTrue();
        
        Problem problem = result.problemOrNull();
        assertThat(problem.code()).isEqualTo("USER.DUPLICATE_EMAIL");
        
        verify(userRepository, never()).save(any());
        verify(metrics).incrementCounter("user.creation.failed");
    }
}
```

---

## 🔌 Integration Tests

Testes com dependências reais (bancos, mensageria, etc.).

### Persistence Integration

```java
@SpringBootTest
@Testcontainers
class UserRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @Transactional
    void shouldSaveAndFindUser() {
        // Given
        User user = createUser();
        
        // When
        userRepository.save(user);
        Result<User> found = userRepository.findById(user.id());
        
        // Then
        assertThat(found.isOk()).isTrue();
        
        User loadedUser = found.getOrNull();
        assertThat(loadedUser.id()).isEqualTo(user.id());
        assertThat(loadedUser.email()).isEqualTo(user.email());
    }
    
    @Test
    @Transactional
    void shouldFindByEmail() {
        // Given
        User user = createUser();
        userRepository.save(user);
        
        // When
        Optional<User> found = userRepository.findByEmail(user.email());
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(user.id());
    }
}
```

### Messaging Integration

```java
@SpringBootTest
@Testcontainers
class KafkaIntegrationTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
    
    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Autowired
    private MessagePublisher publisher;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Test
    void shouldPublishMessageToKafka() throws Exception {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        String payload = "{\"test\": \"data\"}";
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedPayload = new AtomicReference<>();
        
        // Consumer para verificar
        Consumer<String, String> consumer = createConsumer(topic);
        consumer.subscribe(List.of(topic));
        
        // When
        publisher.publish(Message.builder()
            .topic(topic)
            .key(key)
            .payload(payload)
            .build());
        
        // Then - Poll até receber
        consumer.poll(Duration.ofSeconds(10)).forEach(record -> {
            receivedPayload.set(record.value());
            latch.countDown();
        });
        
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedPayload.get()).isEqualTo(payload);
    }
}
```

### Outbox Integration

```java
@SpringBootTest
@Testcontainers
class OutboxIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Autowired
    private OutboxPublisher outboxPublisher;
    
    @Autowired
    private OutboxRepository outboxRepository;
    
    @Autowired
    private OutboxProcessor outboxProcessor;
    
    @Test
    @Transactional
    void shouldStoreMessageInOutbox() {
        // Given
        String topic = "orders.created";
        String aggregateId = "order-123";
        OrderCreatedEvent event = new OrderCreatedEvent(...);
        
        // When
        outboxPublisher.publish(topic, aggregateId, event);
        
        // Then
        List<OutboxMessage> messages = outboxRepository.findPending(10);
        assertThat(messages).hasSize(1);
        
        OutboxMessage message = messages.get(0);
        assertThat(message.topic()).isEqualTo(topic);
        assertThat(message.aggregateId()).isEqualTo(aggregateId);
        assertThat(message.status()).isEqualTo(OutboxStatus.PENDING);
    }
    
    @Test
    void shouldProcessOutboxMessages() {
        // Given
        outboxPublisher.publish("test.topic", "agg-1", new TestEvent());
        
        // When
        outboxProcessor.process();  // Processa batch
        
        // Then
        List<OutboxMessage> pending = outboxRepository.findPending(10);
        assertThat(pending).isEmpty();  // Todos processados
    }
}
```

---

## 🌐 Contract Tests

Testes de contratos com `commons-testkit-contracts`.

### Producer Contract Test

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UserApiContractTest {
    
    @LocalServerPort
    private int port;
    
    @Test
    void shouldReturnUserById() {
        // Given
        String userId = "user-123";
        
        // When
        Response response = RestAssured
            .given()
                .port(port)
                .pathParam("userId", userId)
            .when()
                .get("/api/users/{userId}")
            .then()
                .statusCode(200)
                .extract()
                .response();
        
        // Then - Valida schema
        assertThat(response.jsonPath().getString("id")).isEqualTo(userId);
        assertThat(response.jsonPath().getString("email")).isNotBlank();
        assertThat(response.jsonPath().getString("name")).isNotBlank();
    }
}
```

### Message Contract Test

```java
class OrderEventContractTest {
    
    @Test
    void orderCreatedEventShouldMatchContract() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            EventId.generate(),
            OrderId.generate(),
            CustomerId.generate(),
            Instant.now()
        );
        
        // When
        String json = serialize(event);
        JsonNode node = parseJson(json);
        
        // Then - Valida campos obrigatórios
        assertThat(node.has("eventId")).isTrue();
        assertThat(node.has("orderId")).isTrue();
        assertThat(node.has("customerId")).isTrue();
        assertThat(node.has("occurredAt")).isTrue();
        
        // Valida tipos
        assertThat(node.get("eventId").isTextual()).isTrue();
        assertThat(node.get("orderId").isTextual()).isTrue();
    }
}
```

---

## 🏛️ Architecture Tests

Valida regras arquiteturais com ArchUnit.

```java
@AnalyzeClasses(packages = "com.myapp")
class ArchitectureTest {
    
    @ArchTest
    static final ArchRule domainShouldNotDependOnInfra = 
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");
    
    @ArchTest
    static final ArchRule entitiesShouldExtendEntity =
        classes()
            .that().resideInAPackage("..domain..")
            .and().haveSimpleNameEndingWith("Entity")
            .should().beAssignableTo(Entity.class);
    
    @ArchTest
    static final ArchRule servicesShouldBeAnnotatedWithService =
        classes()
            .that().resideInAPackage("..application..")
            .and().haveSimpleNameEndingWith("Service")
            .should().beAnnotatedWith(Service.class);
}
```

---

## 🎭 Test Fixtures

```java
public class TestFixtures {
    
    public static User createUser() {
        return new User(
            UserId.generate(),
            TenantId.of("test-tenant"),
            Email.of("test@example.com"),
            "Test User",
            AuditStamp.now(ActorId.of("test"))
        );
    }
    
    public static Order createDraftOrder() {
        Order order = Order.create(
            OrderId.generate(),
            TenantId.of("test-tenant"),
            CustomerId.generate(),
            AuditStamp.now()
        );
        return order;
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use nomes descritivos
@Test
void shouldRejectOrderWhenCartIsEmpty() {}

// ✅ Organize com Given-When-Then
// Given (setup)
// When (ação)
// Then (verificação)

// ✅ Teste comportamentos, não implementação
assertThat(order.status()).isEqualTo(OrderStatus.SUBMITTED);
// ❌ assertThat(order.internalState).isEqualTo(...);

// ✅ Use Testcontainers para integrações reais
@Container
static PostgreSQLContainer<?> postgres = ...

// ✅ Isole testes - cada teste independente
@BeforeEach
void setUp() {
    // Setup isolado
}
```

### ❌ DON'T

```java
// ❌ NÃO teste implementação privada
// ❌ NÃO use Thread.sleep() nos testes
// ❌ NÃO ignore testes falhando
// ❌ NÃO tenha testes flaky (não determinísticos)
```

---

## Ver Também

- [commons-testkit-core](../../commons-testkit-core/README.md)
- [commons-testkit-contracts](../../commons-testkit-contracts/README.md)
- [commons-testkit-archunit](../../commons-testkit-archunit/README.md)
