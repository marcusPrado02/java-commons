package com.marcusprado02.commons.ports.persistence.model;

import java.util.List;

/**
 * Pagination result:
 * content: list of items in the current page
 * totalElements: total number of items across all pages
 * page: current page index
 * size: number of items per page
 */
public record PageResult<E>(
        List<E> content,
        long totalElements,
        int page,
        int size
) {}
