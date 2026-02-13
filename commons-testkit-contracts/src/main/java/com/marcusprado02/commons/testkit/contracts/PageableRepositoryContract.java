package com.marcusprado02.commons.testkit.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.persistence.contract.PageableRepository;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.specification.Specification;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Base contract test for {@link PageableRepository} implementations.
 *
 * <p>Extend this class to verify that your repository implementation correctly follows the
 * PageableRepository contract.
 *
 * <p>Example:
 *
 * <pre>{@code
 * class MyRepositoryContractTest extends PageableRepositoryContract<User, String> {
 *   @Override
 *   protected PageableRepository<User, String> createRepository() {
 *     return new JpaUserRepository(...);
 *   }
 *
 *   @Override
 *   protected User createEntity() {
 *     return new User("id-1", "John Doe");
 *   }
 *
 *   @Override
 *   protected String getEntityId(User entity) {
 *     return entity.getId();
 *   }
 *
 *   @Override
 *   protected void cleanupRepository() {
 *     userRepository.deleteAll();
 *   }
 * }
 * }</pre>
 *
 * @param <E> Entity type
 * @param <ID> Entity ID type
 */
public abstract class PageableRepositoryContract<E, ID> {

  protected PageableRepository<E, ID> repository;

  /**
   * Create the repository instance to be tested.
   *
   * @return repository implementation
   */
  protected abstract PageableRepository<E, ID> createRepository();

  /**
   * Create a test entity with unique ID.
   *
   * @return test entity
   */
  protected abstract E createEntity();

  /**
   * Create another test entity with different ID.
   *
   * @return another test entity
   */
  protected abstract E createAnotherEntity();

  /**
   * Get the ID from an entity.
   *
   * @param entity entity
   * @return entity ID
   */
  protected abstract ID getEntityId(E entity);

  /**
   * Clean up repository after each test (optional).
   */
  protected void cleanupRepository() {
    // Override if cleanup is needed
  }

  @BeforeEach
  void setUp() {
    repository = createRepository();
    cleanupRepository();
  }

  @Test
  @DisplayName("Should save and find entity by ID")
  void shouldSaveAndFindById() {
    // Given
    E entity = createEntity();
    ID id = getEntityId(entity);

    // When
    E saved = repository.save(entity);

    // Then
    assertThat(saved).isNotNull();
    Optional<E> found = repository.findById(id);
    assertThat(found).isPresent();
    assertThat(getEntityId(found.get())).isEqualTo(id);
  }

  @Test
  @DisplayName("Should return empty when entity not found")
  void shouldReturnEmptyWhenNotFound() {
    // Given
    E entity = createEntity();
    ID id = getEntityId(entity);

    // When
    Optional<E> found = repository.findById(id);

    // Then
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should delete entity by ID")
  void shouldDeleteById() {
    // Given
    E entity = createEntity();
    ID id = getEntityId(entity);
    repository.save(entity);

    // When
    repository.deleteById(id);

    // Then
    Optional<E> found = repository.findById(id);
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should delete entity")
  void shouldDelete() {
    // Given
    E entity = createEntity();
    ID id = getEntityId(entity);
    E saved = repository.save(entity);

    // When
    repository.delete(saved);

    // Then
    Optional<E> found = repository.findById(id);
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should find all with pagination")
  void shouldFindAllWithPagination() {
    // Given
    E entity1 = createEntity();
    E entity2 = createAnotherEntity();
    repository.save(entity1);
    repository.save(entity2);

    PageRequest pageRequest = new PageRequest(0, 10);
    Specification<E> all = (root, query, builder) -> builder.conjunction();

    // When
    PageResult<E> result = repository.findAll(pageRequest, all);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.content()).hasSize(2);
    assertThat(result.totalElements()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should respect page size")
  void shouldRespectPageSize() {
    // Given
    E entity1 = createEntity();
    E entity2 = createAnotherEntity();
    repository.save(entity1);
    repository.save(entity2);

    PageRequest pageRequest = new PageRequest(0, 1);
    Specification<E> all = (root, query, builder) -> builder.conjunction();

    // When
    PageResult<E> result = repository.findAll(pageRequest, all);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.content()).hasSize(1);
    assertThat(result.totalElements()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should return second page")
  void shouldReturnSecondPage() {
    // Given
    E entity1 = createEntity();
    E entity2 = createAnotherEntity();
    repository.save(entity1);
    repository.save(entity2);

    PageRequest page2 = new PageRequest(1, 1);
    Specification<E> all = (root, query, builder) -> builder.conjunction();

    // When
    PageResult<E> result = repository.findAll(page2, all);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.content()).hasSize(1);
    assertThat(result.page()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should return empty page when no entities")
  void shouldReturnEmptyPage() {
    // Given
    PageRequest pageRequest = new PageRequest(0, 10);
    Specification<E> all = (root, query, builder) -> builder.conjunction();

    // When
    PageResult<E> result = repository.findAll(pageRequest, all);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isZero();
  }
}
