package com.marcusprado02.commons.app.scheduler;

import com.marcusprado02.commons.kernel.result.Result;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Distributed locking mechanism for coordinating job execution across multiple instances.
 *
 * <p>Ensures that only one instance of a job executes at a time in a distributed environment.
 */
public interface DistributedLock {

  /**
   * Executes a task while holding a distributed lock.
   *
   * <p>If the lock cannot be acquired, the task is not executed and a failure result is returned.
   *
   * @param lockName the unique name of the lock
   * @param lockDuration how long the lock should be held
   * @param task the task to execute while holding the lock
   * @param <T> the task result type
   * @return result of the task execution or failure if lock not acquired
   */
  <T> Result<T> executeWithLock(String lockName, Duration lockDuration, Supplier<Result<T>> task);

  /**
   * Executes a task while holding a distributed lock with minimum interval.
   *
   * <p>Ensures at least the specified minimum interval passes between lock acquisitions for the
   * same lock name.
   *
   * @param lockName the unique name of the lock
   * @param lockDuration how long the lock should be held
   * @param minInterval minimum time between lock acquisitions
   * @param task the task to execute while holding the lock
   * @param <T> the task result type
   * @return result of the task execution or failure if lock not acquired
   */
  <T> Result<T> executeWithLock(
      String lockName, Duration lockDuration, Duration minInterval, Supplier<Result<T>> task);

  /**
   * Tries to acquire a lock without executing any task.
   *
   * @param lockName the unique name of the lock
   * @param lockDuration how long the lock should be held
   * @return result indicating whether the lock was acquired
   */
  Result<Boolean> tryLock(String lockName, Duration lockDuration);

  /**
   * Releases a previously acquired lock.
   *
   * @param lockName the unique name of the lock
   * @return result indicating success or failure
   */
  Result<Void> unlock(String lockName);

  /**
   * Checks if a lock is currently held by any instance.
   *
   * @param lockName the unique name of the lock
   * @return result containing true if locked, false otherwise
   */
  Result<Boolean> isLocked(String lockName);
}
