package com.marcusprado02.commons.app.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ApiGatewayTest {

  @Test
  void handle_withMatchingRoute_shouldRouteRequest() {
    AtomicInteger handlerCalls = new AtomicInteger(0);

    Route route =
        Route.builder()
            .id("test")
            .pathPattern("/api/users/**")
            .targetUrl("http://backend:8080")
            .build();

    ApiGateway gateway =
        ApiGateway.builder()
            .addRoute(route)
            .backendHandler(
                request -> {
                  handlerCalls.incrementAndGet();
                  return Result.ok(GatewayResponse.ok("backend response"));
                })
            .build();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users/123").build();

    Result<GatewayResponse> response = gateway.handle(request);

    assertThat(response.isOk()).isTrue();
    assertThat(response.getOrNull().statusCode()).isEqualTo(200);
    assertThat(response.getOrNull().body()).contains("backend response");
    assertThat(handlerCalls.get()).isEqualTo(1);
  }

  @Test
  void handle_withNoMatchingRoute_shouldReturn404() {
    Route route =
        Route.builder()
            .id("test")
            .pathPattern("/api/users/**")
            .targetUrl("http://backend:8080")
            .build();

    ApiGateway gateway = ApiGateway.builder().addRoute(route).build();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/orders/123").build();

    Result<GatewayResponse> response = gateway.handle(request);

    assertThat(response.isOk()).isTrue();
    assertThat(response.getOrNull().statusCode()).isEqualTo(404);
  }

  @Test
  void handle_withMultipleRoutes_shouldSelectFirstMatch() {
    Route route1 =
        Route.builder()
            .id("specific")
            .pathPattern("/api/users/admin")
            .targetUrl("http://admin-backend:8080")
            .priority(1)
            .build();

    Route route2 =
        Route.builder()
            .id("general")
            .pathPattern("/api/users/**")
            .targetUrl("http://backend:8080")
            .priority(2)
            .build();

    ApiGateway gateway =
        ApiGateway.builder()
            .addRoute(route1)
            .addRoute(route2)
            .backendHandler(
                request -> {
                  String routeId = request.<String>getAttribute("route.id").orElse("");
                  return Result.ok(GatewayResponse.ok("route: " + routeId));
                })
            .build();

    GatewayRequest request =
        GatewayRequest.builder().method("GET").path("/api/users/admin").build();

    Result<GatewayResponse> response = gateway.handle(request);

    assertThat(response.isOk()).isTrue();
    assertThat(response.getOrNull().body()).contains("route: specific");
  }

  @Test
  void handle_withFilters_shouldExecuteInOrder() {
    StringBuilder executionOrder = new StringBuilder();

    GatewayFilter filter1 =
        new GatewayFilter() {
          @Override
          public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
            executionOrder.append("1-pre,");
            Result<GatewayResponse> result = chain.next(request);
            executionOrder.append("1-post,");
            return result;
          }

          @Override
          public int getOrder() {
            return 1;
          }
        };

    GatewayFilter filter2 =
        new GatewayFilter() {
          @Override
          public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
            executionOrder.append("2-pre,");
            Result<GatewayResponse> result = chain.next(request);
            executionOrder.append("2-post,");
            return result;
          }

          @Override
          public int getOrder() {
            return 2;
          }
        };

    Route route =
        Route.builder()
            .id("test")
            .pathPattern("/api/users")
            .targetUrl("http://backend:8080")
            .build();

    ApiGateway gateway =
        ApiGateway.builder()
            .addRoute(route)
            .addFilter(filter2)
            .addFilter(filter1)
            .backendHandler(
                request -> {
                  executionOrder.append("handler,");
                  return Result.ok(GatewayResponse.ok("response"));
                })
            .build();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users").build();

    gateway.handle(request);

    assertThat(executionOrder.toString()).isEqualTo("1-pre,2-pre,handler,2-post,1-post,");
  }

  @Test
  void handle_withFilterShortCircuit_shouldNotCallBackend() {
    AtomicInteger backendCalls = new AtomicInteger(0);

    GatewayFilter authFilter =
        (request, chain) -> Result.ok(GatewayResponse.unauthorized("No token"));

    Route route =
        Route.builder()
            .id("test")
            .pathPattern("/api/users")
            .targetUrl("http://backend:8080")
            .build();

    ApiGateway gateway =
        ApiGateway.builder()
            .addRoute(route)
            .addFilter(authFilter)
            .backendHandler(
                request -> {
                  backendCalls.incrementAndGet();
                  return Result.ok(GatewayResponse.ok("response"));
                })
            .build();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users").build();

    Result<GatewayResponse> response = gateway.handle(request);

    assertThat(response.isOk()).isTrue();
    assertThat(response.getOrNull().statusCode()).isEqualTo(401);
    assertThat(backendCalls.get()).isZero();
  }

  @Test
  void handle_shouldEnrichRequestWithRouteInfo() {
    Route route =
        Route.builder()
            .id("test-route")
            .pathPattern("/api/users/{userId}")
            .targetUrl("http://backend:8080")
            .build();

    ApiGateway gateway =
        ApiGateway.builder()
            .addRoute(route)
            .backendHandler(
                request -> {
                  String routeId = request.<String>getAttribute("route.id").orElse("");
                  String targetUrl = request.<String>getAttribute("route.targetUrl").orElse("");
                  Object pathParams = request.getAttribute("route.pathParams").orElse(null);

                  assertThat(routeId).isEqualTo("test-route");
                  assertThat(targetUrl).isEqualTo("http://backend:8080");
                  assertThat(pathParams).isNotNull();

                  return Result.ok(GatewayResponse.ok("ok"));
                })
            .build();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users/123").build();

    gateway.handle(request);
  }
}
