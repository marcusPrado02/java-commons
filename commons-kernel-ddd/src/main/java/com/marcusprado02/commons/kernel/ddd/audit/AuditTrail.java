package com.marcusprado02.commons.kernel.ddd.audit;

import java.util.Objects;

/**
 * Represents the audit trail of an entity, capturing its creation and last update information.
 *
 * <p>Immutability is maintained for the creation stamp, while the update stamp can be modified to
 * reflect changes.
 *
 * <p>Usage:
 *
 * <pre>
 *     AuditStamp creationStamp = AuditStamp.of(Instant.now(), actorId);
 *     AuditTrail auditTrail = AuditTrail.createdNow(creationStamp);
 * </pre>
 *
 * @see AuditStamp
 */
public final class AuditTrail {

  private final AuditStamp created;
  private AuditStamp updated;

  private AuditTrail(AuditStamp created, AuditStamp updated) {
    this.created = Objects.requireNonNull(created, "created");
    this.updated = Objects.requireNonNull(updated, "updated");
  }

  public static AuditTrail createdNow(AuditStamp created) {
    return new AuditTrail(created, created);
  }

  public AuditStamp created() {
    return created;
  }

  public AuditStamp updated() {
    return updated;
  }

  public void touch(AuditStamp updated) {
    this.updated = Objects.requireNonNull(updated, "updated");
  }
}
