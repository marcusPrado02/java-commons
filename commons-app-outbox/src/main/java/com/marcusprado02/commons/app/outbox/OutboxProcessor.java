package com.marcusprado02.commons.app.outbox;

/** OutboxProcessor contract. */
public interface OutboxProcessor {

  /** Process pending messages using the processor's configured batch size. */
  void processAll();

  /**
   * Process at most {@code batchSize} pending messages in a single pass.
   *
   * <p>Default implementation delegates to {@link #processAll()}. Implementations should override
   * this to respect the given batch size limit.
   *
   * @param batchSize maximum number of messages to process
   */
  default void processBatch(int batchSize) {
    processAll();
  }
}
