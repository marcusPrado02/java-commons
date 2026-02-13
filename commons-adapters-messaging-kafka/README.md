# commons-adapters-messaging-kafka

Adapter Kafka para as portas de mensageria da plataforma Commons.

## Visão Geral

Este módulo fornece implementações Kafka para `MessagePublisherPort` e `MessageConsumerPort`, permitindo publicação e consumo de mensagens via Apache Kafka.

## Componentes

### `KafkaPublisherAdapter`

Implementação de `MessagePublisherPort` usando `KafkaProducer`:

- **Serialização**: Suporta qualquer `MessageSerializer<T>` (ex: `JacksonMessageSerializer`)
- **Headers**: Propagação automática de headers (correlationId, causationId, messageId, timestamp)
- **Partition Key**: Suporte para roteamento por chave de partição
- **Idempotência**: Habilitado por padrão (`enable.idempotence=true`)
- **Transações**: Suporte opcional via `transactionalId`
- **Retries**: Configurado com 3 retries automáticas
- **Ordenamento**: `max.in.flight.requests.per.connection=1` garante ordem

### `KafkaConsumerAdapter`

Implementação de `MessageConsumerPort` usando `KafkaConsumer`:

- **Múltiplas Subscri��ões**: Permite subscrever múltiplos tópicos/grupos
- **Deserialização**: Suporta qualquer `MessageSerializer<T>`
- **Headers**: Extração automática de headers Kafka → `MessageHeaders`
- **Lifecycle**: Métodos `start()` e `stop()` para controle do ciclo de vida
- **Thread Pool**: Cada subscrição roda em uma thread separada
- **Auto Commit**: Habilitado por padrão (`enable.auto.commit=true`)
- **Offset Reset**: Configurado para `earliest` por padrão

### `JacksonMessageSerializer`

Implementação de `MessageSerializer<T>` usando Jackson:

- **JSON**: Serialização/deserialização JSON
- **JavaTimeModule**: Suporte para tipos `java.time.*` (Instant, LocalDate, etc.)
- **Customizável**: Aceita `ObjectMapper` customizado via construtor

## Uso

### Configuração e Publicação

```java
// Publisher
var publisher = KafkaPublisherAdapter.builder()
    .bootstrapServers("localhost:9092")
    .build();

var serializer = new JacksonMessageSerializer<MyEvent>();

var envelope = MessageEnvelope.<MyEvent>builder()
    .topic(TopicName.of("my-topic"))
    .payload(new MyEvent("data"))
    .headers(MessageHeaders.builder()
        .correlationId("123")
        .causationId("456")
        .build())
    .partitionKey("user-123")
    .build();

publisher.publish(envelope, serializer);

publisher.close();
```

### Consumo

```java
// Consumer
var consumer = KafkaConsumerAdapter.builder()
    .bootstrapServers("localhost:9092")
    .pollTimeout(Duration.ofMillis(100))
    .build();

var serializer = new JacksonMessageSerializer<MyEvent>();

consumer.subscribe(
    TopicName.of("my-topic"),
    ConsumerGroup.of("my-group"),
    MyEvent.class,
    serializer,
    envelope -> {
        MyEvent event = envelope.payload();
        System.out.println("Received: " + event);
    }
);

consumer.start();

// Quando terminar
consumer.stop();
```

### Transações

```java
var publisher = KafkaPublisherAdapter.builder()
    .bootstrapServers("localhost:9092")
    .transactionalId("my-tx-id")
    .build();

publisher.beginTransaction();
try {
    publisher.publish(envelope1, serializer);
    publisher.publish(envelope2, serializer);
    publisher.commitTransaction();
} catch (Exception ex) {
    publisher.abortTransaction();
    throw ex;
}
```

## Dependências

- `commons-ports-messaging`: Portas de mensageria
- `kafka-clients`: Cliente Kafka oficial
- `jackson-databind` + `jackson-datatype-jsr310`: Serialização JSON

## Testes

Testes de integração usando Testcontainers:

```bash
./mvnw test -pl commons-adapters-messaging-kafka
```

Os testes iniciam um container Kafka real e validam:
- Publicação e consumo de mensagens
- Propagação de headers (correlationId, causationId)
- Suporte a partition keys
- Múltiplas mensagens

## Integração com Outbox Pattern

Este adapter pode ser usado em conjunto com `commons-app-outbox` para garantir entrega transacional de eventos:

1. Salve eventos no outbox dentro da transação do banco
2. Use um publisher transacional Kafka para publicar do outbox
3. Commit da transação Kafka após sucesso

## Configurações Avançadas

### Producer Properties

```java
var publisher = KafkaPublisherAdapter.builder()
    .bootstrapServers("localhost:9092")
    .property(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4")
    .property(ProducerConfig.BATCH_SIZE_CONFIG, 32768)
    .property(ProducerConfig.LINGER_MS_CONFIG, 10)
    .build();
```

### Consumer Properties

```java
var consumer = KafkaConsumerAdapter.builder()
    .bootstrapServers("localhost:9092")
    .property(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500)
    .property(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000)
    .build();
```

## Observações

- O adapter **não** implementa retry/circuit breaker internamente
- Para resiliência adicional, combine com `commons-app-resilience`
- Headers grandes podem impactar performance (Kafka tem limite de 1MB por mensagem)
- Transações Kafka requerem `min.insync.replicas >= 2` para durabilidade
