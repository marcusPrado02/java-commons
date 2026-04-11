package com.marcusprado02.commons.app.resilience;

import java.time.Duration;

/** Configuration policy for retrying a failed operation with exponential backoff. */
public record RetryPolicy(int maxAttempts, Duration initialBackoff, Duration maxBackoff) {}
