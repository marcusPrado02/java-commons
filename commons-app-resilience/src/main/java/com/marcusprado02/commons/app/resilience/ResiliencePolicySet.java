package com.marcusprado02.commons.app.resilience;

public record ResiliencePolicySet(
    RetryPolicy retry,
    TimeoutPolicy timeout,
    CircuitBreakerPolicy circuitBreaker,
    BulkheadPolicy bulkhead) {}
