package com.marcusprado02.commons.app.apigateway;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import java.util.function.Function;

/** Default implementation of the filter chain. */
final class DefaultFilterChain implements FilterChain {

  private final List<GatewayFilter> filters;
  private final Function<GatewayRequest, Result<GatewayResponse>> backendHandler;
  private final int currentIndex;

  DefaultFilterChain(
      List<GatewayFilter> filters,
      Function<GatewayRequest, Result<GatewayResponse>> backendHandler) {
    this(filters, backendHandler, 0);
  }

  private DefaultFilterChain(
      List<GatewayFilter> filters,
      Function<GatewayRequest, Result<GatewayResponse>> backendHandler,
      int currentIndex) {
    this.filters = filters;
    this.backendHandler = backendHandler;
    this.currentIndex = currentIndex;
  }

  @Override
  public Result<GatewayResponse> next(GatewayRequest request) {
    if (currentIndex < filters.size()) {
      // Get current filter and create next chain
      GatewayFilter filter = filters.get(currentIndex);
      FilterChain nextChain = new DefaultFilterChain(filters, backendHandler, currentIndex + 1);

      // Execute filter
      return filter.filter(request, nextChain);
    } else {
      // All filters executed, call backend handler
      return backendHandler.apply(request);
    }
  }
}
