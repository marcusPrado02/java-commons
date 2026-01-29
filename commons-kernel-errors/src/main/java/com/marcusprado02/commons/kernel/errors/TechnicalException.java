package com.marcusprado02.commons.kernel.errors;

public class TechnicalException extends RuntimeException {

  public TechnicalException() {
    super();
  }

  public TechnicalException(String message) {
    super(message);
  }

  public TechnicalException(String message, Throwable cause) {
    super(message, cause);
  }
}
