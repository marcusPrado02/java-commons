package com.marcusprado02.commons.kernel.errors;

public class ConflictException extends DomainException {

  public ConflictException() {
    super();
  }

  public ConflictException(String message) {
    super(message);
  }

  public ConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}
