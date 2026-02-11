package com.marcusprado02.commons.adapters.persistence.inmemory;

/**
 * Extrai ID de uma entidade.
 */
public interface IdExtractor<E, ID> {
    ID getId(E entity);
}
