package com.marcusprado02.commons.kernel.errors;

public final class ConflictException extends DomainException {
  public ConflictException(Problem problem) {
    super(problem);
  }

  public ConflictException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
