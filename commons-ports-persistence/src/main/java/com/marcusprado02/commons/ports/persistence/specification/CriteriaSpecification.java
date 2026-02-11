package com.marcusprado02.commons.ports.persistence.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;

/** Base para especificacoes simples por campo. */
public class CriteriaSpecification<T> implements Specification<T> {

  private final List<PredicateBuilder<T>> predicates;

  public CriteriaSpecification(List<PredicateBuilder<T>> predicates) {
    this.predicates = predicates;
  }

  @Override
  public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
    Predicate result = builder.conjunction();
    for (PredicateBuilder<T> p : predicates) {
      result = builder.and(result, p.build(root, query, builder));
    }
    return result;
  }
}
