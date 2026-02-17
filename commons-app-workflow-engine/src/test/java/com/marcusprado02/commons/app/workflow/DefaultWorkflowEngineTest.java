package com.marcusprado02.commons.app.workflow;

import static org.assertj.core.api.Assertions.*;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.result.Result;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultWorkflowEngineTest {

  private DefaultWorkflowEngine engine;

  @BeforeEach
  void setUp() {
    engine = DefaultWorkflowEngine.create();
  }

  @Test
  void shouldCreateEngine() {
    assertThat(engine).isNotNull();
  }

  @Test
  void shouldRegisterWorkflowDefinition() {
    // Given
    var definition =
        WorkflowDefinition.builder()
            .id("test-workflow")
            .name("Test Workflow")
            .description("A simple test workflow")
            .initialState("start")
            .state(
                WorkflowDefinition.State.builder()
                    .name("start")
                    .type(WorkflowDefinition.State.StateType.START)
                    .build())
            .state(
                WorkflowDefinition.State.builder()
                    .name("end")
                    .type(WorkflowDefinition.State.StateType.END)
                    .build())
            .transition(
                WorkflowDefinition.Transition.builder()
                    .from("start")
                    .to("end")
                    .event("complete")
                    .build())
            .build();

    // When
    var result = engine.registerDefinition(definition);

    // Then
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void shouldStartWorkflow() {
    // Given
    var definition = createSimpleWorkflow();
    engine.registerDefinition(definition);

    // When
    var result = engine.startWorkflow("simple-workflow", Map.of("key", "value"));

    // Then
    assertThat(result.isOk()).isTrue();
    var instance = result.getOrNull();
    assertThat(instance).isNotNull();
    assertThat(instance.id()).isNotEmpty();
    assertThat(instance.definitionId()).isEqualTo("simple-workflow");
    assertThat(instance.currentState()).isEqualTo("start");
    assertThat(instance.status()).isEqualTo(WorkflowInstance.WorkflowStatus.RUNNING);
    assertThat(instance.context()).containsEntry("key", "value");
    assertThat(instance.isActive()).isTrue();
    assertThat(instance.isTerminal()).isFalse();
  }

  @Test
  void shouldFailToStartWorkflowWithUnknownDefinition() {
    // When
    var result = engine.startWorkflow("unknown-workflow", Map.of());

    // Then
    assertThat(result.isFail()).isTrue();
    var problem = result.problemOrNull();
    assertThat(problem.code().value()).isEqualTo("WORKFLOW.DEFINITION_NOT_FOUND");
    assertThat(problem.category()).isEqualTo(ErrorCategory.NOT_FOUND);
  }

  @Test
  void shouldHandleStateTransitions() {
    // Given
    var definition = createSimpleWorkflow();
    engine.registerDefinition(definition);
    var startResult = engine.startWorkflow("simple-workflow", Map.of());
    var workflowId = startResult.getOrNull().id();

    // When
    var result = engine.sendEvent(workflowId, "complete", Map.of("result", "success"));

    // Then
    assertThat(result.isOk()).isTrue();
    var instance = result.getOrNull();
    assertThat(instance.currentState()).isEqualTo("end");
    assertThat(instance.status()).isEqualTo(WorkflowInstance.WorkflowStatus.COMPLETED);
    assertThat(instance.context()).containsEntry("result", "success");
    assertThat(instance.isTerminal()).isTrue();
    assertThat(instance.completedAt()).isPresent();
  }

  @Test
  void shouldFailTransitionWithInvalidEvent() {
    // Given
    var definition = createSimpleWorkflow();
    engine.registerDefinition(definition);
    var startResult = engine.startWorkflow("simple-workflow", Map.of());
    var workflowId = startResult.getOrNull().id();

    // When
    var result = engine.sendEvent(workflowId, "invalid-event", Map.of());

    // Then
    assertThat(result.isFail()).isTrue();
    var problem = result.problemOrNull();
    assertThat(problem.code().value()).isEqualTo("WORKFLOW.NO_TRANSITION");
  }

  @Test
  void shouldRetrieveWorkflowInstance() {
    // Given
    var definition = createSimpleWorkflow();
    engine.registerDefinition(definition);
    var startResult = engine.startWorkflow("simple-workflow", Map.of("key", "value"));
    var workflowId = startResult.getOrNull().id();

    // When
    var result = engine.getWorkflow(workflowId);

    // Then
    assertThat(result.isOk()).isTrue();
    var instance = result.getOrNull();
    assertThat(instance.id()).isEqualTo(workflowId);
    assertThat(instance.context()).containsEntry("key", "value");
  }

  @Test
  void shouldFailToRetrieveUnknownWorkflow() {
    // When
    var result = engine.getWorkflow("unknown-id");

    // Then
    assertThat(result.isFail()).isTrue();
    var problem = result.problemOrNull();
    assertThat(problem.code().value()).isEqualTo("WORKFLOW.INSTANCE_NOT_FOUND");
  }

  @Test
  void shouldExecuteActionsInState() {
    // Given
    var definition =
        WorkflowDefinition.builder()
            .id("action-workflow")
            .name("Action Workflow")
            .initialState("start")
            .state(
                WorkflowDefinition.State.builder()
                    .name("start")
                    .type(WorkflowDefinition.State.StateType.TASK)
                    .action("greet")
                    .build())
            .state(
                WorkflowDefinition.State.builder()
                    .name("end")
                    .type(WorkflowDefinition.State.StateType.END)
                    .build())
            .transition(
                WorkflowDefinition.Transition.builder()
                    .from("start")
                    .to("end")
                    .event("done")
                    .build())
            .build();

    engine.registerDefinition(definition);

    var actionExecuted = new boolean[] {false};
    engine.registerActionExecutor(
        "greet",
        (actionName, context) -> {
          actionExecuted[0] = true;
          return Result.ok(Map.of("greeting", "Hello World"));
        });

    // When
    var result = engine.startWorkflow("action-workflow", Map.of());

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(actionExecuted[0]).isTrue();
    var instance = result.getOrNull();
    assertThat(instance.context()).containsEntry("greeting", "Hello World");
  }

  @Test
  void shouldHandleActionFailureWithCompensation() {
    // Given
    var definition =
        WorkflowDefinition.builder()
            .id("failing-workflow")
            .name("Failing Workflow")
            .initialState("start")
            .state(
                WorkflowDefinition.State.builder()
                    .name("start")
                    .type(WorkflowDefinition.State.StateType.TASK)
                    .action("failing-action")
                    .compensation("compensate-start")
                    .build())
            .state(
                WorkflowDefinition.State.builder()
                    .name("end")
                    .type(WorkflowDefinition.State.StateType.END)
                    .build())
            .build();

    engine.registerDefinition(definition);

    var compensationExecuted = new boolean[] {false};
    engine.registerActionExecutor(
        "failing-action",
        (actionName, context) ->
            Result.fail(
                com.marcusprado02.commons.kernel.errors.Problem.of(
                    com.marcusprado02.commons.kernel.errors.ErrorCode.of("ACTION.FAILED"),
                    ErrorCategory.BUSINESS,
                    com.marcusprado02.commons.kernel.errors.Severity.ERROR,
                    "Action failed intentionally")));

    engine.registerActionExecutor(
        "compensate-start",
        (actionName, context) -> {
          compensationExecuted[0] = true;
          return Result.ok(Map.of("compensated", true));
        });

    // When
    var result = engine.startWorkflow("failing-workflow", Map.of());

    // Then
    assertThat(result.isOk()).isTrue();
    var instance = result.getOrNull();
    assertThat(instance.status()).isEqualTo(WorkflowInstance.WorkflowStatus.COMPENSATED);
    assertThat(compensationExecuted[0]).isTrue();
    assertThat(instance.context()).containsEntry("compensated", true);
  }

  @Test
  void shouldManuallyCompensateWorkflow() {
    // Given
    var definition =
        WorkflowDefinition.builder()
            .id("compensable-workflow")
            .name("Compensable Workflow")
            .initialState("task1")
            .state(
                WorkflowDefinition.State.builder()
                    .name("task1")
                    .type(WorkflowDefinition.State.StateType.TASK)
                    .action("action1")
                    .compensation("compensate1")
                    .build())
            .state(
                WorkflowDefinition.State.builder()
                    .name("task2")
                    .type(WorkflowDefinition.State.StateType.TASK)
                    .action("action2")
                    .compensation("compensate2")
                    .build())
            .state(
                WorkflowDefinition.State.builder()
                    .name("end")
                    .type(WorkflowDefinition.State.StateType.END)
                    .build())
            .transition(
                WorkflowDefinition.Transition.builder()
                    .from("task1")
                    .to("task2")
                    .event("next")
                    .build())
            .transition(
                WorkflowDefinition.Transition.builder()
                    .from("task2")
                    .to("end")
                    .event("complete")
                    .build())
            .build();

    engine.registerDefinition(definition);

    var compensations = new ArrayList<String>();
    engine.registerActionExecutor(
        "action1", (actionName, ctx) -> Result.ok(Map.of("action1", "done")));
    engine.registerActionExecutor(
        "action2", (actionName, ctx) -> Result.ok(Map.of("action2", "done")));
    engine.registerActionExecutor(
        "compensate1",
        (actionName, ctx) -> {
          compensations.add("compensate1");
          return Result.ok(Map.of());
        });
    engine.registerActionExecutor(
        "compensate2",
        (actionName, ctx) -> {
          compensations.add("compensate2");
          return Result.ok(Map.of());
        });

    var startResult = engine.startWorkflow("compensable-workflow", Map.of());
    var workflowId = startResult.getOrNull().id();
    engine.sendEvent(workflowId, "next", Map.of());

    // When
    var result = engine.compensate(workflowId);

    // Then
    assertThat(result.isOk()).isTrue();
    var instance = result.getOrNull();
    assertThat(instance.status()).isEqualTo(WorkflowInstance.WorkflowStatus.COMPENSATED);
    // Compensations executed in reverse order
    assertThat(compensations).containsExactly("compensate2", "compensate1");
  }

  @Test
  void shouldCancelWorkflow() {
    // Given
    var definition = createSimpleWorkflow();
    engine.registerDefinition(definition);
    var startResult = engine.startWorkflow("simple-workflow", Map.of());
    var workflowId = startResult.getOrNull().id();

    // When
    var result = engine.cancel(workflowId, "User cancelled");

    // Then
    assertThat(result.isOk()).isTrue();
    var instance = result.getOrNull();
    assertThat(instance.status()).isEqualTo(WorkflowInstance.WorkflowStatus.FAILED);
    assertThat(instance.error()).hasValue("Cancelled: User cancelled");
    assertThat(instance.completedAt()).isPresent();
  }

  @Test
  void shouldHandleComplexWorkflowWithMultipleTransitions() {
    // Given
    var definition =
        WorkflowDefinition.builder()
            .id("complex-workflow")
            .name("Complex Workflow")
            .initialState("start")
            .state(
                WorkflowDefinition.State.builder()
                    .name("start")
                    .type(WorkflowDefinition.State.StateType.START)
                    .build())
            .state(
                WorkflowDefinition.State.builder()
                    .name("task1")
                    .type(WorkflowDefinition.State.StateType.TASK)
                    .action("action1")
                    .build())
            .state(
                WorkflowDefinition.State.builder()
                    .name("task2")
                    .type(WorkflowDefinition.State.StateType.TASK)
                    .action("action2")
                    .build())
            .state(
                WorkflowDefinition.State.builder()
                    .name("end")
                    .type(WorkflowDefinition.State.StateType.END)
                    .build())
            .transition(
                WorkflowDefinition.Transition.builder()
                    .from("start")
                    .to("task1")
                    .event("begin")
                    .build())
            .transition(
                WorkflowDefinition.Transition.builder()
                    .from("task1")
                    .to("task2")
                    .event("next")
                    .build())
            .transition(
                WorkflowDefinition.Transition.builder()
                    .from("task2")
                    .to("end")
                    .event("finish")
                    .build())
            .build();

    engine.registerDefinition(definition);
    engine.registerActionExecutor("action1", (actionName, ctx) -> Result.ok(Map.of("step", 1)));
    engine.registerActionExecutor("action2", (actionName, ctx) -> Result.ok(Map.of("step", 2)));

    // When
    var startResult = engine.startWorkflow("complex-workflow", Map.of());
    var workflowId = startResult.getOrNull().id();

    var step1 = engine.sendEvent(workflowId, "begin", Map.of());
    var step2 = engine.sendEvent(workflowId, "next", Map.of());
    var step3 = engine.sendEvent(workflowId, "finish", Map.of());

    // Then
    assertThat(step1.isOk()).isTrue();
    assertThat(step1.getOrNull().currentState()).isEqualTo("task1");

    assertThat(step2.isOk()).isTrue();
    assertThat(step2.getOrNull().currentState()).isEqualTo("task2");

    assertThat(step3.isOk()).isTrue();
    assertThat(step3.getOrNull().currentState()).isEqualTo("end");
    assertThat(step3.getOrNull().status()).isEqualTo(WorkflowInstance.WorkflowStatus.COMPLETED);
  }

  @Test
  void shouldNotAllowEventsOnTerminalState() {
    // Given
    var definition = createSimpleWorkflow();
    engine.registerDefinition(definition);
    var startResult = engine.startWorkflow("simple-workflow", Map.of());
    var workflowId = startResult.getOrNull().id();
    engine.sendEvent(workflowId, "complete", Map.of()); // Completes workflow

    // When
    var result = engine.sendEvent(workflowId, "complete", Map.of());

    // Then
    assertThat(result.isFail()).isTrue();
    var problem = result.problemOrNull();
    assertThat(problem.code().value()).isEqualTo("WORKFLOW.ALREADY_TERMINAL");
  }

  private WorkflowDefinition createSimpleWorkflow() {
    return WorkflowDefinition.builder()
        .id("simple-workflow")
        .name("Simple Workflow")
        .description("A simple two-state workflow")
        .initialState("start")
        .state(
            WorkflowDefinition.State.builder()
                .name("start")
                .type(WorkflowDefinition.State.StateType.START)
                .build())
        .state(
            WorkflowDefinition.State.builder()
                .name("end")
                .type(WorkflowDefinition.State.StateType.END)
                .build())
        .transition(
            WorkflowDefinition.Transition.builder()
                .from("start")
                .to("end")
                .event("complete")
                .build())
        .timeout(Duration.ofMinutes(5))
        .build();
  }
}
