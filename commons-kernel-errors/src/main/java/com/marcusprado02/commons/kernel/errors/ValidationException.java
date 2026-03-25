package com.marcusprado02.commons.kernel.errors;

/** Thrown when input validation fails. */
public final class ValidationException extends DomainException {
  public ValidationException(Problem problem) {
    super(problem);
  }

  public ValidationException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
