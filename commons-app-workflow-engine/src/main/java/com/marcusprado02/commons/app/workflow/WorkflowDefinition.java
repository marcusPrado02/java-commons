package com.marcusprado02.commons.app.workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Defines a workflow with states and transitions.
 *
 * @param id workflow definition ID
 * @param name workflow name
 * @param description workflow description
 * @param initialState initial state
 * @param states list of all states
 * @param transitions list of all transitions
 * @param timeout optional global timeout
 */
public record WorkflowDefinition(
    String id,
    String name,
    String description,
    String initialState,
    List<State> states,
    List<Transition> transitions,
    Optional<Duration> timeout) {

  public WorkflowDefinition {
    states = states == null ? List.of() : List.copyOf(states);
    transitions = transitions == null ? List.of() : List.copyOf(transitions);
    timeout = timeout == null ? Optional.empty() : timeout;
  }

  /**
   * Represents a workflow state.
   *
   * @param name state name
   * @param type state type
   * @param action action to execute
   * @param compensation compensation action
   * @param timeout state-specific timeout
   */
  public record State(
      String name,
      StateType type,
      Optional<String> action,
      Optional<String> compensation,
      Optional<Duration> timeout) {

    public State {
      action = action == null ? Optional.empty() : action;
      compensation = compensation == null ? Optional.empty() : compensation;
      timeout = timeout == null ? Optional.empty() : timeout;
    }

    /** State type. */
    public enum StateType {
      START,
      TASK,
      CHOICE,
      PARALLEL,
      END,
      FAIL
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private String name;
      private StateType type = StateType.TASK;
      private Optional<String> action = Optional.empty();
      private Optional<String> compensation = Optional.empty();
      private Optional<Duration> timeout = Optional.empty();

      public Builder name(String name) {
        this.name = name;
        return this;
      }

      public Builder type(StateType type) {
        this.type = type;
        return this;
      }

      public Builder action(String action) {
        this.action = Optional.ofNullable(action);
        return this;
      }

      public Builder compensation(String compensation) {
        this.compensation = Optional.ofNullable(compensation);
        return this;
      }

      public Builder timeout(Duration timeout) {
        this.timeout = Optional.ofNullable(timeout);
        return this;
      }

      public State build() {
        return new State(name, type, action, compensation, timeout);
      }
    }
  }

  /**
   * Represents a state transition.
   *
   * @param from source state
   * @param to target state
   * @param event event that triggers transition
   * @param condition optional condition
   */
  public record Transition(String from, String to, String event, Optional<String> condition) {

    public Transition {
      condition = condition == null ? Optional.empty() : condition;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private String from;
      private String to;
      private String event;
      private Optional<String> condition = Optional.empty();

      public Builder from(String from) {
        this.from = from;
        return this;
      }

      public Builder to(String to) {
        this.to = to;
        return this;
      }

      public Builder event(String event) {
        this.event = event;
        return this;
      }

      public Builder condition(String condition) {
        this.condition = Optional.ofNullable(condition);
        return this;
      }

      public Transition build() {
        return new Transition(from, to, event, condition);
      }
    }
  }

  /**
   * Creates a builder for WorkflowDefinition.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for WorkflowDefinition. */
  public static class Builder {
    private String id;
    private String name;
    private String description;
    private String initialState;
    private List<State> states = new ArrayList<>();
    private List<Transition> transitions = new ArrayList<>();
    private Optional<Duration> timeout = Optional.empty();

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder initialState(String initialState) {
      this.initialState = initialState;
      return this;
    }

    public Builder state(State state) {
      this.states.add(state);
      return this;
    }

    public Builder states(List<State> states) {
      this.states = new ArrayList<>(states);
      return this;
    }

    public Builder transition(Transition transition) {
      this.transitions.add(transition);
      return this;
    }

    public Builder transitions(List<Transition> transitions) {
      this.transitions = new ArrayList<>(transitions);
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.timeout = Optional.ofNullable(timeout);
      return this;
    }

    public WorkflowDefinition build() {
      return new WorkflowDefinition(
          id, name, description, initialState, states, transitions, timeout);
    }
  }
}
