package com.marcusprado02.commons.app.resilience;

import java.time.Duration;

/** Configuration policy for timing out an operation after a fixed duration. */
public record TimeoutPolicy(Duration timeout) {}
