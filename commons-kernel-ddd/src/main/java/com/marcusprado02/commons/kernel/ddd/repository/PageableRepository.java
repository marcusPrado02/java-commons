package com.marcusprado02.commons.kernel.ddd.repository;

import java.util.List;

/**
 * Repository that supports pagination and sorting.
 *
 * <p>Extends the basic Repository with methods to retrieve aggregates in pages.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * public interface CustomerRepository extends PageableRepository<Customer, CustomerId> {
 *     Page<Customer> findByCountry(String country, Pageable pageable);
 * }
 * }</pre>
 *
 * @param <T> the aggregate root type
 * @param <ID> the aggregate identity type
 */
public interface PageableRepository<T, ID> extends Repository<T, ID> {

  /**
   * Finds all aggregates with pagination.
   *
   * @param pageable the pagination information
   * @return a page of aggregates
   */
  Page<T> findAll(Pageable pageable);

  /**
   * Counts all aggregates.
   *
   * @return the total number of aggregates
   */
  long count();

  /**
   * Simple pagination request.
   *
   * @param page the page number (0-indexed)
   * @param size the page size
   * @param sortBy optional sort field
   */
  record Pageable(int page, int size, String sortBy) {
    public static Pageable of(int page, int size) {
      return new Pageable(page, size, null);
    }

    public static Pageable of(int page, int size, String sortBy) {
      return new Pageable(page, size, sortBy);
    }
  }

  /**
   * A page of results.
   *
   * @param content the page content
   * @param totalElements the total number of elements
   * @param totalPages the total number of pages
   * @param page the current page number
   * @param size the page size
   */
  record Page<T>(List<T> content, long totalElements, int totalPages, int page, int size) {
    public boolean hasNext() {
      return page < totalPages - 1;
    }

    public boolean hasPrevious() {
      return page > 0;
    }

    public boolean isEmpty() {
      return content.isEmpty();
    }
  }
}
