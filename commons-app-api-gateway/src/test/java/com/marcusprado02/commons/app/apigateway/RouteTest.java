package com.marcusprado02.commons.app.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RouteTest {

  @Test
  void exactMatch_shouldMatch() {
    Route route =
        Route.builder()
            .id("test")
            .pathPattern("/api/users")
            .method("GET")
            .targetUrl("http://backend:8080")
            .build();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users").build();

    Optional<RouteMatch> match = route.matches(request);

    assertThat(match).isPresent();
    assertThat(match.get().getRouteId()).isEqualTo("test");
  }

  @Test
  void exactMatch_withDifferentMethod_shouldNotMatch() {
    Route route =
        Route.builder()
            .id("test")
            .pathPattern("/api/users")
            .method("GET")
            .targetUrl("http://backend:8080")
            .build();

    GatewayRequest request = GatewayRequest.builder().method("POST").path("/api/users").build();

    Optional<RouteMatch> match = route.matches(request);

    assertThat(match).isEmpty();
  }

  @Test
  void wildcardMatch_shouldMatch() {
    Route route =
        Route.builder()
            .id("test")
            .pathPattern("/api/users/*")
            .targetUrl("http://backend:8080")
            .build();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users/123").build();

    Optional<RouteMatch> match = route.matches(request);

    assertThat(match).isPresent();
  }

  @Test
  void doubleWildcardMatch_shouldMatch() {
    Route route =
        Route.builder()
            .id("test")
            .pathPattern("/api/users/**")
            .targetUrl("http://backend:8080")
            .build();

    GatewayRequest request =
        GatewayRequest.builder().method("GET").path("/api/users/123/orders/456").build();

    Optional<RouteMatch> match = route.matches(request);

    assertThat(match).isPresent();
  }

  @Test
  void pathParameter_shouldExtract() {
    Route route =
        Route.builder()
            .id("test")
            .pathPattern("/api/users/{userId}")
            .targetUrl("http://backend:8080")
            .build();

    GatewayRequest request = GatewayRequest.builder().method("GET").path("/api/users/123").build();

    Optional<RouteMatch> match = route.matches(request);

    assertThat(match).isPresent();
    assertThat(match.get().pathParams()).containsEntry("userId", "123");
  }

  @Test
  void multiplePathParameters_shouldExtract() {
    Route route =
        Route.builder()
            .id("test")
            .pathPattern("/api/users/{userId}/orders/{orderId}")
            .targetUrl("http://backend:8080")
            .build();

    GatewayRequest request =
        GatewayRequest.builder().method("GET").path("/api/users/123/orders/456").build();

    Optional<RouteMatch> match = route.matches(request);

    assertThat(match).isPresent();
    assertThat(match.get().pathParams())
        .containsEntry("userId", "123")
        .containsEntry("orderId", "456");
  }

  @Test
  void nullMethod_shouldMatchAnyMethod() {
    Route route =
        Route.builder()
            .id("test")
            .pathPattern("/api/users")
            .targetUrl("http://backend:8080")
            .build();

    GatewayRequest getRequest = GatewayRequest.builder().method("GET").path("/api/users").build();
    GatewayRequest postRequest = GatewayRequest.builder().method("POST").path("/api/users").build();

    assertThat(route.matches(getRequest)).isPresent();
    assertThat(route.matches(postRequest)).isPresent();
  }

  @Test
  void priority_shouldBeUsedForOrdering() {
    Route route1 =
        Route.builder()
            .id("test1")
            .pathPattern("/api/users")
            .targetUrl("http://backend1:8080")
            .priority(10)
            .build();

    Route route2 =
        Route.builder()
            .id("test2")
            .pathPattern("/api/users")
            .targetUrl("http://backend2:8080")
            .priority(5)
            .build();

    assertThat(route2.priority()).isLessThan(route1.priority());
  }
}
