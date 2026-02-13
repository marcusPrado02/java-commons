package com.marcusprado02.commons.adapters.persistence.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.persistence.model.Order;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.FilterOperator;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.SearchFilter;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MongoPageableRepositoryTest {

  @Container
  static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

  private static MongoTemplate mongoTemplate;
  private MongoPageableRepository<TestEntity, String> repository;

  @BeforeAll
  static void setupMongoTemplate() {
    mongoDBContainer.start();
    mongoTemplate = new MongoTemplate(
        MongoClients.create(mongoDBContainer.getReplicaSetUrl()),
        "test"
    );
  }

  @AfterAll
  static void cleanup() {
    mongoDBContainer.stop();
  }

  @BeforeEach
  void setUp() {
    mongoTemplate.dropCollection(TestEntity.class);
    repository = new MongoPageableRepository<>(mongoTemplate, TestEntity.class);

    // Populate with test data
    repository.save(new TestEntity("1", "Alice", 25, "alice@example.com", true));
    repository.save(new TestEntity("2", "Bob", 30, "bob@example.com", false));
    repository.save(new TestEntity("3", "Charlie", 35, "charlie@example.com", true));
    repository.save(new TestEntity("4", "Diana", 28, "diana@example.com", true));
    repository.save(new TestEntity("5", "Eve", 22, "eve@example.com", false));
  }

  @Test
  void shouldSaveAndFindById() {
    TestEntity entity = new TestEntity("100", "Test", 30, "test@example.com", true);
    repository.save(entity);

    TestEntity found = repository.findById("100").orElseThrow();
    assertThat(found.name()).isEqualTo("Test");
    assertThat(found.age()).isEqualTo(30);
  }

  @Test
  void shouldFindAll() {
    var all = repository.findAll();
    assertThat(all).hasSize(5);
  }

  @Test
  void shouldDeleteById() {
    repository.deleteById("1");
    assertThat(repository.existsById("1")).isFalse();
    assertThat(repository.count()).isEqualTo(4);
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
    SearchFilter filter = SearchFilter.of("email", FilterOperator.LIKE, "%example.com");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(5);
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
  void shouldFilterByInOperator() {
    SearchFilter filter = SearchFilter.of("name", FilterOperator.IN, "Alice,Bob,Charlie");
    SearchCriteria criteria = SearchCriteria.of(filter);
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.findAll(pageRequest, criteria);

    assertThat(result.content()).hasSize(3);
    assertThat(result.content()).extracting(TestEntity::name)
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
    assertThat(result.content())
        .extracting(TestEntity::age)
        .containsExactly(22, 25, 28, 30, 35);
  }

  @Test
  void shouldSortByFieldDescending() {
    Sort sort = Sort.of(new Order("age", Order.Direction.DESC));
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.search(pageRequest, null, sort);

    assertThat(result.content()).hasSize(5);
    assertThat(result.content())
        .extracting(TestEntity::age)
        .containsExactly(35, 30, 28, 25, 22);
  }

  @Test
  void shouldSortByMultipleFields() {
    // Add entities with same age to test secondary sort
    repository.save(new TestEntity("6", "Frank", 25, "frank@example.com", true));
    repository.save(new TestEntity("7", "Grace", 25, "grace@example.com", false));

    Sort sort = Sort.of(
        new Order("age", Order.Direction.ASC),
        new Order("name", Order.Direction.ASC)
    );
    PageRequest pageRequest = new PageRequest(0, 10);

    PageResult<TestEntity> result = repository.search(pageRequest, null, sort);

    assertThat(result.content()).hasSize(7);
    // First entry should be Eve (age 22), then age 25 sorted by name
    assertThat(result.content().get(0).name()).isEqualTo("Eve");
    assertThat(result.content().get(1).age()).isEqualTo(25);
  }
}
