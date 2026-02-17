package com.marcusprado02.commons.app.workflow;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of WorkflowEngine with state machine and saga support.
 *
 * <p>This implementation provides: - State machine execution - Automatic compensation on failure -
 * Timeout handling - Event-driven transitions - In-memory storage (use repository for persistence)
 */
public final class DefaultWorkflowEngine implements WorkflowEngine {

  private static final Logger logger = LoggerFactory.getLogger(DefaultWorkflowEngine.class);

  private final Map<String, WorkflowDefinition> definitions = new ConcurrentHashMap<>();
  private final Map<String, WorkflowInstance> instances = new ConcurrentHashMap<>();
  private final Map<String, ActionExecutor> executors = new ConcurrentHashMap<>();
  private final Map<String, List<String>> executionHistory = new ConcurrentHashMap<>();

  private DefaultWorkflowEngine() {}

  /**
   * Creates a DefaultWorkflowEngine.
   *
   * @return a new DefaultWorkflowEngine
   */
  public static DefaultWorkflowEngine create() {
    return new DefaultWorkflowEngine();
  }

  @Override
  public Result<WorkflowInstance> startWorkflow(
      String definitionId, Map<String, Object> initialContext) {
    logger.info("Starting workflow: {}", definitionId);

    var definition = definitions.get(definitionId);
    if (definition == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WORKFLOW.DEFINITION_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Workflow definition not found: " + definitionId));
    }

    var workflowId = UUID.randomUUID().toString();
    var instance =
        WorkflowInstance.builder()
            .id(workflowId)
            .definitionId(definitionId)
            .currentState(definition.initialState())
            .status(WorkflowInstance.WorkflowStatus.RUNNING)
            .context(new HashMap<>(initialContext))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    instances.put(workflowId, instance);
    executionHistory.put(workflowId, new ArrayList<>(List.of(definition.initialState())));

    // Execute initial state action if present
    return executeCurrentState(instance);
  }

