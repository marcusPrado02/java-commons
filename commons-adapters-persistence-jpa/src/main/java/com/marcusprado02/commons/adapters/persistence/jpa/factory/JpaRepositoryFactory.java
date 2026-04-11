package com.marcusprado02.commons.adapters.persistence.jpa.factory;

import com.marcusprado02.commons.adapters.persistence.jpa.repository.PageableJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Map;

/** Factory for creating JPA repositories and entity managers. */
public final class JpaRepositoryFactory {

  private static final String DEFAULT_PERSISTENCE_UNIT = "defaultPU";

  private JpaRepositoryFactory() {}

  /** Creates an EntityManagerFactory from properties (e.g., URL, driver, dialect). */
  public static EntityManagerFactory createEntityManagerFactory(
      String persistenceUnit, Map<String, Object> properties) {

    return Persistence.createEntityManagerFactory(persistenceUnit, properties);
  }

  /** Creates an EntityManagerFactory with the default persistence.xml. */
  public static EntityManagerFactory createEntityManagerFactory() {
    return createEntityManagerFactory(DEFAULT_PERSISTENCE_UNIT, Map.of());
  }

  /** Creates an EntityManager from an EntityManagerFactory. */
  public static EntityManager createEntityManager(EntityManagerFactory emf) {
    return emf.createEntityManager();
  }

  /** Creates a generic JPA repository for an entity. */
  public static <E, I> PageableJpaRepository<E, I> createRepository(
      Class<E> entityClass, Class<I> idClass, EntityManager em) {
    PageableJpaRepository<E, I> repo = new PageableJpaRepository<>(entityClass, idClass);
    repo.withEntityManager(em);
    return repo;
  }
}
