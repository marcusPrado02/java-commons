package com.marcusprado02.commons.app.resilience;

/** Composite set of resilience policies applied to an operation. */
public record ResiliencePolicySet(
    RetryPolicy retry,
    TimeoutPolicy timeout,
    CircuitBreakerPolicy circuitBreaker,
    BulkheadPolicy bulkhead,
    RateLimiterPolicy rateLimiter,
    CachePolicy cache) {}
