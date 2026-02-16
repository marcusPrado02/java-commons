package com.marcusprado02.commons.app.batch;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Map;

/**
 * Represents a batch job execution unit.
 *
 * <p>A batch job is a long-running, potentially restartable operation that processes data in
 * chunks. Jobs can be configured with retry and skip policies, and can track their progress for
 * restart capabilities.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * public class CustomerImportJob implements BatchJob<CustomerRecord, ImportResult> {
 *
 *   @Override
 *   public String getJobId() {
 *     return "customer-import";
 *   }
 *
 *   @Override
 *   public Result<BatchJobExecution<ImportResult>> execute(
 *       BatchJobContext context) {
 *     // Job execution logic
 *     return Result.ok(execution);
 *   }
 *
 *   @Override
 *   public boolean isRestartable() {
 *     return true;
 *   }
 * }
 * }</pre>
 *
 * @param <I> the input item type to be processed
 * @param <O> the output type produced by the job
 */
public interface BatchJob<I, O> {

  /**
   * Gets the unique identifier for this batch job.
   *
   * <p>This ID is used for tracking job executions, restarts, and metrics.
   *
   * @return the job ID
   */
  String getJobId();

  /**
   * Executes the batch job.
   *
   * @param context the execution context containing parameters and state
   * @return result containing the job execution details or an error
   */
  Result<BatchJobExecution<O>> execute(BatchJobContext context);

  /**
   * Indicates whether this job can be restarted from a previous failed execution.
   *
   * <p>Restartable jobs maintain state and can resume from the point of failure.
   *
   * @return true if the job supports restart
   */
  default boolean isRestartable() {
    return false;
  }

  /**
   * Gets the chunk size for processing items in batches.
   *
   * <p>Items are read and processed in chunks of this size for efficiency and transaction
   * management.
   *
   * @return the chunk size, defaults to 100
   */
  default int getChunkSize() {
    return 100;
  }

  /**
   * Gets the maximum number of skip retries allowed for this job.
   *
   * <p>When an item fails processing, it can be retried up to this many times before being skipped.
   *
   * @return the maximum retry count, defaults to 3
   */
  default int getMaxRetries() {
    return 3;
  }

  /**
   * Gets the maximum number of items that can be skipped before the job fails.
   *
   * <p>This prevents jobs from silently skipping too many items.
   *
   * @return the maximum skip count, defaults to 10
   */
  default int getMaxSkips() {
    return 10;
  }

  /**
   * Determines if an exception should cause the item to be skipped.
   *
   * @param exception the exception that occurred
   * @return true if the item should be skipped
   */
  default boolean shouldSkip(Exception exception) {
    return false;
  }

  /**
   * Determines if an exception should cause a retry.
   *
   * @param exception the exception that occurred
   * @return true if the operation should be retried
   */
  default boolean shouldRetry(Exception exception) {
    return true;
  }

  /**
   * Gets job-specific parameters and configuration.
   *
   * @return the job parameters
   */
  default Map<String, Object> getJobParameters() {
    return Map.of();
  }
}
