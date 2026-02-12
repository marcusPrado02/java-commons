package com.marcusprado02.commons.adapters.persistence.inmemory;

/** Fábrica de repositórios em memória. */
public final class InMemoryRepositoryFactory {

  private InMemoryRepositoryFactory() {}

  /** Cria um Repository em memória. */
  public static <E, ID> BaseInMemoryRepository<E, ID> createRepository(
      IdExtractor<E, ID> extractor) {
    return new BaseInMemoryRepository<>(extractor);
  }

  /** Cria um PageableRepository em memória. */
  public static <E, ID> InMemoryPageableRepository<E, ID> createPageableRepository(
      IdExtractor<E, ID> extractor) {
    return new InMemoryPageableRepository<>(extractor);
  }
}
