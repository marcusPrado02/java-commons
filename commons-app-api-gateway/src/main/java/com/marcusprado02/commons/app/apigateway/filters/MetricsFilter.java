package com.marcusprado02.commons.app.apigateway.filters;

import com.marcusprado02.commons.app.apigateway.FilterChain;
import com.marcusprado02.commons.app.apigateway.GatewayFilter;
import com.marcusprado02.commons.app.apigateway.GatewayRequest;
import com.marcusprado02.commons.app.apigateway.GatewayResponse;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics filter for API Gateway.
 *
 * <p>Collects metrics about request processing.
 *
 * <p>Example:
 *
 * <pre>{@code
 * MetricsFilter metricsFilter = new MetricsFilter();
 * ApiGateway gateway = ApiGateway.builder()
 *     .addFilter(metricsFilter)
 *     .build();
 *
 * // Later, check metrics
 * System.out.println("Total requests: " + metricsFilter.getTotalRequests());
 * System.out.println("Success rate: " + metricsFilter.getSuccessRate());
 * }</pre>
 */
public final class MetricsFilter implements GatewayFilter {

  private final LongAdder totalRequests = new LongAdder();
  private final LongAdder successfulRequests = new LongAdder();
  private final LongAdder failedRequests = new LongAdder();
  private final LongAdder totalLatency = new LongAdder();

  private final int order;

  /** Creates a metrics filter with default order (200). */
  public MetricsFilter() {
    this(200);
  }

  /**
   * Creates a metrics filter with custom order.
   *
   * @param order the filter order
   */
  public MetricsFilter(int order) {
    this.order = order;
  }

  @Override
  public Result<GatewayResponse> filter(GatewayRequest request, FilterChain chain) {
    totalRequests.increment();
    long startTime = System.nanoTime();

    Result<GatewayResponse> result = chain.next(request);

    long latency = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
    totalLatency.add(latency);

    if (result.isOk()) {
      successfulRequests.increment();
    } else {
      failedRequests.increment();
    }

    return result;
  }

  @Override
  public int getOrder() {
    return order;
  }

  /**
   * Gets the total number of requests processed.
   *
   * @return the total request count
   */
  public long getTotalRequests() {
    return totalRequests.sum();
  }

  /**
   * Gets the number of successful requests.
   *
   * @return the successful request count
   */
  public long getSuccessfulRequests() {
    return successfulRequests.sum();
  }

  /**
   * Gets the number of failed requests.
   *
   * @return the failed request count
   */
  public long getFailedRequests() {
    return failedRequests.sum();
  }

  /**
   * Gets the total latency in milliseconds.
   *
   * @return the total latency
   */
  public long getTotalLatency() {
    return totalLatency.sum();
  }

  /**
   * Gets the average latency in milliseconds.
   *
   * @return the average latency, or 0 if no requests processed
   */
  public double getAverageLatency() {
    long total = getTotalRequests();
    return total > 0 ? (double) getTotalLatency() / total : 0.0;
  }

  /**
   * Gets the success rate as a percentage.
   *
   * @return the success rate (0-100), or 0 if no requests processed
   */
  public double getSuccessRate() {
    long total = getTotalRequests();
    return total > 0 ? (double) getSuccessfulRequests() / total * 100 : 0.0;
  }

  /** Resets all metrics. */
  public void reset() {
    totalRequests.reset();
    successfulRequests.reset();
    failedRequests.reset();
    totalLatency.reset();
  }
}
