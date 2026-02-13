package com.marcusprado02.commons.app.outbox.port;

import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for Outbox pattern implementation.
 *
 * <p>State Machine:
 *
 * <pre>
 *   PENDING ──────┐
 *      ↓          │
 *   PROCESSING    │
 *      ↓          │
 *   PUBLISHED     │
 *                 │
 *   FAILED ───────┘ (retry)
 *      ↓
 *   DEAD (max attempts)
 * </pre>
 *
 * <p>Thread Safety: Implementations must handle concurrent access to prevent duplicate processing.
 * Use optimistic locking or SELECT FOR UPDATE when transitioning to PROCESSING.
 */
public interface OutboxRepositoryPort {

  /**
   * Append a new message to the outbox.
   *
   * @param message Message with PENDING status
   */
  void append(OutboxMessage message);

  /**
   * Fetch a batch of messages with given status.
   *
   * <p>For PENDING status, should use row-level locking or optimistic locking to prevent concurrent
   * processing.
   *
   * @param status Status to filter
   * @param limit Maximum number of messages
   * @return List of messages, empty if none found
   */
  List<OutboxMessage> fetchBatch(OutboxStatus status, int limit);

  /**
   * Transition message from PENDING to PROCESSING.
   *
   * <p>Must be atomic and handle concurrency. Only one worker should successfully mark a message as
   * PROCESSING.
   *
   * @param id Message ID
   * @param processingAt Timestamp when processing started
   * @return true if successfully marked, false if already processing by another worker
   */
  boolean markProcessing(OutboxMessageId id, Instant processingAt);

  /**
   * Transition message from PROCESSING to PUBLISHED.
   *
   * @param id Message ID
   * @param publishedAt Timestamp when published
   */
  void markPublished(OutboxMessageId id, Instant publishedAt);

  /**
   * Transition message from PROCESSING to FAILED.
   *
   * @param id Message ID
   * @param reason Failure reason
   * @param attempts Current attempt count
   */
  void markFailed(OutboxMessageId id, String reason, int attempts);

  /**
   * Transition message from FAILED to DEAD (max retries exhausted).
   *
   * @param id Message ID
   * @param reason Final failure reason
   * @param attempts Final attempt count
   */
  void markDead(OutboxMessageId id, String reason, int attempts);

  /**
   * Transition message from FAILED back to PENDING for retry.
   *
   * <p>Resets processing state to allow another attempt.
   *
   * @param id Message ID
   * @param reason Reason for retry
   * @param attempts Current attempt count
   */
  void markRetryable(OutboxMessageId id, String reason, int attempts);

  /**
   * Find message by ID.
   *
   * @param id Message ID
   * @return Optional message
   */
  Optional<OutboxMessage> findById(OutboxMessageId id);

  /**
   * Count messages by status.
   *
   * @param status Status to count
   * @return Number of messages
   */
  long countByStatus(OutboxStatus status);

  /**
   * Delete published messages older than given timestamp.
   *
   * <p>Use for cleanup/archival of old messages.
   *
   * @param olderThan Delete messages published before this timestamp
   * @return Number of deleted messages
   */
  int deletePublishedOlderThan(Instant olderThan);
}
