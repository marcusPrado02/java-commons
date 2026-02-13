package com.marcusprado02.commons.adapters.messaging.kafka;

import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessagePublisherPort;
import com.marcusprado02.commons.ports.messaging.MessageSerializer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KafkaPublisherAdapter implements MessagePublisherPort, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(KafkaPublisherAdapter.class);

  private final KafkaProducer<String, byte[]> producer;
  private final boolean transactional;

  private KafkaPublisherAdapter(KafkaProducer<String, byte[]> producer, boolean transactional) {
    this.producer = Objects.requireNonNull(producer, "producer must not be null");
    this.transactional = transactional;
    if (transactional) {
      producer.initTransactions();
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public <T> void publish(MessageEnvelope<T> message, MessageSerializer<T> serializer) {
    Objects.requireNonNull(message, "message must not be null");
    Objects.requireNonNull(serializer, "serializer must not be null");

    byte[] payload = serializer.serialize(message.payload());
    String key = message.partitionKey().orElse(message.id().value());

    ProducerRecord<String, byte[]> record =
        new ProducerRecord<>(message.topic().value(), key, payload);

    // Add headers
    message
        .headers()
        .asMap()
        .forEach(
            (k, v) -> record.headers().add(k, v.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    record
        .headers()
        .add("messageId", message.id().value().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    record
        .headers()
        .add(
            "timestamp",
            String.valueOf(message.timestamp().toEpochMilli())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));

    try {
      RecordMetadata metadata = producer.send(record).get();
      log.debug(
          "Published message {} to topic {} partition {} offset {}",
          message.id(),
          metadata.topic(),
          metadata.partition(),
          metadata.offset());
    } catch (Exception ex) {
      log.error("Failed to publish message {} to topic {}", message.id(), message.topic(), ex);
      throw new RuntimeException("Failed to publish message", ex);
    }
  }

  public void beginTransaction() {
    if (!transactional) {
      throw new IllegalStateException("Producer is not transactional");
    }
    producer.beginTransaction();
  }

  public void commitTransaction() {
    if (!transactional) {
      throw new IllegalStateException("Producer is not transactional");
    }
    producer.commitTransaction();
  }

  public void abortTransaction() {
    if (!transactional) {
      throw new IllegalStateException("Producer is not transactional");
    }
    producer.abortTransaction();
  }

  @Override
  public void close() {
    producer.close();
  }

  public static final class Builder {
    private String bootstrapServers;
    private final Map<String, Object> properties = new HashMap<>();
    private boolean transactional = false;
    private String transactionalId;

    private Builder() {}

    public Builder bootstrapServers(String bootstrapServers) {
      this.bootstrapServers = bootstrapServers;
      return this;
    }

    public Builder property(String key, Object value) {
      properties.put(key, value);
      return this;
    }

    public Builder transactional(boolean transactional) {
      this.transactional = transactional;
      return this;
    }

    public Builder transactionalId(String transactionalId) {
      this.transactionalId = transactionalId;
      this.transactional = true;
      return this;
    }

    public KafkaPublisherAdapter build() {
      Objects.requireNonNull(bootstrapServers, "bootstrapServers must not be null");

      Properties props = new Properties();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
      props.put(ProducerConfig.ACKS_CONFIG, "all");
      props.put(ProducerConfig.RETRIES_CONFIG, 3);
      props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
      props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

      if (transactional) {
        String txId =
            (transactionalId != null && !transactionalId.isBlank())
                ? transactionalId
                : "kafka-publisher-" + System.currentTimeMillis();
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, txId);
      }

      properties.forEach(props::put);

      return new KafkaPublisherAdapter(new KafkaProducer<>(props), transactional);
    }
  }
}
