package com.marcusprado02.commons.app.workflow;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a workflow instance.
 *
 * @param id workflow instance ID
 * @param definitionId workflow definition ID
 * @param currentState current state
 * @param status workflow status
 * @param context workflow context data
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 * @param completedAt completion timestamp
 * @param error error message if failed
 */
public record WorkflowInstance(
    String id,
    String definitionId,
    String currentState,
    WorkflowStatus status,
    Map<String, Object> context,
    Instant createdAt,
    Instant updatedAt,
    Optional<Instant> completedAt,
    Optional<String> error) {

  public WorkflowInstance {
    context = context == null ? Map.of() : Map.copyOf(context);
    completedAt = completedAt == null ? Optional.empty() : completedAt;
    error = error == null ? Optional.empty() : error;
  }

  /** Workflow status. */
  public enum WorkflowStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    TIMED_OUT
  }

  /**
   * Checks if workflow is active (running or compensating).
   *
   * @return true if active
   */
  public boolean isActive() {
    return status == WorkflowStatus.RUNNING || status == WorkflowStatus.COMPENSATING;
  }

  /**
   * Checks if workflow is terminal (completed, failed, compensated, timed out).
   *
   * @return true if terminal
   */
  public boolean isTerminal() {
    return status == WorkflowStatus.COMPLETED
        || status == WorkflowStatus.FAILED
        || status == WorkflowStatus.COMPENSATED
        || status == WorkflowStatus.TIMED_OUT;
  }

  /**
   * Creates a builder for WorkflowInstance.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for WorkflowInstance. */
  public static class Builder {
    private String id;
    private String definitionId;
    private String currentState;
    private WorkflowStatus status = WorkflowStatus.PENDING;
    private Map<String, Object> context = Map.of();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Optional<Instant> completedAt = Optional.empty();
    private Optional<String> error = Optional.empty();

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder definitionId(String definitionId) {
      this.definitionId = definitionId;
      return this;
    }

    public Builder currentState(String currentState) {
      this.currentState = currentState;
      return this;
    }

    public Builder status(WorkflowStatus status) {
      this.status = status;
      return this;
    }

    public Builder context(Map<String, Object> context) {
      this.context = context;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(Instant updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Builder completedAt(Instant completedAt) {
      this.completedAt = Optional.ofNullable(completedAt);
      return this;
    }

    public Builder error(String error) {
      this.error = Optional.ofNullable(error);
      return this;
    }

    public WorkflowInstance build() {
      return new WorkflowInstance(
          id,
          definitionId,
          currentState,
          status,
          context,
          createdAt,
          updatedAt,
          completedAt,
          error);
    }
  }
}
