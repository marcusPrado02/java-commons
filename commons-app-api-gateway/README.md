# Commons App API Gateway

Comprehensive API Gateway implementation providing routing, filtering, load balancing, and response aggregation patterns.

## Features

- **Request Routing** - Pattern-based routing with wildcards and path parameters
- **Filter Chain** - Composable filters for cross-cutting concerns
- **Load Balancing** - Multiple strategies (round-robin, random, weighted, least connections)
- **Built-in Filters** - Logging, metrics, custom headers
- **Type-Safe API** - Result pattern for error handling
- **Zero Dependencies** - Only depends on commons-kernel modules

## Installation

### Maven

```xml
<dependency>
    <groupId>com.marcusprado02</groupId>
    <artifactId>commons-app-api-gateway</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.marcusprado02:commons-app-api-gateway:${project.version}'
```

## Quick Start

```java
// Define routes
Route usersRoute = Route.builder()
    .id("users-service")
    .pathPattern("/api/users/**")
    .targetUrl("http://users-service:8080")
    .build();

Route ordersRoute = Route.builder()
    .id("orders-service")
    .pathPattern("/api/orders/**")
    .targetUrl("http://orders-service:8080")
    .build();

// Create gateway
ApiGateway gateway = ApiGateway.builder()
    .addRoute(usersRoute)
    .addRoute(ordersRoute)
    .addFilter(new LoggingFilter())
    .addFilter(new MetricsFilter())
    .loadBalancer(LoadBalancer.roundRobin())
    .backendHandler(this::callBackend)
    .build();

// Handle request
GatewayRequest request = GatewayRequest.builder()
    .method("GET")
    .path("/api/users/123")
    .header("Authorization", "Bearer token")
    .build();

Result<GatewayResponse> response = gateway.handle(request);

if (response.isSuccess()) {
    GatewayResponse gatewayResponse = response.getValue();
    System.out.println("Status: " + gatewayResponse.statusCode());
    System.out.println("Body: " + gatewayResponse.body().orElse(""));
}
```

## Routing

### Path Patterns

Routes support flexible path patterns:

```java
// Exact match
Route.builder()
    .pathPattern("/api/users")
    .build();

// Single segment wildcard
Route.builder()
    .pathPattern("/api/users/*")
    .build();

// Multi-segment wildcard
Route.builder()
    .pathPattern("/api/users/**")
    .build();

// Path parameters
Route.builder()
    .pathPattern("/api/users/{id}")
    .build();

// Multiple parameters
Route.builder()
    .pathPattern("/api/users/{userId}/orders/{orderId}")
    .build();
```

### Method Filtering

```java
// Match specific HTTP method
Route.builder()
    .pathPattern("/api/users")
    .method("GET")
    .build();

// Match all methods (omit method)
Route.builder()
    .pathPattern("/api/users")
    .build();
```

### Route Priority

When multiple routes match, lower priority wins:

```java
Route adminRoute = Route.builder()
    .id("admin")
    .pathPattern("/api/users/admin")
    .priority(1)  // Higher priority
    .build();

Route generalRoute = Route.builder()
    .id("general")
    .pathPattern("/api/users/**")
    .priority(10)  // Lower priority
    .build();
```

### Accessing Route Information

Routes add metadata to request attributes:

```java
ApiGateway gateway = ApiGateway.builder()
    .backendHandler(request -> {
        String routeId = request.getAttribute("route.id");
        String targetUrl = request.getAttribute("route.targetUrl");
        Map<String, String> pathParams = request.getAttribute("route.pathParams");

        String userId = pathParams.get("userId");

        // Build URL and execute call
        return executeBackendCall(targetUrl, request);
    })
    .build();
```

## Filters

Filters intercept requests and responses for cross-cutting concerns.

### Built-in Filters

#### Logging Filter

