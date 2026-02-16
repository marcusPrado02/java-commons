package com.marcusprado02.commons.app.batch;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Context information for a batch job execution.
 *
 * <p>Provides access to job metadata, execution state, restart information, and configuration
 * parameters.
 */
public final class BatchJobContext {

  private final String jobId;
  private final String executionId;
  private final Instant startTime;
  private final Map<String, Object> parameters;
  private final boolean isRestart;
  private final Long restartFromPosition;

  private BatchJobContext(Builder builder) {
    this.jobId = Objects.requireNonNull(builder.jobId, "jobId cannot be null");
    this.executionId = Objects.requireNonNull(builder.executionId, "executionId cannot be null");
    this.startTime = Objects.requireNonNull(builder.startTime, "startTime cannot be null");
    this.parameters =
        Map.copyOf(Objects.requireNonNull(builder.parameters, "parameters cannot be null"));
    this.isRestart = builder.isRestart;
    this.restartFromPosition = builder.restartFromPosition;
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
   * Gets the unique identifier for this specific execution instance.
   *
   * @return the execution ID
   */
  public String getExecutionId() {
    return executionId;
  }

  /**
   * Gets the time this execution started.
   *
   * @return the start time
   */
  public Instant getStartTime() {
    return startTime;
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
   * Checks if this execution is a restart from a previous failed execution.
   *
   * @return true if this is a restart
   */
  public boolean isRestart() {
    return isRestart;
  }

  /**
   * Gets the position to restart from, if this is a restart.
   *
   * <p>The position typically represents the last successfully processed item index or offset.
   *
   * @return optional containing the restart position if this is a restart
   */
  public Optional<Long> getRestartFromPosition() {
    return Optional.ofNullable(restartFromPosition);
  }

  public static final class Builder {
    private String jobId;
    private String executionId;
    private Instant startTime = Instant.now();
    private Map<String, Object> parameters = Map.of();
    private boolean isRestart = false;
    private Long restartFromPosition = null;

    private Builder() {}

    public Builder jobId(String jobId) {
      this.jobId = jobId;
      return this;
    }

    public Builder executionId(String executionId) {
      this.executionId = executionId;
      return this;
    }

    public Builder startTime(Instant startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder isRestart(boolean isRestart) {
      this.isRestart = isRestart;
      return this;
    }

    public Builder restartFromPosition(Long restartFromPosition) {
      this.restartFromPosition = restartFromPosition;
      return this;
    }

    public BatchJobContext build() {
      return new BatchJobContext(this);
    }
  }
}
