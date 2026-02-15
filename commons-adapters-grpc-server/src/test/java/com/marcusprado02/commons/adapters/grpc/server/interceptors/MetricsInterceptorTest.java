package com.marcusprado02.commons.adapters.grpc.server.interceptors;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MetricsInterceptorTest {

  private MetricsInterceptor interceptor;
  private ServerCall<String, String> serverCall;
  private ServerCallHandler<String, String> next;
  private ServerCall.Listener<String> listener;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    interceptor = new MetricsInterceptor();
    serverCall = mock(ServerCall.class);
    next = mock(ServerCallHandler.class);
    listener = mock(ServerCall.Listener.class);

    when(next.startCall(any(), any())).thenReturn(listener);
  }

  @Test
  void shouldCollectBasicMetrics() {
    when(serverCall.getMethodDescriptor()).thenReturn(
        io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            "test/Method",
            null,
            null
        )
    );

    interceptor.intercept(serverCall, new Metadata(), next);

    MetricsInterceptor.MethodMetrics metrics = interceptor.getMethodMetrics("test/Method");
    assertNotNull(metrics);
    assertEquals(1, metrics.totalCalls());
  }

  @Test
  void shouldTrackSuccessfulCalls() {
    when(serverCall.getMethodDescriptor()).thenReturn(
        io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            "test/Method",
            null,
            null
        )
    );

    interceptor.intercept(serverCall, new Metadata(), next);

    ArgumentCaptor<ServerCall> callCaptor = ArgumentCaptor.forClass(ServerCall.class);
    verify(next).startCall(callCaptor.capture(), any());

    ServerCall<String, String> wrappedCall = callCaptor.getValue();
    wrappedCall.close(Status.OK, new Metadata());

    MetricsInterceptor.MethodMetrics metrics = interceptor.getMethodMetrics("test/Method");
    assertEquals(1, metrics.successCalls());
    assertEquals(0, metrics.failureCalls());
    assertEquals(100.0, metrics.successRate());
  }

  @Test
  void shouldTrackFailedCalls() {
    when(serverCall.getMethodDescriptor()).thenReturn(
        io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            "test/Method",
            null,
            null
        )
    );

    interceptor.intercept(serverCall, new Metadata(), next);

    ArgumentCaptor<ServerCall> callCaptor = ArgumentCaptor.forClass(ServerCall.class);
    verify(next).startCall(callCaptor.capture(), any());

    ServerCall<String, String> wrappedCall = callCaptor.getValue();
    wrappedCall.close(Status.NOT_FOUND, new Metadata());

    MetricsInterceptor.MethodMetrics metrics = interceptor.getMethodMetrics("test/Method");
    assertEquals(0, metrics.successCalls());
    assertEquals(1, metrics.failureCalls());
    assertEquals(0.0, metrics.successRate());
  }

  @Test
  void shouldTrackMultipleCalls() {
    when(serverCall.getMethodDescriptor()).thenReturn(
        io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            "test/Method",
            null,
            null
        )
    );

    // Simulate 3 successful calls and 2 failed calls
    for (int i = 0; i < 3; i++) {
      interceptor.intercept(serverCall, new Metadata(), next);
      ArgumentCaptor<ServerCall> callCaptor = ArgumentCaptor.forClass(ServerCall.class);
      verify(next, times(i + 1)).startCall(callCaptor.capture(), any());
      callCaptor.getValue().close(Status.OK, new Metadata());
    }

    for (int i = 0; i < 2; i++) {
      interceptor.intercept(serverCall, new Metadata(), next);
      ArgumentCaptor<ServerCall> callCaptor = ArgumentCaptor.forClass(ServerCall.class);
      verify(next, times(3 + i + 1)).startCall(callCaptor.capture(), any());
      callCaptor.getValue().close(Status.INVALID_ARGUMENT, new Metadata());
    }

    MetricsInterceptor.MethodMetrics metrics = interceptor.getMethodMetrics("test/Method");
    assertEquals(5, metrics.totalCalls());
    assertEquals(3, metrics.successCalls());
    assertEquals(2, metrics.failureCalls());
    assertEquals(60.0, metrics.successRate());
  }

  @Test
  void shouldResetMetrics() {
    when(serverCall.getMethodDescriptor()).thenReturn(
        io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            "test/Method",
            null,
            null
        )
    );

    interceptor.intercept(serverCall, new Metadata(), next);
    assertNotNull(interceptor.getMethodMetrics("test/Method"));

    interceptor.reset();
    assertNull(interceptor.getMethodMetrics("test/Method"));
  }

  @Test
  void shouldReturnAllMetrics() {
    when(serverCall.getMethodDescriptor()).thenReturn(
        io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            "test/Method1",
            null,
            null
        )
    );
    interceptor.intercept(serverCall, new Metadata(), next);

    when(serverCall.getMethodDescriptor()).thenReturn(
        io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            "test/Method2",
            null,
            null
        )
    );
    interceptor.intercept(serverCall, new Metadata(), next);

    assertEquals(2, interceptor.getAllMetrics().size());
    assertTrue(interceptor.getAllMetrics().containsKey("test/Method1"));
    assertTrue(interceptor.getAllMetrics().containsKey("test/Method2"));
  }

  @Test
  void shouldCalculateAverageDuration() {
    when(serverCall.getMethodDescriptor()).thenReturn(
        io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            "test/Method",
            null,
            null
        )
    );

    interceptor.intercept(serverCall, new Metadata(), next);

    MetricsInterceptor.MethodMetrics metrics = interceptor.getMethodMetrics("test/Method");
    assertTrue(metrics.averageDurationMs() >= 0.0);
  }
}
