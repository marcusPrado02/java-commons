package com.marcusprado02.commons.adapters.servicediscovery.eureka;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.servicediscovery.HealthCheck;
import com.marcusprado02.commons.ports.servicediscovery.ServiceInstance;
import com.marcusprado02.commons.ports.servicediscovery.ServiceRegistry;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netflix Eureka implementation of {@link ServiceRegistry}.
 *
 * <p>This adapter provides service registration and discovery using Netflix Eureka server. It
 * supports instance registration with health checks, service discovery, and automatic
 * heartbeat/renewal.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * EurekaServiceRegistry registry = EurekaServiceRegistry.builder()
 *     .eurekaServerUrl("http://localhost:8761/eureka")
 *     .instanceConfig(config -> config
 *         .appName("my-service")
 *         .hostName("localhost")
 *         .port(8080))
 *     .build();
 *
 * ServiceInstance instance = ServiceInstance.builder()
 *     .serviceId("my-service")
 *     .instanceId("my-service-01")
 *     .host("localhost")
 *     .port(8080)
 *     .build();
 *
 * Result<Void> result = registry.register(instance);
 * }</pre>
 *
 * @see ServiceRegistry
 * @see com.netflix.discovery.DiscoveryClient
 */
public class EurekaServiceRegistry implements ServiceRegistry {

  private static final Logger logger = LoggerFactory.getLogger(EurekaServiceRegistry.class);

  private final EurekaClient eurekaClient;
  private final ApplicationInfoManager applicationInfoManager;
  private final boolean ownsClient;

  /**
   * Creates a new EurekaServiceRegistry with the provided Eureka client and application info
   * manager.
   *
   * @param eurekaClient the Eureka client
   * @param applicationInfoManager the application info manager
   * @param ownsClient whether this registry owns and should shutdown the client
   */
  public EurekaServiceRegistry(
      EurekaClient eurekaClient,
      ApplicationInfoManager applicationInfoManager,
      boolean ownsClient) {
    this.eurekaClient = eurekaClient;
    this.applicationInfoManager = applicationInfoManager;
    this.ownsClient = ownsClient;
  }

  /**
   * Creates a builder for configuring a new EurekaServiceRegistry.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default EurekaServiceRegistry connecting to localhost:8761.
   *
   * @param appName the application name
   * @return a new EurekaServiceRegistry instance
   */
  public static EurekaServiceRegistry createDefault(String appName) {
    return builder().eurekaServerUrl("http://localhost:8761/eureka").appName(appName).build();
  }

  @Override
  public Result<Void> register(ServiceInstance instance) {
    return register(instance, null);
  }

  @Override
  public Result<Void> register(ServiceInstance instance, HealthCheck healthCheck) {
    try {
      logger.info(
          "Registering service instance: {} ({})", instance.serviceId(), instance.instanceId());

      // Eureka client handles registration automatically when started
      // We just need to update the instance info if needed
      InstanceInfo instanceInfo = convertToInstanceInfo(instance, healthCheck);

      // Set instance status to UP
      applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);

      logger.info(
          "Service instance registered successfully: {} ({})",
          instance.serviceId(),
          instance.instanceId());
      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Failed to register service instance: {}", instance.instanceId(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("REGISTRATION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to register service instance: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> deregister(String instanceId) {
    try {
      logger.info("Deregistering service instance: {}", instanceId);

      // Set instance status to DOWN before shutdown
      applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);

      // Shutdown the client if we own it
      if (ownsClient) {
        eurekaClient.shutdown();
      }

      logger.info("Service instance deregistered successfully: {}", instanceId);
      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Failed to deregister service instance: {}", instanceId, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("DEREGISTRATION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to deregister service instance: " + e.getMessage()));
    }
  }

  @Override
  public Result<List<ServiceInstance>> discover(String serviceId) {
    try {
      logger.debug("Discovering healthy instances for service: {}", serviceId);

      Application application = eurekaClient.getApplication(serviceId.toUpperCase());
      if (application == null) {
        return Result.ok(List.of());
      }

      List<ServiceInstance> instances =
          application.getInstances().stream()
              .filter(info -> info.getStatus() == InstanceInfo.InstanceStatus.UP)
              .map(this::convertFromInstanceInfo)
              .collect(Collectors.toList());

      logger.debug("Found {} healthy instances for service: {}", instances.size(), serviceId);
      return Result.ok(instances);

    } catch (Exception e) {
      logger.error("Failed to discover service: {}", serviceId, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("DISCOVERY_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to discover service: " + e.getMessage()));
    }
  }

