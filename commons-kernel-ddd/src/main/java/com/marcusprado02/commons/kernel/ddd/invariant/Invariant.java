package com.marcusprado02.commons.kernel.ddd.invariant;

import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.ProblemThrower;
import java.util.Objects;

public final class Invariant {

  private Invariant() {}

  public static void check(boolean condition, Problem problem) {
    Objects.requireNonNull(problem, "problem");
    if (!condition) {
      ProblemThrower.raise(problem);
    }
  }
}
