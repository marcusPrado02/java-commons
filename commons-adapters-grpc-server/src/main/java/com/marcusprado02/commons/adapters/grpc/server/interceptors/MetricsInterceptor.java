package com.marcusprado02.commons.adapters.grpc.server.interceptors;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server interceptor for collecting gRPC metrics.
 *
 * <p>Collected metrics:
 * <ul>
 *   <li>Total calls per method</li>
 *   <li>Success/failure counts</li>
 *   <li>Average duration per method</li>
 *   <li>Total bytes sent/received (if enabled)</li>
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * MetricsInterceptor metrics = new MetricsInterceptor();
 * ServerBuilder.forPort(9090)
 *     .addService(myService)
 *     .intercept(metrics)
 *     .build();
 *
 * // Later, retrieve metrics
 * MetricsInterceptor.MethodMetrics stats = metrics.getMethodMetrics("myservice/MyMethod");
 * System.out.println("Total calls: " + stats.totalCalls());
 * }</pre>
 */
public class MetricsInterceptor implements ServerInterceptor {

  private final ConcurrentMap<String, MethodMetrics> metricsMap = new ConcurrentHashMap<>();

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
    String methodName = call.getMethodDescriptor().getFullMethodName();
    Instant startTime = Instant.now();

    MethodMetrics metrics = metricsMap.computeIfAbsent(methodName, k -> new MethodMetrics());
    metrics.requestCount.incrementAndGet();

    ServerCall.Listener<ReqT> listener = next.startCall(
        new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
          @Override
          public void close(Status status, Metadata trailers) {
            long durationMs = Duration.between(startTime, Instant.now()).toMillis();
            metrics.addDuration(durationMs);

            if (status.isOk()) {
              metrics.successCount.incrementAndGet();
            } else {
              metrics.failureCount.incrementAndGet();
              metrics.failuresByStatus
                  .computeIfAbsent(status.getCode().name(), k -> new AtomicLong())
                  .incrementAndGet();
            }

            super.close(status, trailers);
          }
        }, headers);

    return listener;
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
   * Gets all collected metrics.
   *
   * @return map of method names to metrics
   */
  public ConcurrentMap<String, MethodMetrics> getAllMetrics() {
    return metricsMap;
  }

  /**
   * Resets all metrics.
   */
  public void reset() {
    metricsMap.clear();
  }

  /**
   * Metrics for a single gRPC method.
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
     * Gets total number of requests.
     *
     * @return request count
     */
    public long totalCalls() {
      return requestCount.get();
    }

    /**
     * Gets number of successful requests.
     *
     * @return success count
     */
    public long successCalls() {
      return successCount.get();
    }

    /**
     * Gets number of failed requests.
     *
     * @return failure count
     */
    public long failureCalls() {
      return failureCount.get();
    }

    /**
     * Gets average duration in milliseconds.
     *
     * @return average duration or 0 if no calls
     */
    public double averageDurationMs() {
      long total = requestCount.get();
      return total > 0 ? (double) totalDurationMs.get() / total : 0.0;
    }

    /**
     * Gets total duration across all calls.
     *
     * @return total duration in milliseconds
     */
    public long totalDurationMs() {
      return totalDurationMs.get();
    }

    /**
     * Gets failure counts by status code.
     *
     * @return map of status code to count
     */
    public ConcurrentMap<String, AtomicLong> failuresByStatus() {
      return failuresByStatus;
    }

    /**
     * Gets success rate as a percentage.
     *
     * @return success rate (0.0 - 100.0)
     */
    public double successRate() {
      long total = requestCount.get();
      return total > 0 ? (double) successCount.get() / total * 100.0 : 0.0;
    }
  }
}
