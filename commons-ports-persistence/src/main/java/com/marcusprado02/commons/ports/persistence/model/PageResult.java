package com.marcusprado02.commons.ports.persistence.model;

import java.util.List;

/**
 * Pagination result containing content, total elements, and page metadata.
 *
 * @param <E> the element type
 * @param content list of items in the current page
 * @param totalElements total number of items across all pages
 * @param page current page index
 * @param size number of items per page
 */
public record PageResult<E>(List<E> content, long totalElements, int page, int size) {}