```java
ApiGateway gateway = ApiGateway.builder()
    .addFilter(new LoggingFilter())
    .build();

// With custom order
ApiGateway gateway = ApiGateway.builder()
    .addFilter(new LoggingFilter(10))  // Run early
    .build();
```

Logs:
```
INFO  Incoming request: method=GET, path=/api/users/123, requestId=abc123
INFO  Response: status=200, duration=45ms, requestId=abc123
```

#### Metrics Filter

```java
MetricsFilter metricsFilter = new MetricsFilter();

ApiGateway gateway = ApiGateway.builder()
    .addFilter(metricsFilter)
    .build();

// Check metrics
System.out.println("Total requests: " + metricsFilter.getTotalRequests());
System.out.println("Success rate: " + metricsFilter.getSuccessRate() + "%");
System.out.println("Average latency: " + metricsFilter.getAverageLatency() + "ms");

// Reset metrics
metricsFilter.reset();
```

#### Headers Filter

```java
ApiGateway gateway = ApiGateway.builder()
    .addFilter(new HeadersFilter(Map.of(
        "X-Gateway-Version", "1.0",
        "X-Content-Type-Options", "nosniff",
        "X-Frame-Options", "DENY"
    )))
    .build();
```

### Custom Filters

Implement `GatewayFilter`:

```java
public class AuthenticationFilter implements GatewayFilter {

    private final TokenValidator tokenValidator;

    public AuthenticationFilter(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
        Optional<String> authHeader = request.getHeader("Authorization");

        if (authHeader.isEmpty()) {
            return Result.ok(GatewayResponse.unauthorized("Missing Authorization header"));
        }

        String token = authHeader.get().replace("Bearer ", "");

        if (!tokenValidator.isValid(token)) {
            return Result.ok(GatewayResponse.unauthorized("Invalid token"));
        }

        // Add user info to request
        String userId = tokenValidator.extractUserId(token);
        GatewayRequest enrichedRequest = request.withAttribute("user.id", userId);

        return chain.next(enrichedRequest);
    }

    @Override
    public int getOrder() {
        return 10;  // Run early
    }
}
```

### Rate Limiting Filter

Integrate with `commons-app-rate-limiting`:

```java
public class RateLimitingFilter implements GatewayFilter {

    private final RateLimiter rateLimiter;

    public RateLimitingFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
        String clientId = request.getHeader("X-Client-ID").orElse("anonymous");

        Result<Boolean> result = rateLimiter.tryAcquire(clientId);

        if (result.isFailure() || !result.getValue()) {
            return Result.ok(GatewayResponse.tooManyRequests("Rate limit exceeded"));
        }

        return chain.next(request);
    }

    @Override
    public int getOrder() {
        return 20;  // After authentication
    }
}
```

### Filter Ordering

Filters are executed in order (lower number first):

```java
ApiGateway gateway = ApiGateway.builder()
    .addFilter(new AuthenticationFilter())      // order=10
    .addFilter(new RateLimitingFilter())        // order=20
    .addFilter(new LoggingFilter())             // order=100
    .addFilter(new MetricsFilter())             // order=200
    .addFilter(new HeadersFilter(headers))      // order=1000
    .build();
```

Execution flow:
```
Authentication → Rate Limiting → Logging → Metrics → Backend → Metrics → Logging → Headers
```

## Load Balancing

### Round Robin

Distributes requests evenly:

```java
LoadBalancer lb = LoadBalancer.roundRobin();

Result<String> instance = lb.choose(List.of(
    "http://service1:8080",
    "http://service2:8080",
    "http://service3:8080"
));

// Returns: service1, service2, service3, service1, ...
```

### Random

Selects random instance:

```java
LoadBalancer lb = LoadBalancer.random();

Result<String> instance = lb.choose(List.of(
    "http://service1:8080",
    "http://service2:8080",
    "http://service3:8080"
));
```

### Weighted Random

Distributes based on weights:

