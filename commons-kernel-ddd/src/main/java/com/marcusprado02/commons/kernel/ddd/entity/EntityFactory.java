package com.marcusprado02.commons.kernel.ddd.entity;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import com.marcusprado02.commons.kernel.ddd.audit.AuditStamp;
import com.marcusprado02.commons.kernel.ddd.context.ActorProvider;
import com.marcusprado02.commons.kernel.ddd.context.TenantProvider;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import java.time.Instant;

/**
 * Factory helper for creating entities with proper audit stamps.
 *
 * <p>Simplifies entity creation by automatically providing tenant and actor context.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * // In your entity factory/service
 * public class UserFactory {
 *     private final EntityFactory entityFactory;
 *
 *     public User createUser(UserId id, String name) {
 *         return entityFactory.create((tenantId, auditStamp) ->
 *             new User(id, tenantId, name, auditStamp)
 *         );
 *     }
 * }
 * }</pre>
 *
 * <p>Or without context providers:
 *
 * <pre>{@code
 * TenantId tenantId = TenantId.of("tenant-1");
 * String actorId = "user-123";
 * Instant now = Instant.now();
 *
 * User user = EntityFactory.create(tenantId, actorId, now,
 *     (tid, stamp) -> new User(UserId.generate(), tid, "John", stamp));
 * }</pre>
 */
public final class EntityFactory {

  private final TenantProvider tenantProvider;
  private final ActorProvider actorProvider;

  public EntityFactory(TenantProvider tenantProvider, ActorProvider actorProvider) {
    this.tenantProvider = tenantProvider;
    this.actorProvider = actorProvider;
  }

  /**
   * Creates an entity with current tenant, actor, and timestamp.
   *
   * @param creator the entity creator function
   * @param <E> the entity type
   * @return the created entity
   */
  public <E extends Entity<?>> E create(EntityCreator<E> creator) {
    TenantId tenantId = tenantProvider.currentTenant();
    ActorId actorId = actorProvider.currentActor();
    Instant now = Instant.now();
    AuditStamp stamp = AuditStamp.of(now, actorId);

    return creator.create(tenantId, stamp);
  }

  /**
   * Creates an entity with explicit values (no context providers).
   *
   * @param tenantId the tenant ID
   * @param actorId the actor ID
   * @param timestamp the timestamp
   * @param creator the entity creator function
   * @param <E> the entity type
   * @return the created entity
   */
  public static <E extends Entity<?>> E create(
      TenantId tenantId, ActorId actorId, Instant timestamp, EntityCreator<E> creator) {
    AuditStamp stamp = AuditStamp.of(timestamp, actorId);
    return creator.create(tenantId, stamp);
  }

  /**
   * Functional interface for entity creation.
   *
   * @param <E> the entity type
   */
  @FunctionalInterface
  public interface EntityCreator<E extends Entity<?>> {
    E create(TenantId tenantId, AuditStamp auditStamp);
  }
}
