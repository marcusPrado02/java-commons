package com.marcusprado02.commons.kernel.ddd.repository;

import java.util.Optional;

/**
 * Marker interface for repositories in the DDD sense.
 *
 * <p>A Repository is responsible for:
 *
 * <ul>
 *   <li>Providing collection-like access to aggregates
 *   <li>Encapsulating storage, retrieval, and search behavior
 *   <li>Mediating between the domain and data mapping layers
 * </ul>
 *
 * <p><strong>Key DDD Repository Principles:</strong>
 *
 * <ul>
 *   <li>Operate on aggregate roots only, never on entities inside aggregates
 *   <li>Use ubiquitous language in method names (e.g., {@code findActiveCustomers()})
 *   <li>Return domain objects, not DTOs or database entities
 *   <li>Keep interface simple - complex queries should use Specifications
 * </ul>
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * public interface OrderRepository extends Repository<Order, OrderId> {
 *     Optional<Order> findByOrderNumber(String orderNumber);
 *     List<Order> findByCustomerId(CustomerId customerId);
 *     List<Order> findMatching(Specification<Order> spec);
 * }
 * }</pre>
 *
 * @param <T> the aggregate root type
 * @param <ID> the aggregate identity type
 */
public interface Repository<T, ID> {

  /**
   * Saves an aggregate (insert or update).
   *
   * @param aggregate the aggregate to save
   * @return the saved aggregate
   */
  T save(T aggregate);

  /**
   * Finds an aggregate by its identifier.
   *
   * @param id the aggregate identifier
   * @return an Optional containing the aggregate if found
   */
  Optional<T> findById(ID id);

  /**
   * Checks if an aggregate exists by its identifier.
   *
   * @param id the aggregate identifier
   * @return true if the aggregate exists
   */
  boolean existsById(ID id);

  /**
   * Deletes an aggregate.
   *
   * @param aggregate the aggregate to delete
   */
  void delete(T aggregate);

  /**
   * Deletes an aggregate by its identifier.
   *
   * @param id the aggregate identifier
   */
  void deleteById(ID id);
}
