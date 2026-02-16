package ${package}.domain;

import com.marcusprado02.commons.kernel.ddd.AggregateRoot;
import com.marcusprado02.commons.kernel.ddd.DomainEvent;
import java.util.UUID;

/**
 * Example aggregate root.
 *
 * <p>Replace this with your actual domain model.
 */
public class ExampleAggregate extends AggregateRoot<UUID> {

  private String name;
  private String description;

  public ExampleAggregate(UUID id, String name, String description) {
    super(id);
    this.name = name;
    this.description = description;

    registerEvent(new ExampleCreatedEvent(id, name));
  }

  public void updateName(String newName) {
    this.name = newName;
    registerEvent(new ExampleUpdatedEvent(id(), newName));
  }

  // Getters
  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  // Domain Events
  public record ExampleCreatedEvent(UUID id, String name) implements DomainEvent {}

  public record ExampleUpdatedEvent(UUID id, String newName) implements DomainEvent {}
}
