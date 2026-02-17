package com.marcusprado02.commons.app.apigateway.filters;

import com.marcusprado02.commons.app.apigateway.FilterChain;
import com.marcusprado02.commons.app.apigateway.GatewayFilter;
import com.marcusprado02.commons.app.apigateway.GatewayRequest;
import com.marcusprado02.commons.app.apigateway.GatewayResponse;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.Map;

/**
 * Adds standard headers to all responses.
 *
 * <p>Example:
 *
 * <pre>{@code
 * ApiGateway gateway = ApiGateway.builder()
 *     .addFilter(new HeadersFilter(Map.of(
 *         "X-Gateway-Version", "1.0",
 *         "X-Content-Type-Options", "nosniff"
 *     )))
 *     .build();
 * }</pre>
 */
public final class HeadersFilter implements GatewayFilter {

  private final Map<String, String> headers;
  private final int order;

  /**
   * Creates a headers filter with default order (1000).
   *
   * @param headers the headers to add to all responses
   */
  public HeadersFilter(Map<String, String> headers) {
    this(headers, 1000);
  }

  /**
   * Creates a headers filter with custom order.
   *
   * @param headers the headers to add to all responses
   * @param order the filter order
   */
  public HeadersFilter(Map<String, String> headers, int order) {
    this.headers = Map.copyOf(headers);
    this.order = order;
  }

  @Override
  public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
    Result<GatewayResponse> result = chain.next(request);

    if (result.isOk()) {
      GatewayResponse response = result.getOrNull();
      GatewayResponse.Builder builder = response.builder();

      // Add all configured headers
      headers.forEach(builder::header);

      return Result.ok(builder.build());
    }

    return result;
  }

  @Override
  public int getOrder() {
    return order;
  }
}
