package com.marcusprado02.commons.adapters.grpc.server.error;

import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Maps exceptions and Problems to gRPC Status codes.
 *
 * <p>Mapping strategy:
 * <ul>
 *   <li>IllegalArgumentException → INVALID_ARGUMENT</li>
 *   <li>IllegalStateException → FAILED_PRECONDITION</li>
 *   <li>NullPointerException → INVALID_ARGUMENT</li>
 *   <li>SecurityException → PERMISSION_DENIED</li>
 *   <li>UnsupportedOperationException → UNIMPLEMENTED</li>
 *   <li>Problem → based on severity and error code</li>
 *   <li>Other exceptions → INTERNAL</li>
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try {
 *   // Service logic
 * } catch (Exception e) {
 *   throw ErrorMapper.toStatusRuntimeException(e);
 * }
 * }</pre>
 */
public class ErrorMapper {

  private ErrorMapper() {
    // Utility class
  }

  /**
   * Converts an exception to a gRPC StatusRuntimeException.
   *
   * @param throwable exception to convert
   * @return StatusRuntimeException with appropriate code and description
   */
  public static StatusRuntimeException toStatusRuntimeException(Throwable throwable) {
    if (throwable instanceof StatusRuntimeException) {
      return (StatusRuntimeException) throwable;
    }

    Status status = mapToStatus(throwable);
    return status.withDescription(throwable.getMessage())
        .withCause(throwable)
        .asRuntimeException();
  }

  /**
   * Converts a Problem to a gRPC StatusRuntimeException.
   *
   * @param problem problem to convert
   * @return StatusRuntimeException with appropriate code and description
   */
  public static StatusRuntimeException toStatusRuntimeException(Problem problem) {
    Status status = mapProblemToStatus(problem);
    return status.withDescription(problem.message())
        .asRuntimeException();
  }

  /**
   * Maps an exception to a gRPC Status.
   *
   * @param throwable exception to map
   * @return appropriate gRPC Status
   */
  public static Status mapToStatus(Throwable throwable) {
    if (throwable instanceof StatusRuntimeException) {
      return ((StatusRuntimeException) throwable).getStatus();
    }

    return switch (throwable) {
      case IllegalArgumentException ignored -> Status.INVALID_ARGUMENT;
      case NullPointerException ignored -> Status.INVALID_ARGUMENT;
      case IllegalStateException ignored -> Status.FAILED_PRECONDITION;
      case SecurityException ignored -> Status.PERMISSION_DENIED;
      case UnsupportedOperationException ignored -> Status.UNIMPLEMENTED;
      case null, default -> Status.INTERNAL;
    };
  }

  /**
   * Maps a Problem to a gRPC Status based on severity and error code.
   *
   * @param problem problem to map
   * @return appropriate gRPC Status
   */
  public static Status mapProblemToStatus(Problem problem) {
    // Map based on severity
    if (problem. severity() == Severity.CRITICAL || problem.severity() == Severity.ERROR) {
      // Check error code for more specific mapping
      String errorCode = problem.code().value();

      if (errorCode.contains("NOT_FOUND") || errorCode.contains("MISSING")) {
        return Status.NOT_FOUND;
      }

      if (errorCode.contains("UNAUTHORIZED") || errorCode.contains("AUTH")) {
        return Status.UNAUTHENTICATED;
      }

      if (errorCode.contains("FORBIDDEN") || errorCode.contains("PERMISSION")) {
        return Status.PERMISSION_DENIED;
      }

      if (errorCode.contains("INVALID") || errorCode.contains("VALIDATION")) {
        return Status.INVALID_ARGUMENT;
      }

      if (errorCode.contains("CONFLICT") || errorCode.contains("DUPLICATE")) {
        return Status.ALREADY_EXISTS;
      }

      if (errorCode.contains("TIMEOUT") || errorCode.contains("DEADLINE")) {
        return Status.DEADLINE_EXCEEDED;
      }

      if (errorCode.contains("UNAVAILABLE") || errorCode.contains("CONNECTION")) {
        return Status.UNAVAILABLE;
      }

      if (errorCode.contains("PRECONDITION") || errorCode.contains("STATE")) {
        return Status.FAILED_PRECONDITION;
      }

      if (errorCode.contains("LIMIT") || errorCode.contains("QUOTA") ||
          errorCode.contains("THROTTLE")) {
        return Status.RESOURCE_EXHAUSTED;
      }

      if (errorCode.contains("CANCELLED") || errorCode.contains("ABORTED")) {
        return Status.CANCELLED;
      }

      // Default for errors
      return Status.INTERNAL;
    }

    // Warnings map to OK with description
    if (problem.severity() == Severity.WARNING) {
      return Status.OK;
    }

    // Info and debug map to OK
    return Status.OK;
  }

  /**
   * Wraps an exception with context for better error messages.
   *
   * @param operation operation being performed
   * @param throwable exception that occurred
   * @return StatusRuntimeException with contextual description
   */
  public static StatusRuntimeException wrapWithContext(String operation, Throwable throwable) {
    Status status = mapToStatus(throwable);
    String description = String.format("Error during %s: %s", operation, throwable.getMessage());

    return status.withDescription(description)
        .withCause(throwable)
        .asRuntimeException();
  }

  /**
   * Creates a NOT_FOUND status exception.
   *
   * @param resourceType type of resource not found
   * @param identifier   resource identifier
   * @return StatusRuntimeException
   */
  public static StatusRuntimeException notFound(String resourceType, String identifier) {
    String description = String.format("%s not found: %s", resourceType, identifier);
    return Status.NOT_FOUND.withDescription(description).asRuntimeException();
  }

  /**
   * Creates an INVALID_ARGUMENT status exception.
   *
   * @param fieldName field that is invalid
   * @param reason    reason for invalidity
   * @return StatusRuntimeException
   */
  public static StatusRuntimeException invalidArgument(String fieldName, String reason) {
    String description = String.format("Invalid %s: %s", fieldName, reason);
    return Status.INVALID_ARGUMENT.withDescription(description).asRuntimeException();
  }

  /**
   * Creates a PERMISSION_DENIED status exception.
   *
   * @param action action that was denied
   * @return StatusRuntimeException
   */
  public static StatusRuntimeException permissionDenied(String action) {
    String description = String.format("Permission denied for: %s", action);
    return Status.PERMISSION_DENIED.withDescription(description).asRuntimeException();
  }

  /**
   * Creates an ALREADY_EXISTS status exception.
   *
   * @param resourceType type of resource
   * @param identifier   resource identifier
   * @return StatusRuntimeException
   */
  public static StatusRuntimeException alreadyExists(String resourceType, String identifier) {
    String description = String.format("%s already exists: %s", resourceType, identifier);
    return Status.ALREADY_EXISTS.withDescription(description).asRuntimeException();
  }
}
