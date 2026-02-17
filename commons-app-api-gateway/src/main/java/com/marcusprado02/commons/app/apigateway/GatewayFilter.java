package com.marcusprado02.commons.app.apigateway;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Filter interface for the API Gateway.
 *
 * <p>Filters allow you to intercept and modify requests and responses flowing through the gateway.
 *
 * <p>Filters can be used for:
 *
 * <ul>
 *   <li>Authentication and authorization
 *   <li>Rate limiting
 *   <li>Request/response logging
 *   <li>Request transformation
 *   <li>Response aggregation
 *   <li>Circuit breaking
 *   <li>Metrics collection
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * public class AuthenticationFilter implements GatewayFilter {
 *     @Override
 *     public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
 *         String authHeader = request.getHeader("Authorization");
 *         if (authHeader == null || !validateToken(authHeader)) {
 *             return Result.ok(GatewayResponse.unauthorized("Invalid or missing authentication"));
 *         }
 *         return chain.next(request);
 *     }
 *
 *     @Override
 *     public int getOrder() {
 *         return 10; // Authentication should run early
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface GatewayFilter {

  /**
   * Filters the request.
   *
   * <p>The filter can:
   *
   * <ul>
   *   <li>Modify the request and pass it to the next filter
   *   <li>Short-circuit the chain and return a response immediately
   *   <li>Modify the response from downstream filters
   * </ul>
   *
   * @param request the gateway request
   * @param chain the filter chain for invoking the next filter
   * @return the result containing the gateway response
   */
  Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain);

  /**
   * Gets the order of this filter (lower value = higher priority).
   *
   * @return the filter order (default is 0)
   */
  default int getOrder() {
    return 0;
  }
}
