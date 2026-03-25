package com.marcusprado02.commons.kernel.errors;

/** Thrown when a caller is not authenticated or their credentials are invalid. */
public final class UnauthorizedException extends DomainException {
  public UnauthorizedException(Problem problem) {
    super(problem);
  }

  public UnauthorizedException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
