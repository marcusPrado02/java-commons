package com.marcusprado02.commons.adapters.persistence.jpa.it;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.adapters.persistence.jpa.factory.JpaRepositoryFactory;
import com.marcusprado02.commons.ports.persistence.contract.PageableRepository;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.specification.FilterOperator;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.SearchFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PersistencePostgreTest {

  @Container
  static PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:15.4")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  static EntityManagerFactory emf;
  static EntityManager em;

  @BeforeAll
  static void setup() {
    POSTGRES.start();

    Map<String, Object> props =
        Map.of(
            "jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl(),
            "jakarta.persistence.jdbc.user", POSTGRES.getUsername(),
            "jakarta.persistence.jdbc.password", POSTGRES.getPassword(),
            "jakarta.persistence.jdbc.driver", "org.postgresql.Driver",
            "hibernate.hbm2ddl.auto", "create-drop",
            "hibernate.show_sql", "false",
            "hibernate.format_sql", "false");

    emf = JpaRepositoryFactory.createEntityManagerFactory("test-pu", props);
    em = JpaRepositoryFactory.createEntityManager(emf);
  }

  @AfterAll
  static void teardown() {
    if (em != null) em.close();
    if (emf != null) emf.close();
    POSTGRES.stop();
  }

  @Test
  void shouldSaveAndFindEntity() {
    var repo = JpaRepositoryFactory.createRepository(MyEntity.class, Long.class, em);

    // Save entity in one transaction
    em.getTransaction().begin();
    var saved = repo.save(new MyEntity(null, "Felipe"));
    em.getTransaction().commit();

    // Find entity in a separate transaction
    em.getTransaction().begin();
    var found = repo.findById(saved.id());
    em.getTransaction().commit();

    assertTrue(found.isPresent());
    assertEquals("Felipe", found.get().name());
  }

  @Test
  @Disabled(
      "TODO: Implementar filtro real por SearchCriteria no PageableJpaRepository - veja InMemoryPageableRepository também")
  void shouldPaginateResults() {
    var repo = JpaRepositoryFactory.createRepository(MyEntity.class, Long.class, em);

    // Persist some data
    em.getTransaction().begin();
    repo.save(new MyEntity(null, "A"));
    repo.save(new MyEntity(null, "B"));
    repo.save(new MyEntity(null, "C"));
    em.getTransaction().commit();

    // Query page 0, size 2
    var pageReq = new PageRequest(0, 2);

    SearchCriteria criteria =
        SearchCriteria.of(SearchFilter.of("name", FilterOperator.LIKE, "Jo%"));

    PageResult<MyEntity> result =
        ((PageableRepository<MyEntity, Long>) repo).findAll(pageReq, criteria);

    assertEquals(2, result.content().size());
    assertTrue(result.totalElements() >= 3);
  }
}
