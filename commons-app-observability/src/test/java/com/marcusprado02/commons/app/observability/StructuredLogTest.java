package com.marcusprado02.commons.app.observability;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StructuredLogTest {

  @AfterEach
  void tearDown() {
    RequestContext.clear();
  }

  @Test
  void shouldIncludeRequestContextByDefault() {
    RequestContext.put(ContextKeys.CORRELATION_ID, "c1");
    RequestContext.put(ContextKeys.TENANT_ID, "t1");

    Clock clock = Clock.fixed(Instant.parse("2026-02-13T00:00:00Z"), ZoneOffset.UTC);

    Map<String, Object> log =
        StructuredLog.builder(clock).level("INFO").message("hello").field("k", "v").build();

    assertEquals("INFO", log.get("level"));
    assertEquals("hello", log.get("message"));
    assertEquals("c1", log.get(ContextKeys.CORRELATION_ID));
    assertEquals("t1", log.get(ContextKeys.TENANT_ID));
    assertEquals("v", log.get("k"));
    assertEquals("2026-02-13T00:00:00Z", log.get("timestamp"));
  }

  @Test
  void shouldSanitizeFields() {
    Map<String, Object> log = StructuredLog.builder().message("x").field("password", "123").build();

    assertEquals("***", log.get("password"));
  }
}
