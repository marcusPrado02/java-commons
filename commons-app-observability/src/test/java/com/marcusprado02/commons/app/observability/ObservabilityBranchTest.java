package com.marcusprado02.commons.app.observability;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ObservabilityBranchTest {

  @AfterEach
  void clearContext() {
    RequestContext.clear();
  }

  // --- DefaultLogSanitizer ---

  @Test
  void sanitize_null_value_returns_null() {
    LogSanitizer s = DefaultLogSanitizer.basic();
    assertNull(s.sanitize("password", null));
  }

  @Test
  void sanitize_charsequence_with_null_key_falls_through_to_non_sensitive() {
    LogSanitizer s = DefaultLogSanitizer.basic();
    Object result = s.sanitize(null, "some value");
    assertEquals("some value", result);
  }

  @Test
  void sanitize_authorization_endswith_variant() {
    LogSanitizer s = DefaultLogSanitizer.basic();
    Object result = s.sanitize("X-Authorization", "Bearer token123");
    assertEquals("Bearer ***", result);
  }

  @Test
  void sanitize_maskAuthorization_blank_value_returns_blank() {
    LogSanitizer s = DefaultLogSanitizer.basic();
    Object result = s.sanitize("authorization", "   ");
    assertEquals("", ((String) result).trim());
  }

  @Test
  void sanitize_maskAuthorization_no_space_returns_redacted() {
    LogSanitizer s = DefaultLogSanitizer.basic();
    Object result = s.sanitize("authorization", "singletoken");
    assertEquals("***", result);
  }

  @Test
  void sanitize_nested_map_with_null_key() {
    LogSanitizer s = DefaultLogSanitizer.basic();
    Map<Object, Object> inner = new HashMap<>();
    inner.put(null, "val");
    Object result = s.sanitize("data", inner);
    assertNotNull(result);
  }

  @Test
  void sanitize_sensitive_keys_passwd_secret_api_key() {
    LogSanitizer s = DefaultLogSanitizer.basic();
    assertEquals("***", s.sanitize("passwd", "x"));
    assertEquals("***", s.sanitize("secret", "x"));
    assertEquals("***", s.sanitize("api_key", "x"));
    assertEquals("***", s.sanitize("apikey", "x"));
    assertEquals("***", s.sanitize("privatekey", "x"));
    assertEquals("***", s.sanitize("private_key", "x"));
    assertEquals("***", s.sanitize("client_secret", "x"));
    assertEquals("***", s.sanitize("refresh_token", "x"));
    assertEquals("***", s.sanitize("access_token", "x"));
    assertEquals("***", s.sanitize("cookie", "x"));
    assertEquals("***", s.sanitize("set-cookie", "x"));
  }

  @Test
  void sanitize_non_sensitive_key_non_string_value_returned_as_is() {
    LogSanitizer s = DefaultLogSanitizer.basic();
    Object result = s.sanitize("count", 42);
    assertEquals(42, result);
  }

  // --- HealthChecks branches ---

  @Test
  void healthChecks_constructor_null_list_defaults_to_empty() {
    HealthChecks checks = new HealthChecks(null);
    HealthReport report = checks.liveness();
    assertEquals(HealthStatus.UP, report.status());
    assertTrue(report.checks().isEmpty());
  }

  @Test
  void healthChecks_evaluate_skips_wrong_type() {
    HealthCheck livenessCheck =
        new HealthCheck() {
          @Override
          public String name() {
            return "live";
          }

          @Override
          public HealthCheckType type() {
            return HealthCheckType.LIVENESS;
          }

          @Override
          public HealthCheckResult check() {
            return HealthCheckResult.up(name(), type());
          }
        };

    HealthChecks checks = new HealthChecks(List.of(livenessCheck));
    HealthReport report = checks.readiness();
    assertEquals(HealthStatus.UP, report.status());
    assertTrue(report.checks().isEmpty());
  }

  @Test
  void healthChecks_evaluate_handles_runtime_exception() {
    HealthCheck failing =
        new HealthCheck() {
          @Override
          public String name() {
            return "broken";
          }

          @Override
          public HealthCheckType type() {
            return HealthCheckType.LIVENESS;
          }

          @Override
          public HealthCheckResult check() {
            throw new IllegalStateException("boom");
          }
        };

    HealthChecks checks = new HealthChecks(List.of(failing));
    HealthReport report = checks.liveness();
    assertEquals(HealthStatus.DOWN, report.status());
    assertEquals(1, report.checks().size());
  }

  @Test
  void healthChecks_aggregate_returns_degraded_when_any_degraded() {
    HealthCheck degradedCheck =
        new HealthCheck() {
          @Override
          public String name() {
            return "cache";
          }

          @Override
          public HealthCheckType type() {
            return HealthCheckType.READINESS;
          }

          @Override
          public HealthCheckResult check() {
            return HealthCheckResult.degraded(name(), type(), Map.of("latency", "high"));
          }
        };

    HealthChecks checks = new HealthChecks(List.of(degradedCheck));
    HealthReport report = checks.readiness();
    assertEquals(HealthStatus.DEGRADED, report.status());
  }

  // --- HealthCheckResult factory methods ---

  @Test
  void healthCheckResult_degraded_factory() {
    HealthCheckResult r =
        HealthCheckResult.degraded("svc", HealthCheckType.READINESS, Map.of("k", "v"));
    assertEquals(HealthStatus.DEGRADED, r.status());
    assertEquals("svc", r.name());
  }

  @Test
  void healthCheckResult_null_details_defaults_to_empty_map() {
    HealthCheckResult r =
        new HealthCheckResult("n", HealthCheckType.LIVENESS, HealthStatus.UP, null, Instant.now());
    assertTrue(r.details().isEmpty());
  }

  // --- HealthReport ---

  @Test
  void healthReport_null_checks_defaults_to_empty_list() {
    HealthReport r =
        new HealthReport(HealthCheckType.LIVENESS, HealthStatus.UP, null, Instant.now());
    assertTrue(r.checks().isEmpty());
  }

  // --- StructuredLog branches ---

  @Test
  void structuredLog_info_shorthand() {
    Map<String, Object> log = StructuredLog.info("hello");
    assertEquals("INFO", log.get("level"));
    assertEquals("hello", log.get("message"));
  }

  @Test
  void structuredLog_warn_shorthand() {
    Map<String, Object> log = StructuredLog.warn("warning");
    assertEquals("WARN", log.get("level"));
  }

  @Test
  void structuredLog_error_shorthand_includes_error_fields() {
    Map<String, Object> log = StructuredLog.error("oops", new RuntimeException("test error"));
    assertEquals("ERROR", log.get("level"));
    assertNotNull(log.get("error.type"));
    assertEquals("test error", log.get("error.message"));
  }

  @Test
  void structuredLog_builder_null_level_defaults_to_info() {
    Map<String, Object> log = StructuredLog.builder().level(null).message("m").build();
    assertEquals("INFO", log.get("level"));
  }

  @Test
  void structuredLog_builder_blank_level_defaults_to_info() {
    Map<String, Object> log = StructuredLog.builder().level("  ").message("m").build();
    assertEquals("INFO", log.get("level"));
  }

  @Test
  void structuredLog_builder_null_message_defaults_to_empty() {
    Map<String, Object> log = StructuredLog.builder().message(null).build();
    assertEquals("", log.get("message"));
  }

  @Test
  void structuredLog_builder_field_null_key_ignored() {
    Map<String, Object> log = StructuredLog.builder().field(null, "v").message("m").build();
    assertFalse(log.containsKey(null));
  }

  @Test
  void structuredLog_builder_field_blank_key_ignored() {
    Map<String, Object> log = StructuredLog.builder().field("  ", "v").message("m").build();
    assertFalse(log.containsKey("  "));
  }

  @Test
  void structuredLog_builder_field_null_value_ignored() {
    Map<String, Object> log = StructuredLog.builder().field("mykey", null).message("m").build();
    assertFalse(log.containsKey("mykey"));
  }

  @Test
  void structuredLog_builder_fields_null_map_ignored() {
    Map<String, Object> log = StructuredLog.builder().fields(null).message("m").build();
    assertNotNull(log);
  }

  @Test
  void structuredLog_builder_fields_empty_map_ignored() {
    Map<String, Object> log = StructuredLog.builder().fields(Map.of()).message("m").build();
    assertNotNull(log);
  }

  @Test
  void structuredLog_builder_sanitizer_null_uses_default() {
    Map<String, Object> log =
        StructuredLog.builder().sanitizer(null).field("password", "secret").message("m").build();
    assertEquals("***", log.get("password"));
  }

  @Test
  void structuredLog_builder_include_context_false_skips_context() {
    RequestContext.put(ContextKeys.CORRELATION_ID, "cid");
    Map<String, Object> log = StructuredLog.builder().includeContext(false).message("m").build();
    assertFalse(log.containsKey(ContextKeys.CORRELATION_ID));
  }

  @Test
  void structuredLog_formatKeyValue_null_map_returns_empty() {
    assertEquals("", StructuredLog.formatKeyValue(null));
  }

  @Test
  void structuredLog_formatKeyValue_empty_map_returns_empty() {
    assertEquals("", StructuredLog.formatKeyValue(Map.of()));
  }

  @Test
  void structuredLog_formatKeyValue_formats_entries() {
    Map<String, Object> m = new java.util.LinkedHashMap<>();
    m.put("a", "1");
    m.put("b", 2);
    String result = StructuredLog.formatKeyValue(m);
    assertEquals("a=1 b=2", result);
  }

  @Test
  void structuredLog_formatKeyValue_from_builder() {
    String kv = StructuredLog.builder().level("INFO").message("x").formatKeyValue();
    assertTrue(kv.contains("message=x"));
  }
}
