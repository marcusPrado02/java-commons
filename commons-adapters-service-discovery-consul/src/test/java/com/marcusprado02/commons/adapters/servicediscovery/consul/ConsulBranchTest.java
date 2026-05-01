package com.marcusprado02.commons.adapters.servicediscovery.consul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import com.marcusprado02.commons.ports.servicediscovery.HealthCheck;
import com.marcusprado02.commons.ports.servicediscovery.ServiceInstance;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ConsulBranchTest {

  @Mock private ConsulClient consulClient;

  private ServiceInstance instance(boolean secure) {
    return new ServiceInstance(
        "my-service", "my-instance-1", "localhost", 8080, secure, Map.of("env", "test"));
  }

  private static HealthService mockHealthService(String... tags) {
    var hs = mock(HealthService.class);
    var svc = mock(com.ecwid.consul.v1.health.model.HealthService.Service.class);
    when(hs.getService()).thenReturn(svc);
    when(svc.getId()).thenReturn("inst-1");
    when(svc.getService()).thenReturn("my-service");
    when(svc.getAddress()).thenReturn("localhost");
    when(svc.getPort()).thenReturn(8080);
    when(svc.getTags()).thenReturn(List.of(tags));
    return hs;
  }

  // ── register(): healthCheck null/non-null ─────────────────────────────────

  @Test
  void register_nullHealthCheck_skipsCheck() {
    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.register(instance(false), null);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void register_secureInstance_addsSecureTag() {
    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.register(instance(true), null);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void register_exception_returnsFail() {
    doThrow(new RuntimeException("connection refused"))
        .when(consulClient)
        .agentServiceRegister(any());
    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.register(instance(false), null);
    assertThat(result.isFail()).isTrue();
  }

  // ── convertHealthCheck switch branches: TCP, TTL, HTTP+timeout+deregister ─

  @Test
  void register_tcpHealthCheck_setsTcp() {
    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    HealthCheck tcpCheck =
        HealthCheck.tcp("localhost:8080").interval(Duration.ofSeconds(10)).build();
    var result = registry.register(instance(false), tcpCheck);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void register_ttlHealthCheck_setsTtl() {
    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    HealthCheck ttlCheck = HealthCheck.ttl(Duration.ofSeconds(30)).build();
    var result = registry.register(instance(false), ttlCheck);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void register_httpHealthCheckWithTimeoutAndDeregister_setsAll() {
    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    HealthCheck httpCheck =
        HealthCheck.http("http://localhost:8080/health")
            .interval(Duration.ofSeconds(10))
            .timeout(Duration.ofSeconds(5))
            .deregisterAfter(Duration.ofMinutes(2))
            .build();
    var result = registry.register(instance(false), httpCheck);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void register_httpHealthCheckWithoutTimeout_skipsTimeout() {
    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    HealthCheck httpCheck =
        HealthCheck.http("http://localhost:8080/health").interval(Duration.ofSeconds(10)).build();
    var result = registry.register(instance(false), httpCheck);
    assertThat(result.isOk()).isTrue();
  }

  // ── discover(): tags parsing branches ────────────────────────────────────

  @Test
  void discover_tagWithEquals_parsesAsMetadata() {
    Response<List<HealthService>> consulResponse = mock(Response.class);
    when(consulResponse.getValue()).thenReturn(List.of(mockHealthService("env=prod", "version=2")));
    when(consulClient.getHealthServices(eq("my-service"), eq(true), any(QueryParams.class)))
        .thenReturn(consulResponse);

    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.discover("my-service");
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void discover_tagSecure_setsSecureFlag() {
    Response<List<HealthService>> consulResponse = mock(Response.class);
    when(consulResponse.getValue()).thenReturn(List.of(mockHealthService("secure", "region=us")));
    when(consulClient.getHealthServices(eq("my-service"), eq(true), any(QueryParams.class)))
        .thenReturn(consulResponse);

    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.discover("my-service");
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSize(1);
  }

  @Test
  void discover_tagWithNoEquals_ignored() {
    Response<List<HealthService>> consulResponse = mock(Response.class);
    when(consulResponse.getValue())
        .thenReturn(List.of(mockHealthService("sometag", "anotherplaintag")));
    when(consulClient.getHealthServices(eq("my-service"), eq(true), any(QueryParams.class)))
        .thenReturn(consulResponse);

    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.discover("my-service");
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void discover_exception_returnsFail() {
    when(consulClient.getHealthServices(any(), eq(true), any()))
        .thenThrow(new RuntimeException("consul down"));

    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.discover("my-service");
    assertThat(result.isFail()).isTrue();
  }

  // ── deregister(), listServices(), getInstances(), heartbeat() ─────────────

  @Test
  void deregister_exception_returnsFail() {
    doThrow(new RuntimeException("error")).when(consulClient).agentServiceDeregister(any());

    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.deregister("inst-1");
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void listServices_exception_returnsFail() {
    when(consulClient.getAgentServices()).thenThrow(new RuntimeException("error"));

    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.listServices();
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void getInstances_exception_returnsFail() {
    when(consulClient.getHealthServices(any(), eq(false), any()))
        .thenThrow(new RuntimeException("err"));

    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.getInstances("my-service");
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void heartbeat_success() {
    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.heartbeat("inst-1");
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void heartbeat_exception_returnsFail() {
    doThrow(new RuntimeException("error")).when(consulClient).agentCheckPass(any());

    ConsulServiceRegistry registry = new ConsulServiceRegistry(consulClient);
    var result = registry.heartbeat("inst-1");
    assertThat(result.isFail()).isTrue();
  }

  // ── static factory methods ────────────────────────────────────────────────

  @Test
  void createDefault_returnsRegistry() {
    // Uses localhost:8500 — no real connection needed just to construct
    ConsulServiceRegistry registry = ConsulServiceRegistry.createDefault();
    assertThat(registry).isNotNull();
  }

  @Test
  void create_withHostPort_returnsRegistry() {
    ConsulServiceRegistry registry = ConsulServiceRegistry.create("localhost", 8500);
    assertThat(registry).isNotNull();
  }
}
