package com.marcusprado02.commons.ports.servicediscovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class HealthCheckTest {

  @Test
  void http_shouldCreateHttpHealthCheck() {
    HealthCheck check = HealthCheck.http("http://localhost:8080/health").build();

    assertThat(check.type()).isEqualTo(HealthCheck.Type.HTTP);
    assertThat(check.endpoint()).isEqualTo("http://localhost:8080/health");
    assertThat(check.interval()).isEqualTo(Duration.ofSeconds(10));
    assertThat(check.timeout()).isEqualTo(Duration.ofSeconds(5));
    assertThat(check.deregisterAfter()).isEqualTo(Duration.ofMinutes(1));
  }

  @Test
  void tcp_shouldCreateTcpHealthCheck() {
    HealthCheck check = HealthCheck.tcp("localhost:8080").build();

    assertThat(check.type()).isEqualTo(HealthCheck.Type.TCP);
    assertThat(check.endpoint()).isEqualTo("localhost:8080");
  }

  @Test
  void ttl_shouldCreateTtlHealthCheck() {
    HealthCheck check = HealthCheck.ttl(Duration.ofSeconds(30)).build();

    assertThat(check.type()).isEqualTo(HealthCheck.Type.TTL);
    assertThat(check.endpoint()).isNull();
    assertThat(check.interval()).isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  void builder_shouldAllowCustomIntervalTimeoutAndDeregister() {
    HealthCheck check =
        HealthCheck.http("http://svc/health")
            .interval(Duration.ofSeconds(15))
            .timeout(Duration.ofSeconds(3))
            .deregisterAfter(Duration.ofMinutes(5))
            .build();

    assertThat(check.interval()).isEqualTo(Duration.ofSeconds(15));
    assertThat(check.timeout()).isEqualTo(Duration.ofSeconds(3));
    assertThat(check.deregisterAfter()).isEqualTo(Duration.ofMinutes(5));
  }

  @Test
  void constructor_withNullType_shouldThrow() {
    assertThatThrownBy(
            () ->
                new HealthCheck(
                    null, "http://x/health", Duration.ofSeconds(10), Duration.ofSeconds(5), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructor_withNullEndpointForHttpCheck_shouldThrow() {
    assertThatThrownBy(
            () ->
                new HealthCheck(
                    HealthCheck.Type.HTTP,
                    null,
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(5),
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("endpoint cannot be null");
  }

  @Test
  void constructor_withNullEndpointForTtl_shouldBeAllowed() {
    HealthCheck check =
        new HealthCheck(HealthCheck.Type.TTL, null, Duration.ofSeconds(30), null, null);

    assertThat(check.type()).isEqualTo(HealthCheck.Type.TTL);
    assertThat(check.endpoint()).isNull();
  }

  @Test
  void type_shouldHaveThreeValues() {
    assertThat(HealthCheck.Type.values())
        .containsExactlyInAnyOrder(
            HealthCheck.Type.HTTP, HealthCheck.Type.TCP, HealthCheck.Type.TTL);
  }
}
