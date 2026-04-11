package com.marcusprado02.commons.ports.persistence.contract;

import java.util.Optional;

/**
 * Contracts for generic repository operations. Can be implemented for various persistence
 * mechanisms.
 */
public interface Repository<E, I> {

  Optional<E> findById(I id);

  E save(E entity);

  void delete(E entity);

  void deleteById(I id);
}
