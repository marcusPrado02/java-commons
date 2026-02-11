package com.marcusprado02.commons.ports.persistence.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/** Construtor de predicate dinamico. */
@FunctionalInterface
public interface PredicateBuilder<T> {

  Predicate build(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder);

  static <T> PredicateBuilder<T> equal(String field, Object value) {
    return (root, query, builder) -> builder.equal(root.get(field), value);
  }

  static <T> PredicateBuilder<T> like(String field, String pattern) {
    return (root, query, builder) -> builder.like(root.get(field), pattern);
  }

  static <T, Y extends Comparable<? super Y>> PredicateBuilder<T> greaterThan(
      String field, Y value) {
    return (root, query, builder) -> builder.greaterThan(root.get(field), value);
  }

  static <T, Y extends Comparable<? super Y>> PredicateBuilder<T> lessThan(String field, Y value) {
    return (root, query, builder) -> builder.lessThan(root.get(field), value);
  }
}
