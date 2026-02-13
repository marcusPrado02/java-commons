package com.marcusprado02.commons.testkit;

import com.marcusprado02.commons.kernel.ddd.time.ClockProvider;
import java.time.Instant;

/**
 * Common test fixtures and default values.
 *
 * <p>Provides pre-configured test data for common domain concepts.
 */
public final class Fixtures {

  private Fixtures() {}

  // =========== Time Fixtures ===========

  /** Default test timestamp: 2024-01-01T00:00:00Z */
  public static final Instant DEFAULT_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

  /** Default test clock provider (fixed at DEFAULT_INSTANT) */
  public static final ClockProvider DEFAULT_CLOCK = TestClock.fixed(DEFAULT_INSTANT);

  // =========== Identifiers ===========

  /** Default test tenant ID */
  public static final String DEFAULT_TENANT_ID = "test-tenant";

  /** Default test correlation ID */
  public static final String DEFAULT_CORRELATION_ID = "test-correlation-id";

  /** Default test actor/user ID */
  public static final String DEFAULT_ACTOR_ID = "test-user";

  // =========== Factory Methods ===========

  /** Creates a clock provider at a specific instant */
  public static ClockProvider clockAt(Instant instant) {
    return TestClock.fixed(instant);
  }

  /** Creates a clock provider at a specific date/time */
  public static ClockProvider clockAt(int year, int month, int day, int hour, int minute) {
    return TestClock.fixedAt(year, month, day, hour, minute);
  }
}
