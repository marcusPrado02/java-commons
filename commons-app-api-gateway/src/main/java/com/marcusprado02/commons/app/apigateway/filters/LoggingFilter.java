package com.marcusprado02.commons.app.apigateway.filters;

import com.marcusprado02.commons.app.apigateway.FilterChain;
import com.marcusprado02.commons.app.apigateway.GatewayFilter;
import com.marcusprado02.commons.app.apigateway.GatewayRequest;
import com.marcusprado02.commons.app.apigateway.GatewayResponse;
import com.marcusprado02.commons.kernel.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging filter for API Gateway.
 *
 * <p>Logs incoming requests and outgoing responses.
 *
 * <p>Example:
 *
 * <pre>{@code
 * ApiGateway gateway = ApiGateway.builder()
 *     .addFilter(new LoggingFilter())
 *     .build();
 * }</pre>
 */
public final class LoggingFilter implements GatewayFilter {

  private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

  private final int order;

  /** Creates a logging filter with default order (100). */
  public LoggingFilter() {
    this(100);
  }

  /**
   * Creates a logging filter with custom order.
   *
   * @param order the filter order
   */
  public LoggingFilter(int order) {
    this.order = order;
  }

  @Override
  public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
    long startTime = System.currentTimeMillis();
    String requestId = request.getHeader("X-Request-ID").orElse("unknown");

    logger.info(
        "Incoming request: method={}, path={}, requestId={}",
        request.method(),
        request.path(),
        requestId);

    Result<GatewayResponse> result = chain.next(request);

    long duration = System.currentTimeMillis() - startTime;

    if (result.isOk()) {
      GatewayResponse response = result.getOrNull();
      logger.info(
          "Response: status={}, duration={}ms, requestId={}",
          response.statusCode(),
          duration,
          requestId);
    } else {
      logger.error(
          "Request failed: error={}, duration={}ms, requestId={}",
          result.problemOrNull().message(),
          duration,
          requestId);
    }

    return result;
  }

  @Override
  public int getOrder() {
    return order;
  }
}
