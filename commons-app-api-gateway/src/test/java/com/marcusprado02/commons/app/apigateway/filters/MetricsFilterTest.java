package com.marcusprado02.commons.app.apigateway.filters;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.app.apigateway.GatewayRequest;
import com.marcusprado02.commons.app.apigateway.GatewayResponse;
import com.marcusprado02.commons.kernel.result.Result;
import org.junit.jupiter.api.Test;

class MetricsFilterTest {

  @Test
  void filter_shouldCollectMetrics() {
    MetricsFilter filter = new MetricsFilter();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users").build();

    // Execute successful request
    filter.filter(request, req -> Result.ok(GatewayResponse.ok("success")));

    assertThat(filter.getTotalRequests()).isEqualTo(1);
    assertThat(filter.getSuccessfulRequests()).isEqualTo(1);
    assertThat(filter.getFailedRequests()).isZero();
    assertThat(filter.getSuccessRate()).isEqualTo(100.0);
  }

  @Test
  void filter_withFailure_shouldTrackFailedRequests() {
    MetricsFilter filter = new MetricsFilter();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users").build();

    // Execute failed request
    filter.filter(
        request,
        req ->
            Result.fail(
                com.marcusprado02.commons.kernel.errors.Problem.of(
                    new com.marcusprado02.commons.kernel.errors.ErrorCode("ERROR"),
                    com.marcusprado02.commons.kernel.errors.ErrorCategory.TECHNICAL,
                    com.marcusprado02.commons.kernel.errors.Severity.ERROR,
                    "Error")));

    assertThat(filter.getTotalRequests()).isEqualTo(1);
    assertThat(filter.getSuccessfulRequests()).isZero();
    assertThat(filter.getFailedRequests()).isEqualTo(1);
    assertThat(filter.getSuccessRate()).isZero();
  }

  @Test
  void filter_shouldTrackLatency() {
    MetricsFilter filter = new MetricsFilter();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users").build();

    filter.filter(
        request,
        req -> {
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return Result.ok(GatewayResponse.ok("success"));
        });

    assertThat(filter.getTotalLatency()).isGreaterThanOrEqualTo(10);
    assertThat(filter.getAverageLatency()).isGreaterThanOrEqualTo(10);
  }

  @Test
  void reset_shouldClearMetrics() {
    MetricsFilter filter = new MetricsFilter();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users").build();

    filter.filter(request, req -> Result.ok(GatewayResponse.ok("success")));

    assertThat(filter.getTotalRequests()).isEqualTo(1);

    filter.reset();

    assertThat(filter.getTotalRequests()).isZero();
    assertThat(filter.getSuccessfulRequests()).isZero();
    assertThat(filter.getFailedRequests()).isZero();
    assertThat(filter.getTotalLatency()).isZero();
  }
}
