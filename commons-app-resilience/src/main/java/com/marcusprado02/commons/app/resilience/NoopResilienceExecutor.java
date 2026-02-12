package com.marcusprado02.commons.app.resilience;

import java.util.Objects;
import java.util.function.Supplier;

public final class NoopResilienceExecutor implements ResilienceExecutor {

  @Override
  public void run(String name, ResiliencePolicySet policies, Runnable action) {
    Objects.requireNonNull(action, "action must not be null");
    action.run();
  }

  @Override
  public <T> T supply(String name, ResiliencePolicySet policies, Supplier<T> action) {
    Objects.requireNonNull(action, "action must not be null");
    return action.get();
  }
}
