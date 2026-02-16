package ${package}.adapters.web;

import ${package}.application.ExampleService;
import ${package}.domain.ExampleAggregate;
import com.marcusprado02.commons.kernel.result.Result;
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
        : ResponseEntity.badRequest().body(result.getErrorOrNull());
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(
      @PathVariable UUID id, @RequestBody UpdateExampleRequest request) {
    Result<ExampleAggregate> result = exampleService.updateExample(id, request.name());

    return result.isOk()
        ? ResponseEntity.ok(new ExampleResponse(result.getOrNull()))
        : ResponseEntity.badRequest().body(result.getErrorOrNull());
  }

  public record CreateExampleRequest(String name, String description) {}

  public record UpdateExampleRequest(String name) {}

  public record ExampleResponse(UUID id, String name, String description) {
    public ExampleResponse(ExampleAggregate aggregate) {
      this(aggregate.id(), aggregate.getName(), aggregate.getDescription());
    }
  }
}
