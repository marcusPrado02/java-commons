# Adapters Index

Lista completa de adapters (implementações de infraestrutura) disponíveis na plataforma Commons.

---

## 🌐 Web & HTTP

### Web Frameworks

| Adapter | Descrição | Documentação |
|---------|-----------|--------------|
| **commons-adapters-web-spring** | Spring MVC adapter | [README](../../commons-adapters-web-spring/README.md) |
| **commons-adapters-web-spring-webflux** | Spring WebFlux reactive | [README](../../commons-adapters-web-spring-webflux/README.md) |
| **commons-adapters-web-jaxrs** | JAX-RS (Jersey) adapter | [README](../../commons-adapters-web-jaxrs/README.md) |

### HTTP Clients

| Adapter | Descrição | Stack |
|---------|-----------|-------|
| **commons-adapters-http-okhttp** | OkHttp client | OkHttp 4.x |
| **commons-adapters-http-webclient** | WebClient reactive | Spring WebFlux |

**Quando usar:**
- **Spring MVC**: Apps tradicionais blocking I/O
- **WebFlux**: Apps reativas, alta concorrência
- **JAX-RS**: Ambientes Java EE/Jakarta EE

---

## 💾 Persistência

### Databases

| Adapter | Descrição | Tecnologia |
|---------|-----------|------------|
| **commons-adapters-persistence-jpa** | JPA/Hibernate | JPA 3.x, Hibernate |
| **commons-adapters-persistence-mongodb** | MongoDB | MongoDB Driver 4.x |
| **commons-adapters-persistence-inmemory** | In-memory (testing) | ConcurrentHashMap |

**Quando usar:**
- **JPA**: Bancos relacionais (PostgreSQL, MySQL, Oracle)
- **MongoDB**: Dados semi-estruturados, alta escala
- **InMemory**: Testes unitários, protótipos

### Cache

| Adapter | Descrição | Tecnologia |
|---------|-----------|------------|
| **commons-adapters-cache-redis** | Redis cache | Jedis/Lettuce |
| **commons-adapters-cache-memcached** | Memcached | Spymemcached |

**Quando usar:**
- **Redis**: Cache + pub/sub + estruturas de dados
- **Memcached**: Cache simples, alta performance

---

## 📨 Messaging & Events

| Adapter | Descrição | Protocolo |
|---------|-----------|-----------|
| **commons-adapters-messaging-kafka** | Apache Kafka | Kafka 3.x |
| **commons-adapters-messaging-rabbitmq** | RabbitMQ | AMQP |
| **commons-adapters-messaging-azure-servicebus** | Azure Service Bus | AMQP |

**Quando usar:**
- **Kafka**: Event streaming, alto throughput, event sourcing
- **RabbitMQ**: Message routing complexo, transações
- **Azure Service Bus**: Azure-native, managed service

### Queues

| Adapter | Descrição | Cloud |
|---------|-----------|-------|
| **commons-adapters-queue-sqs** | AWS SQS | AWS |
| **commons-adapters-queue-azure-storage-queue** | Azure Storage Queue | Azure |

---

## 🔐 Secrets Management

| Adapter | Descrição | Provider |
|---------|-----------|----------|
| **commons-adapters-secrets-vault** | HashiCorp Vault | On-prem/Cloud |
| **commons-adapters-secrets-azure-keyvault** | Azure Key Vault | Azure |
| **commons-adapters-secrets-aws-secretsmanager** | AWS Secrets Manager | AWS |

**Quando usar:**
- **Vault**: Multi-cloud, on-premises
- **Azure Key Vault**: Azure-native
- **AWS Secrets Manager**: AWS-native

---

## 📁 File Storage

| Adapter | Descrição | Cloud |
|---------|-----------|-------|
| **commons-adapters-files-s3** | AWS S3 | AWS |
| **commons-adapters-files-azure-blob** | Azure Blob Storage | Azure |
| **commons-adapters-files-gcs** | Google Cloud Storage | GCP |

**Quando usar:**
- **S3**: AWS ecosystem, S3-compatible storage
- **Azure Blob**: Azure ecosystem
- **GCS**: Google Cloud ecosystem

---

## 🔍 Search

| Adapter | Descrição | Engine |
|---------|-----------|--------|
| **commons-adapters-search-elasticsearch** | Elasticsearch | Elasticsearch 8.x |
| **commons-adapters-search-opensearch** | OpenSearch | OpenSearch 2.x |

**Quando usar:**
- **Elasticsearch**: Elastic Stack, enterprise features
- **OpenSearch**: Open source, AWS managed

---

## 📧 Communication

### Email

| Adapter | Descrição | Provider |
|---------|-----------|----------|
| **commons-adapters-email-smtp** | SMTP direto | Any SMTP server |
| **commons-adapters-email-sendgrid** | SendGrid API | SendGrid |

### SMS

| Adapter | Descrição | Provider |
|---------|-----------|----------|
| **commons-adapters-sms-twilio** | Twilio SMS | Twilio |
| **commons-adapters-sms-aws-sns** | AWS SNS SMS | AWS |

