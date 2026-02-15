package com.marcusprado02.commons.adapters.graphql.error;

import com.marcusprado02.commons.kernel.errors.DomainException;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.language.SourceLocation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL exception handler that converts domain errors to GraphQL errors.
 *
 * <p>Maps {@link DomainException} to GraphQL errors with proper error codes and extensions.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Bean
 * public DataFetcherExceptionHandler exceptionHandler() {
 *     return new GraphQLExceptionHandler();
 * }
 * }</pre>
 */
@Component
public class GraphQLExceptionHandler implements DataFetcherExceptionHandler {

  @Override
  public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
      DataFetcherExceptionHandlerParameters handlerParameters) {

    Throwable exception = handlerParameters.getException();
    SourceLocation sourceLocation = handlerParameters.getSourceLocation();
    var executionPath = handlerParameters.getPath();

    GraphQLError error;

    if (exception instanceof DomainException domainException) {
      error = DomainGraphQLError.from(domainException, sourceLocation, executionPath.toList());
    } else {
      error = GenericGraphQLError.from(exception, sourceLocation, executionPath.toList());
    }

    return CompletableFuture.completedFuture(
        DataFetcherExceptionHandlerResult.newResult().error(error).build());
  }

  /** GraphQL error for domain errors. */
  private record DomainGraphQLError(
      String message,
      List<SourceLocation> locations,
      List<Object> path,
      ErrorCode errorCode,
      ErrorCategory errorCategory,
      Map<String, Object> details)
      implements GraphQLError {

    static DomainGraphQLError from(
        DomainException domainException, SourceLocation location, List<Object> path) {
      return new DomainGraphQLError(
          domainException.getMessage(),
          List.of(location),
          path,
          domainException.problem().code(),
          domainException.problem().category(),
          domainException.problem().meta());
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public List<SourceLocation> getLocations() {
      return locations;
    }

    @Override
    public List<Object> getPath() {
      return path;
    }

    @Override
    public ErrorClassification getErrorType() {
      return mapErrorCategoryToClassification(errorCategory);
    }

    @Override
    public Map<String, Object> getExtensions() {
      return Map.of(
          "errorCode", errorCode.value(),
          "details", details != null ? details : Map.of());
    }

    private ErrorClassification mapErrorCategoryToClassification(ErrorCategory category) {
      return switch (category) {
        case VALIDATION -> graphql.ErrorType.ValidationError;
        case NOT_FOUND -> graphql.ErrorType.DataFetchingException;
        case BUSINESS -> graphql.ErrorType.ExecutionAborted;
        case UNAUTHORIZED, FORBIDDEN -> graphql.ErrorType.ValidationError;
        default -> graphql.ErrorType.DataFetchingException;
      };
    }
  }

  /** GraphQL error for generic exceptions. */
  private record GenericGraphQLError(
      String message, List<SourceLocation> locations, List<Object> path)
      implements GraphQLError {

    static GenericGraphQLError from(
        Throwable exception, SourceLocation location, List<Object> path) {
      return new GenericGraphQLError(exception.getMessage(), List.of(location), path);
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public List<SourceLocation> getLocations() {
      return locations;
    }

    @Override
    public List<Object> getPath() {
      return path;
    }

    @Override
    public ErrorClassification getErrorType() {
      return graphql.ErrorType.DataFetchingException;
    }
  }
}
