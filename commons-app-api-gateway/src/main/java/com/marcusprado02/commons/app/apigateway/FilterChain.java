package com.marcusprado02.commons.app.apigateway;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Represents the filter chain in the API Gateway.
 *
 * <p>The filter chain is responsible for invoking filters in order and eventually executing the
 * target backend call.
 */
@FunctionalInterface
public interface FilterChain {

  /**
   * Invokes the next filter in the chain or the backend handler.
   *
   * @param request the gateway request
   * @return the result containing the gateway response
   */
  Result<GatewayResponse> next(GatewayRequest request);
}
