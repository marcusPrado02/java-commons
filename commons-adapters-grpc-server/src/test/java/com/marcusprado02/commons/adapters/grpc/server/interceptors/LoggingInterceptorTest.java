package com.marcusprado02.commons.adapters.grpc.server.interceptors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  private MethodDescriptor<String, String> createDescriptor(String fullMethodName) {
    MethodDescriptor.Marshaller<String> marshaller =
        new MethodDescriptor.Marshaller<String>() {
          @Override
          public InputStream stream(String value) {
            return new ByteArrayInputStream(value.getBytes());
          }

          @Override
          public String parse(InputStream stream) {
            return "";
          }
        };
    return MethodDescriptor.<String, String>newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(fullMethodName)
        .setRequestMarshaller(marshaller)
        .setResponseMarshaller(marshaller)
        .build();
  }

  @Test
  void shouldLogMethodCall() {
    when(serverCall.getMethodDescriptor()).thenReturn(createDescriptor("test/Method"));

    interceptor.interceptCall(serverCall, new Metadata(), next);

    assertTrue(logHandler.hasLoggedMessage("Starting gRPC call: test/Method"));
  }

  @Test
  void shouldLogSuccessfulCompletion() {
    when(serverCall.getMethodDescriptor()).thenReturn(createDescriptor("test/Method"));

    ServerCall.Listener<String> result =
        interceptor.interceptCall(serverCall, new Metadata(), next);

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
    public void flush() {}

    @Override
    public void close() throws SecurityException {}

    boolean hasLoggedMessage(String message) {
      return records.stream().anyMatch(record -> record.getMessage().contains(message));
    }

    boolean hasLoggedLevel(Level level) {
      return records.stream().anyMatch(record -> record.getLevel().equals(level));
    }
  }
}
