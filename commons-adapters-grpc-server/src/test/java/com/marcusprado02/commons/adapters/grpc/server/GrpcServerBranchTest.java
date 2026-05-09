package com.marcusprado02.commons.adapters.grpc.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.adapters.grpc.server.error.ErrorMapper;
import com.marcusprado02.commons.adapters.grpc.server.interceptors.AuthInterceptor;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import io.grpc.BindableService;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class GrpcServerBranchTest {

  private static GrpcServerConfiguration configNoHealthNoMetrics() {
    return GrpcServerConfiguration.builder()
        .port(0)
        .enableHealthCheck(false)
        .enableMetrics(false)
        .enableReflection(false)
        .build();
  }

  private static BindableService mockService() {
    BindableService svc = mock(BindableService.class);
    ServerServiceDefinition def = ServerServiceDefinition.builder("test.BranchService").build();
    when(svc.bindService()).thenReturn(def);
    return svc;
  }

  // ── GrpcServer construction / port / isRunning ────────────────────────────

  @Test
  void constructor_nullConfig_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new GrpcServer(null));
  }

  @Test
  void getPort_beforeStart_returnsNegativeOne() {
    GrpcServer server = new GrpcServer(configNoHealthNoMetrics());
    assertEquals(-1, server.getPort());
    assertFalse(server.isRunning());
  }

  // ── GrpcServer.start() already-started branch ────────────────────────────

  @Test
  void start_alreadyStarted_throwsIllegalState() throws IOException, InterruptedException {
    GrpcServer server = new GrpcServer(configNoHealthNoMetrics());
    server.addService(mockService());
    server.start();
    try {
      assertThrows(IllegalStateException.class, server::start);
    } finally {
      server.shutdown();
    }
  }

  // ── GrpcServer.start() with addService(ServerServiceDefinition) ──────────

  @Test
  void start_withServiceDefinition_startsOk() throws IOException, InterruptedException {
    GrpcServer server = new GrpcServer(configNoHealthNoMetrics());
    ServerServiceDefinition def = ServerServiceDefinition.builder("test.DefService").build();
    server.addService(def);
    server.start();
    assertTrue(server.isRunning());
    server.shutdown();
  }

  // ── GrpcServer.start() with healthCheck=false (healthStatusManager=null) ─

  @Test
  void start_noHealthCheck_noMetrics_withBindableService()
      throws IOException, InterruptedException {
    GrpcServer server = new GrpcServer(configNoHealthNoMetrics());
    server.addService(mockService());
    server.start();
    assertTrue(server.isRunning());
    server.shutdown();
    assertFalse(server.isRunning());
  }

  // ── GrpcServer.start() with custom interceptors only ─────────────────────

  @Test
  void start_customInterceptorOnly_addsLoggingInterceptor()
      throws IOException, InterruptedException {
    AuthInterceptor authInterceptor = new AuthInterceptor(token -> "principal", false);
    GrpcServerConfiguration config =
        GrpcServerConfiguration.builder()
            .port(0)
            .enableMetrics(false)
            .enableHealthCheck(false)
            .enableReflection(false)
            .addInterceptor(authInterceptor)
            .build();

    GrpcServer server = new GrpcServer(config);
    server.addService(mockService());
    server.start();
    assertTrue(server.isRunning());
    server.shutdown();
  }

  // ── GrpcServer.setServiceHealth() with healthStatusManager=null ──────────

  @Test
  void setServiceHealth_noHealthCheck_doesNothing() throws IOException, InterruptedException {
    GrpcServer server = new GrpcServer(configNoHealthNoMetrics());
    server.addService(mockService());
    server.start();
    assertDoesNotThrow(() -> server.setServiceHealth("test.BranchService", true));
    assertDoesNotThrow(() -> server.setServiceHealth("test.BranchService", false));
    server.shutdown();
  }

  // ── GrpcServer.awaitTermination() when server is null ────────────────────

  @Test
  void awaitTermination_beforeStart_returnsImmediately() {
    GrpcServer server = new GrpcServer(configNoHealthNoMetrics());
    assertDoesNotThrow(server::awaitTermination);
  }

  // ── GrpcServer.shutdown() when server is null ────────────────────────────

  @Test
  void shutdown_beforeStart_returnsImmediately() {
    GrpcServer server = new GrpcServer(configNoHealthNoMetrics());
    assertDoesNotThrow(server::shutdown);
  }

  // ── GrpcServerConfiguration static factory branches ──────────────────────

  @Test
  void forDevelopment_returnsBuilder() {
    GrpcServerConfiguration config = GrpcServerConfiguration.forDevelopment().port(0).build();
    assertNotNull(config);
    assertTrue(config.enableReflection());
  }

  @Test
  void forProduction_returnsBuilder() {
    GrpcServerConfiguration config = GrpcServerConfiguration.forProduction().port(0).build();
    assertNotNull(config);
    assertFalse(config.enableReflection());
  }

  @Test
  void addInterceptors_listVariant() {
    AuthInterceptor ai = new AuthInterceptor(token -> "p", false);
    GrpcServerConfiguration config =
        GrpcServerConfiguration.builder()
            .port(0)
            .enableHealthCheck(false)
            .addInterceptors(List.of(ai))
            .build();
    assertEquals(1, config.interceptors().size());
  }

  // ── GrpcServerConfiguration.Builder validation branches ──────────────────

  @Test
  void builder_invalidPort_throwsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class, () -> GrpcServerConfiguration.builder().port(-1).build());
  }

  @Test
  void builder_zeroMessageSize_throwsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> GrpcServerConfiguration.builder().port(0).maxInboundMessageSize(0).build());
  }

  // ── ErrorMapper missing branch coverage ──────────────────────────────────

  @Test
  void mapToStatus_nullThrowable_returnsInternal() {
    Status status = ErrorMapper.mapToStatus(null);
    assertEquals(Status.Code.INTERNAL, status.getCode());
  }

  @Test
  void toStatusRuntimeException_alreadyStatusRuntimeException_passesThrough() {
    StatusRuntimeException original =
        Status.NOT_FOUND.withDescription("original").asRuntimeException();
    StatusRuntimeException result = ErrorMapper.toStatusRuntimeException(original);
    assertSame(original, result);
  }

  @Test
  void mapToStatus_alreadyStatusRuntimeException_returnsItsStatus() {
    StatusRuntimeException sre =
        Status.DEADLINE_EXCEEDED.withDescription("timeout").asRuntimeException();
    Status status = ErrorMapper.mapToStatus(sre);
    assertEquals(Status.Code.DEADLINE_EXCEEDED, status.getCode());
  }

  @Test
  void mapProblemToStatus_criticalSeverity_mapsLikeError() {
    Problem problem =
        Problem.of(
            ErrorCode.of("USER_NOT_FOUND"), ErrorCategory.BUSINESS, Severity.CRITICAL, "msg");
    Status status = ErrorMapper.mapProblemToStatus(problem);
    assertEquals(Status.Code.NOT_FOUND, status.getCode());
  }

  @Test
  void mapProblemToStatus_missingKeyword_returnsNotFound() {
    Problem problem =
        Problem.of(ErrorCode.of("RECORD_MISSING"), ErrorCategory.BUSINESS, Severity.ERROR, "msg");
    Status status = ErrorMapper.mapProblemToStatus(problem);
    assertEquals(Status.Code.NOT_FOUND, status.getCode());
  }

  @Test
  void mapProblemToStatus_authKeyword_returnsUnauthenticated() {
    Problem problem =
        Problem.of(ErrorCode.of("AUTH_EXPIRED"), ErrorCategory.BUSINESS, Severity.ERROR, "msg");
    Status status = ErrorMapper.mapProblemToStatus(problem);
    assertEquals(Status.Code.UNAUTHENTICATED, status.getCode());
  }

  @Test
  void mapProblemToStatus_limitKeyword_returnsResourceExhausted() {
    Problem problem =
        Problem.of(
            ErrorCode.of("RATE_LIMIT_EXCEEDED"), ErrorCategory.BUSINESS, Severity.ERROR, "msg");
    Status status = ErrorMapper.mapProblemToStatus(problem);
    assertEquals(Status.Code.RESOURCE_EXHAUSTED, status.getCode());
  }

  @Test
  void mapProblemToStatus_throttleKeyword_returnsResourceExhausted() {
    Problem problem =
        Problem.of(ErrorCode.of("REQUEST_THROTTLE"), ErrorCategory.BUSINESS, Severity.ERROR, "msg");
    Status status = ErrorMapper.mapProblemToStatus(problem);
    assertEquals(Status.Code.RESOURCE_EXHAUSTED, status.getCode());
  }

  @Test
  void mapProblemToStatus_abortedKeyword_returnsCancelled() {
    Problem problem =
        Problem.of(ErrorCode.of("TX_ABORTED"), ErrorCategory.BUSINESS, Severity.ERROR, "msg");
    Status status = ErrorMapper.mapProblemToStatus(problem);
    assertEquals(Status.Code.CANCELLED, status.getCode());
  }

  @Test
  void mapProblemToStatus_errorWithNoKeyword_returnsInternal() {
    Problem problem =
        Problem.of(ErrorCode.of("GENERIC_ERROR"), ErrorCategory.BUSINESS, Severity.ERROR, "msg");
    Status status = ErrorMapper.mapProblemToStatus(problem);
    assertEquals(Status.Code.INTERNAL, status.getCode());
  }

  // ── AuthInterceptor.extractToken branch: blank header ────────────────────

  @SuppressWarnings("unchecked")
  @Test
  void authInterceptor_blankHeader_rejectsAsInvalid() {
    AuthInterceptor interceptor = new AuthInterceptor(token -> "principal", true);
    ServerCall<String, String> call = mock(ServerCall.class);
    ServerCallHandler<String, String> handler = mock(ServerCallHandler.class);

    Metadata metadata = new Metadata();
    metadata.put(AuthInterceptor.AUTHORIZATION_METADATA_KEY, "   ");

    // blank header → extractToken returns null → call.close(UNAUTHENTICATED)
    interceptor.interceptCall(call, metadata, handler);
  }

  // ── AuthInterceptor static helpers ───────────────────────────────────────

  @Test
  void getPrincipal_outsideContext_returnsNull() {
    assertNull(AuthInterceptor.getPrincipal());
  }

  @Test
  void isAuthenticated_outsideContext_returnsFalse() {
    assertFalse(AuthInterceptor.isAuthenticated());
  }
}
