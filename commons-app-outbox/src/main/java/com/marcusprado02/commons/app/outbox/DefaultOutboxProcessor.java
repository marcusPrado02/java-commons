package com.marcusprado02.commons.app.outbox;

import com.marcusprado02.commons.app.outbox.config.OutboxProcessorConfig;
import com.marcusprado02.commons.app.outbox.metrics.NoOpOutboxMetrics;
import com.marcusprado02.commons.app.outbox.metrics.OutboxMetrics;
import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import com.marcusprado02.commons.app.outbox.retry.ExponentialBackoffStrategy;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultOutboxProcessor implements OutboxProcessor {

  private final OutboxRepositoryPort repository;
  private final OutboundPublisher outbound;
  private final OutboxProcessorConfig config;
  private final ExponentialBackoffStrategy backoffStrategy;
  private final OutboxMetrics metrics;
  private final CircuitBreakerWrapper circuitBreaker;

  public DefaultOutboxProcessor(
      OutboxRepositoryPort repository,
      OutboundPublisher outbound,
      OutboxProcessorConfig config,
      OutboxMetrics metrics) {
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
    this.outbound = Objects.requireNonNull(outbound, "outbound must not be null");
    this.config = Objects.requireNonNull(config, "config must not be null");
    this.backoffStrategy = new ExponentialBackoffStrategy(config);
    this.metrics = metrics != null ? metrics : NoOpOutboxMetrics.INSTANCE;
    this.circuitBreaker = new CircuitBreakerWrapper.NoOp();
  }

  public DefaultOutboxProcessor(OutboxRepositoryPort repository, OutboundPublisher outbound) {
    this(repository, outbound, OutboxProcessorConfig.defaults(), NoOpOutboxMetrics.INSTANCE);
  }

  @Override
  public void processAll() {
    long batchStart = System.currentTimeMillis();

    List<OutboxMessage> pending = repository.fetchBatch(OutboxStatus.PENDING, config.batchSize());

    if (pending.isEmpty()) {
      return;
    }

    int processed = 0;
    for (OutboxMessage msg : pending) {
      if (processMessage(msg)) {
        processed++;
      }
    }

    long batchDuration = System.currentTimeMillis() - batchStart;
    metrics.recordBatchProcessing(processed, batchDuration);
  }

  private boolean processMessage(OutboxMessage msg) {
    Instant processingAt = Instant.now();
    boolean marked = repository.markProcessing(msg.id(), processingAt);

    if (!marked) {
      return false;
    }

    try {
      publishWithMetrics(msg);
      repository.markPublished(msg.id(), Instant.now());
      metrics.recordPublished(msg.topic());
      return true;
    } catch (Exception ex) {
      handleFailure(msg, ex);
      return false;
    }
  }

  private void publishWithMetrics(OutboxMessage msg) {
    long start = System.currentTimeMillis();
    try {
      Supplier<Void> publishAction =
          () -> {
            outbound.publish(msg.topic(), msg.payload().body(), msg.headers());
            return null;
          };

      circuitBreaker.execute(publishAction);

      long duration = System.currentTimeMillis() - start;
      metrics.recordLatency(msg.topic(), duration);
    } catch (Exception ex) {
      long duration = System.currentTimeMillis() - start;
      metrics.recordLatency(msg.topic(), duration);
      throw ex;
    }
  }

  private void handleFailure(OutboxMessage msg, Exception ex) {
    int nextAttempts = msg.attempts() + 1;
    String reason = ex.getClass().getSimpleName();

    if (nextAttempts >= config.maxAttempts()) {
      repository.markDead(msg.id(), reason, nextAttempts);
      metrics.recordDead(msg.topic());
    } else {
      repository.markFailed(msg.id(), reason, nextAttempts);
      metrics.recordFailed(msg.topic(), reason);
    }
  }

  /** Wrapper for optional circuit breaker integration. */
  interface CircuitBreakerWrapper {
    <T> T execute(Supplier<T> action);

    class NoOp implements CircuitBreakerWrapper {
      @Override
      public <T> T execute(Supplier<T> action) {
        return action.get();
      }
    }
  }
}