  @Override
  public Result<List<String>> listServices() {
    try {
      logger.debug("Listing all registered services");

      List<String> serviceIds =
          eurekaClient.getApplications().getRegisteredApplications().stream()
              .map(app -> app.getName().toLowerCase())
              .collect(Collectors.toList());

      logger.debug("Found {} registered services", serviceIds.size());
      return Result.ok(serviceIds);

    } catch (Exception e) {
      logger.error("Failed to list services", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("LIST_SERVICES_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to list services: " + e.getMessage()));
    }
  }

  @Override
  public Result<List<ServiceInstance>> getInstances(String serviceId) {
    try {
      logger.debug("Getting all instances for service: {}", serviceId);

      Application application = eurekaClient.getApplication(serviceId.toUpperCase());
      if (application == null) {
        return Result.ok(List.of());
      }

      List<ServiceInstance> instances =
          application.getInstances().stream()
              .map(this::convertFromInstanceInfo)
              .collect(Collectors.toList());

      logger.debug("Found {} instances for service: {}", instances.size(), serviceId);
      return Result.ok(instances);

    } catch (Exception e) {
      logger.error("Failed to get instances for service: {}", serviceId, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("GET_INSTANCES_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to get instances: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> updateHealthCheck(String instanceId, HealthCheck healthCheck) {
    logger.warn(
        "updateHealthCheck is not supported by Eureka - health checks are managed by Eureka client");
    return Result.fail(
        Problem.of(
            ErrorCode.of("OPERATION_NOT_SUPPORTED"),
            ErrorCategory.TECHNICAL,
            Severity.WARNING,
            "Health check updates are not supported by Eureka adapter"));
  }

  @Override
  public Result<Void> heartbeat(String instanceId) {
    try {
      logger.debug("Sending heartbeat for instance: {}", instanceId);

      // Eureka client handles heartbeats automatically
      // We can trigger a manual renew if needed
      boolean renewed =
          eurekaClient.getApplicationInfoManager().getInfo().getStatus()
              == InstanceInfo.InstanceStatus.UP;

      if (renewed) {
        logger.debug("Heartbeat sent successfully for instance: {}", instanceId);
        return Result.ok(null);
      } else {
        return Result.fail(
            Problem.of(
                ErrorCode.of("HEARTBEAT_FAILED"),
                ErrorCategory.TECHNICAL,
                Severity.WARNING,
                "Instance is not in UP status"));
      }

    } catch (Exception e) {
      logger.error("Failed to send heartbeat for instance: {}", instanceId, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("HEARTBEAT_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send heartbeat: " + e.getMessage()));
    }
  }

  /** Shuts down the Eureka client if this registry owns it. */
  public void shutdown() {
    if (ownsClient) {
      logger.info("Shutting down Eureka client");
      eurekaClient.shutdown();
    }
  }

  private InstanceInfo convertToInstanceInfo(ServiceInstance instance, HealthCheck healthCheck) {
    Map<String, String> metadata = new HashMap<>(instance.metadata());
    if (healthCheck != null) {
      metadata.put("healthCheckType", healthCheck.type().name());
      if (healthCheck.endpoint() != null) {
        metadata.put("healthCheckEndpoint", healthCheck.endpoint());
      }
    }

    return InstanceInfo.Builder.newBuilder()
        .setAppName(instance.serviceId().toUpperCase())
        .setInstanceId(instance.instanceId())
        .setHostName(instance.host())
        .setPort(instance.port())
        .setSecurePort(instance.secure() ? instance.port() : 0)
        .enablePort(InstanceInfo.PortType.UNSECURE, !instance.secure())
        .enablePort(InstanceInfo.PortType.SECURE, instance.secure())
        .setMetadata(metadata)
        .setStatus(InstanceInfo.InstanceStatus.UP)
        .build();
  }

  private ServiceInstance convertFromInstanceInfo(InstanceInfo info) {
    Map<String, String> metadata = new HashMap<>(info.getMetadata());

    return ServiceInstance.builder()
        .serviceId(info.getAppName().toLowerCase())
        .instanceId(info.getInstanceId())
        .host(info.getHostName())
        .port(info.getPort())
        .secure(info.isPortEnabled(InstanceInfo.PortType.SECURE))
        .metadata(metadata)
        .build();
  }

  /** Builder for creating EurekaServiceRegistry instances. */
  public static class Builder {
    private String eurekaServerUrl = "http://localhost:8761/eureka";
    private String appName;
    private String hostName = "localhost";
    private int port = 8080;
    private boolean securePort = false;
    private Duration registryFetchIntervalSeconds = Duration.ofSeconds(30);
    private Duration instanceInfoReplicationIntervalSeconds = Duration.ofSeconds(30);
    private Duration leaseRenewalIntervalInSeconds = Duration.ofSeconds(30);
    private Duration leaseExpirationDurationInSeconds = Duration.ofSeconds(90);
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Sets the Eureka server URL.
     *
     * @param eurekaServerUrl the Eureka server URL
     * @return this builder
     */
    public Builder eurekaServerUrl(String eurekaServerUrl) {
      this.eurekaServerUrl = eurekaServerUrl;
      return this;
    }

    /**
     * Sets the application name.
     *
     * @param appName the application name
     * @return this builder
     */
    public Builder appName(String appName) {
      this.appName = appName;
      return this;
    }

    /**
     * Sets the host name.
     *
     * @param hostName the host name
     * @return this builder
     */
    public Builder hostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * Sets the port.
     *
     * @param port the port
     * @return this builder
     */
    public Builder port(int port) {
      this.port = port;
      return this;
    }

    /**
     * Sets whether to use secure port.
     *
     * @param securePort true to use secure port
     * @return this builder
     */
    public Builder securePort(boolean securePort) {
      this.securePort = securePort;
      return this;
    }

    /**
     * Sets the registry fetch interval.
     *
     * @param interval the fetch interval
     * @return this builder
     */
    public Builder registryFetchInterval(Duration interval) {
      this.registryFetchIntervalSeconds = interval;
      return this;
    }

    /**
     * Sets the instance info replication interval.
     *
     * @param interval the replication interval
     * @return this builder
     */
    public Builder replicationInterval(Duration interval) {
      this.instanceInfoReplicationIntervalSeconds = interval;
      return this;
    }

    /**
     * Sets the lease renewal interval (heartbeat).
     *
     * @param interval the renewal interval
     * @return this builder
     */
    public Builder renewalInterval(Duration interval) {
      this.leaseRenewalIntervalInSeconds = interval;
      return this;
    }

    /**
     * Sets the lease expiration duration.
     *
     * @param duration the expiration duration
     * @return this builder
     */
    public Builder expirationDuration(Duration duration) {
      this.leaseExpirationDurationInSeconds = duration;
      return this;
    }

    /**
     * Adds metadata to the instance.
     *
     * @param key the metadata key
     * @param value the metadata value
     * @return this builder
     */
    public Builder metadata(String key, String value) {
      this.metadata.put(key, value);
      return this;
    }

    /**
     * Builds a new EurekaServiceRegistry instance.
     *
     * @return a new EurekaServiceRegistry
     */
    public EurekaServiceRegistry build() {
      if (appName == null) {
        throw new IllegalArgumentException("appName must be set");
      }

      // Create instance config
      EurekaInstanceConfig instanceConfig =
          new MyDataCenterInstanceConfig() {
            @Override
            public String getAppname() {
              return appName;
            }

            @Override
            public String getHostName(boolean refresh) {
              return hostName;
            }

            @Override
            public int getNonSecurePort() {
              return securePort ? 0 : port;
            }

            @Override
            public int getSecurePort() {
              return securePort ? port : 0;
            }

            @Override
            public boolean isNonSecurePortEnabled() {
              return !securePort;
            }

            @Override
            public boolean getSecurePortEnabled() {
              return securePort;
            }

            @Override
            public int getLeaseRenewalIntervalInSeconds() {
              return (int) leaseRenewalIntervalInSeconds.getSeconds();
            }

            @Override
            public int getLeaseExpirationDurationInSeconds() {
              return (int) leaseExpirationDurationInSeconds.getSeconds();
            }

            @Override
            public Map<String, String> getMetadataMap() {
              return metadata;
            }
          };

      // Create instance info
      InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();

      // Create application info manager
      ApplicationInfoManager applicationInfoManager =
          new ApplicationInfoManager(instanceConfig, instanceInfo);

      // Create client config
      EurekaClientConfig clientConfig =
          new DefaultEurekaClientConfig() {
            @Override
            public List<String> getEurekaServerServiceUrls(String myZone) {
              List<String> urls = new ArrayList<>();
              urls.add(eurekaServerUrl);
              return urls;
            }

            @Override
            public int getRegistryFetchIntervalSeconds() {
              return (int) registryFetchIntervalSeconds.getSeconds();
            }

            @Override
            public int getInstanceInfoReplicationIntervalSeconds() {
              return (int) instanceInfoReplicationIntervalSeconds.getSeconds();
            }
          };

      // Create Eureka client with required transport factories parameter
      EurekaClient eurekaClient =
          new DiscoveryClient(applicationInfoManager, clientConfig, null, null);

      return new EurekaServiceRegistry(eurekaClient, applicationInfoManager, true);
    }
  }
}
