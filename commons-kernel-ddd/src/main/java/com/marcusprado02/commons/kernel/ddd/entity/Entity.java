package com.marcusprado02.commons.kernel.ddd.entity;

import com.marcusprado02.commons.kernel.ddd.audit.AuditStamp;
import com.marcusprado02.commons.kernel.ddd.audit.AuditTrail;
import com.marcusprado02.commons.kernel.ddd.audit.DeletionStamp;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import com.marcusprado02.commons.kernel.ddd.version.EntityVersion;
import java.util.Objects;
import java.util.Optional;

/**
 * Base class for domain entities.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * public class User extends Entity<UserId> {
 *     public User(UserId id, TenantId tenantId, AuditStamp created) {
 *         super(id, tenantId, created);
 *     }
 * }
 * }</pre>
 *
 * @param <I> Type of the entity identifier.
 */
public abstract class Entity<I> {

  private final I id;
  private final TenantId tenantId;

  private EntityVersion version;
  private final AuditTrail audit;
  private DeletionStamp deletion; // nullable por design (soft delete)

  protected Entity(I id, TenantId tenantId, AuditStamp created) {
    this.id = Objects.requireNonNull(id, "id");
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.version = EntityVersion.initial();
    this.audit = AuditTrail.createdNow(Objects.requireNonNull(created, "created"));
  }

  /** Returns the entity identifier. */
  public final I id() {
    return id;
  }

  /** Returns the tenant this entity belongs to. */
  public final TenantId tenantId() {
    return tenantId;
  }

  /** Returns the current entity version. */
  public final EntityVersion version() {
    return version;
  }

  /** Returns the full audit trail of this entity. */
  public final AuditTrail audit() {
    return audit;
  }

  /** Returns true if this entity has been soft-deleted. */
  public final boolean isDeleted() {
    return deletion != null;
  }

  /** Returns the deletion stamp if this entity was soft-deleted. */
  public final Optional<DeletionStamp> deletion() {
    return Optional.ofNullable(deletion);
  }

  /** Registers an update to the entity: updates the audit trail and increments the version. */
  protected final void touch(AuditStamp updated) {
    audit.touch(Objects.requireNonNull(updated, "updated"));
    version = version.next();
  }

  /** Soft delete: marks the deletion stamp and touches to register update and bump version. */
  protected final void softDelete(DeletionStamp deleted, AuditStamp updated) {
    if (this.deletion != null) {
      return; // idempotent
    }
    this.deletion = Objects.requireNonNull(deleted, "deleted");
    touch(updated);
  }

  /** Controlled undelete (if your domain allows). Maintains consistent audit/version. */
  protected final void restore(AuditStamp updated) {
    if (this.deletion == null) {
      return;
    }
    this.deletion = null;
    touch(updated);
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Entity<?> entity = (Entity<?>) o;
    return id.equals(entity.id);
  }

  @Override
  public final int hashCode() {
    return id.hashCode();
  }
}
