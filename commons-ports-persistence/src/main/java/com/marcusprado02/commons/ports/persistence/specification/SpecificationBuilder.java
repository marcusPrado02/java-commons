package com.marcusprado02.commons.ports.persistence.specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class SpecificationBuilder<T> {

  public Specification<T> build(SearchCriteria criteria) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();

      for (SearchFilter filter : criteria.filters()) {
        switch (filter.operator()) {
          case EQ -> predicates.add(builder.equal(root.get(filter.field()), filter.value()));
          case LIKE -> predicates.add(builder.like(root.get(filter.field()), filter.value()));
          case GT -> predicates.add(builder.greaterThan(root.get(filter.field()), filter.value()));
          case LT -> predicates.add(builder.lessThan(root.get(filter.field()), filter.value()));
          case GTE ->
              predicates.add(
                  builder.greaterThanOrEqualTo(root.get(filter.field()), filter.value()));
          case LTE ->
              predicates.add(builder.lessThanOrEqualTo(root.get(filter.field()), filter.value()));
          case NEQ -> predicates.add(builder.notEqual(root.get(filter.field()), filter.value()));
          case IN -> {
            String[] values = filter.value().split(",");
            predicates.add(root.get(filter.field()).in((Object[]) values));
          }
        }
      }

      return builder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
