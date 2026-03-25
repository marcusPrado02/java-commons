package com.marcusprado02.commons.kernel.errors;

/** Thrown when a business rule is violated. */
public final class BusinessException extends DomainException {
  public BusinessException(Problem problem) {
    super(problem);
  }

  public BusinessException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
