package com.marcusprado02.commons.testkit;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utilities for generating test IDs.
 *
 * <p>Provides sequential and random ID generation for testing purposes.
 */
public final class TestIds {

  private static final AtomicLong SEQUENCE = new AtomicLong(1);

  private TestIds() {}

  /** Generates a sequential numeric ID as string (1, 2, 3, ...). */
  public static String nextId() {
    return String.valueOf(SEQUENCE.getAndIncrement());
  }

  /** Generates a sequential ID with custom prefix (e.g., "user-1", "user-2"). */
  public static String nextId(String prefix) {
    return prefix + "-" + SEQUENCE.getAndIncrement();
  }

  /** Resets the sequence counter to 1. Useful for test isolation. */
  public static void reset() {
    SEQUENCE.set(1);
  }

  /** Generates a UUID-based random ID. */
  public static String randomId() {
    return java.util.UUID.randomUUID().toString();
  }

  /** Generates a timestamp-based ID (nanoseconds since epoch). */
  public static String timestampId() {
    return String.valueOf(Instant.now().toEpochMilli()) + "-" + System.nanoTime();
  }
}
