package com.marcusprado02.commons.app.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ObservabilityModelTest {

  @AfterEach
  void clearContext() {
    RequestContext.clear();
  }

  // ── RequestContext ────────────────────────────────────────────────────────

  @Test
  void requestContextPutAndGet() {
    RequestContext.put("key", "value");
    assertEquals("value", RequestContext.get("key"));
  }

  @Test
  void requestContextPutNullKeyIsIgnored() {
    RequestContext.put(null, "value");
    assertNull(RequestContext.get(null));
  }

  @Test
  void requestContextPutNullValueIsIgnored() {
    RequestContext.put("key", null);
    assertNull(RequestContext.get("key"));
  }

  @Test
  void requestContextGetNullKey() {
    assertNull(RequestContext.get(null));
  }

  @Test
  void requestContextSnapshot() {
    RequestContext.put("a", "1");
    RequestContext.put("b", "2");
    assertEquals("1", RequestContext.snapshot().get("a"));
    assertEquals("2", RequestContext.snapshot().get("b"));
  }

  @Test
  void requestContextClear() {
    RequestContext.put("x", "y");
    RequestContext.clear();
    assertTrue(RequestContext.snapshot().isEmpty());
  }

  // ── CorrelationId ─────────────────────────────────────────────────────────

  @Test
  void correlationIdNewIdIsUuid() {
    String id1 = CorrelationId.newId();
    String id2 = CorrelationId.newId();
    assertEquals(36, id1.length());
    assertNotEquals(id1, id2);
  }

  // ── ContextKeys constants ─────────────────────────────────────────────────

  @Test
  void contextKeysConstants() {
    assertEquals("correlation_id", ContextKeys.CORRELATION_ID);
    assertEquals("tenant_id", ContextKeys.TENANT_ID);
    assertEquals("actor_id", ContextKeys.ACTOR_ID);
  }

  // ── Metrics.recordRequest branches ───────────────────────────────────────

  @Test
  void metricsRecordRequestSuccess() {
    NoopMetricsFacade noop = NoopMetricsFacade.INSTANCE;
    Metrics.recordRequest(noop, "my-op", Duration.ofMillis(50), true);
  }

  @Test
  void metricsRecordRequestFailure() {
    NoopMetricsFacade noop = NoopMetricsFacade.INSTANCE;
    Metrics.recordRequest(noop, "my-op", Duration.ofMillis(10), false);
  }

  @Test
  void metricsRecordRequestNullOperation() {
    Metrics.recordRequest(NoopMetricsFacade.INSTANCE, null, Duration.ofMillis(5), true);
  }

  @Test
  void metricsRecordRequestBlankOperation() {
    Metrics.recordRequest(NoopMetricsFacade.INSTANCE, "   ", Duration.ofMillis(5), false);
  }

  @Test
  void metricsRecordRequestNullLatency() {
    Metrics.recordRequest(NoopMetricsFacade.INSTANCE, "op", null, true);
  }

  @Test
  void metricsRecordRequestNullMetrics() {
    assertThrows(NullPointerException.class, () -> Metrics.recordRequest(null, "op", null, true));
  }
}
