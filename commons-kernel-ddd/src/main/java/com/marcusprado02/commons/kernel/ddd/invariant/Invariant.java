package com.marcusprado02.commons.kernel.ddd.invariant;

import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.ProblemThrower;
import java.util.Objects;

/** Utility for enforcing domain invariants by throwing when a condition is not satisfied. */
public final class Invariant {

  private Invariant() {}

  /**
   * Asserts that the given condition holds; raises the problem if it does not.
   *
   * @param condition the invariant condition to check
   * @param problem the problem to raise when the condition is false
   */
  public static void check(boolean condition, Problem problem) {
    Objects.requireNonNull(problem, "problem");
    if (!condition) {
      ProblemThrower.raise(problem);
    }
  }
}
