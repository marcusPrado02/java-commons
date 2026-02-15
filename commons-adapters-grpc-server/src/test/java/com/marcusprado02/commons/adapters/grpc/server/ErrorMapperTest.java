package com.marcusprado02.commons.adapters.grpc.server;

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
    Problem problem = Problem.builder()
        .code("USER_NOT_FOUND")
        .title("User not found")
        .severity(ProblemSeverity.ERROR)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.NOT_FOUND, status.getCode());
  }

  @Test
  void shouldMapProblemWithUnauthorizedKeyword() {
    Problem problem = Problem.builder()
        .code("UNAUTHORIZED_ACCESS")
        .title("Unauthorized")
        .severity(ProblemSeverity.ERROR)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.UNAUTHENTICATED, status.getCode());
  }

  @Test
  void shouldMapProblemWithForbiddenKeyword() {
    Problem problem = Problem.builder()
        .code("FORBIDDEN_OPERATION")
        .title("Forbidden")
        .severity(ProblemSeverity.ERROR)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.PERMISSION_DENIED, status.getCode());
  }

  @Test
  void shouldMapProblemWithValidationKeyword() {
    Problem problem = Problem.builder()
        .code("VALIDATION_ERROR")
        .title("Validation failed")
        .severity(ProblemSeverity.ERROR)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.INVALID_ARGUMENT, status.getCode());
  }

  @Test
  void shouldMapProblemWithConflictKeyword() {
    Problem problem = Problem.builder()
        .code("DUPLICATE_ENTRY")
        .title("Conflict")
        .severity(ProblemSeverity.ERROR)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.ALREADY_EXISTS, status.getCode());
  }

  @Test
  void shouldMapProblemWithTimeoutKeyword() {
    Problem problem = Problem.builder()
        .code("TIMEOUT_ERROR")
        .title("Operation timeout")
        .severity(ProblemSeverity.ERROR)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.DEADLINE_EXCEEDED, status.getCode());
  }

  @Test
  void shouldMapProblemWithUnavailableKeyword() {
    Problem problem = Problem.builder()
        .code("SERVICE_UNAVAILABLE")
        .title("Service unavailable")
        .severity(ProblemSeverity.ERROR)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.UNAVAILABLE, status.getCode());
  }

  @Test
  void shouldMapProblemWithPreconditionKeyword() {
    Problem problem = Problem.builder()
        .code("PRECONDITION_FAILED")
        .title("Precondition failed")
        .severity(ProblemSeverity.ERROR)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.FAILED_PRECONDITION, status.getCode());
  }

  @Test
  void shouldMapProblemWithQuotaKeyword() {
    Problem problem = Problem.builder()
        .code("QUOTA_EXCEEDED")
        .title("Quota exceeded")
        .severity(ProblemSeverity.ERROR)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.RESOURCE_EXHAUSTED, status.getCode());
  }

  @Test
  void shouldMapProblemWithCancelledKeyword() {
    Problem problem = Problem.builder()
        .code("OPERATION_CANCELLED")
        .title("Cancelled")
        .severity(ProblemSeverity.ERROR)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.CANCELLED, status.getCode());
  }

  @Test
  void shouldMapWarningProblemToOk() {
    Problem problem = Problem.builder()
        .code("SOME_WARNING")
        .title("Warning")
        .severity(ProblemSeverity.WARNING)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.OK, status.getCode());
  }

  @Test
  void shouldMapInfoProblemToOk() {
    Problem problem = Problem.builder()
        .code("SOME_INFO")
        .title("Info")
        .severity(ProblemSeverity.INFO)
        .build();

    Status status = ErrorMapper.mapProblemToStatus(problem);

    assertEquals(Status.Code.OK, status.getCode());
  }

  @Test
  void shouldCreateNotFoundStatus() {
    StatusRuntimeException ex = ErrorMapper.notFound("Resource not found");

    assertEquals(Status.Code.NOT_FOUND, ex.getStatus().getCode());
    assertEquals("Resource not found", ex.getStatus().getDescription());
  }

  @Test
  void shouldCreateInvalidArgumentStatus() {
    StatusRuntimeException ex = ErrorMapper.invalidArgument("Invalid input");

    assertEquals(Status.Code.INVALID_ARGUMENT, ex.getStatus().getCode());
    assertEquals("Invalid input", ex.getStatus().getDescription());
  }

  @Test
  void shouldCreatePermissionDeniedStatus() {
    StatusRuntimeException ex = ErrorMapper.permissionDenied("Access denied");

    assertEquals(Status.Code.PERMISSION_DENIED, ex.getStatus().getCode());
    assertEquals("Access denied", ex.getStatus().getDescription());
  }

  @Test
  void shouldCreateAlreadyExistsStatus() {
    StatusRuntimeException ex = ErrorMapper.alreadyExists("Resource already exists");

    assertEquals(Status.Code.ALREADY_EXISTS, ex.getStatus().getCode());
    assertEquals("Resource already exists", ex.getStatus().getDescription());
  }

  @Test
  void shouldWrapExceptionWithContext() {
    Exception cause = new IllegalArgumentException("Original error");
    StatusRuntimeException ex = ErrorMapper.wrapWithContext(cause, "Failed to process request");

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
    Problem problem = Problem.builder()
        .code("NOT_FOUND")
        .title("Resource not found")
        .detail("The requested resource was not found")
        .severity(ProblemSeverity.ERROR)
        .build();

    StatusRuntimeException ex = ErrorMapper.toStatusRuntimeException(problem);

    assertEquals(Status.Code.NOT_FOUND, ex.getStatus().getCode());
    assertTrue(ex.getStatus().getDescription().contains("Resource not found"));
  }
}
