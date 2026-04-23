package com.marcusprado02.commons.kernel.time;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class KernelTimeStubsTest {

  @Test
  void clock_provider_stubs_are_instantiable() {
    ClockProvider system = new SystemClockProvider();
    ClockProvider fixed = new FixedClockProvider();
    TimeWindow window = new TimeWindow();

    assertInstanceOf(ClockProvider.class, system);
    assertInstanceOf(ClockProvider.class, fixed);
    assertNotNull(window);
    assertNotSame(system, fixed);
  }
}
