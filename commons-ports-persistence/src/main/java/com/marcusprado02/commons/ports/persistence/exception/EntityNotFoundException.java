package com.marcusprado02.commons.ports.persistence.exception;

/**
 * Class representing an exception when an entity is not found.
 */
public class EntityNotFoundException extends PersistenceException {
    public EntityNotFoundException(String id) {
        super("Entity not found: " + id, null);
    }
}
