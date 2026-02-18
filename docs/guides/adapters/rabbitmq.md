# RabbitMQ Adapter Guide

## Overview

This guide covers the **RabbitMQ adapter** (`commons-adapters-messaging-rabbitmq`) for AMQP-based messaging.

**Key Features:**
- Exchanges (direct, topic, fanout, headers)
- Queues and bindings
- Message routing
- Dead Letter Exchanges (DLX)
- Message TTL and priority
- Publisher confirms
- Consumer acknowledgments

---

## 📦 Installation

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-messaging-rabbitmq</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Spring Boot starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

---

## ⚙️ Configuration

### Application Properties

```yaml
# application.yml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /
    
    # Connection settings
    connection-timeout: 30000
    requested-heartbeat: 30
    
    # Publisher confirms
    publisher-confirm-type: correlated
    publisher-returns: true
    
    # Template settings
    template:
      mandatory: true
      retry:
        enabled: true
        initial-interval: 1000
        max-attempts: 3
        multiplier: 2.0
        max-interval: 10000
    
    # Listener settings
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
        default-requeue-rejected: false
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          multiplier: 2.0
```

---

## 🔧 RabbitMQ Configuration

### Exchange and Queue Setup

```java
@Configuration
public class RabbitMQConfig {
    
    // Direct Exchange
    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange("order.exchange")
            .durable(true)
            .build();
    }
    
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable("order.queue")
            .withArgument("x-message-ttl", 3600000)  // 1 hour TTL
            .withArgument("x-dead-letter-exchange", "order.dlx")
            .withArgument("x-dead-letter-routing-key", "order.failed")
            .build();
    }
    
    @Bean
    public Binding orderBinding() {
        return BindingBuilder
            .bind(orderQueue())
            .to(orderExchange())
            .with("order.created");
    }
    
    // Topic Exchange
    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange("notification.exchange")
            .durable(true)
            .build();
    }
    
    @Bean
    public Queue emailQueue() {
        return new Queue("notification.email", true);
    }
    
    @Bean
    public Queue smsQueue() {
        return new Queue("notification.sms", true);
    }
    
    @Bean
    public Binding emailBinding() {
        return BindingBuilder
            .bind(emailQueue())
            .to(notificationExchange())
            .with("notification.email.*");
    }
    
    @Bean
    public Binding smsBinding() {
        return BindingBuilder
            .bind(smsQueue())
            .to(notificationExchange())
            .with("notification.sms.*");
    }
    
    // Fanout Exchange
    @Bean
    public FanoutExchange auditExchange() {
        return ExchangeBuilder.fanoutExchange("audit.exchange")
            .durable(true)
            .build();
    }
    
    @Bean
    public Queue auditLogQueue() {
        return new Queue("audit.log", true);
    }
    
    @Bean
    public Queue auditArchiveQueue() {
        return new Queue("audit.archive", true);
    }
    
    @Bean
    public Binding auditLogBinding() {
        return BindingBuilder
            .bind(auditLogQueue())
            .to(auditExchange());
    }
    
    @Bean
    public Binding auditArchiveBinding() {
        return BindingBuilder
            .bind(auditArchiveQueue())
            .to(auditExchange());
    }
    
    // Dead Letter Exchange
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("order.dlx");
    }
    
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("order.dlq")
            .withArgument("x-message-ttl", 86400000)  // 24 hours
            .build();
    }
    
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
            .bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with("order.failed");
    }
    
    // Message converter
    @Bean
    public MessageConverter jacksonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter());
        template.setMandatory(true);
        
        // Return callback (message not routed)
        template.setReturnsCallback(returned -> {
            log.error("Message returned")
                .field("message", returned.getMessage())
                .field("replyCode", returned.getReplyCode())
                .field("replyText", returned.getReplyText())
                .field("exchange", returned.getExchange())
                .field("routingKey", returned.getRoutingKey())
                .log();
        });
        
        return template;
    }
}
```

---

## 📤 Producer (Publisher)

### Direct Exchange Publisher

```java
@Component
public class OrderEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE = "order.exchange";
    
    public Result<Void> publishOrderCreated(OrderCreatedEvent event) {
        try {
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setHeader("eventType", "ORDER_CREATED");
            props.setHeader("eventVersion", "1.0");
            props.setHeader("timestamp", System.currentTimeMillis());
            props.setPriority(5);
            
            String json = objectMapper.writeValueAsString(event);
            Message message = new Message(json.getBytes(), props);
            
            rabbitTemplate.send(
                EXCHANGE,
                "order.created",
                message
            );
            
            log.info("Order event published")
                .field("orderId", event.orderId())
                .field("exchange", EXCHANGE)
                .field("routingKey", "order.created")
                .log();
            
            return Result.ok();
            
        } catch (Exception e) {
            log.error("Failed to publish order event")
                .exception(e)
                .log();
            
            return Result.error(Error.of("PUBLISH_ERROR", e.getMessage()));
        }
    }
    
    // Simplified with convertAndSend
    public void publishOrderShipped(OrderShippedEvent event) {
        rabbitTemplate.convertAndSend(
            EXCHANGE,
            "order.shipped",
            event,
            message -> {
                message.getMessageProperties().setHeader("eventType", "ORDER_SHIPPED");
                return message;
            }
        );
    }
}
```

