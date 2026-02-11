package com.marcusprado02.commons.ports.persistence.model;

/** Order representation for ordering query results. ex: field ASC */
public record Order(String field, Direction direction) {

  public enum Direction {
    ASC,
    DESC
  }
}
