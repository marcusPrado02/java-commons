package com.marcusprado02.commons.adapters.graphql.error;

import com.marcusprado02.commons.kernel.errors.DomainException;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import graphql.language.SourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphQLExceptionHandlerTest {

  private GraphQLExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GraphQLExceptionHandler();
  }

  @Test
  void shouldHandleDomainError() throws Exception {
    ErrorCode errorCode = new ErrorCode("USER_NOT_FOUND");
    Problem problem = Problem.of(
        errorCode,
        ErrorCategory.NOT_FOUND,
        Severity.ERROR,
        "User not found"
    );
    DomainException domainException = new DomainException(problem);

    DataFetcherExceptionHandlerParameters params = createParams(domainException);

    CompletableFuture<DataFetcherExceptionHandlerResult> result =
        handler.handleException(params);

    DataFetcherExceptionHandlerResult handlerResult = result.get();
    assertNotNull(handlerResult);

    List<GraphQLError> errors = handlerResult.getErrors();
    assertEquals(1, errors.size());

    GraphQLError error = errors.get(0);
    assertEquals("User not found", error.getMessage());
    assertEquals(ErrorType.DataFetchingException, error.getErrorType());

    Map<String, Object> extensions = error.getExtensions();
    assertNotNull(extensions);
    assertEquals("USER_NOT_FOUND", extensions.get("errorCode"));
    assertTrue(extensions.containsKey("details"));
  }

  @Test
  void shouldHandleGenericException() throws Exception {
    RuntimeException exception = new RuntimeException("Something went wrong");

    DataFetcherExceptionHandlerParameters params = createParams(exception);

    CompletableFuture<DataFetcherExceptionHandlerResult> result =
        handler.handleException(params);

    DataFetcherExceptionHandlerResult handlerResult = result.get();
    GraphQLError error = handlerResult.getErrors().get(0);

    assertEquals("Something went wrong", error.getMessage());
    assertEquals(ErrorType.DataFetchingException, error.getErrorType());
  }

  @Test
  void shouldIncludeSourceLocation() throws Exception {
    RuntimeException exception = new RuntimeException("Test error");

    DataFetcherExceptionHandlerParameters params = createParams(exception);

    CompletableFuture<DataFetcherExceptionHandlerResult> result =
        handler.handleException(params);

    DataFetcherExceptionHandlerResult handlerResult = result.get();
    GraphQLError error = handlerResult.getErrors().get(0);

    assertNotNull(error.getLocations());
    assertFalse(error.getLocations().isEmpty());
  }

  @Test
  void shouldIncludeExecutionPath() throws Exception {
    RuntimeException exception = new RuntimeException("Test error");

    DataFetcherExceptionHandlerParameters params = createParams(exception);

    CompletableFuture<DataFetcherExceptionHandlerResult> result =
        handler.handleException(params);

    DataFetcherExceptionHandlerResult handlerResult = result.get();
    GraphQLError error = handlerResult.getErrors().get(0);

    assertNotNull(error.getPath());
    assertEquals(List.of("user", "email"), error.getPath());
  }

  private DataFetcherExceptionHandlerParameters createParams(Throwable exception) {
    DataFetcherExceptionHandlerParameters params =
        mock(DataFetcherExceptionHandlerParameters.class);

    when(params.getException()).thenReturn(exception);
    when(params.getSourceLocation()).thenReturn(new SourceLocation(10, 5));
    when(params.getPath()).thenReturn(ResultPath.parse("/user/email"));

    return params;
  }
}
