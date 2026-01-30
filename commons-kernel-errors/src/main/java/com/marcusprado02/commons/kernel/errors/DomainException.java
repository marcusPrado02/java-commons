package com.marcusprado02.commons.kernel.errors;

public class DomainException extends RuntimeException {

  private final Problem problem;

  public DomainException(Problem problem) {
    super(problem == null ? null : problem.message());
    this.problem = problem;
  }

  public DomainException(Problem problem, Throwable cause) {
    super(problem == null ? null : problem.message(), cause);
    this.problem = problem;
  }

  public Problem problem() {
    return problem;
  }
}
