package com.marcusprado02.commons.kernel.errors;

public class ValidationException extends DomainException {

  public ValidationException() {
    super();
  }

  public ValidationException(String message) {
    super(message);
  }

  public ValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
