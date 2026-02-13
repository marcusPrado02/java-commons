package com.marcusprado02.commons.app.observability;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthChecksTest {

  @Test
  void readinessShouldBeDownIfAnyCheckDown() {
    HealthCheck ok =
        new HealthCheck() {
          @Override
          public String name() {
            return "db";
          }

          @Override
          public HealthCheckType type() {
            return HealthCheckType.READINESS;
          }

          @Override
          public HealthCheckResult check() {
            return HealthCheckResult.up(name(), type());
          }
        };

    HealthCheck down =
        new HealthCheck() {
          @Override
          public String name() {
            return "queue";
          }

          @Override
          public HealthCheckType type() {
            return HealthCheckType.READINESS;
          }

          @Override
          public HealthCheckResult check() {
            return HealthCheckResult.down(name(), type(), Map.of("reason", "timeout"));
          }
        };

    HealthChecks checks = new HealthChecks(List.of(ok, down));

    HealthReport report = checks.readiness();
    assertEquals(HealthStatus.DOWN, report.status());
    assertEquals(2, report.checks().size());
  }

  @Test
  void livenessShouldBeUpIfNoChecks() {
    HealthChecks checks = new HealthChecks(List.of());

    HealthReport report = checks.liveness();
    assertEquals(HealthStatus.UP, report.status());
  }
}
