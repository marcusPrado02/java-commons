package com.marcusprado02.commons.app.auditlog;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;

/**
 * Repository for storing and retrieving audit events.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * public class DatabaseAuditRepository implements AuditRepository {
 *     @Override
 *     public Result<Void> save(AuditEvent event) {
 *         // Save to database
 *         return Result.ok(null);
 *     }
 *
 *     @Override
 *     public Result<List<AuditEvent>> findByActor(String actor, int limit) {
 *         // Query database
 *         return Result.ok(events);
 *     }
 * }
 * }</pre>
 */
public interface AuditRepository {

  /**
   * Saves an audit event.
   *
   * @param event the event to save
   * @return result of save operation
   */
  Result<Void> save(AuditEvent event);

  /**
   * Saves multiple audit events.
   *
   * @param events the events to save
   * @return result of batch save operation
   */
  default Result<Void> saveAll(List<AuditEvent> events) {
    for (AuditEvent event : events) {
      Result<Void> result = save(event);
      if (!result.isOk()) {
        return result;
      }
    }
    return Result.ok(null);
  }

  /**
   * Finds an audit event by ID.
   *
   * @param id the event ID
   * @return the event, or error if not found
   */
  Result<AuditEvent> findById(String id);

  /**
   * Finds audit events by actor.
   *
   * @param actor the actor identifier
   * @param limit maximum number of events to return
   * @return list of events
   */
  Result<List<AuditEvent>> findByActor(String actor, int limit);

  /**
   * Finds audit events by resource.
   *
   * @param resourceType the resource type
   * @param resourceId the resource ID
   * @param limit maximum number of events to return
   * @return list of events
   */
  Result<List<AuditEvent>> findByResource(String resourceType, String resourceId, int limit);

  /**
   * Finds audit events by event type.
   *
   * @param eventType the event type
   * @param limit maximum number of events to return
   * @return list of events
   */
  Result<List<AuditEvent>> findByEventType(String eventType, int limit);

  /**
   * Queries audit events using criteria.
   *
   * @param query the query criteria
   * @return list of matching events
   */
  default Result<List<AuditEvent>> query(AuditQuery query) {
    return Result.ok(List.of());
  }
}
