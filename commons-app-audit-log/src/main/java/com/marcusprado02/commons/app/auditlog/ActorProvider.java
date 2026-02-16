package com.marcusprado02.commons.app.auditlog;

/**
 * Provides the current actor (user) for audit logging.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * @Component
 * public class SecurityContextActorProvider implements ActorProvider {
 *     @Override
 *     public String getCurrentActor() {
 *         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *         return auth != null ? auth.getName() : "anonymous";
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface ActorProvider {

  /**
   * Gets the current actor identifier.
   *
   * @return actor identifier (e.g., username, user ID)
   */
  String getCurrentActor();

  /**
   * Gets additional actor information (e.g., IP address, user agent).
   *
   * @return actor context, or null if not available
   */
  default ActorContext getActorContext() {
    return null;
  }
}
