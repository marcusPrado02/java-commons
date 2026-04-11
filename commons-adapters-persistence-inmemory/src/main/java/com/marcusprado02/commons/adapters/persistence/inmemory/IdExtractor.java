package com.marcusprado02.commons.adapters.persistence.inmemory;

/**
 * Extracts the ID from an entity of type {@code E}.
 *
 * @param <E> the entity type
 * @param <I> the ID type
 */
public interface IdExtractor<E, I> {

  /**
   * Returns the ID of the given entity.
   *
   * @param entity the entity
   * @return the entity ID
   */
  I getId(E entity);
}
