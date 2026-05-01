package com.marcusprado02.commons.ports.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.persistence.specification.*;
import jakarta.persistence.criteria.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class PersistenceSpecificationTest {

  @Mock Root<Object> root;
  @Mock CriteriaQuery<Object> query;
  @Mock CriteriaBuilder builder;
  @Mock Predicate predicate;
  @Mock Path<Object> path;

  // --- FilterOperator ---

  @Test
  void filterOperator_has_eight_values() {
    assertEquals(8, FilterOperator.values().length);
  }

  @Test
  void filterOperator_all_names_accessible() {
    for (FilterOperator op : FilterOperator.values()) {
      assertNotNull(op.name());
    }
  }

  // --- SearchFilter ---

  @Test
  void searchFilter_of_stores_all_fields() {
    SearchFilter f = SearchFilter.of("status", FilterOperator.EQ, "ACTIVE");
    assertEquals("status", f.field());
    assertEquals(FilterOperator.EQ, f.operator());
    assertEquals("ACTIVE", f.value());
  }

  @Test
  void searchFilter_record_equality() {
    SearchFilter f1 = SearchFilter.of("f", FilterOperator.NEQ, "v");
    SearchFilter f2 = SearchFilter.of("f", FilterOperator.NEQ, "v");
    assertEquals(f1, f2);
  }

  // --- SearchCriteria ---

  @Test
  void searchCriteria_of_stores_all_filters() {
    SearchFilter f1 = SearchFilter.of("name", FilterOperator.EQ, "Alice");
    SearchFilter f2 = SearchFilter.of("age", FilterOperator.GT, "18");
    SearchCriteria c = SearchCriteria.of(f1, f2);
    assertEquals(2, c.filters().size());
    assertEquals("name", c.filters().get(0).field());
  }

  @Test
  void searchCriteria_of_empty_filters() {
    SearchCriteria c = SearchCriteria.of();
    assertTrue(c.filters().isEmpty());
  }

  // --- Specification default methods ---

  @Test
  void specification_and_invokes_builder_and() {
    Specification<Object> s1 = (r, q, b) -> predicate;
    Specification<Object> s2 = (r, q, b) -> predicate;
    when(builder.and(predicate, predicate)).thenReturn(predicate);
    Predicate result = s1.and(s2).toPredicate(root, query, builder);
    assertEquals(predicate, result);
  }

  @Test
  void specification_or_invokes_builder_or() {
    Specification<Object> s1 = (r, q, b) -> predicate;
    Specification<Object> s2 = (r, q, b) -> predicate;
    when(builder.or(predicate, predicate)).thenReturn(predicate);
    Predicate result = s1.or(s2).toPredicate(root, query, builder);
    assertEquals(predicate, result);
  }

  @Test
  void specification_not_invokes_builder_not() {
    Specification<Object> s = (r, q, b) -> predicate;
    when(builder.not(predicate)).thenReturn(predicate);
    Predicate result = s.not().toPredicate(root, query, builder);
    assertEquals(predicate, result);
  }

  // --- PredicateBuilder static factories ---

  @Test
  void predicateBuilder_equal_builds_predicate() {
    when(root.get(anyString())).thenReturn(path);
    PredicateBuilder<Object> pb = PredicateBuilder.equal("field", "val");
    pb.build(root, query, builder);
  }

  @Test
  void predicateBuilder_like_builds_predicate() {
    when(root.get(anyString())).thenReturn(path);
    PredicateBuilder<Object> pb = PredicateBuilder.like("field", "%val%");
    pb.build(root, query, builder);
  }

  @Test
  void predicateBuilder_greaterThan_builds_predicate() {
    when(root.get(anyString())).thenReturn(path);
    PredicateBuilder<Object> pb = PredicateBuilder.greaterThan("age", 18);
    pb.build(root, query, builder);
  }

  @Test
  void predicateBuilder_lessThan_builds_predicate() {
    when(root.get(anyString())).thenReturn(path);
    PredicateBuilder<Object> pb = PredicateBuilder.lessThan("age", 18);
    pb.build(root, query, builder);
  }

  // --- CriteriaSpecification ---

  @Test
  void criteriaSpecification_empty_predicates_returns_conjunction() {
    when(builder.conjunction()).thenReturn(predicate);
    CriteriaSpecification<Object> spec = new CriteriaSpecification<>(List.of());
    Predicate result = spec.toPredicate(root, query, builder);
    assertEquals(predicate, result);
  }

  @Test
  void criteriaSpecification_with_predicates_invokes_and() {
    when(root.get(anyString())).thenReturn(path);
    when(builder.conjunction()).thenReturn(predicate);
    when(builder.and(predicate, null)).thenReturn(predicate);
    PredicateBuilder<Object> pb = PredicateBuilder.equal("f", "v");
    CriteriaSpecification<Object> spec = new CriteriaSpecification<>(List.of(pb));
    spec.toPredicate(root, query, builder);
  }

  // --- SpecificationBuilder — all 8 operators ---

  @Test
  void specBuilder_EQ_operator() {
    invokeSpec(FilterOperator.EQ, "value");
  }

  @Test
  void specBuilder_LIKE_operator() {
    invokeSpec(FilterOperator.LIKE, "%val%");
  }

  @Test
  void specBuilder_GT_operator() {
    invokeSpec(FilterOperator.GT, "10");
  }

  @Test
  void specBuilder_LT_operator() {
    invokeSpec(FilterOperator.LT, "10");
  }

  @Test
  void specBuilder_GTE_operator() {
    invokeSpec(FilterOperator.GTE, "10");
  }

  @Test
  void specBuilder_LTE_operator() {
    invokeSpec(FilterOperator.LTE, "10");
  }

  @Test
  void specBuilder_NEQ_operator() {
    invokeSpec(FilterOperator.NEQ, "value");
  }

  @Test
  void specBuilder_IN_operator() {
    when(root.get(anyString())).thenReturn(path);
    SearchCriteria criteria =
        SearchCriteria.of(SearchFilter.of("status", FilterOperator.IN, "A,B,C"));
    Specification<Object> spec = new SpecificationBuilder<Object>().build(criteria);
    spec.toPredicate(root, query, builder);
  }

  private void invokeSpec(FilterOperator op, String value) {
    when(root.get(anyString())).thenReturn(path);
    SearchCriteria criteria = SearchCriteria.of(SearchFilter.of("field", op, value));
    Specification<Object> spec = new SpecificationBuilder<Object>().build(criteria);
    spec.toPredicate(root, query, builder);
  }
}
