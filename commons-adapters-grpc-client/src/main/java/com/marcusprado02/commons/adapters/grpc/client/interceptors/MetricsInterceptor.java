package com.marcusprado02.commons.adapters.grpc.client.interceptors;

import io.grpc.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Client interceptor for collecting per-method metrics.
 *
 * <p>Metrics collected:
 *
 * <ul>
 *   <li>Total calls
 *   <li>Success count
 *   <li>Failure count
 *   <li>Average duration
 *   <li>Failures by status code
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * MetricsInterceptor metrics = new MetricsInterceptor();
 * ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
 *     .intercept(metrics)
 *     .build();
 *
 * // Later, retrieve metrics
 * MethodMetrics stats = metrics.getMethodMetrics("myservice.MyService/MyMethod");
 * System.out.println("Success rate: " + stats.successRate() + "%");
 * }</pre>
 */
public class MetricsInterceptor implements ClientInterceptor {

  private final ConcurrentMap<String, MethodMetrics> metricsMap = new ConcurrentHashMap<>();

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

    String methodName = method.getFullMethodName();
    Instant startTime = Instant.now();

    MethodMetrics metrics = metricsMap.computeIfAbsent(methodName, k -> new MethodMetrics());
    metrics.requestCount.incrementAndGet();

    ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);

    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(call) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        super.start(
            new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                responseListener) {

              @Override
              public void onClose(Status status, Metadata trailers) {
                long durationMs = Duration.between(startTime, Instant.now()).toMillis();
                metrics.addDuration(durationMs);

                if (status.isOk()) {
                  metrics.successCount.incrementAndGet();
                } else {
                  metrics.failureCount.incrementAndGet();
                  metrics
                      .failuresByStatus
                      .computeIfAbsent(status.getCode().name(), k -> new AtomicLong())
                      .incrementAndGet();
                }

                super.onClose(status, trailers);
              }
            },
            headers);
      }
    };
  }

  /**
   * Gets metrics for a specific method.
   *
   * @param methodName full gRPC method name
   * @return method metrics or null if not found
   */
  public MethodMetrics getMethodMetrics(String methodName) {
    return metricsMap.get(methodName);
  }

  /**
   * Gets all metrics.
   *
   * @return map of method name to metrics
   */
  public Map<String, MethodMetrics> getAllMetrics() {
    return Map.copyOf(metricsMap);
  }

  /**
   * Resets all metrics.
   */
  public void reset() {
    metricsMap.clear();
  }

  /**
   * Metrics for a single method.
   */
  public static class MethodMetrics {
    private final AtomicLong requestCount = new AtomicLong();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private final AtomicLong totalDurationMs = new AtomicLong();
    private final ConcurrentMap<String, AtomicLong> failuresByStatus = new ConcurrentHashMap<>();

    void addDuration(long durationMs) {
      totalDurationMs.addAndGet(durationMs);
    }

    /**
     * Gets total number of calls.
     *
     * @return total calls
     */
    public long totalCalls() {
      return requestCount.get();
    }

    /**
     * Gets number of successful calls.
     *
     * @return success count
     */
    public long successCalls() {
      return successCount.get();
    }

    /**
     * Gets number of failed calls.
     *
     * @return failure count
     */
    public long failureCalls() {
      return failureCount.get();
    }

    /**
     * Gets average call duration in milliseconds.
     *
     * @return average duration
     */
    public double averageDurationMs() {
      long total = requestCount.get();
      return total == 0 ? 0.0 : (double) totalDurationMs.get() / total;
    }

    /**
     * Gets total duration of all calls in milliseconds.
     *
     * @return total duration
     */
    public long totalDurationMs() {
      return totalDurationMs.get();
    }

    /**
     * Gets failures by status code.
     *
     * @return map of status code to count
     */
    public Map<String, Long> failuresByStatus() {
      return failuresByStatus.entrySet().stream()
          .collect(
              java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    /**
     * Gets success rate as percentage.
     *
     * @return success rate (0-100)
     */
    public double successRate() {
      long total = requestCount.get();
      return total == 0 ? 0.0 : (double) successCount.get() / total * 100.0;
    }
  }
}
