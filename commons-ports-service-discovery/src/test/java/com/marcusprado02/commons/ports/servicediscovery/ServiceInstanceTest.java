package com.marcusprado02.commons.ports.servicediscovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ServiceInstanceTest {

  @Test
  void builder_withAllFields_shouldCreateInstance() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("payment-service")
            .instanceId("payment-01")
            .host("192.168.1.100")
            .port(8080)
            .secure(true)
            .metadata(Map.of("version", "1.2.0", "region", "us-east"))
            .build();

    assertThat(instance.serviceId()).isEqualTo("payment-service");
    assertThat(instance.instanceId()).isEqualTo("payment-01");
    assertThat(instance.host()).isEqualTo("192.168.1.100");
    assertThat(instance.port()).isEqualTo(8080);
    assertThat(instance.secure()).isTrue();
    assertThat(instance.metadata())
        .containsEntry("version", "1.2.0")
        .containsEntry("region", "us-east");
  }

  @Test
  void getUri_withSecureTrue_shouldReturnHttpsUri() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("test")
            .instanceId("test-01")
            .host("localhost")
            .port(8443)
            .secure(true)
            .build();

    assertThat(instance.getUri()).isEqualTo("https://localhost:8443");
  }

  @Test
  void getUri_withSecureFalse_shouldReturnHttpUri() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("test")
            .instanceId("test-01")
            .host("localhost")
            .port(8080)
            .secure(false)
            .build();

    assertThat(instance.getUri()).isEqualTo("http://localhost:8080");
  }

  @Test
  void addMetadata_shouldAddToMetadata() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("test")
            .instanceId("test-01")
            .host("localhost")
            .port(8080)
            .addMetadata("key1", "value1")
            .addMetadata("key2", "value2")
            .build();

    assertThat(instance.metadata()).containsEntry("key1", "value1").containsEntry("key2", "value2");
  }

  @Test
  void getMetadata_withExistingKey_shouldReturnValue() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("test")
            .instanceId("test-01")
            .host("localhost")
            .port(8080)
            .metadata(Map.of("version", "1.0.0"))
            .build();

    assertThat(instance.getMetadata("version")).isEqualTo("1.0.0");
  }

  @Test
  void getMetadata_withNonExistingKey_shouldReturnNull() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("test")
            .instanceId("test-01")
            .host("localhost")
            .port(8080)
            .build();

    assertThat(instance.getMetadata("nonexistent")).isNull();
  }

  @Test
  void hasMetadata_withExistingKey_shouldReturnTrue() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("test")
            .instanceId("test-01")
            .host("localhost")
            .port(8080)
            .metadata(Map.of("version", "1.0.0"))
            .build();

    assertThat(instance.hasMetadata("version")).isTrue();
  }

  @Test
  void hasMetadata_withNonExistingKey_shouldReturnFalse() {
    ServiceInstance instance =
        ServiceInstance.builder()
            .serviceId("test")
            .instanceId("test-01")
            .host("localhost")
            .port(8080)
            .build();

    assertThat(instance.hasMetadata("nonexistent")).isFalse();
  }

  @Test
  void constructor_withNullServiceId_shouldThrowException() {
    assertThatThrownBy(
            () -> new ServiceInstance(null, "instance-01", "localhost", 8080, false, Map.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("serviceId cannot be null");
  }

  @Test
  void constructor_withInvalidPort_shouldThrowException() {
    assertThatThrownBy(
            () -> new ServiceInstance("service", "instance-01", "localhost", 0, false, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("port must be between 1 and 65535");

    assertThatThrownBy(
            () ->
                new ServiceInstance("service", "instance-01", "localhost", 70000, false, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("port must be between 1 and 65535");
  }

  @Test
  void constructor_withNullMetadata_shouldUseEmptyMap() {
    ServiceInstance instance =
        new ServiceInstance("service", "instance-01", "localhost", 8080, false, null);

    assertThat(instance.metadata()).isEmpty();
  }
}
