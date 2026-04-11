package com.marcusprado02.commons.adapters.persistence.inmemory;

/** Factory for creating in-memory repository instances. */
public final class InMemoryRepositoryFactory {

  private InMemoryRepositoryFactory() {}

  /**
   * Creates an in-memory {@code Repository}.
   *
   * @param <E> the entity type
   * @param <I> the ID type
   * @param extractor the strategy to extract the ID from an entity
   * @return a new {@code BaseInMemoryRepository}
   */
  public static <E, I> BaseInMemoryRepository<E, I> createRepository(IdExtractor<E, I> extractor) {
    return new BaseInMemoryRepository<>(extractor);
  }

  /**
   * Creates an in-memory {@code PageableRepository}.
   *
   * @param <E> the entity type
   * @param <I> the ID type
   * @param extractor the strategy to extract the ID from an entity
   * @return a new {@code InMemoryPageableRepository}
   */
  public static <E, I> InMemoryPageableRepository<E, I> createPageableRepository(
      IdExtractor<E, I> extractor) {
    return new InMemoryPageableRepository<>(extractor);
  }
}
