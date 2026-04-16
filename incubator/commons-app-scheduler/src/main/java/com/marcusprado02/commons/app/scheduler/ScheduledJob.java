package com.marcusprado02.commons.app.scheduler;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Represents a scheduled job that can be executed.
 *
 * <p>Implementations should be stateless and thread-safe, as they may be executed concurrently by
 * multiple scheduler instances in a distributed environment.
 *
 * @param <T> the type of result returned by the job
 */
@FunctionalInterface
public interface ScheduledJob<T> {

  /**
   * Executes the scheduled job.
   *
   * @param context the job execution context
   * @return result of the job execution
   */
  Result<T> execute(ScheduledJobContext context);

  /**
   * Gets the unique identifier for this job.
   *
   * <p>Default implementation returns the class name. Override to provide a custom identifier.
   *
   * @return the job identifier
   */
  default String getJobId() {
    return this.getClass().getSimpleName();
  }

  /**
   * Gets a human-readable description of this job.
   *
   * <p>Default implementation returns the job ID. Override to provide a more detailed description.
   *
   * @return the job description
   */
  default String getDescription() {
    return getJobId();
  }

  /**
   * Determines whether this job should acquire a distributed lock before execution.
   *
   * <p>When true, only one instance of this job will execute at a time across all scheduler
   * instances. When false, the job may execute concurrently on multiple instances.
   *
   * <p>Default is true for safety. Override to return false for jobs that can run concurrently.
   *
   * @return true if distributed lock should be acquired
   */
  default boolean requiresDistributedLock() {
    return true;
  }

  /**
   * Gets the maximum time this job is allowed to hold a distributed lock.
   *
   * <p>If the job takes longer than this duration, the lock will be considered stale and may be
   * acquired by another instance. Should be set to a value longer than the expected execution time.
   *
   * <p>Default is 10 minutes. Override for jobs with different execution times.
   *
   * @return lock duration in milliseconds
   */
  default long getLockDurationMillis() {
    return 10 * 60 * 1000; // 10 minutes
  }

  /**
   * Gets the minimum time between lock acquisitions for this job.
   *
   * <p>This prevents the lock from being immediately re-acquired after release, which can be useful
   * to ensure other instances have a chance to acquire the lock.
   *
   * <p>Default is 0 (no minimum). Override to add a delay.
   *
   * @return minimum lock interval in milliseconds
   */
  default long getMinLockIntervalMillis() {
    return 0;
  }
}
