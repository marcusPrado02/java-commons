package com.marcusprado02.commons.app.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcusprado02.commons.app.apigateway.filters.LoggingFilter;
import com.marcusprado02.commons.app.apigateway.filters.MetricsFilter;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiGatewayBranchTest {

  // --- GatewayResponse factory methods ---

  @Test
  void gatewayResponse_ok_noBody() {
    GatewayResponse r = GatewayResponse.ok();
    assertThat(r.statusCode()).isEqualTo(200);
    assertThat(r.body()).isEmpty();
  }

  @Test
  void gatewayResponse_created() {
    GatewayResponse r = GatewayResponse.created("created");
    assertThat(r.statusCode()).isEqualTo(201);
    assertThat(r.body()).contains("created");
  }

  @Test
  void gatewayResponse_noContent() {
    GatewayResponse r = GatewayResponse.noContent();
    assertThat(r.statusCode()).isEqualTo(204);
    assertThat(r.body()).isEmpty();
  }

  @Test
  void gatewayResponse_badRequest() {
    GatewayResponse r = GatewayResponse.badRequest("bad");
    assertThat(r.statusCode()).isEqualTo(400);
  }

  @Test
  void gatewayResponse_unauthorized() {
    GatewayResponse r = GatewayResponse.unauthorized("unauth");
    assertThat(r.statusCode()).isEqualTo(401);
  }

  @Test
  void gatewayResponse_forbidden() {
    GatewayResponse r = GatewayResponse.forbidden("forbidden");
    assertThat(r.statusCode()).isEqualTo(403);
  }

  @Test
  void gatewayResponse_tooManyRequests() {
    GatewayResponse r = GatewayResponse.tooManyRequests("rate limit");
    assertThat(r.statusCode()).isEqualTo(429);
  }

  @Test
  void gatewayResponse_serverError() {
    GatewayResponse r = GatewayResponse.serverError("oops");
    assertThat(r.statusCode()).isEqualTo(500);
  }

  @Test
  void gatewayResponse_badGateway() {
    GatewayResponse r = GatewayResponse.badGateway("bad gw");
    assertThat(r.statusCode()).isEqualTo(502);
  }

  @Test
  void gatewayResponse_serviceUnavailable() {
    GatewayResponse r = GatewayResponse.serviceUnavailable("unavailable");
    assertThat(r.statusCode()).isEqualTo(503);
  }

  @Test
  void gatewayResponse_gatewayTimeout() {
    GatewayResponse r = GatewayResponse.gatewayTimeout("timeout");
    assertThat(r.statusCode()).isEqualTo(504);
  }

  @Test
  void gatewayResponse_withHeader() {
    GatewayResponse r = GatewayResponse.ok("body").withHeader("X-Custom", "value");
    assertThat(r.getHeader("x-custom")).contains("value");
  }

  @Test
  void gatewayResponse_builder_headersMap_and_contentType() {
    GatewayResponse r =
        GatewayResponse.builder()
            .statusCode(200)
            .headers(Map.of("X-A", "a"))
            .contentType("application/json")
            .build();
    assertThat(r.getHeader("X-A")).contains("a");
    assertThat(r.getHeader("Content-Type")).contains("application/json");
  }

  @Test
  void gatewayResponse_getHeader_caseInsensitive() {
    GatewayResponse r = GatewayResponse.builder().header("Content-Type", "text/plain").build();
    assertThat(r.getHeader("content-type")).contains("text/plain");
  }

  @Test
  void gatewayResponse_getHeader_missingReturnsEmpty() {
    GatewayResponse r = GatewayResponse.ok();
    assertThat(r.getHeader("X-Missing")).isEmpty();
  }

  // --- GatewayRequest accessors ---

  @Test
  void gatewayRequest_getHeader_caseInsensitive() {
    GatewayRequest req = GatewayRequest.builder().header("Authorization", "Bearer token").build();
    assertThat(req.getHeader("authorization")).contains("Bearer token");
  }

  @Test
  void gatewayRequest_getHeader_missingReturnsEmpty() {
    GatewayRequest req = GatewayRequest.builder().build();
    assertThat(req.getHeader("X-Missing")).isEmpty();
  }

  @Test
  void gatewayRequest_getQueryParam() {
    GatewayRequest req = GatewayRequest.builder().queryParam("page", "2").build();
    assertThat(req.getQueryParam("page")).contains("2");
    assertThat(req.getQueryParam("missing")).isEmpty();
  }

  @Test
  void gatewayRequest_getAttribute() {
    GatewayRequest req = GatewayRequest.builder().attribute("key", "val").build();
    assertThat(req.<String>getAttribute("key")).contains("val");
    assertThat(req.<String>getAttribute("none")).isEmpty();
  }

  @Test
  void gatewayRequest_builder_bulkMethods() {
    GatewayRequest req =
        GatewayRequest.builder()
            .headers(Map.of("X-H", "h"))
            .queryParams(Map.of("q", "v"))
            .attributes(Map.of("a", "b"))
            .body("content")
            .build();
    assertThat(req.getHeader("X-H")).contains("h");
    assertThat(req.getQueryParam("q")).contains("v");
    assertThat(req.<String>getAttribute("a")).contains("b");
    assertThat(req.body()).contains("content");
  }

  // --- ApiGateway.Builder.routes(List) and filters(List) ---

  @Test
  void apiGatewayBuilder_routesList_and_filtersList() {
    Route route = Route.builder().id("r").pathPattern("/ping").targetUrl("http://backend").build();

    MetricsFilter metrics = new MetricsFilter();

    ApiGateway gateway =
        ApiGateway.builder()
            .routes(List.of(route))
            .filters(List.of(metrics))
            .backendHandler(req -> Result.ok(GatewayResponse.ok("pong")))
            .build();

    GatewayRequest req = GatewayRequest.builder().path("/ping").build();
    Result<GatewayResponse> result = gateway.handle(req);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().body()).contains("pong");
    assertThat(metrics.getTotalRequests()).isEqualTo(1);
  }

  // --- ApiGateway default backend handler ---

  @Test
  void apiGatewayBuilder_noBackendHandler_usesDefaultWhichFails() {
    Route route = Route.builder().id("r").pathPattern("/test").targetUrl("http://backend").build();

    ApiGateway gateway = ApiGateway.builder().addRoute(route).build();

    GatewayRequest req = GatewayRequest.builder().path("/test").build();
    Result<GatewayResponse> result = gateway.handle(req);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("NO_HANDLER");
  }

  // --- Route.Builder validation errors ---

  @Test
  void routeBuilder_missingId_throws() {
    assertThatThrownBy(() -> Route.builder().pathPattern("/x").targetUrl("http://x").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("id");
  }

  @Test
  void routeBuilder_missingPathPattern_throws() {
    assertThatThrownBy(() -> Route.builder().id("r").targetUrl("http://x").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pathPattern");
  }

  @Test
  void routeBuilder_missingTargetUrl_throws() {
    assertThatThrownBy(() -> Route.builder().id("r").pathPattern("/x").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("targetUrl");
  }

  // --- LoadBalancer null/empty branches ---

  @Test
  void randomLoadBalancer_withEmptyList_returnsError() {
    LoadBalancer lb = LoadBalancer.random();
    Result<String> result = lb.choose(List.of());
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("NO_INSTANCES");
  }

  @Test
  void randomLoadBalancer_withNullList_returnsError() {
    LoadBalancer lb = LoadBalancer.random();
    Result<String> result = lb.choose(null);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void leastConnections_withEmptyList_returnsError() {
    LoadBalancer lb = LoadBalancer.leastConnections();
    Result<String> result = lb.choose(List.of());
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("NO_INSTANCES");
  }

  @Test
  void leastConnections_releaseConnection_unknownInstance_isNoOp() {
    LeastConnectionsLoadBalancer lb = new LeastConnectionsLoadBalancer();
    lb.releaseConnection("unknown-instance");
  }

  @Test
  void weightedRandom_withEmptyList_returnsError() {
    LoadBalancer lb = LoadBalancer.weightedRandom(List.of(1, 2));
    Result<String> result = lb.choose(List.of());
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("NO_INSTANCES");
  }

  @Test
  void weightedRandom_withZeroWeights_returnsError() {
    LoadBalancer lb = LoadBalancer.weightedRandom(List.of(0, 0));
    Result<String> result = lb.choose(List.of("a", "b"));
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("INVALID_WEIGHTS");
  }

  // --- LoggingFilter ---

  @Test
  void loggingFilter_withSuccessfulResponse_logsAndReturns() {
    LoggingFilter filter = new LoggingFilter();
    GatewayRequest req = GatewayRequest.builder().header("X-Request-ID", "req-1").build();

    Result<GatewayResponse> result = filter.filter(req, r -> Result.ok(GatewayResponse.ok("ok")));

    assertThat(result.isOk()).isTrue();
    assertThat(filter.getOrder()).isEqualTo(100);
  }

  @Test
  void loggingFilter_withFailedResponse_logsError() {
    LoggingFilter filter = new LoggingFilter(50);
    GatewayRequest req = GatewayRequest.builder().build();

    Result<GatewayResponse> result =
        filter.filter(
            req,
            r ->
                Result.fail(
                    com.marcusprado02.commons.kernel.errors.Problem.of(
                        new com.marcusprado02.commons.kernel.errors.ErrorCode("ERR"),
                        com.marcusprado02.commons.kernel.errors.ErrorCategory.TECHNICAL,
                        com.marcusprado02.commons.kernel.errors.Severity.ERROR,
                        "fail")));

    assertThat(result.isFail()).isTrue();
    assertThat(filter.getOrder()).isEqualTo(50);
  }

  // --- MetricsFilter no-requests averageLatency ---

  @Test
  void metricsFilter_averageLatency_zeroWhenNoRequests() {
    MetricsFilter filter = new MetricsFilter();
    assertThat(filter.getAverageLatency()).isEqualTo(0.0);
    assertThat(filter.getSuccessRate()).isEqualTo(0.0);
  }

  @Test
  void metricsFilter_customOrder() {
    MetricsFilter filter = new MetricsFilter(999);
    assertThat(filter.getOrder()).isEqualTo(999);
  }
}
