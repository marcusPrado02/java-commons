package com.marcusprado02.commons.app.observability;

import java.util.function.Supplier;

public interface TracerFacade {

  void inSpan(String spanName, Runnable action);

  <T> T inSpan(String spanName, Supplier<T> action);
}
