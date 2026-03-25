package ${package}.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for {@link ExampleAggregate}.
 *
 * <p>This interface follows the hexagonal architecture principle: the domain defines the contract,
 * and the infrastructure provides the implementation (see {@code adapters/persistence}).
 */
public interface ExampleRepository {

  /** Persists or updates an aggregate. Returns the saved instance. */
  ExampleAggregate save(ExampleAggregate aggregate);

  /** Finds an aggregate by its identity. Returns empty if not found. */
  Optional<ExampleAggregate> findById(UUID id);

  /** Returns all stored aggregates. */
  List<ExampleAggregate> findAll();

  /** Removes an aggregate by its identity. No-op if it does not exist. */
  void deleteById(UUID id);
}
