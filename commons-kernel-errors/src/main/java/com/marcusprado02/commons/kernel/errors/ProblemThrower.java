package com.marcusprado02.commons.kernel.errors;

import java.util.Objects;

/** Utility that converts a {@link Problem} into a typed exception and throws it immediately. */
public final class ProblemThrower {

  private ProblemThrower() {}

  public static void raise(Problem problem) {
    throw ProblemExceptions.from(Objects.requireNonNull(problem, "problem"));
  }
}
