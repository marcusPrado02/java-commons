package com.marcusprado02.commons.ports.persistence.model;

import java.util.List;
import java.util.Objects;

/**
 * Pagination request containing page, size, sort, and filter criteria.
 *
 * @param page zero-based page index
 * @param size number of items per page
 * @param sort list of sorting criteria
 * @param filters list of filtering criteria
 */
public record PageRequest(int page, int size, List<Order> sort, List<Filter> filters) {

  /** Validates pagination parameters. */
  public PageRequest {
    if (page < 0) {
      throw new IllegalArgumentException("page must be >= 0");
    }
    if (size <= 0) {
      throw new IllegalArgumentException("size must be > 0");
    }
    Objects.requireNonNull(sort);
    Objects.requireNonNull(filters);
  }

  public PageRequest(int page, int size) {
    this(page, size, List.of(), List.of());
  }
}
