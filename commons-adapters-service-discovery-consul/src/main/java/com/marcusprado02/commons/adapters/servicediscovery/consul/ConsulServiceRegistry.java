package com.marcusprado02.commons.adapters.servicediscovery.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.agent.model.Service;
import com.ecwid.consul.v1.health.model.HealthService;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.servicediscovery.HealthCheck;
import com.marcusprado02.commons.ports.servicediscovery.ServiceInstance;
import com.marcusprado02.commons.ports.servicediscovery.ServiceRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consul implementation of the ServiceRegistry port.
 *
 * <p>This adapter uses the Consul HTTP API to register/deregister services, perform health checks,
 * and discover service instances. It wraps the consul-api client library.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Service registration with automatic health checks
 *   <li>HTTP, TCP, and TTL health check support
 *   <li>Service discovery with health filtering
 *   <li>Metadata tag support
 *   <li>Automatic cleanup on deregistration
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * ConsulClient consulClient = new ConsulClient("localhost", 8500);
 * ServiceRegistry registry = new ConsulServiceRegistry(consulClient);
 *
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
 * registry.register(instance, check);
 * }</pre>
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public class ConsulServiceRegistry implements ServiceRegistry {

  private static final Logger logger = LoggerFactory.getLogger(ConsulServiceRegistry.class);
  private final ConsulClient consulClient;

  /**
   * Creates a new ConsulServiceRegistry.
   *
   * @param consulClient the Consul client instance
   */
  public ConsulServiceRegistry(ConsulClient consulClient) {
    this.consulClient = Objects.requireNonNull(consulClient, "consulClient cannot be null");
  }

  /**
   * Creates a ConsulServiceRegistry with default localhost connection.
   *
   * @return new registry instance
   */
  public static ConsulServiceRegistry createDefault() {
    return new ConsulServiceRegistry(new ConsulClient("localhost"));
  }

  /**
   * Creates a ConsulServiceRegistry with custom host and port.
   *
   * @param host Consul agent host
   * @param port Consul agent port
   * @return new registry instance
   */
  public static ConsulServiceRegistry create(String host, int port) {
    return new ConsulServiceRegistry(new ConsulClient(host, port));
  }

  @Override
  public Result<Void> register(ServiceInstance instance, HealthCheck healthCheck) {
    try {
      NewService newService = convertToNewService(instance);

      if (healthCheck != null) {
        NewService.Check check = convertHealthCheck(instance, healthCheck);
        newService.setCheck(check);
      }

      consulClient.agentServiceRegister(newService);
      logger.info(
          "Registered service: {} with instance ID: {}",
          instance.serviceId(),
          instance.instanceId());

      return Result.ok(null);
    } catch (Exception e) {
      logger.error("Failed to register service: {}", instance.instanceId(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SERVICE_REGISTRATION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to register service with Consul"));
    }
  }

  @Override
  public Result<Void> register(ServiceInstance instance) {
    return register(instance, null);
  }

  @Override
  public Result<Void> deregister(String instanceId) {
    try {
      consulClient.agentServiceDeregister(instanceId);
      logger.info("Deregistered service instance: {}", instanceId);
      return Result.ok(null);
    } catch (Exception e) {
      logger.error("Failed to deregister service: {}", instanceId, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SERVICE_DEREGISTRATION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to deregister service from Consul"));
    }
  }

  @Override
  public Result<List<ServiceInstance>> discover(String serviceId) {
    try {
      Response<List<HealthService>> response =
          consulClient.getHealthServices(serviceId, true, QueryParams.DEFAULT);

      List<ServiceInstance> instances = new ArrayList<>();
      for (HealthService healthService : response.getValue()) {
        ServiceInstance instance = convertFromHealthService(healthService.getService());
        instances.add(instance);
      }

      logger.debug("Discovered {} instances of service: {}", instances.size(), serviceId);
      return Result.ok(instances);
    } catch (Exception e) {
      logger.error("Failed to discover service: {}", serviceId, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SERVICE_DISCOVERY_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to discover service from Consul"));
    }
  }

  @Override
  public Result<List<String>> listServices() {
    try {
      Response<Map<String, Service>> response = consulClient.getAgentServices();
      List<String> serviceIds =
          response.getValue().values().stream().map(Service::getService).distinct().toList();

      logger.debug("Found {} registered services", serviceIds.size());
      return Result.ok(serviceIds);
    } catch (Exception e) {
      logger.error("Failed to list services", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SERVICE_LIST_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to list services from Consul"));
    }
  }

  @Override
  public Result<List<ServiceInstance>> getInstances(String serviceId) {
    try {
      Response<List<HealthService>> response =
          consulClient.getHealthServices(serviceId, false, QueryParams.DEFAULT);

      List<ServiceInstance> instances = new ArrayList<>();
      for (HealthService healthService : response.getValue()) {
        ServiceInstance instance = convertFromHealthService(healthService.getService());
        instances.add(instance);
      }

      logger.debug(
          "Retrieved {} instances (including unhealthy) of service: {}",
          instances.size(),
          serviceId);
      return Result.ok(instances);
    } catch (Exception e) {
      logger.error("Failed to get instances for service: {}", serviceId, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SERVICE_INSTANCES_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to retrieve service instances from Consul"));
    }
  }

  @Override
  public Result<Void> updateHealthCheck(String instanceId, HealthCheck healthCheck) {
    // Consul doesn't support updating health checks directly.
    // We need to deregister and re-register.
    return Result.fail(
        Problem.of(
            ErrorCode.of("OPERATION_NOT_SUPPORTED"),
            ErrorCategory.VALIDATION,
            Severity.WARNING,
            "Consul does not support updating health checks. Deregister and re-register instead."));
  }

  @Override
  public Result<Void> heartbeat(String instanceId) {
    try {
      // For TTL checks, send a heartbeat via check pass
      String checkId = "service:" + instanceId;
      consulClient.agentCheckPass(checkId);
      logger.debug("Sent heartbeat for instance: {}", instanceId);
      return Result.ok(null);
    } catch (Exception e) {
      logger.error("Failed to send heartbeat for instance: {}", instanceId, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("HEARTBEAT_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send heartbeat to Consul"));
    }
  }

  private NewService convertToNewService(ServiceInstance instance) {
    NewService newService = new NewService();
    newService.setId(instance.instanceId());
    newService.setName(instance.serviceId());
    newService.setAddress(instance.host());
    newService.setPort(instance.port());

    // Convert metadata to tags
    List<String> tags = new ArrayList<>();
    if (instance.secure()) {
      tags.add("secure");
    }
    instance.metadata().forEach((key, value) -> tags.add(key + "=" + value));
    newService.setTags(tags);

    return newService;
  }

  private NewService.Check convertHealthCheck(ServiceInstance instance, HealthCheck healthCheck) {
    NewService.Check check = new NewService.Check();

    switch (healthCheck.type()) {
      case HTTP -> {
        String url = healthCheck.endpoint();
        check.setHttp(url);
        check.setMethod("GET");
      }
      case TCP -> check.setTcp(healthCheck.endpoint());
      case TTL -> check.setTtl(formatDuration(healthCheck.interval()));
    }

    check.setInterval(formatDuration(healthCheck.interval()));
    if (healthCheck.timeout() != null && healthCheck.type() != HealthCheck.Type.TTL) {
      check.setTimeout(formatDuration(healthCheck.timeout()));
    }
    if (healthCheck.deregisterAfter() != null) {
      check.setDeregisterCriticalServiceAfter(formatDuration(healthCheck.deregisterAfter()));
    }

    return check;
  }

  private ServiceInstance convertFromHealthService(
      com.ecwid.consul.v1.health.model.HealthService.Service service) {
    Map<String, String> metadata = new HashMap<>();
    boolean secure = false;

    if (service.getTags() != null) {
      for (String tag : service.getTags()) {
        if ("secure".equals(tag)) {
          secure = true;
        } else if (tag.contains("=")) {
          String[] parts = tag.split("=", 2);
          metadata.put(parts[0], parts[1]);
        }
      }
    }

    return ServiceInstance.builder()
        .serviceId(service.getService())
        .instanceId(service.getId())
        .host(service.getAddress())
        .port(service.getPort())
        .secure(secure)
        .metadata(metadata)
        .build();
  }

  private String formatDuration(java.time.Duration duration) {
    long seconds = duration.getSeconds();
    if (seconds < 60) {
      return seconds + "s";
    } else if (seconds < 3600) {
      return (seconds / 60) + "m";
    } else {
      return (seconds / 3600) + "h";
    }
  }
}
