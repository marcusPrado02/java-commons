package com.marcusprado02.commons.ports.notification;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationResultTest {

  @Test
  void success_creates_success_result() {
    SendNotificationResult r = SendNotificationResult.success("msg-123");
    assertTrue(r.success());
    assertEquals("msg-123", r.messageId());
    assertNull(r.failureReason());
    assertEquals(1, r.successCount());
    assertEquals(0, r.failureCount());
    assertFalse(r.hasFailures());
    assertTrue(r.getFailedTokens().isEmpty());
  }

  @Test
  void failure_creates_failure_result() {
    SendNotificationResult r = SendNotificationResult.failure("Token expired");
    assertFalse(r.success());
    assertNull(r.messageId());
    assertEquals("Token expired", r.failureReason());
    assertEquals(0, r.successCount());
    assertEquals(1, r.failureCount());
    assertTrue(r.hasFailures());
  }

  @Test
  void multicast_all_succeed() {
    SendNotificationResult r = SendNotificationResult.multicast(10, 0, Map.of());
    assertTrue(r.success());
    assertEquals(10, r.successCount());
    assertEquals(0, r.failureCount());
    assertFalse(r.hasFailures());
  }

  @Test
  void multicast_some_fail() {
    Map<String, String> failures = Map.of("token-1", "InvalidToken");
    SendNotificationResult r = SendNotificationResult.multicast(9, 1, failures);
    assertFalse(r.success());
    assertEquals(9, r.successCount());
    assertEquals(1, r.failureCount());
    assertTrue(r.hasFailures());
    assertFalse(r.getFailedTokens().isEmpty());
  }

  @Test
  void null_failures_map_defaults_to_empty() {
    SendNotificationResult r = new SendNotificationResult(true, "id", null, 1, 0, null);
    assertNotNull(r.failures());
    assertTrue(r.failures().isEmpty());
  }

  @Test
  void non_null_failures_map_is_copied() {
    Map<String, String> failures = Map.of("token-abc", "Expired");
    SendNotificationResult r = new SendNotificationResult(false, null, null, 0, 1, failures);
    assertEquals(1, r.failures().size());
    assertEquals(1, r.getFailedTokens().size());
  }
}
