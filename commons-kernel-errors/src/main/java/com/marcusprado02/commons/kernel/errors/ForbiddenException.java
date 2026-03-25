package com.marcusprado02.commons.kernel.errors;

/** Thrown when the caller is authenticated but lacks the required permission. */
public final class ForbiddenException extends DomainException {
  public ForbiddenException(Problem problem) {
    super(problem);
  }

  public ForbiddenException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
