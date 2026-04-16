package com.marcusprado02.commons.app.auditlog;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Service for capturing and storing audit events.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * @Service
 * public class UserService {
 *     private final AuditService auditService;
 *
 *     public void createUser(User user) {
 *         // Create user...
 *
 *         auditService.audit(
 *             AuditEvent.builder()
 *                 .eventType("USER_CREATED")
 *                 .actor(getCurrentUser())
 *                 .action("create")
 *                 .resourceType("User")
 *                 .resourceId(user.getId())
 *                 .build()
 *         );
 *     }
 * }
 * }</pre>
 */
public interface AuditService {

  /**
   * Records an audit event.
   *
   * @param event the event to record
   * @return result of audit operation
   */
  Result<Void> audit(AuditEvent event);

  /**
   * Records an audit event asynchronously.
   *
   * <p>This method returns immediately and the event is processed in the background.
   *
   * @param event the event to record
   */
  void auditAsync(AuditEvent event);
}
