package com.marcusprado02.commons.kernel.errors;

public final class ValidationException extends DomainException {
  public ValidationException(Problem problem) {
    super(problem);
  }

  public ValidationException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
