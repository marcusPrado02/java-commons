package com.marcusprado02.commons.ports.persistence.model;

/**
 * Filter representation for querying entities.
 * ex: field EQ value
 */
public record Filter(String field, Operator operator, Object value) {

    public enum Operator {
        EQ, NEQ, GT, LT, GTE, LTE, LIKE
    }
}
