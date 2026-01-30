package com.marcusprado02.commons.kernel.ddd.time;

import java.time.Clock;
import java.time.Instant;

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
