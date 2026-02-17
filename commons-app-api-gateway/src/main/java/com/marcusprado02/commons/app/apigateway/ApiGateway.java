package com.marcusprado02.commons.app.apigateway;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Main API Gateway implementation.
 *
 * <p>The API Gateway routes incoming requests to backend services, applying filters for
 * cross-cutting concerns like authentication, rate limiting, and logging.
 *
 * <p>Example:
 *
 * <pre>{@code
 * ApiGateway gateway = ApiGateway.builder()
 *     .addRoute(Route.builder()
 *         .id("users-service")
 *         .pathPattern("/api/users/**")
 *         .targetUrl("http://users-service:8080")
 *         .build())
 *     .addFilter(new AuthenticationFilter())
 *     .addFilter(new RateLimitingFilter(rateLimiter))
 *     .loadBalancer(LoadBalancer.roundRobin())
 *     .backendHandler(this::executeBackendCall)
 *     .build();
 *
 * Result<GatewayResponse> response = gateway.handle(request);
 * }</pre>
 */
public final class ApiGateway {

  private final List<Route> routes;
  private final List<GatewayFilter> filters;
  private final LoadBalancer loadBalancer;
  private final Function<GatewayRequest, Result<GatewayResponse>> backendHandler;

  private ApiGateway(
      List<Route> routes,
      List<GatewayFilter> filters,
      LoadBalancer loadBalancer,
      Function<GatewayRequest, Result<GatewayResponse>> backendHandler) {
    this.routes = List.copyOf(routes);
    this.filters =
        filters.stream().sorted(Comparator.comparingInt(GatewayFilter::getOrder)).toList();
    this.loadBalancer = loadBalancer;
    this.backendHandler = backendHandler;
  }

  /**
   * Creates a builder for ApiGateway.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Handles an incoming gateway request.
   *
   * @param request the gateway request
   * @return the result containing the gateway response
   */
  public Result<GatewayResponse> handle(GatewayRequest request) {
    // Find matching route
    Optional<RouteMatch> match = findRoute(request);

    if (match.isEmpty()) {
      return Result.ok(GatewayResponse.notFound("No route found for: " + request.path()));
    }

    RouteMatch routeMatch = match.get();

    // Add route info to request attributes
    GatewayRequest enrichedRequest =
        request
            .withAttribute("route.id", routeMatch.getRouteId())
            .withAttribute("route.targetUrl", routeMatch.getTargetUrl())
            .withAttribute("route.pathParams", routeMatch.pathParams());

    // Create filter chain
    FilterChain chain = new DefaultFilterChain(filters, backendHandler);

    // Execute through filter chain
    return chain.next(enrichedRequest);
  }

  private Optional<RouteMatch> findRoute(GatewayRequest request) {
    return routes.stream()
        .sorted(Comparator.comparingInt(Route::priority))
        .map(route -> route.matches(request))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  /** Builder for ApiGateway. */
  public static class Builder {
    private final List<Route> routes = new ArrayList<>();
    private final List<GatewayFilter> filters = new ArrayList<>();
    private LoadBalancer loadBalancer = LoadBalancer.roundRobin();
    private Function<GatewayRequest, Result<GatewayResponse>> backendHandler;

    public Builder addRoute(Route route) {
      this.routes.add(route);
      return this;
    }

    public Builder routes(List<Route> routes) {
      this.routes.addAll(routes);
      return this;
    }

    public Builder addFilter(GatewayFilter filter) {
      this.filters.add(filter);
      return this;
    }

    public Builder filters(List<GatewayFilter> filters) {
      this.filters.addAll(filters);
      return this;
    }

    public Builder loadBalancer(LoadBalancer loadBalancer) {
      this.loadBalancer = loadBalancer;
      return this;
    }

    public Builder backendHandler(
        Function<GatewayRequest, Result<GatewayResponse>> backendHandler) {
      this.backendHandler = backendHandler;
      return this;
    }

    public ApiGateway build() {
      if (backendHandler == null) {
        backendHandler = this::defaultBackendHandler;
      }
      return new ApiGateway(routes, filters, loadBalancer, backendHandler);
    }

    private Result<GatewayResponse> defaultBackendHandler(GatewayRequest request) {
      return Result.fail(
          Problem.of(
              new ErrorCode("NO_HANDLER"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "No backend handler configured"));
    }
  }
}
