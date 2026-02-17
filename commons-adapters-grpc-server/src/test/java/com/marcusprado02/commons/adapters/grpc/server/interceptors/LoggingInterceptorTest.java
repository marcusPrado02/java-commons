package com.marcusprado02.commons.adapters.grpc.server.interceptors;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoggingInterceptorTest {

  private LoggingInterceptor interceptor;
  private ServerCall<String, String> serverCall;
  private ServerCallHandler<String, String> next;
  private ServerCall.Listener<String> listener;
  private TestLogHandler logHandler;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    interceptor = new LoggingInterceptor();
    serverCall = mock(ServerCall.class);
    next = mock(ServerCallHandler.class);
    listener = mock(ServerCall.Listener.class);

    when(next.startCall(any(), any())).thenReturn(listener);

    // Setup log capture
    logHandler = new TestLogHandler();
    Logger logger = Logger.getLogger(LoggingInterceptor.class.getName());
    logger.addHandler(logHandler);
    logger.setLevel(Level.ALL);
  }

  @Test
  void shouldLogMethodCall() {
    when(serverCall.getMethodDescriptor()).thenReturn(
        io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            "test/Method",
            null,
            null
        )
    );

    interceptor.interceptCall(serverCall, new Metadata(), next);

    assertTrue(logHandler.hasLoggedMessage("Starting gRPC call: test/Method"));
  }

  @Test
  void shouldLogSuccessfulCompletion() {
    when(serverCall.getMethodDescriptor()).thenReturn(
        io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            "test/Method",
            null,
            null
        )
    );

    ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, new Metadata(), next);

    verify(next).startCall(any(), any());
    assertNotNull(result);
  }

  private static class TestLogHandler extends Handler {
    private final java.util.List<LogRecord> records = new java.util.ArrayList<>();

    @Override
    public void publish(LogRecord record) {
      records.add(record);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

    boolean hasLoggedMessage(String message) {
      return records.stream()
          .anyMatch(record -> record.getMessage().contains(message));
    }

    boolean hasLoggedLevel(Level level) {
      return records.stream()
          .anyMatch(record -> record.getLevel().equals(level));
    }
  }
}
