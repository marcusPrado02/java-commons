package com.marcusprado02.commons.adapters.servicediscovery.eureka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.servicediscovery.HealthCheck;
import com.marcusprado02.commons.ports.servicediscovery.ServiceInstance;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EurekaServiceRegistryTest {

  @Mock private EurekaClient eurekaClient;
  @Mock private ApplicationInfoManager applicationInfoManager;
  @Mock private InstanceInfo instanceInfo;

  private EurekaServiceRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new EurekaServiceRegistry(eurekaClient, applicationInfoManager, true);
  }

  // ── register ──────────────────────────────────────────────────────────────

  @Test
  void register_shouldSetInstanceStatusToUpAndReturnOk() {
    ServiceInstance instance = sampleInstance("svc", "svc-01", 8080);

    Result<Void> result = registry.register(instance);

    assertThat(result.isOk()).isTrue();
    verify(applicationInfoManager).setInstanceStatus(InstanceInfo.InstanceStatus.UP);
  }

  @Test
  void registerWithHealthCheck_shouldSucceed() {
    ServiceInstance instance = sampleInstance("svc", "svc-01", 8080);
    HealthCheck check = HealthCheck.http("http://localhost:8080/health").build();

    Result<Void> result = registry.register(instance, check);

    assertThat(result.isOk()).isTrue();
    verify(applicationInfoManager).setInstanceStatus(InstanceInfo.InstanceStatus.UP);
  }

  @Test
  void registerWithTtlHealthCheck_shouldSucceed() {
    ServiceInstance instance = sampleInstance("svc", "svc-01", 8080);
    HealthCheck check = HealthCheck.ttl(java.time.Duration.ofSeconds(30)).build();

    Result<Void> result = registry.register(instance, check);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void register_whenExceptionThrown_shouldReturnFail() {
    ServiceInstance instance = sampleInstance("svc", "svc-01", 8080);
    doThrow(new RuntimeException("boom")).when(applicationInfoManager).setInstanceStatus(any());

    Result<Void> result = registry.register(instance);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("REGISTRATION_FAILED");
  }

  // ── deregister ────────────────────────────────────────────────────────────

  @Test
  void deregister_whenOwnsClient_shouldSetDownAndShutdown() {
    Result<Void> result = registry.deregister("svc-01");

    assertThat(result.isOk()).isTrue();
    verify(applicationInfoManager).setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
    verify(eurekaClient).shutdown();
  }

  @Test
  void deregister_whenNotOwnsClient_shouldNotShutdownClient() {
    EurekaServiceRegistry nonOwning =
        new EurekaServiceRegistry(eurekaClient, applicationInfoManager, false);

    Result<Void> result = nonOwning.deregister("svc-01");

    assertThat(result.isOk()).isTrue();
    verify(eurekaClient, never()).shutdown();
  }

  @Test
  void deregister_whenExceptionThrown_shouldReturnFail() {
    doThrow(new RuntimeException("boom")).when(applicationInfoManager).setInstanceStatus(any());

    Result<Void> result = registry.deregister("svc-01");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("DEREGISTRATION_FAILED");
  }

  // ── discover ──────────────────────────────────────────────────────────────

  @Test
  void discover_whenApplicationNotFound_shouldReturnEmptyList() {
    when(eurekaClient.getApplication("SVC")).thenReturn(null);

    Result<List<ServiceInstance>> result = registry.discover("svc");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }

  @Test
  void discover_shouldReturnOnlyUpInstances() {
    // UP instance: full stub needed because convertFromInstanceInfo is called
    InstanceInfo upInstance = mockInstanceInfo("svc", "svc-01", "localhost", 8080, false);
    when(upInstance.getStatus()).thenReturn(InstanceInfo.InstanceStatus.UP);

    // DOWN instance: only status stub needed; convertFromInstanceInfo is never reached
    InstanceInfo downInstance = mock(InstanceInfo.class);
    when(downInstance.getStatus()).thenReturn(InstanceInfo.InstanceStatus.DOWN);

    Application app = mock(Application.class);
    when(app.getInstances()).thenReturn(List.of(upInstance, downInstance));
    when(eurekaClient.getApplication("SVC")).thenReturn(app);

    Result<List<ServiceInstance>> result = registry.discover("svc");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSize(1);
    assertThat(result.getOrNull().get(0).instanceId()).isEqualTo("svc-01");
  }

  @Test
  void discover_whenExceptionThrown_shouldReturnFail() {
    when(eurekaClient.getApplication(anyString())).thenThrow(new RuntimeException("boom"));

    Result<List<ServiceInstance>> result = registry.discover("svc");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("DISCOVERY_FAILED");
  }

  // ── listServices ──────────────────────────────────────────────────────────

  @Test
  void listServices_shouldReturnLowercaseServiceNames() {
    Application app1 = mock(Application.class);
    when(app1.getName()).thenReturn("SERVICE-A");
    Application app2 = mock(Application.class);
    when(app2.getName()).thenReturn("SERVICE-B");

    Applications apps = mock(Applications.class);
    when(apps.getRegisteredApplications()).thenReturn(List.of(app1, app2));
    when(eurekaClient.getApplications()).thenReturn(apps);

    Result<List<String>> result = registry.listServices();

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).containsExactlyInAnyOrder("service-a", "service-b");
  }

  @Test
  void listServices_whenExceptionThrown_shouldReturnFail() {
    when(eurekaClient.getApplications()).thenThrow(new RuntimeException("boom"));

    Result<List<String>> result = registry.listServices();

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("LIST_SERVICES_FAILED");
  }

  // ── getInstances ──────────────────────────────────────────────────────────

  @Test
  void getInstances_whenApplicationNotFound_shouldReturnEmptyList() {
    when(eurekaClient.getApplication("SVC")).thenReturn(null);

    Result<List<ServiceInstance>> result = registry.getInstances("svc");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }

  @Test
  void getInstances_shouldReturnAllInstancesRegardlessOfStatus() {
    // getInstances does not filter by status — no getStatus() stub needed
    InstanceInfo up = mockInstanceInfo("svc", "svc-01", "localhost", 8080, false);
    InstanceInfo down = mockInstanceInfo("svc", "svc-02", "localhost", 8081, false);

    Application app = mock(Application.class);
    when(app.getInstances()).thenReturn(List.of(up, down));
    when(eurekaClient.getApplication("SVC")).thenReturn(app);

    Result<List<ServiceInstance>> result = registry.getInstances("svc");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSize(2);
  }

  @Test
  void getInstances_whenExceptionThrown_shouldReturnFail() {
    when(eurekaClient.getApplication(anyString())).thenThrow(new RuntimeException("boom"));

    Result<List<ServiceInstance>> result = registry.getInstances("svc");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("GET_INSTANCES_FAILED");
  }

  // ── updateHealthCheck ─────────────────────────────────────────────────────

  @Test
  void updateHealthCheck_shouldAlwaysReturnOperationNotSupported() {
    HealthCheck check = HealthCheck.tcp("localhost:8080").build();

    Result<Void> result = registry.updateHealthCheck("svc-01", check);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("OPERATION_NOT_SUPPORTED");
  }

  // ── heartbeat ─────────────────────────────────────────────────────────────

  @Test
  void heartbeat_whenInstanceIsUp_shouldReturnOk() {
    when(applicationInfoManager.getInfo()).thenReturn(instanceInfo);
    when(instanceInfo.getStatus()).thenReturn(InstanceInfo.InstanceStatus.UP);

    Result<Void> result = registry.heartbeat("svc-01");

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void heartbeat_whenInstanceIsNotUp_shouldReturnFail() {
    when(applicationInfoManager.getInfo()).thenReturn(instanceInfo);
    when(instanceInfo.getStatus()).thenReturn(InstanceInfo.InstanceStatus.DOWN);

    Result<Void> result = registry.heartbeat("svc-01");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("HEARTBEAT_FAILED");
  }

  @Test
  void heartbeat_whenExceptionThrown_shouldReturnFail() {
    when(applicationInfoManager.getInfo()).thenThrow(new RuntimeException("boom"));

    Result<Void> result = registry.heartbeat("svc-01");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("HEARTBEAT_FAILED");
  }

  // ── shutdown ──────────────────────────────────────────────────────────────

  @Test
  void shutdown_whenOwnsClient_shouldShutdownEurekaClient() {
    registry.shutdown();

    verify(eurekaClient).shutdown();
  }

  @Test
  void shutdown_whenNotOwnsClient_shouldNotShutdownEurekaClient() {
    EurekaServiceRegistry nonOwning =
        new EurekaServiceRegistry(eurekaClient, applicationInfoManager, false);

    nonOwning.shutdown();

    verify(eurekaClient, never()).shutdown();
  }

  // ── builder ───────────────────────────────────────────────────────────────

  @Test
  void builder_withNullAppName_shouldThrow() {
    assertThatThrownBy(() -> EurekaServiceRegistry.builder().build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("appName must be set");
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private ServiceInstance sampleInstance(String serviceId, String instanceId, int port) {
    return ServiceInstance.builder()
        .serviceId(serviceId)
        .instanceId(instanceId)
        .host("localhost")
        .port(port)
        .build();
  }

  private InstanceInfo mockInstanceInfo(
      String appName, String instanceId, String host, int port, boolean secure) {
    InstanceInfo info = mock(InstanceInfo.class);
    when(info.getAppName()).thenReturn(appName.toUpperCase());
    when(info.getInstanceId()).thenReturn(instanceId);
    when(info.getHostName()).thenReturn(host);
    when(info.getPort()).thenReturn(port);
    when(info.isPortEnabled(InstanceInfo.PortType.SECURE)).thenReturn(secure);
    when(info.getMetadata()).thenReturn(new java.util.HashMap<>());
    return info;
  }
}
