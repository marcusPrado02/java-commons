package com.marcusprado02.commons.ports.persistence.model;

import java.util.List;
import java.util.Objects;

/**
 * Pagination request:
 * page: zero-based page index
 * size: number of items per page
 * sort: list of sorting criteria
 * filters: list of filtering criteria
 */
public record PageRequest(
        int page,
        int size,
        List<Sort> sort,
        List<Filter> filters
) {

    public PageRequest {
        if (page < 0) throw new IllegalArgumentException("page must be >= 0");
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        Objects.requireNonNull(sort);
        Objects.requireNonNull(filters);
    }

    public PageRequest(int page, int size) {
        this(page, size, List.of(), List.of());
    }
}
