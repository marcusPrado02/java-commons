# Port: Messaging

## Visão Geral

Define contratos para publicação e consumo de mensagens assícronas.

---

## MessagePublisher

Interface para publicar mensagens.

```java
public interface MessagePublisher {
    
    /**
     * Publica mensagem
     */
    void publish(Message message);
    
    /**
     * Publica mensagem com tipo
     */
    <T> void publish(String topic, String key, T payload);
}
```

### Message

```java
public class Message {
    private String topic;
    private String key;
    private String payload;
    private Map<String, String> headers;
    private Instant timestamp;
    
    public static MessageBuilder builder() {
        return new MessageBuilder();
    }
}
```

### Exemplo

```java
@Service
public class OrderService {
    
    private final MessagePublisher publisher;
    
    public void createOrder(Order order) {
        orderRepository.save(order);
        
        // Publica evento
        publisher.publish(Message.builder()
            .topic("orders.created")
            .key(order.id().value())
            .payload(serializeEvent(order))
            .header("event-type", "OrderCreated")
            .header("tenant-id", order.tenantId().value())
            .build());
    }
}
```

---

## MessageConsumer

Interface para consumir mensagens.

```java
public interface MessageConsumer {
    
    /**
     * Registra handler para tópico
     */
    void subscribe(String topic, MessageHandler handler);
    
    /**
     * Para de consumir
     */
    void unsubscribe(String topic);
}

@FunctionalInterface
public interface MessageHandler {
    void handle(Message message);
}
```

### Exemplo

```java
@Component
public class OrderEventConsumer {
    
    private final MessageConsumer consumer;
    
    @PostConstruct
    public void init() {
        consumer.subscribe("orders.created", this::handleOrderCreated);
    }
    
    private void handleOrderCreated(Message message) {
        OrderCreatedEvent event = deserialize(message.payload());
        // Processa evento
    }
}
```

---

## Adapters Disponíveis

| Adapter | Protocolo | Características |
|---------|-----------|-----------------|
| **Kafka** | Kafka | High throughput, streaming |
| **RabbitMQ** | AMQP | Routing complexo, transações |
| **Azure Service Bus** | AMQP | Managed, Azure-native |

---

## Ver Também

- [Kafka Adapter](../../adapters/messaging-kafka.md)
- [RabbitMQ Adapter](../../adapters/messaging-rabbitmq.md)
- [Domain Events Guide](../../../guides/domain-events.md)
