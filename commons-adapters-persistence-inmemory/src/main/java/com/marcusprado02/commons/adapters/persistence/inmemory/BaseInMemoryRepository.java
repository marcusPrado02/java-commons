package com.marcusprado02.commons.adapters.persistence.inmemory;

import com.marcusprado02.commons.ports.persistence.contract.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base in-memory implementation of {@code Repository} using entity type {@code E} and ID type
 * {@code I}.
 *
 * @param <E> the entity type
 * @param <I> the ID type
 */
public class BaseInMemoryRepository<E, I> implements Repository<E, I> {

  /** In-memory storage map keyed by entity ID. */
  protected final Map<I, E> storage = new ConcurrentHashMap<>();

  private final IdExtractor<E, I> idExtractor;

  /**
   * Creates a new repository with the given ID extractor.
   *
   * @param idExtractor the strategy to extract the ID from an entity
   */
  public BaseInMemoryRepository(IdExtractor<E, I> idExtractor) {
    this.idExtractor = idExtractor;
  }

  @Override
  public Optional<E> findById(I id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public E save(E entity) {
    I id = idExtractor.getId(entity);
    storage.put(id, entity);
    return entity;
  }

  @Override
  public void delete(E entity) {
    I id = idExtractor.getId(entity);
    storage.remove(id);
  }

  @Override
  public void deleteById(I id) {
    storage.remove(id);
  }
}
