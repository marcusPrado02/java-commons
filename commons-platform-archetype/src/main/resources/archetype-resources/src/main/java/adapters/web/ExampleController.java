package ${package}.adapters.web;

import ${package}.application.ExampleService;
import ${package}.domain.ExampleAggregate;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Example resources.
 */
@RestController
@RequestMapping("/api/examples")
public class ExampleController {

  private final ExampleService exampleService;

  public ExampleController(ExampleService exampleService) {
    this.exampleService = exampleService;
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody CreateExampleRequest request) {
    Result<ExampleAggregate> result =
        exampleService.createExample(request.name(), request.description());

    return result.isOk()
        ? ResponseEntity.ok(new ExampleResponse(result.getOrNull()))
        : ResponseEntity.badRequest().body(result.problemOrNull());
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getById(@PathVariable UUID id) {
    Result<ExampleAggregate> result = exampleService.getExample(id);

    return result.isOk()
        ? ResponseEntity.ok(new ExampleResponse(result.getOrNull()))
        : ResponseEntity.notFound().build();
  }

  @GetMapping
  public ResponseEntity<List<ExampleResponse>> listAll() {
    return exampleService.listExamples().getOrElse(List.of()).stream()
        .map(ExampleResponse::new)
        .collect(java.util.stream.Collectors.collectingAndThen(
            java.util.stream.Collectors.toList(),
            ResponseEntity::ok));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(
      @PathVariable UUID id, @RequestBody UpdateExampleRequest request) {
    Result<ExampleAggregate> result = exampleService.updateExample(id, request.name());

    return result.isOk()
        ? ResponseEntity.ok(new ExampleResponse(result.getOrNull()))
        : ResponseEntity.badRequest().body(result.problemOrNull());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable UUID id) {
    Result<Void> result = exampleService.deleteExample(id);

    return result.isOk()
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  public record CreateExampleRequest(String name, String description) {}

  public record UpdateExampleRequest(String name) {}

  public record ExampleResponse(UUID id, String name, String description) {
    public ExampleResponse(ExampleAggregate aggregate) {
      this(aggregate.id(), aggregate.getName(), aggregate.getDescription());
    }
  }
}
