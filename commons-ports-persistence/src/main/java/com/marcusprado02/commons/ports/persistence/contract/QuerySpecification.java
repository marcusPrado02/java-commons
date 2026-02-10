package com.marcusprado02.commons.ports.persistence.contract;

/** Abstraction for query specifications. Used to define criteria for querying entities. */
public interface QuerySpecification<E> {
  boolean isSatisfiedBy(E entity);
}