```java
// 60%, 30%, 10% distribution
LoadBalancer lb = LoadBalancer.weightedRandom(List.of(60, 30, 10));

Result<String> instance = lb.choose(List.of(
    "http://service1:8080",  // 60% of requests
    "http://service2:8080",  // 30% of requests
    "http://service3:8080"   // 10% of requests
));
```

### Least Connections

Routes to instance with fewest active connections:

```java
LoadBalancer lb = LoadBalancer.leastConnections();

Result<String> instance = lb.choose(List.of(
    "http://service1:8080",
    "http://service2:8080",
    "http://service3:8080"
));

// Release connection after request completes
if (lb instanceof LeastConnectionsLoadBalancer lclb) {
    lclb.releaseConnection(instance.getValue());
}
```

### Custom Load Balancer

```java
public class ConsistentHashLoadBalancer implements LoadBalancer {

    private final ConsistentHash<String> consistentHash;

    @Override
    public Result<String> choose(List<String> instances) {
        if (instances.isEmpty()) {
            return Result.fail(/* error */);
        }

        String requestId = getCurrentRequestId();
        String instance = consistentHash.get(requestId, instances);

        return Result.ok(instance);
    }
}
```

## Backend Handler

The backend handler executes the actual HTTP call:

```java
private Result<GatewayResponse> callBackend(GatewayRequest request) {
    // Get route information
    String targetUrl = request.getAttribute("route.targetUrl");
    Map<String, String> pathParams = request.getAttribute("route.pathParams");

    // Build final URL
    String url = buildUrl(targetUrl, request.path(), pathParams);

    try {
        // Execute HTTP call
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method(request.method(), HttpRequest.BodyPublishers.ofString(
                request.body().orElse("")))
            .build();

        request.headers().forEach((name, values) ->
            values.forEach(value ->
                httpRequest.headers().add(name, value)));

        HttpResponse<String> httpResponse = httpClient.send(
            httpRequest,
            HttpResponse.BodyHandlers.ofString());

        // Convert to gateway response
        return Result.ok(GatewayResponse.builder()
            .statusCode(httpResponse.statusCode())
            .headers(httpResponse.headers().map())
            .body(httpResponse.body())
            .build());

    } catch (Exception e) {
        return Result.fail(new Problem(
            new ErrorCode("BACKEND_ERROR"),
            ErrorCategory.TECHNICAL,
            Severity.HIGH,
            "Backend call failed: " + e.getMessage()));
    }
}
```

## Spring Boot Integration

```java
@Configuration
public class GatewayConfiguration {

    @Bean
    public ApiGateway apiGateway(
            List<Route> routes,
            List<GatewayFilter> filters,
            LoadBalancer loadBalancer,
            HttpClient httpClient) {

        return ApiGateway.builder()
            .routes(routes)
            .filters(filters)
            .loadBalancer(loadBalancer)
            .backendHandler(request -> callBackend(request, httpClient))
            .build();
    }

    @Bean
    public List<Route> routes() {
        return List.of(
            Route.builder()
                .id("users-service")
                .pathPattern("/api/users/**")
                .targetUrl("${services.users.url}")
                .build(),
            Route.builder()
                .id("orders-service")
                .pathPattern("/api/orders/**")
                .targetUrl("${services.orders.url}")
                .build()
        );
    }

    @Bean
    public List<GatewayFilter> filters(RateLimiter rateLimiter) {
        return List.of(
            new LoggingFilter(),
            new MetricsFilter(),
            new RateLimitingFilter(rateLimiter),
            new HeadersFilter(Map.of(
                "X-Gateway-Version", "1.0"
            ))
        );
    }

    @Bean
    public LoadBalancer loadBalancer() {
        return LoadBalancer.roundRobin();
    }
}

@RestController
public class GatewayController {

    private final ApiGateway gateway;

    public GatewayController(ApiGateway gateway) {
        this.gateway = gateway;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> handleRequest(
            HttpServletRequest servletRequest,
            @RequestBody(required = false) String body) {

        // Convert servlet request to gateway request
        GatewayRequest gatewayRequest = convertToGatewayRequest(servletRequest, body);

        // Execute through gateway
        Result<GatewayResponse> result = gateway.handle(gatewayRequest);

        if (result.isFailure()) {
            return ResponseEntity.status(500)
                .body("Gateway error: " + result.getError().message());
        }

        // Convert gateway response to servlet response
        GatewayResponse gatewayResponse = result.getValue();
        return ResponseEntity.status(gatewayResponse.statusCode())
            .headers(convertHeaders(gatewayResponse.headers()))
            .body(gatewayResponse.body().orElse(""));
    }
}
```

