package com.marcusprado02.commons.adapters.persistence.jpa.factory;

import com.marcusprado02.commons.adapters.persistence.jpa.repository.PageableJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Map;

public final class JpaRepositoryFactory {

  private static final String DEFAULT_PERSISTENCE_UNIT = "defaultPU";

  private JpaRepositoryFactory() {}

  /** Create an EntityManagerFactory from properties (e.g., URL, driver, dialect) */
  public static EntityManagerFactory createEntityManagerFactory(
      String persistenceUnit, Map<String, Object> properties) {

    return Persistence.createEntityManagerFactory(persistenceUnit, properties);
  }

  /** Create an EntityManagerFactory with the default persistence.xml */
  public static EntityManagerFactory createEntityManagerFactory() {
    return createEntityManagerFactory(DEFAULT_PERSISTENCE_UNIT, Map.of());
  }

  /** Create an EntityManager from an EntityManagerFactory */
  public static EntityManager createEntityManager(EntityManagerFactory emf) {
    return emf.createEntityManager();
  }

  /** Create a generic JPA repository for an entity */
  public static <E, ID> PageableJpaRepository<E, ID> createRepository(
      Class<E> entityClass, Class<ID> idClass, EntityManager em) {
    PageableJpaRepository<E, ID> repo = new PageableJpaRepository<>(entityClass, idClass);
    repo.withEntityManager(em);
    return repo;
  }
}
