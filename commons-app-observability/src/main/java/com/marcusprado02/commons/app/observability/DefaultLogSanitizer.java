package com.marcusprado02.commons.app.observability;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Default log sanitizer with conservative redaction of common secret fields. */
public final class DefaultLogSanitizer implements LogSanitizer {

  private static final String REDACTED = "***";

  private DefaultLogSanitizer() {}

  public static DefaultLogSanitizer basic() {
    return new DefaultLogSanitizer();
  }

  @Override
  public Object sanitize(String key, Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof CharSequence cs && key != null && looksLikeAuthorizationHeader(key)) {
      return maskAuthorization(cs.toString());
    }

    if (key != null && isSensitiveKey(key)) {
      return REDACTED;
    }

    if (value instanceof Map<?, ?> map) {
      return sanitizeNestedMap(map);
    }

    return value;
  }

  private static Map<String, Object> sanitizeNestedMap(Map<?, ?> input) {
    Map<String, Object> out = new LinkedHashMap<>();
    input.forEach(
        (k, v) -> {
          String key = (k == null) ? null : String.valueOf(k);
          out.put(key, DefaultLogSanitizer.basic().sanitize(key, v));
        });
    return out;
  }

  private static boolean isSensitiveKey(String key) {
    String k = key.toLowerCase(Locale.ROOT);

    return k.contains("password")
        || k.contains("passwd")
        || k.contains("secret")
        || k.contains("token")
        || k.contains("apikey")
        || k.contains("api_key")
        || k.contains("privatekey")
        || k.contains("private_key")
        || k.contains("client_secret")
        || k.contains("refresh_token")
        || k.contains("access_token")
        || k.equals("cookie")
        || k.equals("set-cookie");
  }

  private static boolean looksLikeAuthorizationHeader(String key) {
    String k = key.toLowerCase(Locale.ROOT);
    return k.equals("authorization") || k.endsWith("authorization");
  }

  private static String maskAuthorization(String value) {
    Objects.requireNonNull(value, "value must not be null");

    String trimmed = value.trim();
    if (trimmed.isBlank()) {
      return trimmed;
    }

    // Preserve scheme when present.
    int space = trimmed.indexOf(' ');
    if (space > 0) {
      return trimmed.substring(0, space) + " " + REDACTED;
    }

    return REDACTED;
  }
}
