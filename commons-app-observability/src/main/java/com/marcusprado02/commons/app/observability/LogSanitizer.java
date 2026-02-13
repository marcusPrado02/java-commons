package com.marcusprado02.commons.app.observability;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public interface LogSanitizer {

  Object sanitize(String key, Object value);

  default Map<String, Object> sanitizeMap(Map<String, ?> input) {
    Objects.requireNonNull(input, "input must not be null");

    Map<String, Object> out = new LinkedHashMap<>();
    input.forEach((k, v) -> out.put(k, sanitize(k, v)));
    return out;
  }
}
