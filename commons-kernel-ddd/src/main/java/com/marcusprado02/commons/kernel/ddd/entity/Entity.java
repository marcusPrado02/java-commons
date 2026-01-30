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
 * <pre>
 *     public class User extends Entity<UserId> {
 *         public User(UserId id, TenantId tenantId, AuditStamp created) {
 *             super(id, tenantId, created);
 *         }
 *     }
 * </pre>
 *
 * @param <ID> Type of the entity identifier.
 */
public abstract class Entity<ID> {

  private final ID id;
  private final TenantId tenantId;

  private EntityVersion version;
  private final AuditTrail audit;
  private DeletionStamp deletion; // nullable por design (soft delete)

  protected Entity(ID id, TenantId tenantId, AuditStamp created) {
    this.id = Objects.requireNonNull(id, "id");
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.version = EntityVersion.initial();
    this.audit = AuditTrail.createdNow(Objects.requireNonNull(created, "created"));
  }

  public final ID id() {
    return id;
  }

  public final TenantId tenantId() {
    return tenantId;
  }

  public final EntityVersion version() {
    return version;
  }

  public final AuditTrail audit() {
    return audit;
  }

  public final boolean isDeleted() {
    return deletion != null;
  }

  public final Optional<DeletionStamp> deletion() {
    return Optional.ofNullable(deletion);
  }

  /** Registers an update to the entity: - updates the audit trail - increments the version */
  protected final void touch(AuditStamp updated) {
    audit.touch(Objects.requireNonNull(updated, "updated"));
    version = version.next();
  }

  /** Soft delete: - marks the deletion stamp - touches to register update + bump version */
  protected final void softDelete(DeletionStamp deleted, AuditStamp updated) {
    if (this.deletion != null) return; // idempotent
    this.deletion = Objects.requireNonNull(deleted, "deleted");
    touch(updated);
  }

  /** Controlled undelete (if your domain allows). Maintains consistent audit/version. */
  protected final void restore(AuditStamp updated) {
    if (this.deletion == null) return;
    this.deletion = null;
    touch(updated);
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Entity<?> entity = (Entity<?>) o;
    return id.equals(entity.id);
  }

  @Override
  public final int hashCode() {
    return id.hashCode();
  }
}
