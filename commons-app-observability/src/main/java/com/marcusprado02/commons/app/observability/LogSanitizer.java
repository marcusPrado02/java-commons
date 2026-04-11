package com.marcusprado02.commons.app.observability;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Strategy interface for sanitizing sensitive log field values before output. */
public interface LogSanitizer {

  /**
   * Sanitizes a single log field value.
   *
   * @param key the field key
   * @param value the field value to sanitize
   * @return the sanitized value
   */
  Object sanitize(String key, Object value);

  /**
   * Sanitizes all entries in a map by applying {@link #sanitize} to each value.
   *
   * @param input the map to sanitize
   * @return a new map with sanitized values
   */
  default Map<String, Object> sanitizeMap(Map<String, ?> input) {
    Objects.requireNonNull(input, "input must not be null");

    Map<String, Object> out = new LinkedHashMap<>();
    input.forEach((k, v) -> out.put(k, sanitize(k, v)));
    return out;
  }
}
