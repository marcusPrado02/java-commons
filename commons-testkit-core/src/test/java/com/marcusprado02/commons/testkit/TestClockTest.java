package com.marcusprado02.commons.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TestClockTest {

  @Test
  void shouldCreateFixedClock() {
    Instant instant = Instant.parse("2024-01-15T10:30:00Z");
    var clock = TestClock.fixed(instant);

    assertThat(clock.now()).isEqualTo(instant);
    assertThat(clock.now()).isEqualTo(instant); // Always same
  }

  @Test
  void shouldCreateFixedClockAtSpecificDate() {
    var clock = TestClock.fixedAt(2024, 6, 15, 14, 30);
    Instant expected = Instant.parse("2024-06-15T14:30:00Z");

    assertThat(clock.now()).isEqualTo(expected);
  }

  @Test
  void shouldCreateAdvancingClock() {
    Instant start = Instant.parse("2024-01-01T00:00:00Z");
    var clock = TestClock.advancing(start, 1000); // 1 second increment

    assertThat(clock.now()).isEqualTo(start);
    assertThat(clock.now()).isEqualTo(start.plusMillis(1000));
    assertThat(clock.now()).isEqualTo(start.plusMillis(2000));
  }

  @Test
  void shouldProvideDefaultInstant() {
    Instant instant = TestClock.defaultInstant();
    assertThat(instant).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
  }

  @Test
  void shouldProvideDefaultClockProvider() {
    var clock = TestClock.defaultClockProvider();
    assertThat(clock.now()).isEqualTo(TestClock.defaultInstant());
  }
}
