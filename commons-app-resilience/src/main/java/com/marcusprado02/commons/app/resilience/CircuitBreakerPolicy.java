package com.marcusprado02.commons.app.resilience;

public record CircuitBreakerPolicy(
        float failureRateThreshold,
        int slidingWindowSize
) {
}
