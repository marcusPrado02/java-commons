package com.marcusprado02.commons.adapters.messaging.kafka;

import com.marcusprado02.commons.ports.messaging.ConsumerGroup;
import com.marcusprado02.commons.ports.messaging.MessageConsumerPort;
import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessageHeaders;
import com.marcusprado02.commons.ports.messaging.MessageId;
import com.marcusprado02.commons.ports.messaging.MessageSerializer;
import com.marcusprado02.commons.ports.messaging.TopicName;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KafkaConsumerAdapter implements MessageConsumerPort, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(KafkaConsumerAdapter.class);

  private final String bootstrapServers;
  private final Map<String, Object> consumerProperties;
  private final Duration pollTimeout;
  private final Map<String, SubscriptionEntry<?>> subscriptions = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final AtomicBoolean running = new AtomicBoolean(false);

  private KafkaConsumerAdapter(
      String bootstrapServers, Map<String, Object> consumerProperties, Duration pollTimeout) {
    this.bootstrapServers =
        Objects.requireNonNull(bootstrapServers, "bootstrapServers must not be null");
    this.consumerProperties = Map.copyOf(consumerProperties);
    this.pollTimeout = pollTimeout;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public <T> void subscribe(
      TopicName topic,
      ConsumerGroup group,
      Class<T> messageType,
      MessageSerializer<T> serializer,
      Consumer<MessageEnvelope<T>> handler) {
    Objects.requireNonNull(topic, "topic must not be null");
    Objects.requireNonNull(group, "group must not be null");
    Objects.requireNonNull(messageType, "messageType must not be null");
    Objects.requireNonNull(serializer, "serializer must not be null");
    Objects.requireNonNull(handler, "handler must not be null");

    String key = subscriptionKey(topic, group);
    if (subscriptions.containsKey(key)) {
      throw new IllegalStateException("Already subscribed to " + topic + " with group " + group);
    }

    SubscriptionEntry<T> entry =
        new SubscriptionEntry<>(topic, group, messageType, serializer, handler);
    subscriptions.put(key, entry);

    if (running.get()) {
      startConsumer(entry);
    }
  }

  @Override
  public void unsubscribe(TopicName topic, ConsumerGroup group) {
    String key = subscriptionKey(topic, group);
    SubscriptionEntry<?> entry = subscriptions.remove(key);
    if (entry != null) {
      entry.stop();
    }
  }

  @Override
  public void start() {
    if (running.compareAndSet(false, true)) {
      log.info("Starting Kafka consumer adapter");
      subscriptions.values().forEach(this::startConsumer);
    }
  }

  @Override
  public void stop() {
    if (running.compareAndSet(true, false)) {
      log.info("Stopping Kafka consumer adapter");
      subscriptions.values().forEach(SubscriptionEntry::stop);
      executor.shutdown();
      try {
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        executor.shutdownNow();
      }
    }
  }

  @Override
  public void close() {
    stop();
  }

  private <T> void startConsumer(SubscriptionEntry<T> entry) {
    executor.submit(() -> consumeLoop(entry));
  }

  private <T> void consumeLoop(SubscriptionEntry<T> entry) {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, entry.group.value());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
    consumerProperties.forEach(props::put);

    try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
      consumer.subscribe(List.of(entry.topic.value()));
      log.info("Subscribed to {} with group {}", entry.topic, entry.group);

      while (running.get() && !Thread.currentThread().isInterrupted()) {
        ConsumerRecords<String, byte[]> records = consumer.poll(pollTimeout);
        for (ConsumerRecord<String, byte[]> record : records) {
          try {
            MessageEnvelope<T> envelope = toEnvelope(record, entry);
            entry.handler.accept(envelope);
          } catch (Exception ex) {
            log.error(
                "Error processing message from {} partition {} offset {}",
                record.topic(),
                record.partition(),
                record.offset(),
                ex);
          }
        }
      }
    } catch (Exception ex) {
      log.error("Consumer loop error for topic {}", entry.topic, ex);
    } finally {
      log.info("Consumer loop stopped for topic {}", entry.topic);
    }
  }

  private <T> MessageEnvelope<T> toEnvelope(
      ConsumerRecord<String, byte[]> record, SubscriptionEntry<T> entry) {
    T payload = entry.serializer.deserialize(record.value(), entry.messageType);

    MessageHeaders.Builder headersBuilder = MessageHeaders.builder();
    for (Header header : record.headers()) {
      String value = new String(header.value(), StandardCharsets.UTF_8);
      headersBuilder.header(header.key(), value);
    }

    String messageIdValue = extractHeader(record, "messageId").orElseGet(() -> record.key());
    MessageId messageId = MessageId.of(messageIdValue);

    long timestampMillis = record.timestamp();
    Instant timestamp = Instant.ofEpochMilli(timestampMillis);

    return MessageEnvelope.<T>builder()
        .id(messageId)
        .topic(entry.topic)
        .payload(payload)
        .headers(headersBuilder.build())
        .timestamp(timestamp)
        .partitionKey(record.key())
        .build();
  }

  private java.util.Optional<String> extractHeader(
      ConsumerRecord<String, byte[]> record, String key) {
    Header header = record.headers().lastHeader(key);
    if (header == null) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(new String(header.value(), StandardCharsets.UTF_8));
  }

  private String subscriptionKey(TopicName topic, ConsumerGroup group) {
    return topic.value() + ":" + group.value();
  }

  private static final class SubscriptionEntry<T> {
    private final TopicName topic;
    private final ConsumerGroup group;
    private final Class<T> messageType;
    private final MessageSerializer<T> serializer;
    private final Consumer<MessageEnvelope<T>> handler;

    private SubscriptionEntry(
        TopicName topic,
        ConsumerGroup group,
        Class<T> messageType,
        MessageSerializer<T> serializer,
        Consumer<MessageEnvelope<T>> handler) {
      this.topic = topic;
      this.group = group;
      this.messageType = messageType;
      this.serializer = serializer;
      this.handler = handler;
    }

    private void stop() {
      // Consumer is stopped when running flag is set to false
    }
  }

  public static final class Builder {
    private String bootstrapServers;
    private final Map<String, Object> consumerProperties = new HashMap<>();
    private Duration pollTimeout = Duration.ofMillis(100);

    private Builder() {}

    public Builder bootstrapServers(String bootstrapServers) {
      this.bootstrapServers = bootstrapServers;
      return this;
    }

    public Builder property(String key, Object value) {
      consumerProperties.put(key, value);
      return this;
    }

    public Builder pollTimeout(Duration pollTimeout) {
      this.pollTimeout = pollTimeout;
      return this;
    }

    public KafkaConsumerAdapter build() {
      Objects.requireNonNull(bootstrapServers, "bootstrapServers must not be null");
      Objects.requireNonNull(pollTimeout, "pollTimeout must not be null");
      return new KafkaConsumerAdapter(bootstrapServers, consumerProperties, pollTimeout);
    }
  }
}
