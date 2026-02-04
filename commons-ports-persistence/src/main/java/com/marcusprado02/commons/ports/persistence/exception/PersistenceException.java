package com.marcusprado02.commons.ports.persistence.exception;

/**
 * Class representing a generic persistence exception.
 */
public class PersistenceException extends RuntimeException {
    public PersistenceException(String message, Throwable cause) {
       super(message, cause);
    }
}
