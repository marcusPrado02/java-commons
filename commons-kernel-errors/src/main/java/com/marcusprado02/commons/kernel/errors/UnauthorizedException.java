package com.marcusprado02.commons.kernel.errors;

public final class UnauthorizedException extends DomainException {
  public UnauthorizedException(Problem problem) {
    super(problem);
  }

  public UnauthorizedException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
