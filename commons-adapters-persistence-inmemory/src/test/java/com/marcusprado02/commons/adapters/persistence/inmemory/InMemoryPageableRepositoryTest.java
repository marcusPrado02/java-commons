package com.marcusprado02.commons.adapters.persistence.inmemory;

import static org.assertj.core.api.Assertions.assertThat;

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

  record TestEntity(Long id, String name, int age, String email, boolean active) {}
}
