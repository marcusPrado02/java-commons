package com.marcusprado02.commons.app.ratelimiting;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterFactoryTest {

  @Test
  void shouldBuildInMemoryRateLimiter() {
    var config =
        RateLimitConfig.builder()
            .capacity(10)
            .refillRate(5)
            .refillPeriod(Duration.ofSeconds(1))
            .build();

    var limiter = RateLimiterFactory.inMemory().withConfig(config).build();

    assertThat(limiter).isNotNull();
    var result = limiter.tryConsume("key");
    assertThat(result.isAllowed()).isTrue();
  }

  @Test
  void shouldThrowWhenInMemoryConfigMissing() {
    assertThatThrownBy(() -> RateLimiterFactory.inMemory().build())
        .isInstanceOf(IllegalStateException.class);
  }
}
