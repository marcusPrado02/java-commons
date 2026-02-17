package com.marcusprado02.commons.app.workflow;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Map;

/**
 * Workflow orchestration engine.
 *
 * <p>Manages workflow instances, executes state machines, and handles compensations.
 */
public interface WorkflowEngine {

  /**
   * Starts a new workflow instance.
   *
   * @param definitionId workflow definition ID
   * @param initialContext initial context data
   * @return the result containing the workflow instance
   */
  Result<WorkflowInstance> startWorkflow(String definitionId, Map<String, Object> initialContext);

  /**
   * Sends an event to a workflow instance.
   *
   * @param workflowId workflow instance ID
   * @param event event name
   * @param eventData event data
   * @return the result containing the updated workflow instance
   */
  Result<WorkflowInstance> sendEvent(
      String workflowId, String event, Map<String, Object> eventData);

  /**
   * Gets a workflow instance by ID.
   *
   * @param workflowId workflow instance ID
   * @return the result containing the workflow instance
   */
  Result<WorkflowInstance> getWorkflow(String workflowId);

  /**
   * Compensates a workflow (executes compensation actions).
   *
   * @param workflowId workflow instance ID
   * @return the result containing the compensated workflow instance
   */
  Result<WorkflowInstance> compensate(String workflowId);

  /**
   * Cancels a workflow instance.
   *
   * @param workflowId workflow instance ID
   * @param reason cancellation reason
   * @return the result containing the cancelled workflow instance
   */
  Result<WorkflowInstance> cancel(String workflowId, String reason);

  /**
   * Registers a workflow definition.
   *
   * @param definition workflow definition
   * @return the result indicating success
   */
  Result<Void> registerDefinition(WorkflowDefinition definition);

  /**
   * Registers an action executor.
   *
   * @param actionName action name
   * @param executor action executor
   * @return the result indicating success
   */
  Result<Void> registerActionExecutor(String actionName, ActionExecutor executor);
}
