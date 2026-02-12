package com.marcusprado02.commons.adapters.web.rest;

import com.marcusprado02.commons.ports.persistence.contract.PageableRepository;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import java.util.Map;
import java.util.Objects;

/**
 * Framework-agnostic generic search controller. Handles dynamic filtering, pagination, and sorting
 * via query parameters.
 *
 * <p>This class is NOT tied to any web framework (Spring, JAX-RS, etc.). It provides the core logic
 * for parsing query params and executing searches.
 *
 * <p>Usage example in a framework-specific controller:
 *
 * <pre>
 * var controller = new GenericSearchController("/users", userRepository);
 * var response = controller.search(queryParams);
 * </pre>
 *
 * @param <E> Entity type
 * @param <ID> Entity ID type
 */
public class GenericSearchController<E, ID> {

  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIZE = 100;

  private final String basePath;
  private final PageableRepository<E, ID> repository;

  /**
   * Creates a new GenericSearchController.
   *
   * @param basePath the base path for this controller
   * @param repository the repository to use for searches
   */
  public GenericSearchController(String basePath, PageableRepository<E, ID> repository) {
    this.basePath = Objects.requireNonNull(basePath, "basePath cannot be null");
    this.repository = Objects.requireNonNull(repository, "repository cannot be null");
  }

  /**
   * Executes a search based on query parameters.
   *
   * @param queryParams map of query parameters (page, size, filter, sort)
   * @return PageableResponse with search results
   */
  public PageableResponse<E> search(Map<String, String> queryParams) {
    Objects.requireNonNull(queryParams, "queryParams cannot be null");

    // Extract and parse pagination params
    int page = parseIntParam(queryParams.get("page"), DEFAULT_PAGE);
    int size = parseIntParam(queryParams.get("size"), DEFAULT_SIZE);

    // Enforce max size limit
    size = Math.min(size, MAX_SIZE);

    // Parse filter and sort
    String filterParam = queryParams.get("filter");
    String sortParam = queryParams.get("sort");

    SearchCriteria criteria = QueryParamsParser.parseFilters(filterParam);
    Sort sort = QueryParamsParser.parseSort(sortParam);

    // Execute search
    PageRequest pageRequest = new PageRequest(page, size);
    PageResult<E> pageResult = repository.search(pageRequest, null, sort);

    // If criteria has filters, search with criteria
    if (!criteria.filters().isEmpty()) {
      pageResult = repository.findAll(pageRequest, criteria);
    }

    // Map to response DTO
    return new PageableResponse<>(
        pageResult.content(), pageResult.totalElements(), pageResult.page(), pageResult.size());
  }

  /**
   * Parses an integer parameter with a default fallback.
   *
   * @param value the string value to parse
   * @param defaultValue the default value if parsing fails
   * @return parsed integer or default
   */
  private int parseIntParam(String value, int defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      int parsed = Integer.parseInt(value);
      return Math.max(0, parsed);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Gets the base path for this controller.
   *
   * @return the base path
   */
  public String getBasePath() {
    return basePath;
  }
}
