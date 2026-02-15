package com.marcusprado02.commons.app.ratelimiting;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitConfigTest {

  @Test
  void testBuilderPattern() {
    RateLimitConfig config = RateLimitConfig.builder()
        .capacity(100)
        .refillRate(50)
        .refillPeriod(Duration.ofMinutes(1))
        .build();

    assertEquals(100, config.getCapacity());
    assertEquals(50, config.getRefillRate());
    assertEquals(Duration.ofMinutes(1), config.getRefillPeriod());
  }

  @Test
  void testFactoryMethods() {
    // Test of() method
    RateLimitConfig config1 = RateLimitConfig.of(100, 50, Duration.ofMinutes(1));
    assertEquals(100, config1.getCapacity());
    assertEquals(50, config1.getRefillRate());
    assertEquals(Duration.ofMinutes(1), config1.getRefillPeriod());

    // Test withBurst() method
    RateLimitConfig config2 = RateLimitConfig.withBurst(100, 150, Duration.ofMinutes(1));
    assertEquals(150, config2.getCapacity()); // burst capacity
    assertEquals(100, config2.getRefillRate());
    assertEquals(Duration.ofMinutes(1), config2.getRefillPeriod());
  }

  @Test
  void testTimeBasedFactoryMethods() {
    // Test perSecond
    RateLimitConfig perSecond = RateLimitConfig.perSecond(10);
    assertEquals(10, perSecond.getCapacity());
    assertEquals(10, perSecond.getRefillRate());
    assertEquals(Duration.ofSeconds(1), perSecond.getRefillPeriod());

    // Test perMinute
    RateLimitConfig perMinute = RateLimitConfig.perMinute(60);
    assertEquals(60, perMinute.getCapacity());
    assertEquals(60, perMinute.getRefillRate());
    assertEquals(Duration.ofMinutes(1), perMinute.getRefillPeriod());

    // Test perHour
    RateLimitConfig perHour = RateLimitConfig.perHour(3600);
    assertEquals(3600, perHour.getCapacity());
    assertEquals(3600, perHour.getRefillRate());
    assertEquals(Duration.ofHours(1), perHour.getRefillPeriod());
  }

  @Test
  void testValidation() {
    // Valid configuration should not throw
    assertDoesNotThrow(() -> RateLimitConfig.builder()
        .capacity(10)
        .refillRate(5)
        .refillPeriod(Duration.ofSeconds(1))
        .build());

    // Invalid capacity
    assertThrows(IllegalArgumentException.class, () ->
        RateLimitConfig.builder()
            .capacity(0)
            .refillRate(5)
            .refillPeriod(Duration.ofSeconds(1))
            .build());

    assertThrows(IllegalArgumentException.class, () ->
        RateLimitConfig.builder()
            .capacity(-1)
            .refillRate(5)
            .refillPeriod(Duration.ofSeconds(1))
            .build());

    // Invalid refill rate
    assertThrows(IllegalArgumentException.class, () ->
        RateLimitConfig.builder()
            .capacity(10)
            .refillRate(0)
            .refillPeriod(Duration.ofSeconds(1))
            .build());

    assertThrows(IllegalArgumentException.class, () ->
        RateLimitConfig.builder()
            .capacity(10)
            .refillRate(-1)
            .refillPeriod(Duration.ofSeconds(1))
            .build());

    // Invalid refill period
    assertThrows(IllegalArgumentException.class, () ->
        RateLimitConfig.builder()
            .capacity(10)
            .refillRate(5)
            .refillPeriod(Duration.ZERO)
            .build());

    assertThrows(IllegalArgumentException.class, () ->
        RateLimitConfig.builder()
            .capacity(10)
            .refillRate(5)
            .refillPeriod(Duration.ofSeconds(-1))
            .build());

    // Null refill period
    assertThrows(IllegalArgumentException.class, () ->
        RateLimitConfig.builder()
            .capacity(10)
            .refillRate(5)
            .refillPeriod(null)
            .build());
  }

  @Test
  void testUtilityMethods() {
    RateLimitConfig config = RateLimitConfig.builder()
        .capacity(100)
        .refillRate(50)
        .refillPeriod(Duration.ofMinutes(1))
        .build();

    // Test rate per second calculation
    double ratePerSecond = config.getRatePerSecond();
    assertEquals(50.0 / 60.0, ratePerSecond, 0.001); // 50 per minute = ~0.833 per second

    // Test refill interval
    Duration refillInterval = config.getRefillInterval();
    assertEquals(Duration.ofSeconds(1).multipliedBy(60).dividedBy(50), refillInterval);
  }

  @Test
  void testEqualsAndHashCode() {
    RateLimitConfig config1 = RateLimitConfig.builder()
        .capacity(100)
        .refillRate(50)
        .refillPeriod(Duration.ofMinutes(1))
        .build();

    RateLimitConfig config2 = RateLimitConfig.builder()
        .capacity(100)
        .refillRate(50)
        .refillPeriod(Duration.ofMinutes(1))
        .build();

    RateLimitConfig config3 = RateLimitConfig.builder()
        .capacity(200)
        .refillRate(50)
        .refillPeriod(Duration.ofMinutes(1))
        .build();

    assertEquals(config1, config2);
    assertNotEquals(config1, config3);
    assertEquals(config1.hashCode(), config2.hashCode());
    assertNotEquals(config1.hashCode(), config3.hashCode());
  }

  @Test
  void testToString() {
    RateLimitConfig config = RateLimitConfig.perMinute(100);
    String toString = config.toString();

    assertTrue(toString.contains("capacity=100"));
    assertTrue(toString.contains("refillRate=100"));
    assertTrue(toString.contains("refillPeriod=PT1M"));
  }

  @Test
  void testImmutability() {
    RateLimitConfig config = RateLimitConfig.perMinute(100);

    // Verify that the object is immutable by checking no setters exist
    // and that all fields are final (this is enforced by the record)
    assertEquals(100, config.getCapacity());
    assertEquals(100, config.getRefillRate());
    assertEquals(Duration.ofMinutes(1), config.getRefillPeriod());

    // Creating a new config should not affect the original
    RateLimitConfig newConfig = RateLimitConfig.perSecond(10);
    assertEquals(100, config.getCapacity()); // Original unchanged
    assertEquals(10, newConfig.getCapacity()); // New config has different value
  }

  @Test
  void testEdgeCases() {
    // Very small capacity
    RateLimitConfig smallConfig = RateLimitConfig.builder()
        .capacity(1)
        .refillRate(1)
        .refillPeriod(Duration.ofSeconds(1))
        .build();

    assertEquals(1, smallConfig.getCapacity());
    assertEquals(1.0, smallConfig.getRatePerSecond(), 0.001);

    // Very large capacity
    RateLimitConfig largeConfig = RateLimitConfig.builder()
        .capacity(Long.MAX_VALUE)
        .refillRate(1000000)
        .refillPeriod(Duration.ofHours(1))
        .build();

    assertEquals(Long.MAX_VALUE, largeConfig.getCapacity());

    // Very small refill period
    RateLimitConfig fastRefill = RateLimitConfig.builder()
        .capacity(10)
        .refillRate(10)
        .refillPeriod(Duration.ofMillis(1))
        .build();

    assertEquals(Duration.ofMillis(1), fastRefill.getRefillPeriod());
    assertEquals(10000.0, fastRefill.getRatePerSecond(), 0.001); // 10 per millisecond = 10000 per second
  }
}
