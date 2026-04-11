package com.marcusprado02.commons.ports.persistence.specification;

/** Single filter criterion: field, operator, and value. */
public record SearchFilter(String field, FilterOperator operator, String value) {

  public static SearchFilter of(String field, FilterOperator op, String value) {
    return new SearchFilter(field, op, value);
  }
}
