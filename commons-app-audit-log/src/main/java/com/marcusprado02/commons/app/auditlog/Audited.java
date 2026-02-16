package com.marcusprado02.commons.app.auditlog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for automatic audit logging.
 *
 * <p>When applied to a method, an audit event is automatically created and recorded.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * @Service
 * public class UserService {
 *
 *     @Audited(
 *         eventType = "USER_CREATED",
 *         action = "create",
 *         resourceType = "User"
 *     )
 *     public User createUser(User user) {
 *         // Method implementation
 *         return user;
 *     }
 *
 *     @Audited(
 *         eventType = "USER_UPDATED",
 *         action = "update",
 *         resourceType = "User",
 *         resourceIdParam = "userId"
 *     )
 *     public User updateUser(String userId, UserUpdateDto dto) {
 *         // Method implementation
 *         return user;
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

  /**
   * The event type for this audit event.
   *
   * @return event type
   */
  String eventType();

  /**
   * The action being performed.
   *
   * @return action name
   */
  String action();

  /**
   * The resource type being audited.
   *
   * @return resource type
   */
  String resourceType() default "";

  /**
   * Parameter name containing the resource ID.
   *
   * <p>If specified, the value will be extracted from the method parameter with this name.
   *
   * @return parameter name
   */
  String resourceIdParam() default "";

  /**
   * SpEL expression to extract resource ID.
   *
   * <p>Evaluated against method result or parameters.
   *
   * @return SpEL expression
   */
  String resourceIdExpression() default "";

  /**
   * Whether to include method parameters in metadata.
   *
   * @return true to include parameters
   */
  boolean includeParameters() default false;

  /**
   * Whether to include method result in metadata.
   *
   * @return true to include result
   */
  boolean includeResult() default false;

  /**
   * Whether to audit even on method failure.
   *
   * @return true to audit failures
   */
  boolean auditOnFailure() default true;
}
