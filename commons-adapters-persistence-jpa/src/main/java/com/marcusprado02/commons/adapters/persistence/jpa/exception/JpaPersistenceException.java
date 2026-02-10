package com.marcusprado02.commons.adapters.persistence.jpa.exception;

public class JpaPersistenceException extends RuntimeException {
  public JpaPersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
