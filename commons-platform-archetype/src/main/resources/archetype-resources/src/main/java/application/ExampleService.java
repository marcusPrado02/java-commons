package ${package}.application;

import ${package}.domain.ExampleAggregate;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Example application service.
 *
 * <p>Contains use cases and orchestrates domain logic.
 */
@Service
public class ExampleService {

  public Result<ExampleAggregate> createExample(String name, String description) {
    UUID id = UUID.randomUUID();
    ExampleAggregate aggregate = new ExampleAggregate(id, name, description);

    // TODO: Persist aggregate using repository

    return Result.ok(aggregate);
  }

  public Result<ExampleAggregate> updateExample(UUID id, String newName) {
    // TODO: Load aggregate from repository
    // aggregate.updateName(newName);
    // TODO: Save aggregate

    return Result.ok(null);
  }
}
