# commons-adapters-messaging-rabbitmq

Adapter RabbitMQ para as portas de mensageria da plataforma Commons.

## Visão Geral

Este módulo fornece implementações RabbitMQ para `MessagePublisherPort` e `MessageConsumerPort`, permitindo publicação e consumo de mensagens via RabbitMQ AMQP.

## Componentes

### `RabbitMQPublisherAdapter`

Implementação de `MessagePublisherPort` usando `amqp-client`:

- **Serialização**: Suporta qualquer `MessageSerializer<T>` (ex: `JacksonMessageSerializer`)
- **Headers**: Propagação automática de headers AMQP (correlationId, causationId, messageId, timestamp)
- **Routing Key**: Utiliza `TopicName` como routing key (queue name)
- **Persistência**: Mensagens persistentes por padrão (`deliveryMode=2`)
- **Confirmação**: Publisher confirms habilitado por padrão para garantir entrega
- **Reconnection**: Automatic recovery habilitado (reconecta após falhas)
- **Content Type**: Mensagens enviadas como `application/json` com `UTF-8`

### `RabbitMQConsumerAdapter`

Implementação de `MessageConsumerPort` usando `amqp-client`:

 **Múltiplas Subscrições**: Permite subscrever múltiplos tópicos/grupos
- **Deserialização**: Suporta qualquer `MessageSerializer<T>`
- **Headers**: Extração automática de headers AMQP → `MessageHeaders`
- **Lifecycle**: Métodos `start()` e `stop()` para controle do ciclo de vida
- **Ack Manual**: Mensagens são confirmadas manualmente após processamento bem-sucedido
- **Nack + Requeue**: Em caso de erro, mensagem volta para fila
- **QoS**: `basicQos(1)` por padrão (processa 1 mensagem por vez)
- **Queue Durável**: Queues declaradas como duráveis

### `JacksonMessageSerializer`

Implementação de `MessageSerializer<T>` usando Jackson:

- **JSON**: Serialização/deserialização JSON
- **JavaTimeModule**: Suporte para tipos `java.time.*` (Instant, LocalDate, etc.)
- **Customizável**: Aceita `ObjectMapper` customizado via construtor

## Uso

### Configuração e Publicação

```java
// Publisher
var publisher = RabbitMQPublisherAdapter.builder()
    .host("localhost")
    .port(5672)
    .username("guest")
    .password("guest")
    .build();

var serializer = new JacksonMessageSerializer<MyEvent>();

var envelope = MessageEnvelope.<MyEvent>builder()
    .topic(TopicName.of("my-queue"))
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
var consumer = RabbitMQConsumerAdapter.builder()
    .host("localhost")
    .port(5672)
    .username("guest")
    .password("guest")
    .build();

var serializer = new JacksonMessageSerializer<MyEvent>();

consumer.subscribe(
    TopicName.of("my-queue"),
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

### Configuração Avançada

```java
// Publisher com exchange customizado
var publisher = RabbitMQPublisherAdapter.builder()
    .host("localhost")
    .port(5672)
    .exchange("my-exchange")
    .confirmEnabled(true)
    .build();

// Consumer com virtual host
var consumer = RabbitMQConsumerAdapter.builder()
    .host("localhost")
    .port(5672)
    .virtualHost("/production")
    .username("app-user")
    .password("secret")
    .build();
```

## Dependências

- `commons-ports-messaging`: Portas de mensageria
- `amqp-client`: Cliente RabbitMQ oficial
- `jackson-databind` + `jackson-datatype-jsr310`: Serialização JSON

## Testes

Testes de integração usando Testcontainers:

```bash
./mvnw test -pl commons-adapters-messaging-rabbitmq
```

Os testes iniciam um container RabbitMQ real (rabbitmq:3.13-alpine) e validam:
- Publicação e consumo de mensagens
- Propagação de headers (correlationId, causationId)
- Suporte a partition keys
- Múltiplas mensagens

## Diferenças vs. Kafka

| Característica | RabbitMQ | Kafka |
|----------------|----------|-------|
| **Modelo** | Message broker (push) | Distributed log (pull) |
| **Garantias** | At-least-once (default) | Exactly-once (transacional) |
| **Ordering** | Por queue | Por partition |
| **Routing** | Exchange + routing key | Topic + partition key |
| **Retenção** | Até consumo + TTL | Baseado em tempo |
| **Backpressure** | QoS (prefetch) | Poll batch size |

## RabbitMQ vs. Kafka - Quando usar?

**Use RabbitMQ quando:**
- Precisa de routing complexo (exchange types: topic, fanout, headers)
- Mensagens devem ser descartadas após consumo
- Baixa latência é crítica (<10ms)
- Trabalho distribuído (work queues)

**Use Kafka quando:**
- Event sourcing e reprocessamento de eventos
- Alta vazão (milhões de msg/s)
- Retenção de longo prazo
- Stream processing

## Integração com Outbox Pattern

Para garantir entrega transacional com RabbitMQ:

1. Salve eventos no outbox dentro da transação do banco
2. Publisher processa outbox em background
3. Use confirms (`confirmEnabled=true`) para garantir que RabbitMQ recebeu

**Nota**: RabbitMQ não suporta transações distribuídas (2PC) nativamente. Para garantias exactly-once, considere:
- Idempotência no consumidor
- Deduplicação por `messageId`
- Outbox pattern + at-least-once delivery

## Dead Letter Queues (DLQ)

Para configurar DLQ (requer declaração manual de queues):

```java
// Consumer que rejeita mensagens após 3 tentativas
Map<String, Object> args = new HashMap<>();
args.put("x-dead-letter-exchange", "dlx");
args.put("x-dead-letter-routing-key", "dead-letters");

// Declare queue com DLQ (requer acesso direto ao Channel)
channel.queueDeclare("my-queue", true, false, false, args);
```

**Nota**: Este adapter não gerencia DLQ automaticamente. Para DLQ, você pode:
- Declarar queues manualmente antes de usar o adapter
- Implementar retry logic no handler e descartar mensagens ruins
- Usar RabbitMQ management plugins

## Observações

- **Confirmações**: Confirmação síncrona (`waitForConfirmsOrDie`) pode impactar throughput. Para alta vazão, considere confirms assíncronos.
- **Reconnection**: Automatic recovery está habilitado (5s de intervalo). Mensagens em trânsito podem ser perdidas durante reconexão.
- **Headers grandes**: AMQP tem limite de frame (~128KB por padrão). Headers muito grandes podem falhar.
- **Queue Naming**: Queue name = `TopicName.value()` (não usa consumer group no nome da queue; múltiplos consumers da mesma queue competem por mensagens).
