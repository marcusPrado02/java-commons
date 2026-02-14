package com.marcusprado02.commons.kernel.result;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aggregates multiple Results, collecting all errors.
 *
 * <p>Unlike flatMap which short-circuits on the first error, ValidationResult collects all errors
 * from multiple validations.
 *
 * @param <T> value type
 */
public final class ValidationResult<T> {

  private final T value;
  private final List<Problem> problems;

  private ValidationResult(T value, List<Problem> problems) {
    this.value = value;
    this.problems = problems;
  }

  /**
   * Creates a successful ValidationResult.
   *
   * @param value the value
   * @param <T> value type
   * @return validation result with no errors
   */
  public static <T> ValidationResult<T> valid(T value) {
    return new ValidationResult<>(value, Collections.emptyList());
  }

  /**
   * Creates a failed ValidationResult with a single error.
   *
   * @param problem the problem
   * @param <T> value type
   * @return validation result with one error
   */
  public static <T> ValidationResult<T> invalid(Problem problem) {
    Objects.requireNonNull(problem);
    return new ValidationResult<>(null, List.of(problem));
  }

  /**
   * Creates a failed ValidationResult with multiple errors.
   *
   * @param problems the problems
   * @param <T> value type
   * @return validation result with multiple errors
   */
  public static <T> ValidationResult<T> invalid(List<Problem> problems) {
    Objects.requireNonNull(problems);
    if (problems.isEmpty()) {
      throw new IllegalArgumentException("Problems list cannot be empty");
    }
    return new ValidationResult<>(null, new ArrayList<>(problems));
  }

  /**
   * Creates a ValidationResult from a Result.
   *
   * @param result the result
   * @param <T> value type
   * @return validation result
   */
  public static <T> ValidationResult<T> of(Result<T> result) {
    Objects.requireNonNull(result);
    if (result.isOk()) {
      return valid(result.getOrNull());
    }
    return invalid(result.problemOrNull());
  }

  /**
   * Checks if this ValidationResult is valid (no errors).
   *
   * @return true if valid
   */
  public boolean isValid() {
    return problems.isEmpty();
  }

  /**
   * Checks if this ValidationResult is invalid (has errors).
   *
   * @return true if invalid
   */
  public boolean isInvalid() {
    return !problems.isEmpty();
  }

  /**
   * Returns the value (null if invalid).
   *
   * @return the value or null
   */
  public T getValue() {
    return value;
  }

  /**
   * Returns the list of problems (empty if valid).
   *
   * @return immutable list of problems
   */
  public List<Problem> getProblems() {
    return Collections.unmodifiableList(problems);
  }

  /**
   * Combines this ValidationResult with another, collecting all errors.
   *
   * @param other the other validation result
   * @param combiner function to combine values if both are valid
   * @param <U> other value type
   * @param <R> result value type
   * @return combined validation result
   */
  public <U, R> ValidationResult<R> combine(
      ValidationResult<U> other, Function<T, Function<U, R>> combiner) {
    Objects.requireNonNull(other);
    Objects.requireNonNull(combiner);

    List<Problem> allProblems = new ArrayList<>();
    allProblems.addAll(this.problems);
    allProblems.addAll(other.problems);

    if (allProblems.isEmpty()) {
      R result = combiner.apply(this.value).apply(other.value);
      return ValidationResult.valid(result);
    }

    return ValidationResult.invalid(allProblems);
  }

  /**
   * Maps the value if valid.
   *
   * @param fn mapping function
   * @param <U> new value type
   * @return new validation result
   */
  public <U> ValidationResult<U> map(Function<T, U> fn) {
    Objects.requireNonNull(fn);
    if (isValid()) {
      return ValidationResult.valid(fn.apply(value));
    }
    return ValidationResult.invalid(problems);
  }

  /**
   * Adds a validation step, collecting errors.
   *
   * @param fn validation function
   * @param <U> new value type
   * @return new validation result
   */
  public <U> ValidationResult<U> andThen(Function<T, ValidationResult<U>> fn) {
    Objects.requireNonNull(fn);
    if (isInvalid()) {
      return ValidationResult.invalid(problems);
    }

    ValidationResult<U> next = fn.apply(value);
    if (next.isInvalid()) {
      return next;
    }

    return next;
  }

  /**
   * Converts to a Result.
   *
   * <p>If there are multiple Problems, they are combined into a single Problem.
   *
   * @param combiner function to combine multiple Problems into one
   * @return result
   */
  public Result<T> toResult(Function<List<Problem>, Problem> combiner) {
    Objects.requireNonNull(combiner);
    if (isValid()) {
      return Result.ok(value);
    }
    return Result.fail(combiner.apply(problems));
  }

  /**
   * Converts to a Result using a default combiner.
   *
   * <p>The default combiner creates a Problem with title "Validation failed" and details listing
   * all problems.
   *
   * @return result
   */
  public Result<T> toResult() {
    return toResult(ValidationResult::combineProblems);
  }

  private static Problem combineProblems(List<Problem> problems) {
    if (problems.size() == 1) {
      return problems.get(0);
    }

    String detailsText =
        problems.stream()
            .map(
                p ->
                    "- "
                        + p.message()
                        + (p.details() != null && !p.details().isEmpty() ? ": " + p.details() : ""))
            .collect(Collectors.joining("\n"));

    return Problem.of(
        ErrorCode.of("VALIDATION.MULTIPLE_ERRORS"),
        ErrorCategory.VALIDATION,
        Severity.ERROR,
        "Validation failed: " + detailsText);
  }

  /** Builder for combining multiple ValidationResults. */
  public static final class Builder {
    private final List<Problem> problems = new ArrayList<>();

    /**
     * Adds a Result to the builder.
     *
     * @param result the result
     * @return this builder
     */
    public Builder add(Result<?> result) {
      Objects.requireNonNull(result);
      if (result.isFail()) {
        problems.add(result.problemOrNull());
      }
      return this;
    }

    /**
     * Adds a ValidationResult to the builder.
     *
     * @param validation the validation result
     * @return this builder
     */
    public Builder add(ValidationResult<?> validation) {
      Objects.requireNonNull(validation);
      if (validation.isInvalid()) {
        problems.addAll(validation.problems);
      }
      return this;
    }

    /**
     * Builds a ValidationResult with the accumulated value.
     *
     * @param value the value if all validations passed
     * @param <T> value type
     * @return validation result
     */
    public <T> ValidationResult<T> build(T value) {
      if (problems.isEmpty()) {
        return ValidationResult.valid(value);
      }
      return ValidationResult.invalid(problems);
    }

    /**
     * Builds a ValidationResult, using the supplier only if all validations passed.
     *
     * @param valueSupplier supplier for the value
     * @param <T> value type
     * @return validation result
     */
    public <T> ValidationResult<T> build(java.util.function.Supplier<T> valueSupplier) {
      Objects.requireNonNull(valueSupplier);
      if (problems.isEmpty()) {
        return ValidationResult.valid(valueSupplier.get());
      }
      return ValidationResult.invalid(problems);
    }
  }

  /**
   * Creates a new Builder.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }
}
