package com.marcusprado02.commons.app.apigateway;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a route configuration in the API Gateway.
 *
 * <p>A route defines how incoming requests should be matched and forwarded to backend services.
 *
 * @param id unique identifier for the route
 * @param pathPattern path pattern (supports wildcards and path parameters)
 * @param method HTTP method filter (null matches all methods)
 * @param targetUrl the backend service URL
 * @param priority route priority (lower number = higher priority)
 */
public record Route(String id, String pathPattern, String method, String targetUrl, int priority) {

  /**
   * Creates a builder for Route.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Matches the given request against this route.
   *
   * @param request the gateway request
   * @return match result containing path parameters if matched
   */
  public Optional<RouteMatch> matches(GatewayRequest request) {
    // Check method match
    if (method != null && !method.equalsIgnoreCase(request.method())) {
      return Optional.empty();
    }

    // Convert path pattern to regex
    String regex = pathPatternToRegex(pathPattern);
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(request.path());

    if (matcher.matches()) {
      Map<String, String> pathParams = extractPathParams(pathPattern, request.path());
      return Optional.of(new RouteMatch(this, pathParams));
    }

    return Optional.empty();
  }

  private String pathPatternToRegex(String pattern) {
    // Replace {param} with named capture groups
    return pattern
        .replaceAll("\\{([^/]+)}", "(?<$1>[^/]+)")
        .replaceAll("\\*\\*", ".*")
        .replaceAll("(?<!\\.)\\*", "[^/]+");
  }

  private Map<String, String> extractPathParams(String pattern, String path) {
    java.util.Map<String, String> params = new java.util.HashMap<>();
    String regex = pathPatternToRegex(pattern);
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(path);

    if (m.matches()) {
      // Extract named groups
      Pattern paramPattern = Pattern.compile("\\{([^/]+)}");
      Matcher paramMatcher = paramPattern.matcher(pattern);

      while (paramMatcher.find()) {
        String paramName = paramMatcher.group(1);
        try {
          String value = m.group(paramName);
          if (value != null) {
            params.put(paramName, value);
          }
        } catch (IllegalArgumentException ignored) {
          // Named group not found
        }
      }
    }

    return params;
  }

  /** Builder for Route. */
  public static class Builder {
    private String id;
    private String pathPattern;
    private String method;
    private String targetUrl;
    private int priority = 0;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder pathPattern(String pathPattern) {
      this.pathPattern = pathPattern;
      return this;
    }

    public Builder method(String method) {
      this.method = method;
      return this;
    }

    public Builder targetUrl(String targetUrl) {
      this.targetUrl = targetUrl;
      return this;
    }

    public Builder priority(int priority) {
      this.priority = priority;
      return this;
    }

    public Route build() {
      if (id == null) {
        throw new IllegalArgumentException("Route id is required");
      }
      if (pathPattern == null) {
        throw new IllegalArgumentException("Route pathPattern is required");
      }
      if (targetUrl == null) {
        throw new IllegalArgumentException("Route targetUrl is required");
      }
      return new Route(id, pathPattern, method, targetUrl, priority);
    }
  }
}
