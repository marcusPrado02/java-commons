package com.marcusprado02.commons.app.apigateway;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;

/**
 * Load balancer interface for selecting backend instances.
 *
 * <p>Load balancers distribute incoming requests across multiple backend instances using different
 * strategies.
 *
 * <p>Example:
 *
 * <pre>{@code
 * LoadBalancer loadBalancer = LoadBalancer.roundRobin();
 * Result<String> instance = loadBalancer.choose(
 *     List.of(
 *         "http://service1:8080",
 *         "http://service2:8080",
 *         "http://service3:8080"
 *     )
 * );
 * }</pre>
 */
@FunctionalInterface
public interface LoadBalancer {

  /**
   * Chooses a backend instance from the available instances.
   *
   * @param instances the list of available backend URLs
   * @return the result containing the selected instance URL
   */
  Result<String> choose(List<String> instances);

  /**
   * Creates a round-robin load balancer.
   *
   * @return a round-robin load balancer instance
   */
  static LoadBalancer roundRobin() {
    return new RoundRobinLoadBalancer();
  }

  /**
   * Creates a random load balancer.
   *
   * @return a random load balancer instance
   */
  static LoadBalancer random() {
    return new RandomLoadBalancer();
  }

  /**
   * Creates a weighted random load balancer.
   *
   * @param weights the weights for each instance (must match instances list size)
   * @return a weighted random load balancer instance
   */
  static LoadBalancer weightedRandom(List<Integer> weights) {
    return new WeightedRandomLoadBalancer(weights);
  }

  /**
   * Creates a least connections load balancer.
   *
   * @return a least connections load balancer instance
   */
  static LoadBalancer leastConnections() {
    return new LeastConnectionsLoadBalancer();
  }
}
