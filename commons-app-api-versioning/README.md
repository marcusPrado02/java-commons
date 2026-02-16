# Commons App API Versioning

Abstractions for managing API versioning in REST APIs supporting multiple versioning strategies.

## Features

- **Multiple Versioning Strategies**: URL path, headers, media types, query parameters
- **Version Registry**: Track supported, deprecated, and sunset versions
- **Deprecation Management**: Sunset dates, migration guides, replacement versions
- **Composite Resolvers**: Combine multiple strategies with fallback
- **Framework Agnostic**: Works with any HTTP framework via adapters

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-app-api-versioning</artifactId>
</dependency>
```

## Versioning Strategies

### 1. URL Path Versioning

Version embedded in the URL path:

```
GET /v1/users
GET /v2/products
GET /api/v1/orders
```

**Pros:**
- Explicit and visible
- Easy to understand
- RESTful
- Cacheable

**Cons:**
- Changes URL structure
- Less flexible
- Requires routing changes

**Usage:**

```java
VersionResolver<HttpServletRequest> resolver =
    new UrlPathVersionResolver<>(HttpServletRequest::getRequestURI);

Optional<ApiVersion> version = resolver.resolve(request);
// From "/v1/users" -> ApiVersion.of(1, 0)
```

### 2. Header Versioning

Version specified in custom headers:

```
GET /users
Api-Version: 1
```

**Pros:**
- Clean URLs
- Flexible
- Easy to version independently

**Cons:**
- Not visible in URL
- Harder to cache
- Requires client configuration

**Usage:**

```java
VersionResolver<HttpServletRequest> resolver =
    new HeaderVersionResolver<>(
        "Api-Version",
        HttpServletRequest::getHeader
    );

Optional<ApiVersion> version = resolver.resolve(request);
// From Header "Api-Version: v2" -> ApiVersion.of(2, 0)
```

### 3. Content Negotiation (Media Type)

Version in Accept header using vendor-specific media types:

```
GET /users
Accept: application/vnd.mycompany.v1+json
```

**Pros:**
- RESTful (proper use of Accept header)
- Clean URLs
- Supports multiple formats

**Cons:**
- Complex
- Not well understood by developers
- Harder to test

**Usage:**

```java
VersionResolver<HttpServletRequest> resolver =
    new MediaTypeVersionResolver<>(
        "mycompany",
        req -> req.getHeader("Accept")
    );

Optional<ApiVersion> version = resolver.resolve(request);
// From Accept: application/vnd.mycompany.v2+json -> ApiVersion.of(2, 0)
```

### 4. Query Parameter Versioning

Version as query parameter:

```
GET /users?version=1
GET /products?api-version=v2
```

**Pros:**
- Simple for testing
- Easy to implement
- Works with browser

**Cons:**
- Not RESTful
- Pollutes query string
- Caching issues

**Usage:**

```java
VersionResolver<HttpServletRequest> resolver =
    new QueryParameterVersionResolver<>(
        "version",
        HttpServletRequest::getParameter
    );

Optional<ApiVersion> version = resolver.resolve(request);
// From "/users?version=1" -> ApiVersion.of(1, 0)
```

## Version Management

### Define Versions

```java
ApiVersion v1 = ApiVersion.of(1, 0);
ApiVersion v2 = ApiVersion.of(2, 0);
ApiVersion v3 = ApiVersion.of(3, 0);

// Or use constants
ApiVersion v1 = ApiVersion.V1;
ApiVersion v2 = ApiVersion.V2;
```

### Parse Versions

```java
ApiVersion v1 = ApiVersion.parse("v1");      // -> 1.0
ApiVersion v2 = ApiVersion.parse("2");       // -> 2.0
ApiVersion v3 = ApiVersion.parse("v1.5");    // -> 1.5
```

### Compare Versions

```java
ApiVersion v1 = ApiVersion.of(1, 0);
ApiVersion v2 = ApiVersion.of(2, 0);

v2.isNewerThan(v1);          // true
v1.isOlderThan(v2);          // true
v1.isCompatibleWith(v1_5);   // true (same major version)
```

## Version Registry

Track supported, deprecated, and sunset versions:

```java
VersionRegistry registry = VersionRegistry.builder()
    .defaultVersion(ApiVersion.V1)
    .latestVersion(ApiVersion.V3)
    .deprecate(
        DeprecationInfo.builder(ApiVersion.V1)
            .sunsetDate(LocalDate.of(2025, 12, 31))
            .reason("Superseded by v2 with better performance")
            .replacementVersion(ApiVersion.V2)
            .migrationGuide("https://docs.example.com/migrate-v1-to-v2")
            .build()
    )
    .build();

// Check version status
boolean deprecated = registry.isDeprecated(ApiVersion.V1);
boolean sunset = registry.isSunset(ApiVersion.V1);
boolean supported = registry.isSupported(ApiVersion.V2);

