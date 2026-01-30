package com.marcusprado02.commons.kernel.errors;

public final class NotFoundException extends DomainException {
  public NotFoundException(Problem problem) {
    super(problem);
  }

  public NotFoundException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
