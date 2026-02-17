package com.marcusprado02.commons.app.apigateway.filters;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.app.apigateway.GatewayRequest;
import com.marcusprado02.commons.app.apigateway.GatewayResponse;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HeadersFilterTest {

  @Test
  void filter_shouldAddHeaders() {
    HeadersFilter filter =
        new HeadersFilter(Map.of("X-Gateway-Version", "1.0", "X-Content-Type-Options", "nosniff"));

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users").build();

    Result<GatewayResponse> result =
        filter.filter(request, req -> Result.ok(GatewayResponse.ok("response")));

    assertThat(result.isOk()).isTrue();
    GatewayResponse response = result.getOrNull();
    assertThat(response.getHeader("X-Gateway-Version")).contains("1.0");
    assertThat(response.getHeader("X-Content-Type-Options")).contains("nosniff");
  }

  @Test
  void filter_withFailedResponse_shouldNotModify() {
    HeadersFilter filter = new HeadersFilter(Map.of("X-Gateway-Version", "1.0"));

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users").build();

    Result<GatewayResponse> result =
        filter.filter(
            request,
            req ->
                Result.fail(
                    com.marcusprado02.commons.kernel.errors.Problem.of(
                        new com.marcusprado02.commons.kernel.errors.ErrorCode("ERROR"),
                        com.marcusprado02.commons.kernel.errors.ErrorCategory.TECHNICAL,
                        com.marcusprado02.commons.kernel.errors.Severity.ERROR,
                        "Error")));

    assertThat(result.isFail()).isTrue();
  }
}
