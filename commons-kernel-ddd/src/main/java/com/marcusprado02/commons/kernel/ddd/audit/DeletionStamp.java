package com.marcusprado02.commons.kernel.ddd.audit;

import java.time.Instant;
import java.util.Objects;

/**
 * Soft delete information.
 *
 * <p>Usage:
 *
 * <pre>
 *     DeletionStamp deletionStamp = DeletionStamp.of(Instant.now(), actorId);
 * </pre>
 *
 * @see ActorId
 */
public record DeletionStamp(Instant at, ActorId by) {
  public DeletionStamp {
    Objects.requireNonNull(at, "at");
    Objects.requireNonNull(by, "by");
  }

  public static DeletionStamp of(Instant at, ActorId by) {
    return new DeletionStamp(at, by);
  }
}
