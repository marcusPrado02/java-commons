package com.marcusprado02.commons.app.apigateway;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Least connections load balancer implementation. */
final class LeastConnectionsLoadBalancer implements LoadBalancer {

  private final Map<String, AtomicInteger> connections = new ConcurrentHashMap<>();

  @Override
  public Result<String> choose(List<String> instances) {
    if (instances == null || instances.isEmpty()) {
      return Result.fail(
          Problem.of(
              new ErrorCode("NO_INSTANCES"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "No backend instances available"));
    }

    // Find instance with least connections
    String selected = null;
    int minConnections = Integer.MAX_VALUE;

    for (String instance : instances) {
      int count = connections.computeIfAbsent(instance, k -> new AtomicInteger(0)).get();
      if (count < minConnections) {
        minConnections = count;
        selected = instance;
      }
    }

    if (selected != null) {
      connections.get(selected).incrementAndGet();
    }

    return selected != null
        ? Result.ok(selected)
        : Result.fail(
            Problem.of(
                new ErrorCode("NO_INSTANCE_SELECTED"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to select instance"));
  }

  /**
   * Decrements the connection count for an instance.
   *
   * @param instance the instance URL
   */
  public void releaseConnection(String instance) {
    AtomicInteger counter = connections.get(instance);
    if (counter != null) {
      counter.decrementAndGet();
    }
  }
}