// Get deprecation info
registry.getDeprecationInfo(ApiVersion.V1)
    .ifPresent(info -> {
        System.out.println("Sunset: " + info.getSunsetDate());
        System.out.println("Use: " + info.getReplacementVersion());
    });
```

## Combining Strategies

Use composite resolvers with fallback:

```java
// Try URL first, then header, then default
VersionResolver<HttpServletRequest> resolver = VersionResolver.composite(
    new UrlPathVersionResolver<>(HttpServletRequest::getRequestURI),
    new HeaderVersionResolver<>("Api-Version", HttpServletRequest::getHeader),
    request -> Optional.of(ApiVersion.V1) // Default
);

Optional<ApiVersion> version = resolver.resolve(request);
```

## Complete Example

```java
public class ApiVersioningFilter implements Filter {

    private final VersionRegistry registry;
    private final VersionResolver<HttpServletRequest> resolver;

    public ApiVersioningFilter() {
        // Setup registry
        this.registry = VersionRegistry.builder()
            .defaultVersion(ApiVersion.V1)
            .latestVersion(ApiVersion.V2)
            .deprecate(
                DeprecationInfo.builder(ApiVersion.V1)
                    .sunsetDate(LocalDate.of(2025, 6, 30))
                    .replacementVersion(ApiVersion.V2)
                    .build()
            )
            .build();

        // Setup resolver
        this.resolver = VersionResolver.composite(
            new UrlPathVersionResolver<>(HttpServletRequest::getRequestURI),
            new HeaderVersionResolver<>("X-API-Version", HttpServletRequest::getHeader)
        );
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Resolve version
        ApiVersion version = resolver.resolve(req)
            .orElse(registry.getDefaultVersion());

        // Check if sunset
        if (registry.isSunset(version)) {
            res.setStatus(410); // Gone
            res.getWriter().write("API version " + version + " is no longer supported");
            return;
        }

        // Add deprecation warnings
        registry.getDeprecationInfo(version).ifPresent(info -> {
            res.setHeader("Deprecation", "true");
            info.getSunsetDate().ifPresent(date ->
                res.setHeader("Sunset", date.toString())
            );
            info.getReplacementVersion().ifPresent(replacement ->
                res.setHeader("Link", "<" + replacement.toPathSegment() + ">; rel=\"successor-version\"")
            );
        });

        // Store version for controllers
        req.setAttribute("api.version", version);

        chain.doFilter(request, response);
    }
}
```

## Best Practices

### 1. Version Semantics

- **Major version**: Breaking changes, incompatible API changes
- **Minor version**: Backward-compatible features
- Use major version in public APIs: `v1`, `v2`, `v3`

### 2. Sunset Strategy

- Provide at least **6-12 months** notice before sunset
- Use HTTP headers to warn clients:
  - `Deprecation: true`
  - `Sunset: 2025-12-31`
  - `Link: <v2>; rel="successor-version"`

### 3. Multiple Versions

- Support **at least 2 major versions** simultaneously
- Make default version the **most stable**, not always the latest
- Use version registry to track lifecycle

### 4. Documentation

- Document all breaking changes
- Provide migration guides
- Show examples for each version

### 5. Error Handling

- Return `410 Gone` for sunset versions
- Return `400 Bad Request` for invalid versions
- Include deprecation info in error responses

## Integration with Spring

### Spring MVC

```java
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {

    private final VersionRegistry registry;
    private final VersionResolver<HttpServletRequest> resolver;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        ApiVersion version = resolver.resolve(request)
            .orElse(registry.getDefaultVersion());

        if (registry.isSunset(version)) {
            response.setStatus(HttpStatus.GONE.value());
            return false;
        }

        request.setAttribute("api.version", version);
        return true;
    }
}
```

### Spring WebFlux

```java
@Component
public class ApiVersionWebFilter implements WebFilter {

    private final VersionRegistry registry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Extract version from request
        String path = exchange.getRequest().getURI().getPath();
        ApiVersion version = extractVersion(path)
            .orElse(registry.getDefaultVersion());

        // Check sunset
        if (registry.isSunset(version)) {
            exchange.getResponse().setStatusCode(HttpStatus.GONE);
            return exchange.getResponse().setComplete();
        }

        // Store in exchange attributes
        exchange.getAttributes().put("api.version", version);

        return chain.filter(exchange);
    }
}
```

## Testing

```java
class UrlPathVersionResolverTest {

    @Test
    void shouldResolveVersionFromPath() {
        VersionResolver<String> resolver =
            new UrlPathVersionResolver<>(path -> path);

        Optional<ApiVersion> version = resolver.resolve("/v1/users");

        assertThat(version).contains(ApiVersion.of(1, 0));
    }

    @Test
    void shouldResolveMinorVersion() {
        VersionResolver<String> resolver =
            new UrlPathVersionResolver<>(path -> path);

        Optional<ApiVersion> version = resolver.resolve("/v1.5/users");

        assertThat(version).contains(ApiVersion.of(1, 5));
    }
}
```

## License

Copyright © 2026 Marcus Prado. All rights reserved.
