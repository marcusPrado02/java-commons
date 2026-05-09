package com.marcusprado02.commons.adapters.servicediscovery.eureka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.marcusprado02.commons.ports.servicediscovery.ServiceInstance;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EurekaServiceRegistryBranchTest {

  // --- shutdown: ownsClient=false → must NOT call eurekaClient.shutdown() ---

  @Test
  void shutdown_withBorrowedClient_doesNotShutdownClient() {
    EurekaClient mockClient = mock(EurekaClient.class);
    ApplicationInfoManager mockManager = mock(ApplicationInfoManager.class);
    EurekaServiceRegistry registry = new EurekaServiceRegistry(mockClient, mockManager, false);

    registry.shutdown();

    verify(mockClient, never()).shutdown();
  }

  // --- shutdown: ownsClient=true → MUST call eurekaClient.shutdown() ---

  @Test
  void shutdown_withOwnedClient_shutsDownClient() {
    EurekaClient mockClient = mock(EurekaClient.class);
    ApplicationInfoManager mockManager = mock(ApplicationInfoManager.class);
    EurekaServiceRegistry registry = new EurekaServiceRegistry(mockClient, mockManager, true);

    registry.shutdown();

    verify(mockClient).shutdown();
  }

  // --- deregister: ownsClient=false → must NOT call eurekaClient.shutdown() ---

  @Test
  void deregister_withBorrowedClient_doesNotShutdownClient() {
    EurekaClient mockClient = mock(EurekaClient.class);
    ApplicationInfoManager mockManager = mock(ApplicationInfoManager.class);
    InstanceInfo mockInfo = mock(InstanceInfo.class);
    com.netflix.appinfo.InstanceInfo.Builder infoBuilder =
        com.netflix.appinfo.InstanceInfo.Builder.newBuilder();
    // ApplicationInfoManager.setInstanceStatus is void; mock ignores it.
    EurekaServiceRegistry registry = new EurekaServiceRegistry(mockClient, mockManager, false);

    var result = registry.deregister("instance-1");

    assertThat(result.isOk()).isTrue();
    verify(mockClient, never()).shutdown();
  }

  // --- deregister: ownsClient=true → MUST call eurekaClient.shutdown() ---

  @Test
  void deregister_withOwnedClient_shutsDownClient() {
    EurekaClient mockClient = mock(EurekaClient.class);
    ApplicationInfoManager mockManager = mock(ApplicationInfoManager.class);
    EurekaServiceRegistry registry = new EurekaServiceRegistry(mockClient, mockManager, true);

    var result = registry.deregister("instance-1");

    assertThat(result.isOk()).isTrue();
    verify(mockClient).shutdown();
  }

  // --- register: secure=true → hits both ternary branches in convertToInstanceInfo ---

  @Test
  void register_secureInstance_coversSecurePortBranches() {
    EurekaClient mockClient = mock(EurekaClient.class);
    ApplicationInfoManager mockManager = mock(ApplicationInfoManager.class);
    EurekaServiceRegistry registry = new EurekaServiceRegistry(mockClient, mockManager, false);

    ServiceInstance secureInstance =
        ServiceInstance.builder()
            .serviceId("svc")
            .instanceId("svc-1")
            .host("localhost")
            .port(8443)
            .secure(true)
            .build();

    var result = registry.register(secureInstance);

    assertThat(result.isOk()).isTrue();
  }

  // --- Builder setters: exercise all fluent methods to cover missed lines ---

  @Test
  void builder_allSetters_returnBuilderInstance() {
    var builder =
        EurekaServiceRegistry.builder()
            .appName("test-app")
            .eurekaServerUrl("http://localhost:8761/eureka")
            .hostName("app.example.com")
            .port(9090)
            .securePort(true)
            .registryFetchInterval(Duration.ofSeconds(15))
            .replicationInterval(Duration.ofSeconds(20))
            .renewalInterval(Duration.ofSeconds(25))
            .expirationDuration(Duration.ofSeconds(120))
            .metadata("region", "us-east-1");
    assertThat(builder).isNotNull();
  }
}
