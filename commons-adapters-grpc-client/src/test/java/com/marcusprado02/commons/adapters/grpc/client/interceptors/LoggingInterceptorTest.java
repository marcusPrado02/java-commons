package com.marcusprado02.commons.adapters.grpc.client.interceptors;

import io.grpc.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoggingInterceptorTest {

  private LoggingInterceptor interceptor;
  private TestLogHandler logHandler;
  private Channel channel;
  private ClientCall<String, String> mockCall;

  @BeforeEach
  void setUp() {
    interceptor = new LoggingInterceptor();

    // Setup log handler
    logHandler = new TestLogHandler();
    Logger logger = Logger.getLogger(LoggingInterceptor.class.getName());
    logger.addHandler(logHandler);
    logger.setLevel(Level.ALL);

    // Setup mocks
    channel = mock(Channel.class);
    mockCall = mock(ClientCall.class);

    when(channel.newCall(any(), any())).thenReturn(mockCall);
  }

  @Test
  void shouldLogCallStart() {
    MethodDescriptor<String, String> method = createTestMethodDescriptor();

    interceptor.interceptCall(method, CallOptions.DEFAULT, channel);

    // Verify start log
    assertTrue(logHandler.hasLog(Level.INFO, "gRPC client call started: test.service/TestMethod"));
  }

  @Test
  void shouldLogSuccessfulCall() {
    MethodDescriptor<String, String> method = createTestMethodDescriptor();
    ArgumentCaptor<ClientCall.Listener<String>> listenerCaptor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);

    ClientCall<String, String> call =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);

    call.start(new ClientCall.Listener<String>() {}, new Metadata());

    verify(mockCall).start(listenerCaptor.capture(), any(Metadata.class));

    // Simulate successful completion
    listenerCaptor.getValue().onClose(Status.OK, new Metadata());

    // Verify success log
    assertTrue(logHandler.hasLogContaining(Level.INFO, "gRPC client call completed: test.service/TestMethod"));
    assertTrue(logHandler.hasLogContaining(Level.INFO, "duration:"));
  }

  @Test
  void shouldLogFailedCall() {
    MethodDescriptor<String, String> method = createTestMethodDescriptor();
    ArgumentCaptor<ClientCall.Listener<String>> listenerCaptor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);

    ClientCall<String, String> call =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);

    call.start(new ClientCall.Listener<String>() {}, new Metadata());

    verify(mockCall).start(listenerCaptor.capture(), any(Metadata.class));

    // Simulate failed completion
    Status errorStatus = Status.INTERNAL.withDescription("Test error");
    listenerCaptor.getValue().onClose(errorStatus, new Metadata());

    // Verify failure log
    assertTrue(logHandler.hasLogContaining(Level.WARNING, "gRPC client call failed: test.service/TestMethod"));
    assertTrue(logHandler.hasLogContaining(Level.WARNING, "status: INTERNAL"));
    assertTrue(logHandler.hasLogContaining(Level.WARNING, "Test error"));
  }

  private MethodDescriptor<String, String> createTestMethodDescriptor() {
    return MethodDescriptor.<String, String>newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName("test.service/TestMethod")
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

  private static class TestLogHandler extends Handler {
    private final java.util.List<LogRecord> records = new java.util.ArrayList<>();

    @Override
    public void publish(LogRecord record) {
      records.add(record);
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}

    public boolean hasLog(Level level, String message) {
      return records.stream()
          .anyMatch(r -> r.getLevel().equals(level) && r.getMessage().equals(message));
    }

    public boolean hasLogContaining(Level level, String substring) {
      return records.stream()
          .anyMatch(
              r -> r.getLevel().equals(level) && r.getMessage().contains(substring));
    }
  }
}
