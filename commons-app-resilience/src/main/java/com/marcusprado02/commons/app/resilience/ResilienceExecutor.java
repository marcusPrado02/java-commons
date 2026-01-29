package com.marcusprado02.commons.app.resilience;

import java.util.function.Supplier;

public interface ResilienceExecutor {

    void run(String name, ResiliencePolicySet policies, Runnable action);

    <T> T supply(String name, ResiliencePolicySet policies, Supplier<T> action);
}
