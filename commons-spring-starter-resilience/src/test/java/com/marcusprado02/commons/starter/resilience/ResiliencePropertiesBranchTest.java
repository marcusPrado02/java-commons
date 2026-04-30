package com.marcusprado02.commons.starter.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import com.marcusprado02.commons.starter.resilience.actuator.CircuitBreakersEndpoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResiliencePropertiesBranchTest {

  @Test
  void circuitBreakersEndpoint_returns_empty_for_non_resilience4j_executor() {
    CircuitBreakersEndpoint endpoint = new CircuitBreakersEndpoint(mock(ResilienceExecutor.class));
    assertThat(endpoint.circuitBreakers()).isEqualTo(List.of());
  }
}
