package com.marcusprado02.commons.ports.persistence.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/** Representa um predicado sobre a entidade de dominio T. */
@FunctionalInterface
public interface Specification<T> {

  /**
   * @param root raiz da query
   * @param query a CriteriaQuery
   * @param builder o CriteriaBuilder
   * @return um Predicate que representa a condicao da especificacao
   */
  Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder);

  /** Combina essa especificacao com outra usando AND. */
  default Specification<T> and(Specification<T> other) {
    return (root, query, builder) ->
        builder.and(
            this.toPredicate(root, query, builder), other.toPredicate(root, query, builder));
  }

  /** Combina essa especificacao com outra usando OR. */
  default Specification<T> or(Specification<T> other) {
    return (root, query, builder) ->
        builder.or(this.toPredicate(root, query, builder), other.toPredicate(root, query, builder));
  }

  /** Nega essa especificacao. */
  default Specification<T> not() {
    return (root, query, builder) -> builder.not(this.toPredicate(root, query, builder));
  }
}
