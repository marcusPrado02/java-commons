package com.marcusprado02.commons.adapters.web;

import java.util.List;

/** PageResponse data. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {}
