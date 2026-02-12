package com.marcusprado02.commons.app.resilience;

public record BulkheadPolicy(int maxConcurrentCalls) {}
