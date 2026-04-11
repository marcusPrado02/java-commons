package com.marcusprado02.commons.platform.time;

import java.time.Instant;

/** Provides the current system time as an {@link java.time.Instant}. */
public final class SystemClock {

  private SystemClock() {}

  public static Instant now() {
    return Instant.now();
  }
}
