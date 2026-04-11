package com.marcusprado02.commons.app.resilience;

/** Configuration policy for a circuit breaker. */
public record CircuitBreakerPolicy(float failureRateThreshold, int slidingWindowSize) {}
