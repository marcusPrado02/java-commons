package com.marcusprado02.commons.app.auditlog.inmemory;

import com.marcusprado02.commons.app.auditlog.AuditEvent;
import com.marcusprado02.commons.app.auditlog.AuditQuery;
import com.marcusprado02.commons.app.auditlog.AuditRepository;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.ProblemBuilder;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link AuditRepository}.
 *
 * <p>Useful for testing and development. Not recommended for production.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * AuditRepository repository = new InMemoryAuditRepository();
 * AuditService auditService = new DefaultAuditService(repository);
 * }</pre>
 */
public final class InMemoryAuditRepository implements AuditRepository {

  private final Map<String, AuditEvent> events = new ConcurrentHashMap<>();

  @Override
  public Result<Void> save(AuditEvent event) {
    String id = event.getId() != null ? event.getId() : UUID.randomUUID().toString();

    AuditEvent eventWithId =
        AuditEvent.builder()
            .id(id)
            .eventType(event.getEventType())
            .actor(event.getActor())
            .action(event.getAction())
            .resourceType(event.getResourceType())
            .resourceId(event.getResourceId())
            .timestamp(event.getTimestamp())
            .ipAddress(event.getIpAddress())
            .userAgent(event.getUserAgent())
            .metadata(event.getMetadata())
            .result(event.getResult())
            .errorMessage(event.getErrorMessage())
            .build();

    events.put(id, eventWithId);
    return Result.ok(null);
  }

  @Override
  public Result<AuditEvent> findById(String id) {
    AuditEvent event = events.get(id);
    if (event == null) {
      Problem problem =
          ProblemBuilder.notFound("AUDIT_EVENT_NOT_FOUND", "Audit event not found: " + id)
              .detail("eventId", id)
              .build();
      return Result.fail(problem);
    }
    return Result.ok(event);
  }

  @Override
  public Result<List<AuditEvent>> findByActor(String actor, int limit) {
    List<AuditEvent> result =
        events.values().stream()
            .filter(e -> actor.equals(e.getActor()))
            .sorted(Comparator.comparing(AuditEvent::getTimestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());

    return Result.ok(result);
  }

  @Override
  public Result<List<AuditEvent>> findByResource(
      String resourceType, String resourceId, int limit) {
    List<AuditEvent> result =
        events.values().stream()
            .filter(
                e ->
                    resourceType.equals(e.getResourceType())
                        && resourceId.equals(e.getResourceId()))
            .sorted(Comparator.comparing(AuditEvent::getTimestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());

    return Result.ok(result);
  }

  @Override
  public Result<List<AuditEvent>> findByEventType(String eventType, int limit) {
    List<AuditEvent> result =
        events.values().stream()
            .filter(e -> eventType.equals(e.getEventType()))
            .sorted(Comparator.comparing(AuditEvent::getTimestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());

    return Result.ok(result);
  }

  @Override
  public Result<List<AuditEvent>> query(AuditQuery query) {
    List<AuditEvent> result = new ArrayList<>(events.values());

    // Filter by actor
    if (query.getActor() != null) {
      result =
          result.stream()
              .filter(e -> query.getActor().equals(e.getActor()))
              .collect(Collectors.toList());
    }

    // Filter by event type
    if (query.getEventType() != null) {
      result =
          result.stream()
              .filter(e -> query.getEventType().equals(e.getEventType()))
              .collect(Collectors.toList());
    }

    // Filter by action
    if (query.getAction() != null) {
      result =
          result.stream()
              .filter(e -> query.getAction().equals(e.getAction()))
              .collect(Collectors.toList());
    }

    // Filter by resource type
    if (query.getResourceType() != null) {
      result =
          result.stream()
              .filter(e -> query.getResourceType().equals(e.getResourceType()))
              .collect(Collectors.toList());
    }

    // Filter by resource ID
    if (query.getResourceId() != null) {
      result =
          result.stream()
              .filter(e -> query.getResourceId().equals(e.getResourceId()))
              .collect(Collectors.toList());
    }

    // Filter by time range
    if (query.getFrom() != null) {
      result =
          result.stream()
              .filter(e -> !e.getTimestamp().isBefore(query.getFrom()))
              .collect(Collectors.toList());
    }

    if (query.getTo() != null) {
      result =
          result.stream()
              .filter(e -> !e.getTimestamp().isAfter(query.getTo()))
              .collect(Collectors.toList());
    }

    // Filter by result
    if (query.getResult() != null) {
      result =
          result.stream()
              .filter(e -> query.getResult().equals(e.getResult()))
              .collect(Collectors.toList());
    }

    // Sort by timestamp descending
    result.sort(Comparator.comparing(AuditEvent::getTimestamp).reversed());

    // Apply pagination
    int offset = query.getOffset();
    int limit = query.getLimit();

    if (offset >= result.size()) {
      return Result.ok(List.of());
    }

    int toIndex = Math.min(offset + limit, result.size());
    return Result.ok(result.subList(offset, toIndex));
  }

  /**
   * Clears all audit events.
   *
   * <p>Useful for testing.
   */
  public void clear() {
    events.clear();
  }

  /**
   * Gets the total number of audit events.
   *
   * @return event count
   */
  public int size() {
    return events.size();
  }
}
