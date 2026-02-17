package com.marcusprado02.commons.app.apigateway;

import java.util.Map;

/**
 * Represents a successful route match.
 *
 * @param route the matched route
 * @param pathParams extracted path parameters
 */
public record RouteMatch(Route route, Map<String, String> pathParams) {

  public RouteMatch {
    pathParams = Map.copyOf(pathParams);
  }

  /**
   * Gets the target URL for this match.
   *
   * @return the target URL
   */
  public String getTargetUrl() {
    return route.targetUrl();
  }

  /**
   * Gets the route ID.
   *
   * @return the route ID
   */
  public String getRouteId() {
    return route.id();
  }
}
