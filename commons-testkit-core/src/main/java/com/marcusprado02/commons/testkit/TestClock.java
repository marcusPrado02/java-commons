package com.marcusprado02.commons.testkit;

import com.marcusprado02.commons.kernel.ddd.time.ClockProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Utilities for creating test clocks with fixed or controllable time.
 *
 * <p>Useful for testing time-dependent logic without relying on system time.
 */
public final class TestClock {

  private TestClock() {}

  /**
   * Creates a ClockProvider fixed at the given instant.
   *
   * @param instant the fixed time
   * @return a ClockProvider that always returns the same instant
   */
  public static ClockProvider fixed(Instant instant) {
    return () -> instant;
  }

  /**
   * Creates a ClockProvider fixed at a specific date/time.
   *
   * @param year year
   * @param month month (1-12)
   * @param day day of month
   * @param hour hour (0-23)
   * @param minute minute (0-59)
   * @return a ClockProvider fixed at the specified time
   */
  public static ClockProvider fixedAt(int year, int month, int day, int hour, int minute) {
    Instant instant =
        Instant.parse(
            String.format(
                "%04d-%02d-%02dT%02d:%02d:00Z", year, month, day, hour, minute));
    return fixed(instant);
  }

  /**
   * Creates a ClockProvider that starts at the given instant and advances on each call.
   *
   * @param start starting instant
   * @param incrementMillis milliseconds to advance on each now() call
   * @return a ClockProvider that advances time
   */
  public static ClockProvider advancing(Instant start, long incrementMillis) {
    return new AdvancingClockProvider(start, incrementMillis);
  }

  /** ClockProvider implementation that advances time on each call. */
  private static final class AdvancingClockProvider implements ClockProvider {
    private Instant current;
    private final long incrementMillis;

    AdvancingClockProvider(Instant start, long incrementMillis) {
      this.current = start;
      this.incrementMillis = incrementMillis;
    }

    @Override
    public synchronized Instant now() {
      Instant result = current;
      current = current.plusMillis(incrementMillis);
      return result;
    }
  }

  /** Creates a Clock (java.time.Clock) fixed at the given instant in UTC. */
  public static Clock fixedClock(Instant instant) {
    return Clock.fixed(instant, ZoneId.of("UTC"));
  }

  /** Default test instant: 2024-01-01T00:00:00Z */
  public static Instant defaultInstant() {
    return Instant.parse("2024-01-01T00:00:00Z");
  }

  /** Default test ClockProvider: fixed at 2024-01-01T00:00:00Z */
  public static ClockProvider defaultClockProvider() {
    return fixed(defaultInstant());
  }
}
