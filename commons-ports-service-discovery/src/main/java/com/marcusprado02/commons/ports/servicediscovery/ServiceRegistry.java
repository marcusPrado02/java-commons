package com.marcusprado02.commons.ports.servicediscovery;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;

/**
 * Port interface for service discovery and registration.
 *
 * <p>Defines operations to register/deregister services, query available instances, and manage
 * health checks in a service registry.
 *
 * <p>Implementations should support:
 *
 * <ul>
 *   <li>Service registration with health checks
 *   <li>Service deregistration
 *   <li>Service discovery by name
 *   <li>Listing all available services
 *   <li>Health check management
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ServiceInstance instance = ServiceInstance.builder()
 *     .serviceId("payment-service")
 *     .instanceId("payment-01")
 *     .host("192.168.1.100")
 *     .port(8080)
 *     .build();
 *
 * HealthCheck check = HealthCheck.http("http://192.168.1.100:8080/health")
 *     .interval(Duration.ofSeconds(10))
 *     .build();
 *
 * Result<Void> result = registry.register(instance, check);
 * }</pre>
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public interface ServiceRegistry {

  /**
   * Registers a service instance with a health check.
   *
   * @param instance the service instance to register
   * @param healthCheck the health check configuration
   * @return Result indicating success or failure
   */
  Result<Void> register(ServiceInstance instance, HealthCheck healthCheck);

  /**
   * Registers a service instance without a health check.
   *
   * @param instance the service instance to register
   * @return Result indicating success or failure
   */
  Result<Void> register(ServiceInstance instance);

  /**
   * Deregisters a service instance.
   *
   * @param instanceId the unique instance ID to deregister
   * @return Result indicating success or failure
   */
  Result<Void> deregister(String instanceId);

  /**
   * Discovers all instances of a service by name.
   *
   * @param serviceId the logical service name
   * @return Result containing list of healthy service instances
   */
  Result<List<ServiceInstance>> discover(String serviceId);

  /**
   * Lists all registered service names.
   *
   * @return Result containing list of service IDs
   */
  Result<List<String>> listServices();

  /**
   * Gets all instances of a specific service (including unhealthy).
   *
   * @param serviceId the logical service name
   * @return Result containing all instances regardless of health status
   */
  Result<List<ServiceInstance>> getInstances(String serviceId);

  /**
   * Updates the health check for an existing service instance.
   *
   * @param instanceId the instance ID
   * @param healthCheck the new health check configuration
   * @return Result indicating success or failure
   */
  Result<Void> updateHealthCheck(String instanceId, HealthCheck healthCheck);

  /**
   * Sends a heartbeat for a TTL-based health check.
   *
   * @param instanceId the instance ID
   * @return Result indicating success or failure
   */
  Result<Void> heartbeat(String instanceId);
}