### Push Notifications

| Adapter | Descrição | Platform |
|---------|-----------|----------|
| **commons-adapters-notification-fcm** | Firebase Cloud Messaging | Android/iOS |
| **commons-adapters-notification-apns** | Apple Push Notification | iOS |

---

## 📊 Observability

| Adapter | Descrição | Stack |
|---------|-----------|-------|
| **commons-adapters-otel** | OpenTelemetry | OTEL |
| **commons-adapters-tracing-jaeger** | Jaeger tracing | Jaeger |
| **commons-adapters-metrics-prometheus** | Prometheus metrics | Prometheus |

**Stack recomendado:**
- Logs: Structured JSON → ELK/Loki
- Metrics: Prometheus → Grafana
- Tracing: OpenTelemetry → Jaeger/Tempo

---

## 🛡️ Resilience

| Adapter | Descrição | Library |
|---------|-----------|---------|
| **commons-adapters-resilience4j** | Resilience4j | Resilience4j 2.x |

Implementa:
- Circuit Breaker
- Retry
- Bulkhead  
- Rate Limiter
- Timeout
- Cache

---

## 🔧 Utilities

### Templates

| Adapter | Descrição | Engine |
|---------|-----------|--------|
| **commons-adapters-template-thymeleaf** | Thymeleaf templates | Thymeleaf 3.x |

### Documents

| Adapter | Descrição | Format |
|---------|-----------|--------|
| **commons-adapters-pdf-itext** | PDF generation | iText 7.x |
| **commons-adapters-excel-poi** | Excel generation | Apache POI |

### Serialization

| Adapter | Descrição | Format |
|---------|-----------|--------|
| **commons-adapters-serialization-protobuf** | Protocol Buffers | Protobuf 3.x |

### Compression

| Adapter | Descrição | Algorithm |
|---------|-----------|-----------|
| **commons-adapters-compression** | Gzip, Deflate, etc. | Multiple |

---

## 🌍 Service Discovery

| Adapter | Descrição | Registry |
|---------|-----------|----------|
| **commons-adapters-service-discovery-consul** | Consul | Consul |
| **commons-adapters-service-discovery-eureka** | Netflix Eureka | Eureka |

**Quando usar:**
- **Consul**: Multi-datacenter, service mesh ready
- **Eureka**: Spring Cloud ecosystem

---

## 🚀 API Protocols

| Adapter | Descrição | Protocol |
|---------|-----------|----------|
| **commons-adapters-grpc-server** | gRPC server | gRPC |
| **commons-adapters-grpc-client** | gRPC client | gRPC |
| **commons-adapters-graphql-server** | GraphQL server | GraphQL |

---

## 🔗 Blockchain

| Adapter | Descrição | Chain |
|---------|-----------|-------|
| **commons-adapters-blockchain-web3j** | Ethereum | Web3j |

---

## 💳 Payment

| Adapter | Descrição | Provider |
|---------|-----------|----------|
| **commons-adapters-payment-stripe** | Stripe API | Stripe |

---

## 🤖 Machine Learning

| Adapter | Descrição | Framework |
|---------|-----------|-----------|
| **commons-adapters-ml-tensorflow** | TensorFlow | TensorFlow Java |

---

## 🔏 Biometric

| Adapter | Descrição | Standard |
|---------|-----------|----------|
| **commons-adapters-biometric-webauthn** | WebAuthn/FIDO2 | WebAuthn |

---

## Como Escolher um Adapter

### 1. Identifique a capability necessária

```java
// Preciso persistir entidades
→ commons-ports-persistence
  → JPA / MongoDB / InMemory?

// Preciso enviar mensagens
→ commons-ports-messaging
  → Kafka / RabbitMQ / Azure Service Bus?
```

### 2. Adicione a dependência

```xml
<!-- Port (interface) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-messaging</artifactId>
</dependency>

<!-- Adapter (implementação) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-messaging-kafka</artifactId>
</dependency>
```

### 3. Configure e injete

```java
@Configuration
public class MessagingConfig {
    
    @Bean
    public MessagePublisher messagePublisher() {
        KafkaConfiguration config = KafkaConfiguration.builder()
            .bootstrapServers("localhost:9092")
            .build();
        
        return new KafkaMessagePublisher(config);
    }
}
```

### 4. Troque facilmente

```java
// Trocar de Kafka para RabbitMQ:
// 1. Mude a dependência no pom.xml
// 2. Mude a configuração do @Bean
// 3. Código de negócio NÃO muda!

@Service
public class OrderService {
    private final MessagePublisher publisher;  // Interface, não implementação
    
    public void createOrder(Order order) {
        publisher.publish(...);  // Funciona com qualquer adapter!
    }
}
```

---

## Ver Também

- [Ports Documentation](../api-reference/ports/README.md)
- [Hexagonal Architecture](../adr/0001-hexagonal-architecture.md)
- [Dependency Rules](../01-dependency-rules.md)
