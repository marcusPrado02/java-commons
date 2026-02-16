package com.marcusprado02.commons.app.scheduler;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Context information for a scheduled job execution.
 *
 * <p>Provides access to job metadata, execution state, and configuration parameters.
 */
public final class ScheduledJobContext {

  private final String jobId;
  private final Instant scheduledFireTime;
  private final Instant actualFireTime;
  private final Map<String, Object> parameters;
  private final int refireCount;

  private ScheduledJobContext(Builder builder) {
    this.jobId = Objects.requireNonNull(builder.jobId, "jobId cannot be null");
    this.scheduledFireTime =
        Objects.requireNonNull(builder.scheduledFireTime, "scheduledFireTime cannot be null");
    this.actualFireTime =
        Objects.requireNonNull(builder.actualFireTime, "actualFireTime cannot be null");
    this.parameters =
        Map.copyOf(Objects.requireNonNull(builder.parameters, "parameters cannot be null"));
    this.refireCount = builder.refireCount;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the unique identifier of the job being executed.
   *
   * @return the job ID
   */
  public String getJobId() {
    return jobId;
  }

  /**
   * Gets the time the job was scheduled to fire.
   *
   * @return the scheduled fire time
   */
  public Instant getScheduledFireTime() {
    return scheduledFireTime;
  }

  /**
   * Gets the actual time the job started executing.
   *
   * @return the actual fire time
   */
  public Instant getActualFireTime() {
    return actualFireTime;
  }

  /**
   * Gets the current time.
   *
   * @return the current time
   */
  public Instant getCurrentTime() {
    return Instant.now();
  }

  /**
   * Gets a parameter value by key.
   *
   * @param key the parameter key
   * @param <T> the expected parameter type
   * @return optional containing the parameter value if present
   */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getParameter(String key) {
    return Optional.ofNullable((T) parameters.get(key));
  }

  /**
   * Gets a parameter value by key, or returns a default value if not present.
   *
   * @param key the parameter key
   * @param defaultValue the default value
   * @param <T> the expected parameter type
   * @return the parameter value or default value
   */
  @SuppressWarnings("unchecked")
  public <T> T getParameter(String key, T defaultValue) {
    return (T) parameters.getOrDefault(key, defaultValue);
  }

  /**
   * Gets all parameters as an immutable map.
   *
   * @return the parameters map
   */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  /**
   * Gets the number of times this job execution has been retried.
   *
   * <p>0 indicates the first execution attempt.
   *
   * @return the refire count
   */
  public int getRefireCount() {
    return refireCount;
  }

  /**
   * Checks if this is a retry execution (refire count > 0).
   *
   * @return true if this is a retry
   */
  public boolean isRetry() {
    return refireCount > 0;
  }

  public static final class Builder {
    private String jobId;
    private Instant scheduledFireTime;
    private Instant actualFireTime;
    private Map<String, Object> parameters = Map.of();
    private int refireCount = 0;

    private Builder() {}

    public Builder jobId(String jobId) {
      this.jobId = jobId;
      return this;
    }

    public Builder scheduledFireTime(Instant scheduledFireTime) {
      this.scheduledFireTime = scheduledFireTime;
      return this;
    }

    public Builder actualFireTime(Instant actualFireTime) {
      this.actualFireTime = actualFireTime;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder refireCount(int refireCount) {
      this.refireCount = refireCount;
      return this;
    }

    public ScheduledJobContext build() {
      return new ScheduledJobContext(this);
    }
  }
}
