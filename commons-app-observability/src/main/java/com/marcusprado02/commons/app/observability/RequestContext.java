package com.marcusprado02.commons.app.observability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RequestContext {

  private static final ThreadLocal<Map<String, String>> CTX = ThreadLocal.withInitial(HashMap::new);

  private RequestContext() {}

  public static void put(String key, String value) {
    if (key == null || value == null) {
      return;
    }
    CTX.get().put(key, value);
  }

  public static String get(String key) {
    if (key == null) {
      return null;
    }
    return CTX.get().get(key);
  }

  public static Map<String, String> snapshot() {
    return Collections.unmodifiableMap(new HashMap<>(CTX.get()));
  }

  public static void clear() {
    CTX.remove();
  }
}
