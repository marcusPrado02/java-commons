/**
 * API Gateway implementation providing routing, filtering, and load balancing.
 *
 * <p>This module provides a flexible API Gateway pattern implementation with:
 *
 * <ul>
 *   <li><strong>Request Routing</strong> - Pattern-based routing with path parameters
 *   <li><strong>Filter Chain</strong> - Composable filters for cross-cutting concerns
 *   <li><strong>Load Balancing</strong> - Multiple strategies (round-robin, random, weighted, least
 *       connections)
 *   <li><strong>Built-in Filters</strong> - Logging, metrics, headers
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <pre>{@code
 * // Define routes
 * Route usersRoute = Route.builder()
 *     .id("users-service")
 *     .pathPattern("/api/users/**")
 *     .targetUrl("http://users-service:8080")
 *     .build();
 *
 * Route ordersRoute = Route.builder()
 *     .id("orders-service")
 *     .pathPattern("/api/orders/**")
 *     .targetUrl("http://orders-service:8080")
 *     .build();
 *
 * // Create gateway
 * ApiGateway gateway = ApiGateway.builder()
 *     .addRoute(usersRoute)
 *     .addRoute(ordersRoute)
 *     .addFilter(new LoggingFilter())
 *     .addFilter(new MetricsFilter())
 *     .loadBalancer(LoadBalancer.roundRobin())
 *     .backendHandler(this::callBackend)
 *     .build();
 *
 * // Handle request
 * GatewayRequest request = GatewayRequest.builder()
 *     .method("GET")
 *     .path("/api/users/123")
 *     .header("Authorization", "Bearer token")
 *     .build();
 *
 * Result<GatewayResponse> response = gateway.handle(request);
 * }</pre>
 *
 * <h2>Path Patterns</h2>
 *
 * <p>Routes support flexible path patterns:
 *
 * <ul>
 *   <li><code>/api/users</code> - Exact match
 *   <li><code>/api/users/*</code> - Single segment wildcard
 *   <li><code>/api/users/**</code> - Multi-segment wildcard
 *   <li><code>/api/users/{id}</code> - Path parameter
 *   <li><code>/api/users/{id}/orders/{orderId}</code> - Multiple parameters
 * </ul>
 *
 * <h2>Custom Filters</h2>
 *
 * <pre>{@code
 * public class AuthenticationFilter implements GatewayFilter {
 *     @Override
 *     public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
 *         String token = request.getHeader("Authorization");
 *         if (!isValidToken(token)) {
 *             return Result.ok(GatewayResponse.unauthorized("Invalid token"));
 *         }
 *         return chain.next(request);
 *     }
 *
 *     @Override
 *     public int getOrder() {
 *         return 10; // Run early
 *     }
 * }
 * }</pre>
 *
 * <h2>Load Balancing</h2>
 *
 * <pre>{@code
 * // Round-robin
 * LoadBalancer lb1 = LoadBalancer.roundRobin();
 *
 * // Random
 * LoadBalancer lb2 = LoadBalancer.random();
 *
 * // Weighted random
 * LoadBalancer lb3 = LoadBalancer.weightedRandom(List.of(50, 30, 20));
 *
 * // Least connections
 * LoadBalancer lb4 = LoadBalancer.leastConnections();
 * }</pre>
 *
 * <h2>Backend Handler</h2>
 *
 * <pre>{@code
 * private Result<GatewayResponse> callBackend(GatewayRequest request) {
 *     String targetUrl = request.getAttribute("route.targetUrl");
 *     Map<String, String> pathParams = request.getAttribute("route.pathParams");
 *
 *     // Replace path parameters in URL
 *     String url = buildUrl(targetUrl, request.path(), pathParams);
 *
 *     // Execute HTTP call
 *     HttpResponse httpResponse = httpClient.execute(url, request);
 *
 *     // Convert to gateway response
 *     return Result.ok(GatewayResponse.builder()
 *         .statusCode(httpResponse.getStatusCode())
 *         .headers(httpResponse.getHeaders())
 *         .body(httpResponse.getBody())
 *         .build());
 * }
 * }</pre>
 *
 * @see com.marcusprado02.commons.app.apigateway.ApiGateway
 * @see com.marcusprado02.commons.app.apigateway.GatewayFilter
 * @see com.marcusprado02.commons.app.apigateway.Route
 * @see com.marcusprado02.commons.app.apigateway.LoadBalancer
 */
package com.marcusprado02.commons.app.apigateway;
