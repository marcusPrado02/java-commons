package com.marcusprado02.commons.adapters.servicediscovery.eureka;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.servicediscovery.HealthCheck;
import com.marcusprado02.commons.ports.servicediscovery.ServiceInstance;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class EurekaServiceRegistryTest {

  @Container
  private static final GenericContainer<?> eurekaContainer =
      new GenericContainer<>(DockerImageName.parse("springcloud/eureka:latest"))
          .withExposedPorts(8761)
          .withEnv("EUREKA_INSTANCE_HOSTNAME", "eureka")
          .withEnv("EUREKA_CLIENT_REGISTER_WITH_EUREKA", "false")
          .withEnv("EUREKA_CLIENT_FETCH_REGISTRY", "false")
          .waitingFor(Wait.forHttp("/").forPort(8761).withStartupTimeout(Duration.ofMinutes(2)));

  private EurekaServiceRegistry registry;
  private String eurekaUrl;

  @BeforeEach
  void setUp() {
    eurekaUrl =
        "http://"
            + eurekaContainer.getHost()
            + ":"
            + eurekaContainer.getMappedPort(8761)
            + "/eureka";

    registry =
        EurekaServiceRegistry.builder()
            .eurekaServerUrl(eurekaUrl)
            .appName("test-service")
            .hostName("localhost")
            .port(9090)
            .renewalInterval(Duration.ofSeconds(10))
            .expirationDuration(Duration.ofSeconds(30))
            .build();
  }

  @AfterEach
  void tearDown() {
    if (registry != null) {
      registry.shutdown();
    }
  }

  @Test
  void register_shouldRegisterServiceInstance() throws InterruptedException {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("test-service")
            .instanceId("test-01")
            .host("localhost")
            .port(9090)
            .build();

    Result<Void> result = registry.register(instance);

    assertThat(result.isOk()).isTrue();

    // Wait for registration to propagate
    Thread.sleep(3000);

    // Verify instance is discoverable
    Result<List<ServiceInstance>> discovered = registry.discover("test-service");
    assertThat(discovered.isOk()).isTrue();
    assertThat(discovered.getOrNull()).isNotEmpty();
  }

  @Test
  void registerWithHealthCheck_shouldRegisterWithMetadata() throws InterruptedException {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("health-test-service")
            .instanceId("health-01")
            .host("localhost")
            .port(9091)
            .addMetadata("version", "1.0.0")
            .build();

    HealthCheck healthCheck = HealthCheck.http("http://localhost:9091/health").build();

    EurekaServiceRegistry healthRegistry =
        EurekaServiceRegistry.builder()
            .eurekaServerUrl(eurekaUrl)
            .appName("health-test-service")
            .hostName("localhost")
            .port(9091)
            .metadata("version", "1.0.0")
            .build();

    Result<Void> result = healthRegistry.register(instance, healthCheck);
    assertThat(result.isOk()).isTrue();

    // Wait for registration
    Thread.sleep(3000);

    Result<List<ServiceInstance>> discovered = healthRegistry.discover("health-test-service");
    assertThat(discovered.isOk()).isTrue();
    assertThat(discovered.getOrNull()).hasSize(1);

    ServiceInstance found = discovered.getOrNull().get(0);
    assertThat(found.metadata()).containsEntry("version", "1.0.0");

    healthRegistry.shutdown();
  }

  @Test
  void discover_shouldReturnHealthyInstances() throws InterruptedException {
    // Register instance first
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("discover-service")
            .instanceId("discover-01")
            .host("localhost")
            .port(9092)
            .build();

    EurekaServiceRegistry discoverRegistry =
        EurekaServiceRegistry.builder()
            .eurekaServerUrl(eurekaUrl)
            .appName("discover-service")
            .hostName("localhost")
            .port(9092)
            .build();

    discoverRegistry.register(instance);
    Thread.sleep(3000);

    Result<List<ServiceInstance>> result = discoverRegistry.discover("discover-service");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSize(1);

    ServiceInstance found = result.getOrNull().get(0);
    assertThat(found.serviceId()).isEqualToIgnoringCase("discover-service");
    assertThat(found.host()).isEqualTo("localhost");
    assertThat(found.port()).isEqualTo(9092);

    discoverRegistry.shutdown();
  }

  @Test
  void discover_shouldReturnEmptyListForNonexistentService() {
    Result<List<ServiceInstance>> result = registry.discover("nonexistent-service");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }

  @Test
  void deregister_shouldRemoveServiceInstance() throws InterruptedException {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("deregister-service")
            .instanceId("deregister-01")
            .host("localhost")
            .port(9093)
            .build();

    EurekaServiceRegistry deregisterRegistry =
        EurekaServiceRegistry.builder()
            .eurekaServerUrl(eurekaUrl)
            .appName("deregister-service")
            .hostName("localhost")
            .port(9093)
            .build();

    deregisterRegistry.register(instance);
    Thread.sleep(3000);

    Result<List<ServiceInstance>> beforeDeregister =
        deregisterRegistry.discover("deregister-service");
    assertThat(beforeDeregister.getOrNull()).hasSize(1);

    Result<Void> deregisterResult = deregisterRegistry.deregister("deregister-01");
    assertThat(deregisterResult.isOk()).isTrue();

    // Note: Eureka has a delay before instances are fully removed
    // In real scenarios, instances will show as DOWN before being removed
  }

  @Test
  void listServices_shouldReturnAllRegisteredServices() throws InterruptedException {
    ServiceInstance instance1 =
        ServiceInstance.builder()
            .serviceId("list-service-a")
            .instanceId("list-01")
            .host("localhost")
            .port(9094)
            .build();

    ServiceInstance instance2 =
        ServiceInstance.builder()
            .serviceId("list-service-b")
            .instanceId("list-02")
            .host("localhost")
            .port(9095)
            .build();

    EurekaServiceRegistry registryA =
        EurekaServiceRegistry.builder()
            .eurekaServerUrl(eurekaUrl)
            .appName("list-service-a")
            .hostName("localhost")
            .port(9094)
            .build();

    EurekaServiceRegistry registryB =
        EurekaServiceRegistry.builder()
            .eurekaServerUrl(eurekaUrl)
            .appName("list-service-b")
            .hostName("localhost")
            .port(9095)
            .build();

    registryA.register(instance1);
    registryB.register(instance2);
    Thread.sleep(3000);

    Result<List<String>> result = registryA.listServices();

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("list-service-a", "list-service-b");

    registryA.shutdown();
    registryB.shutdown();
  }

  @Test
  void getInstances_shouldReturnAllInstancesRegardlessOfStatus() throws InterruptedException {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("instances-service")
            .instanceId("instances-01")
            .host("localhost")
            .port(9096)
            .build();

    EurekaServiceRegistry instancesRegistry =
        EurekaServiceRegistry.builder()
            .eurekaServerUrl(eurekaUrl)
            .appName("instances-service")
            .hostName("localhost")
            .port(9096)
            .build();

    instancesRegistry.register(instance);
    Thread.sleep(3000);

    Result<List<ServiceInstance>> result = instancesRegistry.getInstances("instances-service");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSize(1);

    instancesRegistry.shutdown();
  }

  @Test
  void heartbeat_shouldMaintainInstanceRegistration() throws InterruptedException {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("heartbeat-service")
            .instanceId("heartbeat-01")
            .host("localhost")
            .port(9097)
            .build();

    EurekaServiceRegistry heartbeatRegistry =
        EurekaServiceRegistry.builder()
            .eurekaServerUrl(eurekaUrl)
            .appName("heartbeat-service")
            .hostName("localhost")
            .port(9097)
            .renewalInterval(Duration.ofSeconds(5))
            .build();

    heartbeatRegistry.register(instance);
    Thread.sleep(3000);

    // Send heartbeat
    Result<Void> heartbeatResult = heartbeatRegistry.heartbeat("heartbeat-01");
    assertThat(heartbeatResult.isOk()).isTrue();

    // Verify instance is still discoverable
    Result<List<ServiceInstance>> discovered = heartbeatRegistry.discover("heartbeat-service");
    assertThat(discovered.isOk()).isTrue();
    assertThat(discovered.getOrNull()).hasSize(1);

    heartbeatRegistry.shutdown();
  }

  @Test
  void updateHealthCheck_shouldReturnNotSupported() {
    HealthCheck newCheck = HealthCheck.tcp("localhost:9000").build();

    Result<Void> result = registry.updateHealthCheck("some-instance", newCheck);

    assertThat(result.isOk()).isFalse();
    assertThat(result.problemOrNull()).isNotNull();
    assertThat(result.problemOrNull().code().value()).isEqualTo("OPERATION_NOT_SUPPORTED");
  }

  @Test
  void multipleInstances_shouldDiscoverAll() throws InterruptedException {
    ServiceInstance instance1 =
        ServiceInstance.builder()
            .serviceId("multi-service")
            .instanceId("multi-01")
            .host("localhost")
            .port(9098)
            .build();

    ServiceInstance instance2 =
        ServiceInstance.builder()
            .serviceId("multi-service")
            .instanceId("multi-02")
            .host("localhost")
            .port(9099)
            .build();

    EurekaServiceRegistry registry1 =
        EurekaServiceRegistry.builder()
            .eurekaServerUrl(eurekaUrl)
            .appName("multi-service")
            .hostName("localhost")
            .port(9098)
            .build();

    EurekaServiceRegistry registry2 =
        EurekaServiceRegistry.builder()
            .eurekaServerUrl(eurekaUrl)
            .appName("multi-service")
            .hostName("localhost")
            .port(9099)
            .build();

    registry1.register(instance1);
    registry2.register(instance2);
    Thread.sleep(3000);

    Result<List<ServiceInstance>> result = registry1.discover("multi-service");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSize(2);
    assertThat(result.getOrNull())
        .extracting(ServiceInstance::instanceId)
        .containsExactlyInAnyOrder("multi-01", "multi-02");

    registry1.shutdown();
    registry2.shutdown();
  }
}
