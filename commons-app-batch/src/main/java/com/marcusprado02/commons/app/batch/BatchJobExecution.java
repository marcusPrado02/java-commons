package com.marcusprado02.commons.app.batch;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents the result of a batch job execution.
 *
 * @param <O> the output type produced by the job
 */
public final class BatchJobExecution<O> {

  private final String executionId;
  private final BatchJobStatus status;
  private final Instant startTime;
  private final Instant endTime;
  private final long itemsRead;
  private final long itemsProcessed;
  private final long itemsSkipped;
  private final long itemsWritten;
  private final O result;
  private final String errorMessage;

  private BatchJobExecution(Builder<O> builder) {
    this.executionId = Objects.requireNonNull(builder.executionId, "executionId cannot be null");
    this.status = Objects.requireNonNull(builder.status, "status cannot be null");
    this.startTime = Objects.requireNonNull(builder.startTime, "startTime cannot be null");
    this.endTime = builder.endTime;
    this.itemsRead = builder.itemsRead;
    this.itemsProcessed = builder.itemsProcessed;
    this.itemsSkipped = builder.itemsSkipped;
    this.itemsWritten = builder.itemsWritten;
    this.result = builder.result;
    this.errorMessage = builder.errorMessage;
  }

  public static <O> Builder<O> builder() {
    return new Builder<>();
  }

  public String getExecutionId() {
    return executionId;
  }

  public BatchJobStatus getStatus() {
    return status;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public Duration getDuration() {
    if (endTime == null) {
      return Duration.between(startTime, Instant.now());
    }
    return Duration.between(startTime, endTime);
  }

  public long getItemsRead() {
    return itemsRead;
  }

  public long getItemsProcessed() {
    return itemsProcessed;
  }

  public long getItemsSkipped() {
    return itemsSkipped;
  }

  public long getItemsWritten() {
    return itemsWritten;
  }

  public O getResult() {
    return result;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public boolean isSuccessful() {
    return status == BatchJobStatus.COMPLETED;
  }

  public boolean isFailed() {
    return status == BatchJobStatus.FAILED;
  }

  public static final class Builder<O> {
    private String executionId;
    private BatchJobStatus status = BatchJobStatus.STARTING;
    private Instant startTime = Instant.now();
    private Instant endTime;
    private long itemsRead = 0;
    private long itemsProcessed = 0;
    private long itemsSkipped = 0;
    private long itemsWritten = 0;
    private O result;
    private String errorMessage;

    private Builder() {}

    public Builder<O> executionId(String executionId) {
      this.executionId = executionId;
      return this;
    }

    public Builder<O> status(BatchJobStatus status) {
      this.status = status;
      return this;
    }

    public Builder<O> startTime(Instant startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder<O> endTime(Instant endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder<O> itemsRead(long itemsRead) {
      this.itemsRead = itemsRead;
      return this;
    }

    public Builder<O> itemsProcessed(long itemsProcessed) {
      this.itemsProcessed = itemsProcessed;
      return this;
    }

    public Builder<O> itemsSkipped(long itemsSkipped) {
      this.itemsSkipped = itemsSkipped;
      return this;
    }

    public Builder<O> itemsWritten(long itemsWritten) {
      this.itemsWritten = itemsWritten;
      return this;
    }

    public Builder<O> result(O result) {
      this.result = result;
      return this;
    }

    public Builder<O> errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public BatchJobExecution<O> build() {
      return new BatchJobExecution<>(this);
    }
  }
}
