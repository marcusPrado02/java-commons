package ${package}.application;

import ${package}.domain.ExampleAggregate;
import ${package}.domain.ExampleRepository;
import com.marcusprado02.commons.kernel.errors.Problems;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Application service for Example use cases.
 *
 * <p>Orchestrates domain logic and delegates persistence to {@link ExampleRepository}.
 */
@Service
public class ExampleService {

  private final ExampleRepository repository;

  public ExampleService(ExampleRepository repository) {
    this.repository = repository;
  }

  public Result<ExampleAggregate> createExample(String name, String description) {
    UUID id = UUID.randomUUID();
    ExampleAggregate aggregate = new ExampleAggregate(id, name, description);
    repository.save(aggregate);
    return Result.ok(aggregate);
  }

  public Result<ExampleAggregate> getExample(UUID id) {
    return repository
        .findById(id)
        .map(Result::ok)
        .orElse(Result.fail(Problems.notFound("EXAMPLE.NOT_FOUND", "Example not found: " + id)));
  }

  public Result<ExampleAggregate> updateExample(UUID id, String newName) {
    return repository
        .findById(id)
        .map(
            aggregate -> {
              aggregate.updateName(newName);
              repository.save(aggregate);
              return Result.ok(aggregate);
            })
        .orElse(Result.fail(Problems.notFound("EXAMPLE.NOT_FOUND", "Example not found: " + id)));
  }

  public Result<List<ExampleAggregate>> listExamples() {
    return Result.ok(repository.findAll());
  }

  public Result<Void> deleteExample(UUID id) {
    if (repository.findById(id).isEmpty()) {
      return Result.fail(Problems.notFound("EXAMPLE.NOT_FOUND", "Example not found: " + id));
    }
    repository.deleteById(id);
    return Result.ok(null);
  }
}
