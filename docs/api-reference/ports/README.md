# Ports Reference Index

## Visão Geral

Os **Ports** são interfaces que definem contratos de comunicação com infraestrutura externa, seguindo a Arquitetura Hexagonal. Adapters implementam estas interfaces, permitindo trocar implementações sem afetar o domínio.

---

## 🔌 Available Ports

### Persistence

| Port | Descrição | Adapters |
|------|-----------|----------|
| **Repository<T, ID>** | CRUD genérico | JPA, MongoDB, InMemory |
| **SpecificationRepository<T>** | Queries com specifications | JPA |
| **EventStore** | Event Sourcing store | PostgreSQL, MongoDB |

📖 **[Persistence Port Documentation](persistence.md)**

---

### Messaging

| Port | Descrição | Adapters |
|------|-----------|----------|
| **MessagePublisher** | Publica mensagens | Kafka, RabbitMQ, Azure Service Bus |
| **MessageConsumer** | Consome mensagens | Kafka, RabbitMQ, Azure Service Bus |
| **TopicSubscriber** | Pub/Sub pattern | Kafka, RabbitMQ |

📖 **[Messaging Port Documentation](messaging.md)**

---

### HTTP Client

| Port | Descrição | Adapters |
|------|-----------|----------|
| **HttpClient** | Cliente HTTP | OkHttp, WebClient |
| **RestClient** | Cliente REST | OkHttp, WebClient |

📖 **[HTTP Port Documentation](http.md)**

---

### Cache

| Port | Descrição | Adapters |
|------|-----------|----------|
| **CacheProvider** | Cache genérico | Redis, Memcached, Caffeine |
| **DistributedCache** | Cache distribuído | Redis, Memcached |

📖 **[Cache Port Documentation](cache.md)**

---

### Secrets Management

| Port | Descrição | Adapters |
|------|-----------|----------|
| **SecretsProvider** | Provider de secrets | Vault, Azure Key Vault, AWS Secrets Manager |
| **EncryptionService** | Criptografia | AES, RSA |

📖 **[Secrets Port Documentation](secrets.md)**

---

### File Storage

| Port | Descrição | Adapters |
|------|-----------|----------|
| **FileStorage** | Storage de arquivos | S3, Azure Blob, GCS |
| **FileRepository** | Repository de files | S3, Azure Blob, GCS |

📖 **[Files Port Documentation](files.md)**

---

### Communication

| Port | Descrição | Adapters |
|------|-----------|----------|
| **EmailSender** | Envio de email | SMTP, SendGrid |
| **SmsSender** | Envio de SMS | Twilio, AWS SNS |
| **PushNotificationSender** | Push notifications | FCM, APNS |

📖 **[Communication Ports Documentation](communication.md)**

---

### Search

| Port | Descrição | Adapters |
|------|-----------|----------|
| **SearchEngine** | Full-text search | Elasticsearch, OpenSearch |
| **SearchRepository<T>** | Repository com search | Elasticsearch, OpenSearch |

📖 **[Search Port Documentation](search.md)**

---

### Queue

| Port | Descrição | Adapters |
|------|-----------|----------|
| **QueuePublisher** | Publica em fila | SQS, Azure Storage Queue |
| **QueueConsumer** | Consome de fila | SQS, Azure Storage Queue |

📖 **[Queue Port Documentation](queue.md)**

---

### Templates

| Port | Descrição | Adapters |
|------|-----------|----------|
| **TemplateEngine** | Template rendering | Thymeleaf, Freemarker |

📖 **[Templates Port Documentation](templates.md)**

---

### Documents

| Port | Descrição | Adapters |
|------|-----------|----------|
| **PdfGenerator** | Geração de PDF | iText |
| **ExcelGenerator** | Geração de Excel | Apache POI |

📖 **[Documents Port Documentation](documents.md)**

---

## 🎯 Port Design Principles

### 1. Framework-Agnostic

```java
// ✅ Port não depende de framework
public interface MessagePublisher {
    void publish(Message message);
}

// ❌ NÃO dependa de frameworks
public interface MessagePublisher {
    void publish(org.springframework.messaging.Message message);  // ❌
}
```

### 2. Abstração Clara

```java
// ✅ Abstração de alto nível
public interface FileStorage {
    Result<FileMetadata> upload(InputStream content, String path);
    Result<InputStream> download(String path);
    Result<Void> delete(String path);
}

// ❌ NÃO exponha detalhes de implementação
public interface S3Storage {  // ❌ Nome específico
    PutObjectResult putObject(...);  // ❌ Tipo AWS-specific
}
```

### 3. Domínio-Orientado

```java
// ✅ Use tipos do domínio
public interface OrderRepository extends Repository<Order, OrderId> {
    Optional<Order> findByNumber(OrderNumber number);
}

// ❌ NÃO use tipos genéricos demais
public interface OrderRepository extends CrudRepository<Order, String> {  // ❌
    Optional<Order> findByNumber(String number);  // ❌
}
```

---

## 🔄 Como Usar Ports

### 1. Declare Dependência na Port

```xml
<!-- Apenas a interface -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-messaging</artifactId>
</dependency>
```

### 2. Injete a Port no Serviço

```java
@Service
public class OrderService {
    
    private final MessagePublisher publisher;  // ✅ Depende da interface
    
    public OrderService(MessagePublisher publisher) {
        this.publisher = publisher;
    }
    
    public void createOrder(Order order) {
        // Usa a abstração
        publisher.publish(new OrderCreatedMessage(order));
    }
}
```

### 3. Configure o Adapter

```xml
<!-- Escolha a implementação -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-messaging-kafka</artifactId>
</dependency>
```

```java
@Configuration
public class MessagingConfig {
    
    @Bean
    public MessagePublisher messagePublisher() {
        return new KafkaMessagePublisher(kafkaTemplate);
    }
}
```

### 4. Troque Facilmente

```java
// Produção: Kafka
@Profile("prod")
@Bean
public MessagePublisher kafkaPublisher() {
    return new KafkaMessagePublisher(...);
}

// Desenvolvimento: In-Memory
@Profile("dev")
@Bean
public MessagePublisher inMemoryPublisher() {
    return new InMemoryMessagePublisher();
}

// Código de negócio NÃO muda!
```

---

## 🧪 Testing com Ports

### Mock Simples

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private MessagePublisher publisher;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void shouldPublishMessageWhenOrderCreated() {
        // Given
        Order order = createOrder();
        
        // When
        orderService.createOrder(order);
        
        // Then
        verify(publisher).publish(argThat(msg -> 
            msg.topic().equals("orders.created")
        ));
    }
}
```

### In-Memory Adapter

```java
@SpringBootTest
class OrderServiceIntegrationTest {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private InMemoryMessagePublisher publisher;  // In-memory para testes
    
    @Test
    void shouldPublishMessage() {
        // When
        orderService.createOrder(order);
        
        // Then
        List<Message> published = publisher.getPublishedMessages();
        assertThat(published).hasSize(1);
    }
}
```

---

## Ver Também

- [Hexagonal Architecture](../../adr/0001-hexagonal-architecture.md)
- [Adapters Index](../adapters/README.md)
- [Dependency Rules](../../01-dependency-rules.md)