### Topic Exchange Publisher

```java
@Component
public class NotificationPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE = "notification.exchange";
    
    public void sendEmailNotification(EmailNotification notification) {
        String routingKey = "notification.email." + notification.priority();
        
        rabbitTemplate.convertAndSend(
            EXCHANGE,
            routingKey,
            notification
        );
        
        log.info("Email notification sent")
            .field("recipient", notification.recipient())
            .field("routingKey", routingKey)
            .log();
    }
    
    public void sendSmsNotification(SmsNotification notification) {
        String routingKey = "notification.sms." + notification.priority();
        
        rabbitTemplate.convertAndSend(
            EXCHANGE,
            routingKey,
            notification
        );
    }
}
```

### Fanout Exchange Publisher

```java
@Component
public class AuditEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE = "audit.exchange";
    
    public void publishAuditEvent(AuditEvent event) {
        // Fanout broadcasts to all bound queues
        rabbitTemplate.convertAndSend(EXCHANGE, "", event);
        
        log.info("Audit event broadcasted")
            .field("eventType", event.type())
            .field("userId", event.userId())
            .log();
    }
}
```

---

## 📥 Consumer (Listener)

### Direct Exchange Consumer

```java
@Component
public class OrderEventConsumer {
    
    private final OrderService orderService;
    
    @RabbitListener(queues = "order.queue")
    public void handleOrderCreated(
        @Payload OrderCreatedEvent event,
        @Header("eventType") String eventType,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        
        log.info("Processing order created event")
            .field("orderId", event.orderId())
            .field("eventType", eventType)
            .log();
        
        try {
            // Process event
            Result<Void> result = orderService.processOrderCreated(event);
            
            if (result.isSuccess()) {
                // Acknowledge message
                channel.basicAck(deliveryTag, false);
                
                log.info("Order processed successfully")
                    .field("orderId", event.orderId())
                    .log();
            } else {
                // Reject and send to DLX
                channel.basicReject(deliveryTag, false);
                
                log.error("Order processing failed")
                    .field("orderId", event.orderId())
                    .field("error", result.error().message())
                    .log();
            }
            
        } catch (Exception e) {
            log.error("Exception processing order")
                .exception(e)
                .field("orderId", event.orderId())
                .log();
            
            // Reject and send to DLX
            channel.basicReject(deliveryTag, false);
        }
    }
}
```

### Topic Exchange Consumer

```java
@Component
public class NotificationConsumer {
    
    @RabbitListener(queues = "notification.email")
    public void handleEmailNotification(
        EmailNotification notification,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        
        try {
            emailService.send(notification);
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("Failed to send email")
                .exception(e)
                .log();
            
            channel.basicReject(deliveryTag, false);
        }
    }
    
    @RabbitListener(queues = "notification.sms")
    public void handleSmsNotification(
        SmsNotification notification,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        
        try {
            smsService.send(notification);
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("Failed to send SMS")
                .exception(e)
                .log();
            
            channel.basicReject(deliveryTag, false);
        }
    }
}
```

### Dead Letter Queue Consumer

```java
@Component
public class DeadLetterConsumer {
    
    @RabbitListener(queues = "order.dlq")
    public void handleFailedMessage(
        Message message,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        
        String body = new String(message.getBody());
        MessageProperties props = message.getMessageProperties();
        
        log.error("Processing dead letter message")
            .field("body", body)
            .field("headers", props.getHeaders())
            .field("x-death", props.getHeader("x-death"))
            .log();
        
        try {
            // Store failed message for manual review
            failedMessageRepository.save(new FailedMessage(
                body,
                props.getHeaders(),
                LocalDateTime.now()
            ));
            
            // Alert ops team
            alertService.sendAlert(
                "RabbitMQ DLQ Message",
                "Failed message: " + body
            );
            
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("Failed to process DLQ message")
                .exception(e)
                .log();
            
            // Don't requeue from DLQ
            channel.basicReject(deliveryTag, false);
        }
    }
}
```

---

## 🎯 Routing Patterns

### Direct Routing

