package com.marcusprado02.commons.kernel.ddd.time;

import java.time.Clock;
import java.time.Instant;

/** Abstraction over the system clock; allows deterministic testing via fixed instants. */
public interface ClockProvider {

  Instant now();

  static ClockProvider system() {
    Clock clock = Clock.systemUTC();
    return clock::instant;
  }

  static ClockProvider fixed(Instant instant) {
    return () -> instant;
  }
}
