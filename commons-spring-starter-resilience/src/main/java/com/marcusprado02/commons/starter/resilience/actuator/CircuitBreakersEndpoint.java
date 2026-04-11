package com.marcusprado02.commons.starter.resilience.actuator;

import com.marcusprado02.commons.adapters.resilience4j.Resilience4jExecutor;
import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import java.util.List;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/** Actuator endpoint exposing circuit breaker status information. */
@Endpoint(id = "commonsCircuitBreakers")
public final class CircuitBreakersEndpoint {

  private final ResilienceExecutor resilienceExecutor;

  public CircuitBreakersEndpoint(ResilienceExecutor resilienceExecutor) {
    this.resilienceExecutor = resilienceExecutor;
  }

  /**
   * Returns the status of all known circuit breakers.
   *
   * @return list of circuit breaker status snapshots
   */
  @ReadOperation
  public List<Resilience4jExecutor.CircuitBreakerStatus> circuitBreakers() {
    if (resilienceExecutor instanceof Resilience4jExecutor resilience4jExecutor) {
      return resilience4jExecutor.circuitBreakerStatuses();
    }
    return List.of();
  }
}
