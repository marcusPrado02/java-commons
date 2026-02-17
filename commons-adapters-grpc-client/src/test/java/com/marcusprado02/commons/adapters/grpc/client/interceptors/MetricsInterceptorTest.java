package com.marcusprado02.commons.adapters.grpc.client.interceptors;

import io.grpc.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MetricsInterceptorTest {

  private MetricsInterceptor interceptor;
  private Channel channel;
  private ClientCall<String, String> mockCall;

  @SuppressWarnings({"unchecked", "rawtypes"})
  @BeforeEach
  void setUp() {
    interceptor = new MetricsInterceptor();
    channel = mock(Channel.class);
    mockCall = mock(ClientCall.class);

    when(channel.newCall(any(), any())).thenReturn((ClientCall) mockCall);
  }

  @Test
  void shouldTrackRequestCount() {
    MethodDescriptor<String, String> method = createTestMethodDescriptor("Method1");

    interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
    interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
    interceptor.interceptCall(method, CallOptions.DEFAULT, channel);

    var metrics = interceptor.getMethodMetrics("test.service/Method1");
    assertNotNull(metrics);
    assertEquals(3, metrics.totalCalls());
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldTrackSuccessCount() {
    MethodDescriptor<String, String> method = createTestMethodDescriptor("Method1");
    ArgumentCaptor<ClientCall.Listener<String>> listenerCaptor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);

    ClientCall<String, String> call =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);

    call.start(new ClientCall.Listener<String>() {}, new Metadata());

    verify(mockCall).start(listenerCaptor.capture(), any(Metadata.class));

    // Simulate successful completion
    listenerCaptor.getValue().onClose(Status.OK, new Metadata());

    var metrics = interceptor.getMethodMetrics("test.service/Method1");
    assertNotNull(metrics);
    assertEquals(1, metrics.successCalls());
    assertEquals(0, metrics.failureCalls());
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldTrackFailureCount() {
    MethodDescriptor<String, String> method = createTestMethodDescriptor("Method1");
    ArgumentCaptor<ClientCall.Listener<String>> listenerCaptor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);

    ClientCall<String, String> call =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);

    call.start(new ClientCall.Listener<String>() {}, new Metadata());

    verify(mockCall).start(listenerCaptor.capture(), any(Metadata.class));

    // Simulate failed completion
    listenerCaptor.getValue().onClose(Status.INTERNAL, new Metadata());

    var metrics = interceptor.getMethodMetrics("test.service/Method1");
    assertNotNull(metrics);
    assertEquals(0, metrics.successCalls());
    assertEquals(1, metrics.failureCalls());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  void shouldTrackFailuresByStatus() {
    MethodDescriptor<String, String> method = createTestMethodDescriptor("Method1");
    ArgumentCaptor<ClientCall.Listener<String>> listenerCaptor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);

    // First failure
    ClientCall<String, String> call1 =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
    call1.start(new ClientCall.Listener<String>() {}, new Metadata());
    verify(mockCall).start(listenerCaptor.capture(), any(Metadata.class));
    listenerCaptor.getValue().onClose(Status.INTERNAL, new Metadata());

    // Second failure with different status
    reset(mockCall);
    when(channel.newCall(any(), any())).thenReturn((ClientCall) mockCall);
    ClientCall<String, String> call2 =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
    call2.start(new ClientCall.Listener<String>() {}, new Metadata());
    verify(mockCall).start(listenerCaptor.capture(), any(Metadata.class));
    listenerCaptor.getValue().onClose(Status.NOT_FOUND, new Metadata());

    // Third failure with same status as first
    reset(mockCall);
    when(channel.newCall(any(), any())).thenReturn((ClientCall) mockCall);
    ClientCall<String, String> call3 =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
    call3.start(new ClientCall.Listener<String>() {}, new Metadata());
    verify(mockCall).start(listenerCaptor.capture(), any(Metadata.class));
    listenerCaptor.getValue().onClose(Status.INTERNAL, new Metadata());

    var metrics = interceptor.getMethodMetrics("test.service/Method1");
    assertNotNull(metrics);
    assertEquals(3, metrics.failureCalls());

    var failuresByStatus = metrics.failuresByStatus();
    assertEquals(2, failuresByStatus.get("INTERNAL"));
    assertEquals(1, failuresByStatus.get("NOT_FOUND"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldTrackDuration() throws InterruptedException {
    MethodDescriptor<String, String> method = createTestMethodDescriptor("Method1");
    ArgumentCaptor<ClientCall.Listener<String>> listenerCaptor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);

    ClientCall<String, String> call =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);

    call.start(new ClientCall.Listener<String>() {}, new Metadata());

    verify(mockCall).start(listenerCaptor.capture(), any(Metadata.class));

    // Simulate some delay
    Thread.sleep(50);

    // Simulate completion
    listenerCaptor.getValue().onClose(Status.OK, new Metadata());

    var metrics = interceptor.getMethodMetrics("test.service/Method1");
    assertNotNull(metrics);
    assertTrue(metrics.averageDurationMs() >= 50);
    assertTrue(metrics.totalDurationMs() >= 50);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  void shouldCalculateSuccessRate() {
    MethodDescriptor<String, String> method = createTestMethodDescriptor("Method1");
    ArgumentCaptor<ClientCall.Listener<String>> listenerCaptor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);

    // 3 successful calls
    for (int i = 0; i < 3; i++) {
      reset(mockCall);
      when(channel.newCall(any(), any())).thenReturn((ClientCall) mockCall);
      ClientCall<String, String> call =
          interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
      call.start(new ClientCall.Listener<String>() {}, new Metadata());
      verify(mockCall).start(listenerCaptor.capture(), any(Metadata.class));
      listenerCaptor.getValue().onClose(Status.OK, new Metadata());
    }

    // 1 failed call
    reset(mockCall);
    when(channel.newCall(any(), any())).thenReturn((ClientCall) mockCall);
    ClientCall<String, String> call =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
    call.start(new ClientCall.Listener<String>() {}, new Metadata());
    verify(mockCall).start(listenerCaptor.capture(), any(Metadata.class));
    listenerCaptor.getValue().onClose(Status.INTERNAL, new Metadata());

    var metrics = interceptor.getMethodMetrics("test.service/Method1");
    assertNotNull(metrics);
    assertEquals(4, metrics.totalCalls());
    assertEquals(3, metrics.successCalls());
    assertEquals(1, metrics.failureCalls());
    assertEquals(75.0, metrics.successRate(), 0.01);
  }

  @Test
  void shouldTrackMultipleMethods() {
    MethodDescriptor<String, String> method1 = createTestMethodDescriptor("Method1");
    MethodDescriptor<String, String> method2 = createTestMethodDescriptor("Method2");

    interceptor.interceptCall(method1, CallOptions.DEFAULT, channel);
    interceptor.interceptCall(method1, CallOptions.DEFAULT, channel);
    interceptor.interceptCall(method2, CallOptions.DEFAULT, channel);

    var allMetrics = interceptor.getAllMetrics();
    assertEquals(2, allMetrics.size());
    assertEquals(2, allMetrics.get("test.service/Method1").totalCalls());
    assertEquals(1, allMetrics.get("test.service/Method2").totalCalls());
  }

  @Test
  void shouldResetMetrics() {
    MethodDescriptor<String, String> method = createTestMethodDescriptor("Method1");

    interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
    interceptor.interceptCall(method, CallOptions.DEFAULT, channel);

    assertNotNull(interceptor.getMethodMetrics("test.service/Method1"));

    interceptor.reset();

    assertNull(interceptor.getMethodMetrics("test.service/Method1"));
    assertTrue(interceptor.getAllMetrics().isEmpty());
  }

  @Test
  void shouldReturnNullForNonExistentMethod() {
    assertNull(interceptor.getMethodMetrics("nonexistent.Method"));
  }

  @Test
  void shouldHandleZeroDivision() {
    MethodDescriptor<String, String> method = createTestMethodDescriptor("Method1");

    // Just create the call without completing it
    interceptor.interceptCall(method, CallOptions.DEFAULT, channel);

    // Get metrics before any completion (only requestCount is incremented)
    var metrics = interceptor.getMethodMetrics("test.service/Method1");
    assertNotNull(metrics);

    // Should handle division by zero gracefully
    assertEquals(0.0, metrics.averageDurationMs());
  }

  private MethodDescriptor<String, String> createTestMethodDescriptor(String methodName) {
    return MethodDescriptor.<String, String>newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName("test.service/" + methodName)
        .setRequestMarshaller(new StringMarshaller())
        .setResponseMarshaller(new StringMarshaller())
        .setSampledToLocalTracing(false)
        .build();
  }

  private static class StringMarshaller implements MethodDescriptor.Marshaller<String> {
    @Override
    public java.io.InputStream stream(String value) {
      return new java.io.ByteArrayInputStream(value.getBytes());
    }

    @Override
    public String parse(java.io.InputStream stream) {
      return new String(readAllBytes(stream));
    }

    private byte[] readAllBytes(java.io.InputStream stream) {
      try {
        return stream.readAllBytes();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
