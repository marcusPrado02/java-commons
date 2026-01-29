package com.marcusprado02.commons.kernel.errors;

public class BusinessException extends DomainException {

  public BusinessException() {
    super();
  }

  public BusinessException(String message) {
    super(message);
  }

  public BusinessException(String message, Throwable cause) {
    super(message, cause);
  }
}
