package com.marcusprado02.commons.adapters.servicediscovery.consul;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecwid.consul.v1.ConsulClient;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.servicediscovery.HealthCheck;
import com.marcusprado02.commons.ports.servicediscovery.ServiceInstance;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ConsulServiceRegistryTest {

  @Container
  static ConsulContainer consulContainer =
      new ConsulContainer(DockerImageName.parse("consul:1.15.4")).withExposedPorts(8500);

  private ConsulServiceRegistry registry;

  @BeforeEach
  void setUp() {
    String host = consulContainer.getHost();
    int port = consulContainer.getMappedPort(8500);
    ConsulClient consulClient = new ConsulClient(host, port);
    registry = new ConsulServiceRegistry(consulClient);
  }

  @Test
  void register_withValidInstance_shouldSucceed() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("test-service")
            .instanceId("test-01")
            .host("localhost")
            .port(8080)
            .build();

    Result<Void> result = registry.register(instance);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void register_withHealthCheck_shouldSucceed() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("test-service-http")
            .instanceId("test-http-01")
            .host("localhost")
            .port(8080)
            .build();

    HealthCheck healthCheck =
        HealthCheck.http("http://localhost:8080/health")
            .interval(Duration.ofSeconds(10))
            .timeout(Duration.ofSeconds(5))
            .build();

    Result<Void> result = registry.register(instance, healthCheck);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void discover_afterRegistration_shouldFindInstance() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("payment-service")
            .instanceId("payment-01")
            .host("192.168.1.100")
            .port(8080)
            .secure(true)
            .addMetadata("version", "1.0.0")
            .build();

    registry.register(instance);

    Result<List<ServiceInstance>> result = registry.discover("payment-service");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSize(1);

    ServiceInstance found = result.getOrNull().get(0);
    assertThat(found.serviceId()).isEqualTo("payment-service");
    assertThat(found.instanceId()).isEqualTo("payment-01");
    assertThat(found.host()).isEqualTo("192.168.1.100");
    assertThat(found.port()).isEqualTo(8080);
    assertThat(found.secure()).isTrue();
    assertThat(found.metadata()).containsEntry("version", "1.0.0");
  }

  @Test
  void discover_withNonExistentService_shouldReturnEmptyList() {
    Result<List<ServiceInstance>> result = registry.discover("nonexistent-service");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }

  @Test
  void deregister_afterRegistration_shouldRemoveInstance() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("temp-service")
            .instanceId("temp-01")
            .host("localhost")
            .port(9090)
            .build();

    registry.register(instance);
    Result<List<ServiceInstance>> beforeDeregister = registry.discover("temp-service");
    assertThat(beforeDeregister.getOrNull()).hasSize(1);

    Result<Void> deregisterResult = registry.deregister("temp-01");
    assertThat(deregisterResult.isOk()).isTrue();

    Result<List<ServiceInstance>> afterDeregister = registry.discover("temp-service");
    assertThat(afterDeregister.getOrNull()).isEmpty();
  }

  @Test
  void listServices_afterMultipleRegistrations_shouldReturnAllServices() {
    ServiceInstance instance1 =
        ServiceInstance.builder()
            .serviceId("service-a")
            .instanceId("a-01")
            .host("localhost")
            .port(8001)
            .build();

    ServiceInstance instance2 =
        ServiceInstance.builder()
            .serviceId("service-b")
            .instanceId("b-01")
            .host("localhost")
            .port(8002)
            .build();

    registry.register(instance1);
    registry.register(instance2);

    Result<List<String>> result = registry.listServices();

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("service-a", "service-b");
  }

  @Test
  void getInstances_shouldReturnAllInstancesIncludingUnhealthy() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("all-instances-test")
            .instanceId("all-01")
            .host("localhost")
            .port(8100)
            .build();

    registry.register(instance);

    Result<List<ServiceInstance>> result = registry.getInstances("all-instances-test");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSize(1);
  }

  @Test
  void registerMultipleInstancesOfSameService_shouldAllBeDiscoverable() {
    ServiceInstance instance1 =
        ServiceInstance.builder()
            .serviceId("multi-instance-service")
            .instanceId("multi-01")
            .host("192.168.1.10")
            .port(8080)
            .build();

    ServiceInstance instance2 =
        ServiceInstance.builder()
            .serviceId("multi-instance-service")
            .instanceId("multi-02")
            .host("192.168.1.11")
            .port(8080)
            .build();

    registry.register(instance1);
    registry.register(instance2);

    Result<List<ServiceInstance>> result = registry.discover("multi-instance-service");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSize(2);
    assertThat(result.getOrNull())
        .extracting(ServiceInstance::instanceId)
        .containsExactlyInAnyOrder("multi-01", "multi-02");
  }

  @Test
  void updateHealthCheck_shouldReturnNotSupported() {
    HealthCheck newCheck = HealthCheck.tcp("localhost:9000").build();

    Result<Void> result = registry.updateHealthCheck("some-instance", newCheck);

    assertThat(result.isOk()).isFalse();
    assertThat(result.problemOrNull()).isNotNull();
    assertThat(result.problemOrNull().code().value()).isEqualTo("OPERATION_NOT_SUPPORTED");
  }
}
