package com.marcusprado02.commons.kernel.errors;

public final class ForbiddenException extends DomainException {
  public ForbiddenException(Problem problem) {
    super(problem);
  }

  public ForbiddenException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
