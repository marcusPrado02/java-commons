package com.marcusprado02.commons.adapters.persistence.inmemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcusprado02.commons.ports.persistence.model.Order;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.FilterOperator;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.SearchFilter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryPageableRepositoryTest {

  private InMemoryPageableRepository<TestEntity, Long> repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryPageableRepository<>(TestEntity::id);

    // Populate with test data
    repository.save(new TestEntity(1L, "Alice", 25, "alice@example.com", true));
    repository.save(new TestEntity(2L, "Bob", 30, "bob@example.com", false));
    repository.save(new TestEntity(3L, "Charlie", 35, "charlie@example.com", true));
    repository.save(new TestEntity(4L, "Diana", 28, "diana@example.com", true));
    repository.save(new TestEntity(5L, "Eve", 22, "eve@example.com", false));
  }

  @Test
  void shouldFindAllWithPagination() {
    PageRequest pageRequest = new PageRequest(0, 2);
    PageResult<TestEntity> result = repository.findAll(pageRequest);

    assertThat(result.content()).hasSize(2);
    assertThat(result.totalElements()).isEqualTo(5);
    assertThat(result.page()).isEqualTo(0);
    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  void shouldFindSecondPage() {
    PageRequest pageRequest = new PageRequest(1, 2);
    PageResult<TestEntity> result = repository.findAll(pageRequest);

    assertThat(result.content()).hasSize(2);
    assertThat(result.totalElements()).isEqualTo(5);
    assertThat(result.page()).isEqualTo(1);
  }

  @Test
  void shouldReturnEmptyPageWhenOutOfBounds() {
    PageRequest pageRequest = new PageRequest(10, 2);
    PageResult<TestEntity> result = repository.findAll(pageRequest);

    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isEqualTo(5);
  }

  @Test
  void shouldFilterByEqualOperator() {
    SearchFilter filter = SearchFilter.of("name", FilterOperator.EQ, "Alice");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).name()).isEqualTo("Alice");
  }

  @Test
  void shouldFilterByNotEqualOperator() {
    SearchFilter filter = SearchFilter.of("name", FilterOperator.NEQ, "Alice");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(4);
    assertThat(result.content()).noneMatch(e -> e.name().equals("Alice"));
  }

  @Test
  void shouldFilterByLikeOperator() {
    SearchFilter filter = SearchFilter.of("email", FilterOperator.LIKE, "%@example.com");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(5);
  }

  @Test
  void shouldFilterByLikeOperatorWithWildcard() {
    SearchFilter filter = SearchFilter.of("name", FilterOperator.LIKE, "A%");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).name()).isEqualTo("Alice");
  }

  @Test
  void shouldFilterByGreaterThanOperator() {
    SearchFilter filter = SearchFilter.of("age", FilterOperator.GT, "28");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(2);
    assertThat(result.content()).allMatch(e -> e.age() > 28);
  }

  @Test
  void shouldFilterByLessThanOperator() {
    SearchFilter filter = SearchFilter.of("age", FilterOperator.LT, "28");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(2);
    assertThat(result.content()).allMatch(e -> e.age() < 28);
  }

  @Test
  void shouldFilterByGreaterThanOrEqualOperator() {
    SearchFilter filter = SearchFilter.of("age", FilterOperator.GTE, "28");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(3);
    assertThat(result.content()).allMatch(e -> e.age() >= 28);
  }

  @Test
  void shouldFilterByLessThanOrEqualOperator() {
    SearchFilter filter = SearchFilter.of("age", FilterOperator.LTE, "28");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(3);
    assertThat(result.content()).allMatch(e -> e.age() <= 28);
  }

  @Test
  void shouldFilterByInOperator() {
    SearchFilter filter = SearchFilter.of("name", FilterOperator.IN, "Alice,Bob,Charlie");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(3);
    assertThat(result.content())
        .extracting(TestEntity::name)
        .containsExactlyInAnyOrder("Alice", "Bob", "Charlie");
  }

  @Test
  void shouldFilterByBooleanField() {
    SearchFilter filter = SearchFilter.of("active", FilterOperator.EQ, "true");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(3);
    assertThat(result.content()).allMatch(TestEntity::active);
  }

  @Test
  void shouldApplyMultipleFilters() {
    SearchFilter filter1 = SearchFilter.of("age", FilterOperator.GT, "25");
    SearchFilter filter2 = SearchFilter.of("active", FilterOperator.EQ, "true");
    SearchCriteria criteria = SearchCriteria.of(filter1, filter2);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(2);
    assertThat(result.content()).allMatch(e -> e.age() > 25 && e.active());
  }

  @Test
  void shouldSortByFieldAscending() {
    Sort sort = Sort.of(new Order("age", Order.Direction.ASC));
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.search(pageRequest, null, sort);

    assertThat(result.content()).hasSize(5);
    assertThat(result.content()).extracting(TestEntity::age).containsExactly(22, 25, 28, 30, 35);
  }

  @Test
  void shouldSortByFieldDescending() {
    Sort sort = Sort.of(new Order("age", Order.Direction.DESC));
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.search(pageRequest, null, sort);

    assertThat(result.content()).hasSize(5);
    assertThat(result.content()).extracting(TestEntity::age).containsExactly(35, 30, 28, 25, 22);
  }

  @Test
  void shouldSortByMultipleFields() {
    // Add entities with same age to test secondary sort
    repository.save(new TestEntity(6L, "Frank", 25, "frank@example.com", true));
    repository.save(new TestEntity(7L, "Grace", 25, "grace@example.com", false));

    Sort sort =
        Sort.of(new Order("age", Order.Direction.ASC), new Order("name", Order.Direction.ASC));
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.search(pageRequest, null, sort);

    assertThat(result.content()).hasSize(7);
    assertThat(result.content())
        .extracting(TestEntity::name)
        .startsWith("Eve", "Alice", "Frank", "Grace"); // Eve age 22, then age 25 sorted by name
  }

  @Test
  void shouldCombineFilteringAndSorting() {
    SearchFilter filter = SearchFilter.of("age", FilterOperator.GT, "25");
    SearchCriteria criteria = SearchCriteria.of(filter);
    Sort sort = Sort.of(new Order("age", Order.Direction.DESC));
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);
    List<TestEntity> sortedFiltered =
        repository.search(pageRequest, null, sort).content().stream()
            .filter(e -> e.age() > 25)
            .toList();

    assertThat(result.content()).hasSize(3);
    assertThat(sortedFiltered).extracting(TestEntity::age).containsExactly(35, 30, 28);
  }

  @Test
  void shouldHandleEmptyFilters() {
    SearchCriteria criteria = SearchCriteria.of();
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(5);
  }

  @Test
  void shouldHandleNullSort() {
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.search(pageRequest, null, null);

    assertThat(result.content()).hasSize(5);
  }

  // --- findAll(PageRequest, Specification) delegates to findAll(PageRequest) ---

  @Test
  void shouldFindAllWithSpecificationDelegatingToUnfiltered() {
    PageRequest pageRequest = new PageRequest(0, 10);

    // Specification is ignored by the in-memory implementation (delegates to findAll(pageRequest))
    PageResult<TestEntity> result =
        repository.findAll(
            pageRequest,
            (com.marcusprado02.commons.ports.persistence.specification.Specification<TestEntity>)
                null);

    assertThat(result.content()).hasSize(5);
    assertThat(result.totalElements()).isEqualTo(5);
  }

  // --- matchesCriteria: null criteria ---

  @Test
  void shouldReturnAllWhenCriteriaIsNull() {
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, (SearchCriteria) null);

    assertThat(result.content()).hasSize(5);
  }

  // --- matchesFilter: null field value ---

  @Test
  void shouldMatchNullFieldValueWhenFilterValueIsAlsoNull() {
    repository.save(new TestEntity(10L, null, 40, "null@example.com", true));

    SearchFilter filter = SearchFilter.of("name", FilterOperator.EQ, null);
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).anyMatch(e -> e.id().equals(10L));
  }

  @Test
  void shouldNotMatchNullFieldValueWhenFilterValueIsNotNull() {
    repository.save(new TestEntity(11L, null, 40, "null2@example.com", true));

    SearchFilter filter = SearchFilter.of("name", FilterOperator.EQ, "Alice");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).noneMatch(e -> e.id().equals(11L));
  }

  // --- convertToType: Long, Double, Float, Boolean ---

  @Test
  void shouldFilterByLongField() {
    SearchFilter filter = SearchFilter.of("id", FilterOperator.GT, "3");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(2);
    assertThat(result.content()).allMatch(e -> e.id() > 3L);
  }

  @Test
  void shouldFilterByLongFieldLTE() {
    SearchFilter filter = SearchFilter.of("id", FilterOperator.LTE, "2");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(2);
    assertThat(result.content()).allMatch(e -> e.id() <= 2L);
  }

  @Test
  void shouldFilterByDoubleField() {
    InMemoryPageableRepository<DoubleEntity, Long> repo =
        new InMemoryPageableRepository<>(DoubleEntity::id);
    repo.save(new DoubleEntity(1L, 1.5));
    repo.save(new DoubleEntity(2L, 2.5));
    repo.save(new DoubleEntity(3L, 3.5));

    SearchFilter filter = SearchFilter.of("value", FilterOperator.GT, "2.0");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<DoubleEntity> result = repo.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(2);
  }

  @Test
  void shouldFilterByFloatField() {
    InMemoryPageableRepository<FloatEntity, Long> repo =
        new InMemoryPageableRepository<>(FloatEntity::id);
    repo.save(new FloatEntity(1L, 1.5f));
    repo.save(new FloatEntity(2L, 2.5f));

    SearchFilter filter = SearchFilter.of("value", FilterOperator.LT, "2.0");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<FloatEntity> result = repo.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).id()).isEqualTo(1L);
  }

  @Test
  void shouldFilterByBooleanFieldGTE() {
    SearchFilter filter = SearchFilter.of("active", FilterOperator.GTE, "true");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    // Boolean.parseBoolean returns Boolean which is Comparable
    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).isNotNull();
  }

  // --- applySorting: empty orders list (not null Sort) ---

  @Test
  void shouldReturnUnsortedWhenSortOrdersAreEmpty() {
    Sort sort = Sort.of(); // empty varargs
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.search(pageRequest, null, sort);

    assertThat(result.content()).hasSize(5);
  }

  // --- createComparator: null values in sort ---

  @Test
  void shouldHandleNullFieldValuesInSort() {
    // Add entities where name is null to exercise null-handling in comparator
    repository.save(new TestEntity(20L, null, 99, "z@example.com", false));
    repository.save(new TestEntity(21L, null, 100, "y@example.com", false));

    Sort sort = Sort.of(new Order("name", Order.Direction.ASC));
    PageRequest pageRequest = new PageRequest(0, 20);

    PageResult<TestEntity> result = repository.search(pageRequest, null, sort);

    assertThat(result.content()).hasSize(7);
  }

  @Test
  void shouldHandleNullFieldValueV1InSortDesc() {
    // v1 is null, v2 is not null -> direction DESC should return 1
    repository.save(new TestEntity(22L, null, 50, "a@example.com", true));

    Sort sort = Sort.of(new Order("name", Order.Direction.DESC));
    PageRequest pageRequest = new PageRequest(0, 20);

    PageResult<TestEntity> result = repository.search(pageRequest, null, sort);

    assertThat(result.content()).isNotEmpty();
  }

  @Test
  void shouldHandleNullFieldValueV2InSortAsc() {
    // v2 is null, v1 is not null -> direction ASC should return 1
    repository.save(new TestEntity(23L, null, 51, "b@example.com", true));

    Sort sort = Sort.of(new Order("name", Order.Direction.ASC));
    PageRequest pageRequest = new PageRequest(0, 20);

    PageResult<TestEntity> result = repository.search(pageRequest, null, sort);

    assertThat(result.content()).isNotEmpty();
  }

  // --- createComparator: non-Comparable field (toString fallback) ---

  @Test
  void shouldSortByNonComparableFieldUsingToString() {
    InMemoryPageableRepository<NonComparableEntity, Long> repo =
        new InMemoryPageableRepository<>(NonComparableEntity::id);
    repo.save(new NonComparableEntity(1L, new NonComparableValue("beta")));
    repo.save(new NonComparableEntity(2L, new NonComparableValue("alpha")));

    Sort sort = Sort.of(new Order("value", Order.Direction.ASC));
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<NonComparableEntity> result = repo.search(pageRequest, null, sort);

    assertThat(result.content()).hasSize(2);
    assertThat(result.content().get(0).value().label()).isEqualTo("alpha");
  }

  // --- findField: superclass field lookup ---

  @Test
  void shouldFilterByInheritedField() {
    InMemoryPageableRepository<ChildEntity, Long> repo =
        new InMemoryPageableRepository<>(ChildEntity::id);
    repo.save(new ChildEntity(1L, "parent-value", "child-value"));
    repo.save(new ChildEntity(2L, "other-value", "child-value"));

    SearchFilter filter = SearchFilter.of("parentField", FilterOperator.EQ, "parent-value");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<ChildEntity> result = repo.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).id()).isEqualTo(1L);
  }

  // --- matchesFilter: field not found ---

  @Test
  void shouldThrowWhenFieldDoesNotExist() {
    SearchFilter filter = SearchFilter.of("nonExistentField", FilterOperator.EQ, "value");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    assertThatThrownBy(() -> repository.findAll(pageRequest, criteria))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("nonExistentField");
  }

  // --- Helper entity records/classes ---

  record TestEntity(Long id, String name, int age, String email, boolean active) {}

  record DoubleEntity(Long id, double value) {}

  record FloatEntity(Long id, float value) {}

  record NonComparableEntity(Long id, NonComparableValue value) {}

  static class NonComparableValue {
    private final String label;

    NonComparableValue(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  static class ParentEntity {
    protected final Long id;
    private final String parentField;

    ParentEntity(Long id, String parentField) {
      this.id = id;
      this.parentField = parentField;
    }

    public Long id() {
      return id;
    }

    public String parentField() {
      return parentField;
    }
  }

  static class ChildEntity extends ParentEntity {
    private final String childField;

    ChildEntity(Long id, String parentField, String childField) {
      super(id, parentField);
      this.childField = childField;
    }

    public String childField() {
      return childField;
    }
  }
}
