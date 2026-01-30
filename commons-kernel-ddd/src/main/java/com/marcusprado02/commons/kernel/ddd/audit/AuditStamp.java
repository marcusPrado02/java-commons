package com.marcusprado02.commons.kernel.ddd.audit;

import java.time.Instant;
import java.util.Objects;

public record AuditStamp(Instant at, ActorId by) {
  public AuditStamp {
    Objects.requireNonNull(at, "at");
    Objects.requireNonNull(by, "by");
  }

  public static AuditStamp of(Instant at, ActorId by) {
    return new AuditStamp(at, by);
  }
}
