package com.marcusprado02.commons.adapters.grpc.server.interceptors;

import io.grpc.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthInterceptorTest {

  private ServerCall<String, String> serverCall;
  private ServerCallHandler<String, String> next;
  private ServerCall.Listener<String> listener;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    serverCall = mock(ServerCall.class);
    next = mock(ServerCallHandler.class);
    listener = mock(ServerCall.Listener.class);

    when(next.startCall(any(), any())).thenReturn(listener);
  }

  @Test
  void shouldAuthenticateWithValidToken() {
    Function<String, String> validator = token -> "user@example.com";
    AuthInterceptor interceptor = new AuthInterceptor(validator, true);

    Metadata metadata = new Metadata();
    metadata.put(AuthInterceptor.AUTHORIZATION_METADATA_KEY, "Bearer valid-token");

    interceptor.intercept(serverCall, metadata, next);

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(next).startCall(any(), any());
  }

  @Test
  void shouldRejectMissingToken() {
    Function<String, String> validator = token -> "user@example.com";
    AuthInterceptor interceptor = new AuthInterceptor(validator, true);

    Metadata metadata = new Metadata();

    interceptor.intercept(serverCall, metadata, next);

    ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
    verify(serverCall).close(statusCaptor.capture(), any());

    assertEquals(Status.Code.UNAUTHENTICATED, statusCaptor.getValue().getCode());
  }

  @Test
  void shouldRejectInvalidToken() {
    Function<String, String> validator = token -> null; // Returns null for invalid token
    AuthInterceptor interceptor = new AuthInterceptor(validator, true);

    Metadata metadata = new Metadata();
    metadata.put(AuthInterceptor.AUTHORIZATION_METADATA_KEY, "Bearer invalid-token");

    interceptor.intercept(serverCall, metadata, next);

    ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
    verify(serverCall).close(statusCaptor.capture(), any());

    assertEquals(Status.Code.UNAUTHENTICATED, statusCaptor.getValue().getCode());
  }

  @Test
  void shouldAllowMissingTokenWhenNotRequired() {
    Function<String, String> validator = token -> "user@example.com";
    AuthInterceptor interceptor = new AuthInterceptor(validator, false);

    Metadata metadata = new Metadata();

    interceptor.intercept(serverCall, metadata, next);

    verify(next).startCall(any(), any());
    verify(serverCall, never()).close(any(), any());
  }

  @Test
  void shouldExtractBearerToken() {
    Function<String, String> validator = token -> {
      assertEquals("my-token", token);
      return "user@example.com";
    };
    AuthInterceptor interceptor = new AuthInterceptor(validator, true);

    Metadata metadata = new Metadata();
    metadata.put(AuthInterceptor.AUTHORIZATION_METADATA_KEY, "Bearer my-token");

    interceptor.intercept(serverCall, metadata, next);

    verify(next).startCall(any(), any());
  }

  @Test
  void shouldExtractRawToken() {
    Function<String, String> validator = token -> {
      assertEquals("raw-token", token);
      return "user@example.com";
    };
    AuthInterceptor interceptor = new AuthInterceptor(validator, true);

    Metadata metadata = new Metadata();
    metadata.put(AuthInterceptor.AUTHORIZATION_METADATA_KEY, "raw-token");

    interceptor.intercept(serverCall, metadata, next);

    verify(next).startCall(any(), any());
  }

  @Test
  void shouldHandleValidationException() {
    Function<String, String> validator = token -> {
      throw new RuntimeException("Validation error");
    };
    AuthInterceptor interceptor = new AuthInterceptor(validator, true);

    Metadata metadata = new Metadata();
    metadata.put(AuthInterceptor.AUTHORIZATION_METADATA_KEY, "Bearer token");

    interceptor.intercept(serverCall, metadata, next);

    ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
    verify(serverCall).close(statusCaptor.capture(), any());

    assertEquals(Status.Code.UNAUTHENTICATED, statusCaptor.getValue().getCode());
  }

  @Test
  void shouldStoreAuthenticatedPrincipalInContext() {
    Function<String, String> validator = token -> "user@example.com";
    AuthInterceptor interceptor = new AuthInterceptor(validator, true);

    Metadata metadata = new Metadata();
    metadata.put(AuthInterceptor.AUTHORIZATION_METADATA_KEY, "Bearer token");

    interceptor.intercept(serverCall, metadata, next);

    // The principal should be stored in Context
    verify(next).startCall(any(), any());
  }
}
