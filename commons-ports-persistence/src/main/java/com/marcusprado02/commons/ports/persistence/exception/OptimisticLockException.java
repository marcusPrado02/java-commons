package com.marcusprado02.commons.ports.persistence.exception;

/**
 * Class representing an optimistic lock exception.
 */
public class OptimisticLockException extends PersistenceException {
    public OptimisticLockException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
