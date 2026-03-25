package ${package}.adapters.persistence;

import ${package}.domain.ExampleAggregate;
import ${package}.domain.ExampleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of {@link ExampleRepository}.
 *
 * <p>Suitable for local development, tests, and initial bootstrapping.
 * Replace with a JPA or other persistence adapter for production use.
 */
@Repository
public class InMemoryExampleRepository implements ExampleRepository {

  private final Map<UUID, ExampleAggregate> store = new ConcurrentHashMap<>();

  @Override
  public ExampleAggregate save(ExampleAggregate aggregate) {
    store.put(aggregate.id(), aggregate);
    return aggregate;
  }

  @Override
  public Optional<ExampleAggregate> findById(UUID id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<ExampleAggregate> findAll() {
    return new ArrayList<>(store.values());
  }

  @Override
  public void deleteById(UUID id) {
    store.remove(id);
  }
}
