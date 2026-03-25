package com.marcusprado02.commons.kernel.errors;

/** Thrown when a resource conflict is detected (e.g., optimistic lock or duplicate entry). */
public final class ConflictException extends DomainException {
  public ConflictException(Problem problem) {
    super(problem);
  }

  public ConflictException(Problem problem, Throwable cause) {
    super(problem, cause);
  }
}