## Advanced Examples

### Circuit Breaker Filter

```java
public class CircuitBreakerFilter implements GatewayFilter {

    private final CircuitBreaker circuitBreaker;

    @Override
    public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
        String serviceId = request.getAttribute("route.id");

        if (!circuitBreaker.allowRequest(serviceId)) {
            return Result.ok(GatewayResponse.serviceUnavailable(
                "Service temporarily unavailable"));
        }

        Result<GatewayResponse> result = chain.next(request);

        if (result.isFailure()) {
            circuitBreaker.recordFailure(serviceId);
        } else {
            circuitBreaker.recordSuccess(serviceId);
        }

        return result;
    }
}
```

### Request Transformation

```java
public class RequestTransformationFilter implements GatewayFilter {

    @Override
    public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
        // Add correlation ID
        String correlationId = UUID.randomUUID().toString();

        GatewayRequest transformedRequest = GatewayRequest.builder()
            .method(request.method())
            .path(request.path())
            .headers(request.headers())
            .header("X-Correlation-ID", correlationId)
            .queryParams(request.queryParams())
            .body(request.body().orElse(null))
            .attributes(request.attributes())
            .build();

        return chain.next(transformedRequest);
    }
}
```

### Response Caching

```java
public class CachingFilter implements GatewayFilter {

    private final Cache<String, GatewayResponse> cache;

    @Override
    public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
        if (!request.method().equals("GET")) {
            return chain.next(request);
        }

        String cacheKey = request.path() + "?" + request.queryParams();
        GatewayResponse cached = cache.get(cacheKey);

        if (cached != null) {
            return Result.ok(cached.withHeader("X-Cache", "HIT"));
        }

        Result<GatewayResponse> result = chain.next(request);

        if (result.isSuccess()) {
            cache.put(cacheKey, result.getValue());
        }

        return result;
    }
}
```

## Testing

```java
@Test
void testGateway() {
    Route route = Route.builder()
        .id("test")
        .pathPattern("/api/users/{id}")
        .targetUrl("http://backend:8080")
        .build();

    ApiGateway gateway = ApiGateway.builder()
        .addRoute(route)
        .backendHandler(request ->
            Result.ok(GatewayResponse.ok("user data")))
        .build();

    GatewayRequest request = GatewayRequest.builder()
        .method("GET")
        .path("/api/users/123")
        .build();

    Result<GatewayResponse> response = gateway.handle(request);

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getValue().statusCode()).isEqualTo(200);
}
```

## Performance Considerations

1. **Filter Order**: Place short-circuit filters early (authentication, rate limiting)
2. **Connection Pooling**: Reuse HTTP client instances in backend handler
3. **Async Processing**: Consider CompletableFuture for parallel backend calls
4. **Caching**: Cache route matches and filter chains

## Comparison with Other Gateway Solutions

| Feature | Commons Gateway | Spring Cloud Gateway | Kong | Nginx |
|---------|----------------|---------------------|------|-------|
| Language | Java | Java | Lua/Plugin | C |
| Deployment | Library | Framework | Standalone | Standalone |
| Routing | Pattern-based | Predicate-based | Admin API | Config file |
| Filters | Java code | Java code | Plugins | Lua/Config |
| Load Balancing | Built-in | Built-in | Built-in | Built-in |

## License

This project is licensed under the MIT License.
