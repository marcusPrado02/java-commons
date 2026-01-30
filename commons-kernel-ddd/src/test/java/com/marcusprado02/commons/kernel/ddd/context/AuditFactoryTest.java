package com.marcusprado02.commons.kernel.ddd.context;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import com.marcusprado02.commons.kernel.ddd.time.ClockProvider;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditFactoryTest {

  @Test
  void should_create_stamps_with_fixed_time_and_actor() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    AuditFactory factory =
        new AuditFactory(ClockProvider.fixed(now), new FixedActorProvider(ActorId.of("user:1")));

    assertEquals(now, factory.created().at());
    assertEquals("user:1", factory.created().by().value());
  }
}
