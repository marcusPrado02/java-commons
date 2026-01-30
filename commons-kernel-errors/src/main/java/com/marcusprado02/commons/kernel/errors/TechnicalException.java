package com.marcusprado02.commons.kernel.errors;

public final class TechnicalException extends DomainException {
  public TechnicalException(Problem problem) {
    super(problem);
  }

  public TechnicalException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
