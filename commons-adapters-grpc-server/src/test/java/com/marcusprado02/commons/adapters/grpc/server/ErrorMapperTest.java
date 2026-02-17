package com.marcusprado02.commons.adapters.grpc.server;

import com.marcusprado02.commons.adapters.grpc.server.error.ErrorMapper;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorMapperTest {

  @Test
  void shouldMapIllegalArgumentException() {
    IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");
    Status status = ErrorMapper.mapToStatus(ex);

    assertEquals(Status.Code.INVALID_ARGUMENT, status.getCode());
    assertEquals("Invalid argument", status.getDescription());
  }

  @Test
  void shouldMapNullPointerException() {
    NullPointerException ex = new NullPointerException("Null value");
    Status status = ErrorMapper.mapToStatus(ex);

    assertEquals(Status.Code.INVALID_ARGUMENT, status.getCode());
    assertEquals("Null value", status.getDescription());
  }

  @Test
  void shouldMapIllegalStateException() {
    IllegalStateException ex = new IllegalStateException("Invalid state");
    Status status = ErrorMapper.mapToStatus(ex);

    assertEquals(Status.Code.FAILED_PRECONDITION, status.getCode());
    assertEquals("Invalid state", status.getDescription());
  }

  @Test
  void shouldMapSecurityException() {
    SecurityException ex = new SecurityException("Access denied");
    Status status = ErrorMapper.mapToStatus(ex);

    assertEquals(Status.Code.PERMISSION_DENIED, status.getCode());
    assertEquals("Access denied", status.getDescription());
  }

  @Test
  void shouldMapUnsupportedOperationException() {
    UnsupportedOperationException ex = new UnsupportedOperationException("Not supported");
    Status status = ErrorMapper.mapToStatus(ex);

    assertEquals(Status.Code.UNIMPLEMENTED, status.getCode());
    assertEquals("Not supported", status.getDescription());
  }

  @Test
  void shouldMapGenericException() {
    Exception ex = new Exception("Generic error");
    Status status = ErrorMapper.mapToStatus(ex);

    assertEquals(Status.Code.INTERNAL, status.getCode());
    assertEquals("Generic error", status.getDescription());
  }

  @Test
  void shouldMapProblemWithNotFoundKeyword() {
    Problem problem = Problem.of(
        ErrorCode.of("USER_NOT_FOUND"),
        ErrorCategory.BUSINESS,
        Severity.ERROR,
        "User not found");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.NOT_FOUND, status.getCode());
  }

  @Test
  void shouldMapProblemWithUnauthorizedKeyword() {
    Problem problem = Problem.of(
        ErrorCode.of("UNAUTHORIZED_ACCESS"), ErrorCategory.BUSINESS,
        Severity.ERROR, "Unauthorized");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.UNAUTHENTICATED, status.getCode());
  }

  @Test
  void shouldMapProblemWithForbiddenKeyword() {
    Problem problem = Problem.of(
        ErrorCode.of("FORBIDDEN_OPERATION"), ErrorCategory.BUSINESS,
        Severity.ERROR, "Forbidden");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.PERMISSION_DENIED, status.getCode());
  }

  @Test
  void shouldMapProblemWithValidationKeyword() {
    Problem problem = Problem.of(
        ErrorCode.of("VALIDATION_ERROR"), ErrorCategory.BUSINESS,
        Severity.ERROR, "Validation failed");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.INVALID_ARGUMENT, status.getCode());
  }

  @Test
  void shouldMapProblemWithConflictKeyword() {
    Problem problem = Problem.of(
        ErrorCode.of("DUPLICATE_ENTRY"), ErrorCategory.BUSINESS,
        Severity.ERROR, "Conflict");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.ALREADY_EXISTS, status.getCode());
  }

  @Test
  void shouldMapProblemWithTimeoutKeyword() {
    Problem problem = Problem.of(
        ErrorCode.of("TIMEOUT_ERROR"), ErrorCategory.BUSINESS,
        Severity.ERROR, "Operation timeout");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.DEADLINE_EXCEEDED, status.getCode());
  }

  @Test
  void shouldMapProblemWithUnavailableKeyword() {
    Problem problem = Problem.of(
        ErrorCode.of("SERVICE_UNAVAILABLE"), ErrorCategory.BUSINESS,
        Severity.ERROR, "Service unavailable");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.UNAVAILABLE, status.getCode());
  }

  @Test
  void shouldMapProblemWithPreconditionKeyword() {
    Problem problem = Problem.of(
        ErrorCode.of("PRECONDITION_FAILED"), ErrorCategory.BUSINESS,
        Severity.ERROR, "Precondition failed");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.FAILED_PRECONDITION, status.getCode());
  }

  @Test
  void shouldMapProblemWithQuotaKeyword() {
    Problem problem = Problem.of(
        ErrorCode.of("QUOTA_EXCEEDED"), ErrorCategory.BUSINESS,
        Severity.ERROR, "Quota exceeded");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.RESOURCE_EXHAUSTED, status.getCode());
  }

  @Test
  void shouldMapProblemWithCancelledKeyword() {
    Problem problem = Problem.of(
        ErrorCode.of("OPERATION_CANCELLED"), ErrorCategory.BUSINESS,
        Severity.ERROR, "Cancelled");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.CANCELLED, status.getCode());
  }

  @Test
  void shouldMapWarningProblemToOk() {
    Problem problem = Problem.of(
        ErrorCode.of("SOME_WARNING"), ErrorCategory.BUSINESS,
        Severity.WARNING, "Warning");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.OK, status.getCode());
  }

  @Test
  void shouldMapInfoProblemToOk() {
    Problem problem = Problem.of(
        ErrorCode.of("SOME_INFO"), ErrorCategory.BUSINESS,
        Severity.INFO, "Info");

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.OK, status.getCode());
  }

  @Test
  void shouldCreateNotFoundStatus() {
    StatusRuntimeException ex = ErrorMapper.notFound("User", "123");

    assertEquals(Status.Code.NOT_FOUND, ex.getStatus().getCode());
    assertEquals("Resource not found", ex.getStatus().getDescription());
  }

  @Test
  void shouldCreateInvalidArgumentStatus() {
    StatusRuntimeException ex = ErrorMapper.invalidArgument("email", "Invalid format");

    assertEquals(Status.Code.INVALID_ARGUMENT, ex.getStatus().getCode());
    assertTrue(ex.getStatus().getDescription().contains("email"));
  }

  @Test
  void shouldCreatePermissionDeniedStatus() {
    StatusRuntimeException ex = ErrorMapper.permissionDenied("Access denied");

    assertEquals(Status.Code.PERMISSION_DENIED, ex.getStatus().getCode());
    assertEquals("Access denied", ex.getStatus().getDescription());
  }

  @Test
  void shouldCreateAlreadyExistsStatus() {
    StatusRuntimeException ex = ErrorMapper.alreadyExists("User", "john@example.com");

    assertEquals(Status.Code.ALREADY_EXISTS, ex.getStatus().getCode());
    assertEquals("Resource already exists", ex.getStatus().getDescription());
  }

  @Test
  void shouldWrapExceptionWithContext() {
    Exception cause = new IllegalArgumentException("Original error");
    StatusRuntimeException ex = ErrorMapper.wrapWithContext("Failed to process request", cause);

    assertEquals(Status.Code.INVALID_ARGUMENT, ex.getStatus().getCode());
    assertTrue(ex.getStatus().getDescription().contains("Failed to process request"));
    assertTrue(ex.getStatus().getDescription().contains("Original error"));
  }

  @Test
  void shouldConvertExceptionToStatusRuntimeException() {
    IllegalArgumentException ex = new IllegalArgumentException("Test error");
    StatusRuntimeException statusEx = ErrorMapper.toStatusRuntimeException(ex);

    assertEquals(Status.Code.INVALID_ARGUMENT, statusEx.getStatus().getCode());
    assertEquals("Test error", statusEx.getStatus().getDescription());
  }

  @Test
  void shouldConvertProblemToStatusRuntimeException() {
    Problem problem = Problem.of(
        ErrorCode.of("NOT_FOUND"), ErrorCategory.BUSINESS,
        Severity.ERROR, "Resource not found");

    StatusRuntimeException ex = ErrorMapper.toStatusRuntimeException(problem);

    assertEquals(Status.Code.NOT_FOUND, ex.getStatus().getCode());
    assertTrue(ex.getStatus().getDescription().contains("Resource not found"));
  }
}