```java
// Publisher
rabbitTemplate.convertAndSend("order.exchange", "order.created", event);

// Consumer
@RabbitListener(queues = "order.queue")
public void handle(OrderCreatedEvent event) {
    // Only receives messages with routing key "order.created"
}
```

### Topic Routing

```java
// Publisher
rabbitTemplate.convertAndSend("notification.exchange", "notification.email.high", event);
rabbitTemplate.convertAndSend("notification.exchange", "notification.sms.low", event);

// Consumers
@RabbitListener(queues = "high-priority-queue")
@RabbitListener(bindings = @QueueBinding(
    value = @Queue("high-priority-queue"),
    exchange = @Exchange(value = "notification.exchange", type = "topic"),
    key = "notification.*.high"
))
public void handleHighPriority(Notification notification) {
    // Receives notification.email.high and notification.sms.high
}

@RabbitListener(queues = "email-queue")
@RabbitListener(bindings = @QueueBinding(
    value = @Queue("email-queue"),
    exchange = @Exchange(value = "notification.exchange", type = "topic"),
    key = "notification.email.*"
))
public void handleEmail(Notification notification) {
    // Receives all notification.email.* messages
}
```

### Headers Routing

```java
// Configuration
@Bean
public HeadersExchange headersExchange() {
    return new HeadersExchange("headers.exchange");
}

@Bean
public Binding headersBinding() {
    Map<String, Object> headers = new HashMap<>();
    headers.put("format", "pdf");
    headers.put("type", "report");
    
    return BindingBuilder
        .bind(pdfReportQueue())
        .to(headersExchange())
        .whereAll(headers).match();  // All headers must match
}

// Publisher
rabbitTemplate.convertAndSend(
    "headers.exchange",
    "",
    report,
    message -> {
        message.getMessageProperties().setHeader("format", "pdf");
        message.getMessageProperties().setHeader("type", "report");
        return message;
    }
);
```

---

## 🚀 Performance Optimization

### Prefetch Configuration

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 10  # Fetch 10 messages at a time
        concurrency: 5  # 5 concurrent consumers
        max-concurrency: 10
```

### Batch Publishing

```java
@Component
public class BatchOrderPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishBatch(List<OrderCreatedEvent> events) {
        rabbitTemplate.execute(channel -> {
            for (OrderCreatedEvent event : events) {
                String json = objectMapper.writeValueAsString(event);
                
                AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2)  // Persistent
                    .build();
                
                channel.basicPublish(
                    "order.exchange",
                    "order.created",
                    props,
                    json.getBytes()
                );
            }
            
            // Wait for confirms
            channel.waitForConfirmsOrDie(5000);
            
            return null;
        });
    }
}
```

---

## 🧪 Testing

### Embedded RabbitMQ Test

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672"
})
@Testcontainers
class OrderEventPublisherTest {
    
    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.11-management")
        .withQueue("order.queue")
        .withExchange("order.exchange", "direct")
        .withBinding("order.exchange", "order.queue", Map.of(), "order.created", Map.of());
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
    }
    
    @Autowired
    private OrderEventPublisher publisher;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Test
    void shouldPublishOrderCreatedEvent() throws Exception {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            "order-123",
            "customer-456",
            100.0
        );
        
        // When
        Result<Void> result = publisher.publishOrderCreated(event);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        
        // Verify message in queue
        Message message = rabbitTemplate.receive("order.queue", 5000);
        assertThat(message).isNotNull();
        
        String body = new String(message.getBody());
        assertThat(body).contains("order-123");
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use manual acknowledgment
@RabbitListener(queues = "order.queue", ackMode = "MANUAL")

// ✅ Set message TTL
.withArgument("x-message-ttl", 3600000)

// ✅ Configure Dead Letter Exchange
.withArgument("x-dead-letter-exchange", "order.dlx")

// ✅ Use publisher confirms
spring.rabbitmq.publisher-confirm-type: correlated

// ✅ Set prefetch count
spring.rabbitmq.listener.simple.prefetch: 10
```

### ❌ DON'T

```java
// ❌ NÃO use auto-ack em produção
@RabbitListener(queues = "order.queue")  // ❌ Auto-ack!

// ❌ NÃO ignore DLQ messages
// Always monitor and process DLQ

// ❌ NÃO block consumer threads
Thread.sleep(10000);  // ❌ Blocks consumer!

// ❌ NÃO requeue infinitely
channel.basicReject(deliveryTag, true);  // ❌ Infinite loop!

// ❌ NÃO send large messages
// Use claim check pattern for large payloads
```

---

## Ver Também

- [Messaging Port](../api-reference/ports/messaging.md) - Port interface
- [Domain Events](../guides/domain-events.md) - Event patterns
- [Kafka Adapter](./kafka.md) - Alternative messaging
