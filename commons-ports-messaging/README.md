# commons-ports-messaging

Interfaces hexagonais de mensageria. Sem dependências de Kafka, RabbitMQ ou qualquer broker.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-ports-messaging</artifactId>
</dependency>
```

## MessagePublisherPort

```java
public interface MessagePublisherPort {
    <T> void publish(MessageEnvelope<T> message, MessageSerializer<T> serializer);
    <T> void publishBatch(List<MessageEnvelope<T>> messages, MessageSerializer<T> serializer);
}
```

### Publicar mensagem

```java
// Criar envelope
MessageEnvelope<OrderCreatedEvent> envelope = MessageEnvelope.<OrderCreatedEvent>builder()
    .id(MessageId.generate())
    .topic(Topic.of("orders.created"))
    .payload(event)
    .partitionKey(event.orderId().value())  // roteamento por chave
    .header("correlationId", correlationId)
    .build();

// Publicar
publisher.publish(envelope, new JacksonMessageSerializer<>(objectMapper, OrderCreatedEvent.class));
```

## MessageConsumerPort

```java
public interface MessageConsumerPort {
    <T> Subscription subscribe(Topic topic, ConsumerGroup group,
                               MessageSerializer<T> serializer,
                               MessageHandler<T> handler);
    void start();
    void stop();
}
```

### Consumir mensagens

```java
MessageConsumerPort consumer = ...; // Kafka, RabbitMQ, etc.

Subscription sub = consumer.subscribe(
    Topic.of("orders.created"),
    ConsumerGroup.of("order-processor"),
    new JacksonMessageSerializer<>(objectMapper, OrderCreatedEvent.class),
    (envelope, ack) -> {
        processOrder(envelope.payload());
        ack.acknowledge();
    }
);

consumer.start();
// ...
consumer.stop();
```

## JacksonMessageSerializer

```java
// Serializer genérico com Jackson
MessageSerializer<OrderCreatedEvent> serializer =
    new JacksonMessageSerializer<>(objectMapper, OrderCreatedEvent.class);

byte[] bytes = serializer.serialize(event);
OrderCreatedEvent deserialized = serializer.deserialize(bytes);
```

## Adapters disponíveis

| Adapter | Módulo |
|---------|--------|
| `KafkaPublisherAdapter` | `commons-adapters-messaging-kafka` |
| `KafkaConsumerAdapter` | `commons-adapters-messaging-kafka` |
| `RabbitMQPublisherAdapter` | `commons-adapters-messaging-rabbitmq` |
| `AzureServiceBusAdapter` | `commons-adapters-messaging-azure-servicebus` |
