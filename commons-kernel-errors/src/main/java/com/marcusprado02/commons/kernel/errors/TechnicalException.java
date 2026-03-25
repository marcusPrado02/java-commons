package com.marcusprado02.commons.kernel.errors;

/** Thrown for infrastructure or technical failures (e.g., database, external service). */
public final class TechnicalException extends DomainException {
  public TechnicalException(Problem problem) {
    super(problem);
  }

  public TechnicalException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