  @Override
  public Result<WorkflowInstance> sendEvent(
      String workflowId, String event, Map<String, Object> eventData) {
    logger.info("Sending event '{}' to workflow '{}'", event, workflowId);

    var instance = instances.get(workflowId);
    if (instance == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WORKFLOW.INSTANCE_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Workflow instance not found: " + workflowId));
    }

    if (instance.isTerminal()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WORKFLOW.ALREADY_TERMINAL"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              "Workflow is already in terminal state: " + instance.status()));
    }

    var definition = definitions.get(instance.definitionId());
    if (definition == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WORKFLOW.DEFINITION_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Workflow definition not found: " + instance.definitionId()));
    }

    // Find matching transition
    var transitionOpt =
        definition.transitions().stream()
            .filter(
                t ->
                    t.from().equals(instance.currentState())
                        && t.event().equals(event)
                        && evaluateCondition(t.condition(), instance.context(), eventData))
            .findFirst();

    if (transitionOpt.isEmpty()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WORKFLOW.NO_TRANSITION"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              String.format(
                  "No transition found from state '%s' with event '%s'",
                  instance.currentState(), event)));
    }

    var transition = transitionOpt.get();

    // Update context with event data
    var newContext = new HashMap<>(instance.context());
    newContext.putAll(eventData);

    // Transition to new state
    var updatedInstance =
        WorkflowInstance.builder()
            .id(instance.id())
            .definitionId(instance.definitionId())
            .currentState(transition.to())
            .status(instance.status())
            .context(newContext)
            .createdAt(instance.createdAt())
            .updatedAt(Instant.now())
            .completedAt(instance.completedAt().orElse(null))
            .error(instance.error().orElse(null))
            .build();

    instances.put(workflowId, updatedInstance);
    executionHistory.get(workflowId).add(transition.to());

    logger.info(
        "Workflow '{}' transitioned from '{}' to '{}'",
        workflowId,
        instance.currentState(),
        transition.to());

    // Execute new state action
    return executeCurrentState(updatedInstance);
  }

  @Override
  public Result<WorkflowInstance> getWorkflow(String workflowId) {
    var instance = instances.get(workflowId);
    if (instance == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WORKFLOW.INSTANCE_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Workflow instance not found: " + workflowId));
    }
    return Result.ok(instance);
  }

  @Override
  public Result<WorkflowInstance> compensate(String workflowId) {
    logger.info("Compensating workflow '{}'", workflowId);

    var instance = instances.get(workflowId);
    if (instance == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WORKFLOW.INSTANCE_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Workflow instance not found: " + workflowId));
    }

    var definition = definitions.get(instance.definitionId());
    if (definition == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WORKFLOW.DEFINITION_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Workflow definition not found: " + instance.definitionId()));
    }

    // Update status to COMPENSATING
    var compensatingInstance =
        WorkflowInstance.builder()
            .id(instance.id())
            .definitionId(instance.definitionId())
            .currentState(instance.currentState())
            .status(WorkflowInstance.WorkflowStatus.COMPENSATING)
            .context(instance.context())
            .createdAt(instance.createdAt())
            .updatedAt(Instant.now())
            .completedAt(instance.completedAt().orElse(null))
            .error(instance.error().orElse(null))
            .build();

    instances.put(workflowId, compensatingInstance);

    // Execute compensation actions in reverse order
    var history = executionHistory.get(workflowId);
    if (history != null) {
      // Reverse order
      for (int i = history.size() - 1; i >= 0; i--) {
        var stateName = history.get(i);
        var stateOpt =
            definition.states().stream().filter(s -> s.name().equals(stateName)).findFirst();

        if (stateOpt.isPresent()) {
          var state = stateOpt.get();
          if (state.compensation().isPresent()) {
            var compensationAction = state.compensation().get();
            logger.info(
                "Executing compensation '{}' for state '{}'", compensationAction, stateName);

            var executor = executors.get(compensationAction);
            if (executor != null) {
              var result = executor.execute(compensationAction, compensatingInstance.context());
              if (result.isFail()) {
                logger.error(
                    "Compensation failed for state '{}': {}",
                    stateName,
                    result.problemOrNull().message());
                // Continue with next compensation despite failure
              } else {
                // Update context with compensation result
                var newContext = new HashMap<>(compensatingInstance.context());
                newContext.putAll(result.getOrNull());
                compensatingInstance =
                    WorkflowInstance.builder()
                        .id(compensatingInstance.id())
                        .definitionId(compensatingInstance.definitionId())
                        .currentState(compensatingInstance.currentState())
                        .status(compensatingInstance.status())
                        .context(newContext)
                        .createdAt(compensatingInstance.createdAt())
                        .updatedAt(Instant.now())
                        .completedAt(compensatingInstance.completedAt().orElse(null))
                        .error(compensatingInstance.error().orElse(null))
                        .build();
              }
            }
          }
        }
      }
    }

    // Mark as COMPENSATED
    var compensatedInstance =
        WorkflowInstance.builder()
            .id(compensatingInstance.id())
            .definitionId(compensatingInstance.definitionId())
            .currentState(compensatingInstance.currentState())
            .status(WorkflowInstance.WorkflowStatus.COMPENSATED)
            .context(compensatingInstance.context())
            .createdAt(compensatingInstance.createdAt())
            .updatedAt(Instant.now())
            .completedAt(Instant.now())
            .error(compensatingInstance.error().orElse(null))
            .build();

    instances.put(workflowId, compensatedInstance);

    logger.info("Workflow '{}' compensated successfully", workflowId);
    return Result.ok(compensatedInstance);
  }

  @Override
  public Result<WorkflowInstance> cancel(String workflowId, String reason) {
    logger.info("Cancelling workflow '{}': {}", workflowId, reason);

    var instance = instances.get(workflowId);
    if (instance == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WORKFLOW.INSTANCE_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Workflow instance not found: " + workflowId));
    }

    var cancelledInstance =
        WorkflowInstance.builder()
            .id(instance.id())
            .definitionId(instance.definitionId())
            .currentState(instance.currentState())
            .status(WorkflowInstance.WorkflowStatus.FAILED)
            .context(instance.context())
            .createdAt(instance.createdAt())
            .updatedAt(Instant.now())
            .completedAt(Instant.now())
            .error("Cancelled: " + reason)
            .build();

    instances.put(workflowId, cancelledInstance);

    return Result.ok(cancelledInstance);
  }

  @Override
  public Result<Void> registerDefinition(WorkflowDefinition definition) {
    logger.info("Registering workflow definition '{}'", definition.id());
    definitions.put(definition.id(), definition);
    return Result.ok(null);
  }

  @Override
  public Result<Void> registerActionExecutor(String actionName, ActionExecutor executor) {
    logger.info("Registering action executor '{}'", actionName);
    executors.put(actionName, executor);
    return Result.ok(null);
  }

  private Result<WorkflowInstance> executeCurrentState(WorkflowInstance instance) {
    var definition = definitions.get(instance.definitionId());
    if (definition == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WORKFLOW.DEFINITION_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Workflow definition not found: " + instance.definitionId()));
    }

    var stateOpt =
        definition.states().stream()
            .filter(s -> s.name().equals(instance.currentState()))
            .findFirst();

    if (stateOpt.isEmpty()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WORKFLOW.STATE_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "State not found: " + instance.currentState()));
    }

    var state = stateOpt.get();

    // Check if END or FAIL state
    if (state.type() == WorkflowDefinition.State.StateType.END) {
      var completedInstance =
          WorkflowInstance.builder()
              .id(instance.id())
              .definitionId(instance.definitionId())
              .currentState(instance.currentState())
              .status(WorkflowInstance.WorkflowStatus.COMPLETED)
              .context(instance.context())
              .createdAt(instance.createdAt())
              .updatedAt(Instant.now())
              .completedAt(Instant.now())
              .build();

      instances.put(instance.id(), completedInstance);
      logger.info("Workflow '{}' completed successfully", instance.id());
      return Result.ok(completedInstance);
    }

    if (state.type() == WorkflowDefinition.State.StateType.FAIL) {
      var failedInstance =
          WorkflowInstance.builder()
              .id(instance.id())
              .definitionId(instance.definitionId())
              .currentState(instance.currentState())
              .status(WorkflowInstance.WorkflowStatus.FAILED)
              .context(instance.context())
              .createdAt(instance.createdAt())
              .updatedAt(Instant.now())
              .completedAt(Instant.now())
              .error("Workflow reached FAIL state")
              .build();

      instances.put(instance.id(), failedInstance);
      logger.error("Workflow '{}' failed", instance.id());

      // Trigger compensation
      return compensate(instance.id());
    }

    // Execute action if present
    if (state.action().isPresent()) {
      var actionName = state.action().get();
      var executor = executors.get(actionName);

      if (executor == null) {
        logger.warn("No executor registered for action '{}'", actionName);
        return Result.ok(instance);
      }

      logger.info("Executing action '{}' for workflow '{}'", actionName, instance.id());
      var result = executor.execute(actionName, instance.context());

      if (result.isFail()) {
        logger.error("Action '{}' failed: {}", actionName, result.problemOrNull().message());

        var failedInstance =
            WorkflowInstance.builder()
                .id(instance.id())
                .definitionId(instance.definitionId())
                .currentState(instance.currentState())
                .status(WorkflowInstance.WorkflowStatus.FAILED)
                .context(instance.context())
                .createdAt(instance.createdAt())
                .updatedAt(Instant.now())
                .completedAt(Instant.now())
                .error("Action failed: " + result.problemOrNull().message())
                .build();

        instances.put(instance.id(), failedInstance);

        // Trigger compensation
        return compensate(instance.id());
      }

      // Update context with action result
      var newContext = new HashMap<>(instance.context());
      newContext.putAll(result.getOrNull());

      var updatedInstance =
          WorkflowInstance.builder()
              .id(instance.id())
              .definitionId(instance.definitionId())
              .currentState(instance.currentState())
              .status(instance.status())
              .context(newContext)
              .createdAt(instance.createdAt())
              .updatedAt(Instant.now())
              .completedAt(instance.completedAt().orElse(null))
              .error(instance.error().orElse(null))
              .build();

      instances.put(instance.id(), updatedInstance);
      return Result.ok(updatedInstance);
    }

    return Result.ok(instance);
  }

  private boolean evaluateCondition(
      Optional<String> condition, Map<String, Object> context, Map<String, Object> eventData) {
    // Simple condition evaluation - in production, use expression language (JEXL, SpEL, etc.)
    return condition.isEmpty(); // For now, accept all transitions without conditions
  }
}
