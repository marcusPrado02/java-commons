package com.marcusprado02.commons.testkit.containers;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pre-configured Kafka test container.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @Container
 * static KafkaContainer kafka = TestKafka.container();
 *
 * // Configure Kafka with:
 * String bootstrapServers = kafka.getBootstrapServers();
 * }</pre>
 */
public final class TestKafka {

  private static final String DEFAULT_IMAGE = "confluentinc/cp-kafka:7.5.0";

  private TestKafka() {}

  /** Creates a Kafka container with default settings. */
  public static KafkaContainer container() {
    return new KafkaContainer(DockerImageName.parse(DEFAULT_IMAGE)).withReuse(true);
  }

  /** Creates a Kafka container with a specific Confluent Platform version. */
  public static KafkaContainer container(String confluentVersion) {
    return new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:" + confluentVersion))
        .withReuse(true);
  }

  /** Creates a Kafka container with custom image. */
  public static KafkaContainer container(DockerImageName image) {
    return new KafkaContainer(image).withReuse(true);
  }
}
