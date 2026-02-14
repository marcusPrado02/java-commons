package com.marcusprado02.commons.app.observability;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class StructuredLog {
  private StructuredLog() {}

  public static Builder builder() {
    return new Builder(Clock.systemUTC());
  }

  public static Builder builder(Clock clock) {
    return new Builder(clock);
  }

  public static Map<String, Object> info(String message) {
    return builder().level("INFO").message(message).build();
  }

  public static Map<String, Object> warn(String message) {
    return builder().level("WARN").message(message).build();
  }

  public static Map<String, Object> error(String message, Throwable error) {
    return builder().level("ERROR").message(message).error(error).build();
  }

  public static final class Builder {
    private final Clock clock;

    private String level;
    private String message;
    private Throwable error;

    private Map<String, Object> fields = new LinkedHashMap<>();
    private boolean includeContext = true;
    private LogSanitizer sanitizer = DefaultLogSanitizer.basic();

    private Builder(Clock clock) {
      this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Builder level(String level) {
      this.level = level;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder error(Throwable error) {
      this.error = error;
      return this;
    }

    public Builder field(String key, Object value) {
      if (key != null && !key.isBlank() && value != null) {
        fields.put(key, value);
      }
      return this;
    }

    public Builder fields(Map<String, ?> fields) {
      if (fields != null && !fields.isEmpty()) {
        fields.forEach((k, v) -> field(k, v));
      }
      return this;
    }

    public Builder includeContext(boolean includeContext) {
      this.includeContext = includeContext;
      return this;
    }

    public Builder sanitizer(LogSanitizer sanitizer) {
      this.sanitizer = (sanitizer == null) ? DefaultLogSanitizer.basic() : sanitizer;
      return this;
    }

    public Map<String, Object> build() {
      Instant now = clock.instant();

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("timestamp", now.toString());
      result.put("level", (level == null || level.isBlank()) ? "INFO" : level.trim());
      result.put("message", (message == null) ? "" : message);

      if (includeContext) {
        RequestContext.snapshot()
            .forEach(
                (k, v) -> {
                  if (k != null && !k.isBlank() && v != null && !v.isBlank()) {
                    result.put(k, v);
                  }
                });
      }

      fields.forEach(
          (k, v) -> {
            if (k != null && !k.isBlank() && v != null) {
              result.put(k, v);
            }
          });

      if (error != null) {
        result.put("error.type", error.getClass().getName());
        result.put("error.message", error.getMessage());
      }

      Map<String, Object> sanitized = sanitizer.sanitizeMap(result);
      return Collections.unmodifiableMap(sanitized);
    }

    public String formatKeyValue() {
      return StructuredLog.formatKeyValue(build());
    }
  }

  public static String formatKeyValue(Map<String, ?> fields) {
    if (fields == null || fields.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, ?> e : fields.entrySet()) {
      if (!first) {
        sb.append(' ');
      }
      first = false;

      String key = e.getKey();
      Object value = e.getValue();

      sb.append(key == null ? "" : key);
      sb.append('=');
      sb.append(value == null ? "null" : String.valueOf(value));
    }
    return sb.toString();
  }
}
