package com.marcusprado02.commons.app.outbox.metrics;

/** No-op implementation of OutboxMetrics for when metrics are disabled. */
public final class NoOpOutboxMetrics implements OutboxMetrics {

  public static final NoOpOutboxMetrics INSTANCE = new NoOpOutboxMetrics();

  private NoOpOutboxMetrics() {}

  @Override
  public void recordPublished(String topic) {}

  @Override
  public void recordFailed(String topic, String reason) {}

  @Override
  public void recordDead(String topic) {}

  @Override
  public void recordLatency(String topic, long durationMillis) {}

  @Override
  public void recordBatchProcessing(int batchSize, long durationMillis) {}
}
