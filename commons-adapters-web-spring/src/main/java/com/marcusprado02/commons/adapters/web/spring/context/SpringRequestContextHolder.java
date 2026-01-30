package com.marcusprado02.commons.adapters.web.spring.context;

import com.marcusprado02.commons.platform.context.RequestContextSnapshot;
import java.util.Optional;

public final class SpringRequestContextHolder {

  private static final ThreadLocal<RequestContextSnapshot> CTX = new ThreadLocal<>();

  private SpringRequestContextHolder() {}

  public static void set(RequestContextSnapshot snapshot) {
    CTX.set(snapshot);
  }

  public static Optional<RequestContextSnapshot> get() {
    return Optional.ofNullable(CTX.get());
  }

  public static void clear() {
    CTX.remove();
  }
}
