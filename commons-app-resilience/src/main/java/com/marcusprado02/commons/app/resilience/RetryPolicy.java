package com.marcusprado02.commons.app.resilience;

import java.time.Duration;

public record RetryPolicy(
        int maxAttempts,
        Duration initialBackoff,
        Duration maxBackoff
) {
}
