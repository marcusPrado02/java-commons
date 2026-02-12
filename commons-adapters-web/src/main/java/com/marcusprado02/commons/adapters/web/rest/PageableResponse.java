package com.marcusprado02.commons.adapters.web.rest;

import java.util.List;

/**
 * Framework-agnostic pageable response for REST APIs. Maps domain PageResult to a simple DTO
 * structure.
 *
 * @param <T> the type of content elements
 */
public record PageableResponse<T>(List<T> content, long totalElements, int page, int size) {

  public PageableResponse {
    if (content == null) {
      throw new IllegalArgumentException("content cannot be null");
    }
    if (totalElements < 0) {
      throw new IllegalArgumentException("totalElements must be >= 0");
    }
    if (page < 0) {
      throw new IllegalArgumentException("page must be >= 0");
    }
    if (size <= 0) {
      throw new IllegalArgumentException("size must be > 0");
    }
  }
}
