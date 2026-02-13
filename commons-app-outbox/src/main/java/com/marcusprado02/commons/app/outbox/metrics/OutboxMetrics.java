package com.marcusprado02.commons.app.outbox.metrics;

/**
 * Interface for outbox metrics collection.
 *
 * <p>Implementations should integrate with monitoring systems (Micrometer, Prometheus, etc.).
 */
public interface OutboxMetrics {

  /** Record a message was successfully published. */
  void recordPublished(String topic);

  /**
   * Record a message publishing failed.
   *
   * @param topic Target topic
   * @param reason Failure reason (exception class or error code)
   */
  void recordFailed(String topic, String reason);

  /**
   * Record a message moved to dead letter queue.
   *
   * @param topic Target topic
   */
  void recordDead(String topic);

  /**
   * Record publishing latency.
   *
   * @param topic Target topic
   * @param durationMillis Time taken to publish in milliseconds
   */
  void recordLatency(String topic, long durationMillis);

  /**
   * Record batch processing time.
   *
   * @param batchSize Number of messages in batch
   * @param durationMillis Time taken to process batch in milliseconds
   */
  void recordBatchProcessing(int batchSize, long durationMillis);
}
