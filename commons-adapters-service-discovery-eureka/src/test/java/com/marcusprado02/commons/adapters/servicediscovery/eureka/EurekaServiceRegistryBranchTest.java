package com.marcusprado02.commons.adapters.servicediscovery.eureka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockConstruction;

import com.netflix.discovery.DiscoveryClient;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class EurekaServiceRegistryBranchTest {

  // --- Builder.build(): appName set → covers false branch of "if (appName == null)" ---
  // --- MyDataCenterInstanceConfig: securePort=false → ternary false branches covered ---

  @Test
  void builder_withValidAppNameNonSecure_builds() {
    try (MockedConstruction<DiscoveryClient> ignored = mockConstruction(DiscoveryClient.class)) {
      EurekaServiceRegistry registry =
          EurekaServiceRegistry.builder()
              .appName("test-app")
              .eurekaServerUrl("http://localhost:8761/eureka")
              .hostName("localhost")
              .port(8080)
              .securePort(false)
              .build();
      assertThat(registry).isNotNull();
      registry.shutdown();
    }
  }

  // --- MyDataCenterInstanceConfig: securePort=true → ternary true branches covered ---

  @Test
  void builder_withValidAppNameSecure_builds() {
    try (MockedConstruction<DiscoveryClient> ignored = mockConstruction(DiscoveryClient.class)) {
      EurekaServiceRegistry registry =
          EurekaServiceRegistry.builder()
              .appName("test-app-secure")
              .eurekaServerUrl("http://localhost:8761/eureka")
              .hostName("localhost")
              .port(8443)
              .securePort(true)
              .build();
      assertThat(registry).isNotNull();
      registry.shutdown();
    }
  }
}
