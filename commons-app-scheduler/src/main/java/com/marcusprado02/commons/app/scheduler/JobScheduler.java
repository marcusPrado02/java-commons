package com.marcusprado02.commons.app.scheduler;

import com.marcusprado02.commons.kernel.result.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Scheduler for executing jobs at specific times or intervals.
 *
 * <p>Provides methods to schedule one-time and recurring jobs with support for cron expressions,
 * fixed delays, and fixed rates.
 */
public interface JobScheduler {

  /**
   * Schedules a job to run once at a specific time.
   *
   * @param job the job to execute
   * @param fireTime the time to execute the job
   * @param <T> the job result type
   * @return result containing the scheduled job ID
   */
  <T> Result<String> scheduleOnce(ScheduledJob<T> job, Instant fireTime);

  /**
   * Schedules a job to run once at a specific time with parameters.
   *
   * @param job the job to execute
   * @param fireTime the time to execute the job
   * @param parameters job parameters
   * @param <T> the job result type
   * @return result containing the scheduled job ID
   */
  <T> Result<String> scheduleOnce(
      ScheduledJob<T> job, Instant fireTime, Map<String, Object> parameters);

  /**
   * Schedules a job to run at fixed intervals.
   *
   * @param job the job to execute
   * @param initialDelay delay before first execution
   * @param interval interval between executions
   * @param <T> the job result type
   * @return result containing the scheduled job ID
   */
  <T> Result<String> scheduleWithFixedDelay(
      ScheduledJob<T> job, Duration initialDelay, Duration interval);

  /**
   * Schedules a job to run at fixed intervals with parameters.
   *
   * @param job the job to execute
   * @param initialDelay delay before first execution
   * @param interval interval between executions
   * @param parameters job parameters
   * @param <T> the job result type
   * @return result containing the scheduled job ID
   */
  <T> Result<String> scheduleWithFixedDelay(
      ScheduledJob<T> job,
      Duration initialDelay,
      Duration interval,
      Map<String, Object> parameters);

  /**
   * Schedules a job using a cron expression.
   *
   * @param job the job to execute
   * @param cronExpression the cron expression (e.g., "0 0 * * * ?" for daily at midnight)
   * @param <T> the job result type
   * @return result containing the scheduled job ID
   */
  <T> Result<String> scheduleWithCron(ScheduledJob<T> job, String cronExpression);

  /**
   * Schedules a job using a cron expression with parameters.
   *
   * @param job the job to execute
   * @param cronExpression the cron expression
   * @param parameters job parameters
   * @param <T> the job result type
   * @return result containing the scheduled job ID
   */
  <T> Result<String> scheduleWithCron(
      ScheduledJob<T> job, String cronExpression, Map<String, Object> parameters);

  /**
   * Unschedules a job by its ID.
   *
   * @param jobId the job ID returned when scheduling
   * @return result indicating success or failure
   */
  Result<Void> unschedule(String jobId);

  /**
   * Pauses a scheduled job.
   *
   * @param jobId the job ID
   * @return result indicating success or failure
   */
  Result<Void> pause(String jobId);

  /**
   * Resumes a paused job.
   *
   * @param jobId the job ID
   * @return result indicating success or failure
   */
  Result<Void> resume(String jobId);

  /**
   * Checks if a job is currently scheduled.
   *
   * @param jobId the job ID
   * @return result containing true if scheduled, false otherwise
   */
  Result<Boolean> isScheduled(String jobId);

  /**
   * Gets the next execution time for a scheduled job.
   *
   * @param jobId the job ID
   * @return result containing the next fire time if scheduled
   */
  Result<Instant> getNextFireTime(String jobId);

  /**
   * Gets all currently scheduled job IDs.
   *
   * @return result containing list of job IDs
   */
  Result<List<String>> getAllScheduledJobIds();

  /**
   * Shuts down the scheduler, completing currently executing jobs.
   *
   * @return result indicating success or failure
   */
  Result<Void> shutdown();

  /**
   * Shuts down the scheduler immediately, interrupting executing jobs.
   *
   * @return result indicating success or failure
   */
  Result<Void> shutdownNow();
}
