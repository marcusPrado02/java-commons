package com.marcusprado02.commons.app.observability;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultLogSanitizerTest {

  @Test
  void shouldRedactSensitiveKeys() {
    LogSanitizer sanitizer = DefaultLogSanitizer.basic();

    Map<String, Object> sanitized =
        sanitizer.sanitizeMap(
            Map.of(
                "password", "123",
                "apiKey", "k",
                "token", "t",
                "safe", "ok"));

    assertEquals("***", sanitized.get("password"));
    assertEquals("***", sanitized.get("apiKey"));
    assertEquals("***", sanitized.get("token"));
    assertEquals("ok", sanitized.get("safe"));
  }

  @Test
  void shouldMaskAuthorizationHeaderValue() {
    LogSanitizer sanitizer = DefaultLogSanitizer.basic();

    Map<String, Object> sanitized =
        sanitizer.sanitizeMap(Map.of("Authorization", "Bearer abc.def.ghi"));

    assertEquals("Bearer ***", sanitized.get("Authorization"));
  }
}
