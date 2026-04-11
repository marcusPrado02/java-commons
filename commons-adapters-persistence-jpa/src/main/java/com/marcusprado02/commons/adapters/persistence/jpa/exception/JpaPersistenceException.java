package com.marcusprado02.commons.adapters.persistence.jpa.exception;

/** Exception thrown when a JPA persistence operation fails. */
public class JpaPersistenceException extends RuntimeException {
  public JpaPersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
