package com.marcusprado02.commons.app.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowBranchTest {

  // ── WorkflowInstance.isActive() and isTerminal() ─────────────────────────

  @Test
  void isActive_running_returnsTrue() {
    WorkflowInstance inst = instance(WorkflowInstance.WorkflowStatus.RUNNING);
    assertThat(inst.isActive()).isTrue();
  }

  @Test
  void isActive_compensating_returnsTrue() {
    WorkflowInstance inst = instance(WorkflowInstance.WorkflowStatus.COMPENSATING);
    assertThat(inst.isActive()).isTrue();
  }

  @Test
  void isActive_completed_returnsFalse() {
    WorkflowInstance inst = instance(WorkflowInstance.WorkflowStatus.COMPLETED);
    assertThat(inst.isActive()).isFalse();
  }

  @Test
  void isActive_timedOut_returnsFalse() {
    WorkflowInstance inst = instance(WorkflowInstance.WorkflowStatus.TIMED_OUT);
    assertThat(inst.isActive()).isFalse();
  }

  @Test
  void isTerminal_timedOut_returnsTrue() {
    WorkflowInstance inst = instance(WorkflowInstance.WorkflowStatus.TIMED_OUT);
    assertThat(inst.isTerminal()).isTrue();
  }

  @Test
  void isTerminal_compensated_returnsTrue() {
    WorkflowInstance inst = instance(WorkflowInstance.WorkflowStatus.COMPENSATED);
    assertThat(inst.isTerminal()).isTrue();
  }

  @Test
  void isTerminal_running_returnsFalse() {
    WorkflowInstance inst = instance(WorkflowInstance.WorkflowStatus.RUNNING);
    assertThat(inst.isTerminal()).isFalse();
  }

  @Test
  void isTerminal_pending_returnsFalse() {
    WorkflowInstance inst = instance(WorkflowInstance.WorkflowStatus.PENDING);
    assertThat(inst.isTerminal()).isFalse();
  }

  // ── WorkflowInstance builder with null optional fields ────────────────────

  @Test
  void builder_nullContext_defaultsToEmptyMap() {
    WorkflowInstance inst =
        WorkflowInstance.builder()
            .id("id")
            .definitionId("def")
            .currentState("START")
            .status(WorkflowInstance.WorkflowStatus.PENDING)
            .context(null)
            .build();
    assertThat(inst.context()).isEmpty();
  }

  @Test
  void builder_withCompletedAt_setsOptional() {
    Instant now = Instant.now();
    WorkflowInstance inst =
        WorkflowInstance.builder()
            .id("id")
            .definitionId("def")
            .currentState("END")
            .completedAt(now)
            .error("some error")
            .build();
    assertThat(inst.completedAt()).contains(now);
    assertThat(inst.error()).contains("some error");
  }

  // ── WorkflowDefinition/State/Transition null-normalization branches ───────

  @Test
  void state_nullAction_normalizesToEmpty() {
    WorkflowDefinition.State state =
        WorkflowDefinition.State.builder()
            .name("S1")
            .action((String) null)
            .compensation((String) null)
            .build();
    assertThat(state.action()).isEmpty();
    assertThat(state.compensation()).isEmpty();
  }

  @Test
  void state_withTimeout_setsOptional() {
    WorkflowDefinition.State state =
        WorkflowDefinition.State.builder().name("S1").timeout(Duration.ofSeconds(30)).build();
    assertThat(state.timeout()).contains(Duration.ofSeconds(30));
  }

  @Test
  void transition_nullCondition_normalizesToEmpty() {
    WorkflowDefinition.Transition t =
        WorkflowDefinition.Transition.builder()
            .from("A")
            .to("B")
            .event("go")
            .condition((String) null)
            .build();
    assertThat(t.condition()).isEmpty();
  }

  @Test
  void transition_withCondition_setsOptional() {
    WorkflowDefinition.Transition t =
        WorkflowDefinition.Transition.builder()
            .from("A")
            .to("B")
            .event("go")
            .condition("ctx.ready == true")
            .build();
    assertThat(t.condition()).contains("ctx.ready == true");
  }

  // ── DefaultWorkflowEngine: FAIL state → auto-compensate ──────────────────

  @Test
  void startWorkflow_failState_triggersAutoCompensation() {
    DefaultWorkflowEngine engine = DefaultWorkflowEngine.create();

    WorkflowDefinition def =
        WorkflowDefinition.builder()
            .id("fail-wf")
            .name("Fail Workflow")
            .initialState("FAIL_STATE")
            .state(
                WorkflowDefinition.State.builder()
                    .name("FAIL_STATE")
                    .type(WorkflowDefinition.State.StateType.FAIL)
                    .build())
            .build();

    engine.registerDefinition(def);
    var result = engine.startWorkflow("fail-wf", Map.of());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().status()).isEqualTo(WorkflowInstance.WorkflowStatus.COMPENSATED);
  }

  // ── DefaultWorkflowEngine: compensation with registered executor ──────────

  @Test
  void compensate_withExecutor_executesCompensationAction() {
    DefaultWorkflowEngine engine = DefaultWorkflowEngine.create();

    ActionExecutor compensator = mock(ActionExecutor.class);
    when(compensator.execute(eq("undo-step1"), any())).thenReturn(Result.ok(Map.of()));

    engine.registerActionExecutor("undo-step1", compensator);

    WorkflowDefinition def =
        WorkflowDefinition.builder()
            .id("comp-wf")
            .name("Compensate Workflow")
            .initialState("STEP1")
            .state(
                WorkflowDefinition.State.builder()
                    .name("STEP1")
                    .type(WorkflowDefinition.State.StateType.TASK)
                    .compensation("undo-step1")
                    .build())
            .build();

    engine.registerDefinition(def);

    var startResult = engine.startWorkflow("comp-wf", Map.of());
    assertThat(startResult.isOk()).isTrue();
    String workflowId = startResult.getOrNull().id();

    var compResult = engine.compensate(workflowId);
    assertThat(compResult.isOk()).isTrue();
    assertThat(compResult.getOrNull().status())
        .isEqualTo(WorkflowInstance.WorkflowStatus.COMPENSATED);
  }

  @Test
  void compensate_withExecutorReturningFail_continuesCompensation() {
    DefaultWorkflowEngine engine = DefaultWorkflowEngine.create();

    ActionExecutor failingCompensator = mock(ActionExecutor.class);
    when(failingCompensator.execute(eq("undo-fail"), any()))
        .thenReturn(
            Result.fail(
                Problem.of(
                    ErrorCode.of("COMP_FAIL"),
                    ErrorCategory.TECHNICAL,
                    Severity.ERROR,
                    "compensation failed")));

    engine.registerActionExecutor("undo-fail", failingCompensator);

    WorkflowDefinition def =
        WorkflowDefinition.builder()
            .id("comp-fail-wf")
            .name("Compensation Fail Workflow")
            .initialState("S1")
            .state(
                WorkflowDefinition.State.builder()
                    .name("S1")
                    .type(WorkflowDefinition.State.StateType.TASK)
                    .compensation("undo-fail")
                    .build())
            .build();

    engine.registerDefinition(def);
    var startResult = engine.startWorkflow("comp-fail-wf", Map.of());
    String workflowId = startResult.getOrNull().id();

    var compResult = engine.compensate(workflowId);
    assertThat(compResult.isOk()).isTrue();
    assertThat(compResult.getOrNull().status())
        .isEqualTo(WorkflowInstance.WorkflowStatus.COMPENSATED);
  }

  // ── sendEvent() with condition → no matching transition ──────────────────

  @Test
  void sendEvent_transitionWithCondition_noMatchFound() {
    DefaultWorkflowEngine engine = DefaultWorkflowEngine.create();

    WorkflowDefinition def =
        WorkflowDefinition.builder()
            .id("cond-wf")
            .name("Conditional Workflow")
            .initialState("S1")
            .state(WorkflowDefinition.State.builder().name("S1").build())
            .state(WorkflowDefinition.State.builder().name("S2").build())
            .transition(
                WorkflowDefinition.Transition.builder()
                    .from("S1")
                    .to("S2")
                    .event("go")
                    .condition("impossible-condition")
                    .build())
            .build();

    engine.registerDefinition(def);
    var startResult = engine.startWorkflow("cond-wf", Map.of());
    String workflowId = startResult.getOrNull().id();

    var eventResult = engine.sendEvent(workflowId, "go", Map.of());
    assertThat(eventResult.isFail()).isTrue();
    assertThat(eventResult.problemOrNull().code().value()).isEqualTo("WORKFLOW.NO_TRANSITION");
  }

  // ── getWorkflow() and cancel() ────────────────────────────────────────────

  @Test
  void getWorkflow_notFound_returnsFail() {
    DefaultWorkflowEngine engine = DefaultWorkflowEngine.create();
    var result = engine.getWorkflow("nonexistent-id");
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void cancel_notFound_returnsFail() {
    DefaultWorkflowEngine engine = DefaultWorkflowEngine.create();
    var result = engine.cancel("nonexistent-id", "reason");
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void compensate_notFound_returnsFail() {
    DefaultWorkflowEngine engine = DefaultWorkflowEngine.create();
    var result = engine.compensate("nonexistent-id");
    assertThat(result.isFail()).isTrue();
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  private static WorkflowInstance instance(WorkflowInstance.WorkflowStatus status) {
    return WorkflowInstance.builder()
        .id("id")
        .definitionId("def")
        .currentState("STATE")
        .status(status)
        .build();
  }
}
