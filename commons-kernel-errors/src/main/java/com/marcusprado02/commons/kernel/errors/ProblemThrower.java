package com.marcusprado02.commons.kernel.errors;

import java.util.Objects;

public final class ProblemThrower {

  private ProblemThrower() {}

  public static void raise(Problem problem) {
    throw ProblemExceptions.from(Objects.requireNonNull(problem, "problem"));
  }
}
