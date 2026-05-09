package com.marcusprado02.commons.adapters.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.adapters.graphql.directive.AuthDirective;
import com.marcusprado02.commons.adapters.graphql.error.GraphQLExceptionHandler;
import com.marcusprado02.commons.adapters.graphql.subscription.GraphQLSubscriptionManager;
import com.marcusprado02.commons.kernel.errors.DomainException;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.ResultPath;
import graphql.language.SourceLocation;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLAppliedDirectiveArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class GraphQLBranchTest {

  // ── DomainGraphQLError.mapErrorCategoryToClassification switch branches ───

  @Test
  void handleException_validationCategory_mapsToValidationError() throws Exception {
    var error = handleWith(ErrorCategory.VALIDATION);
    assertThat(error.getErrorType()).isEqualTo(ErrorType.ValidationError);
  }

  @Test
  void handleException_businessCategory_mapsToExecutionAborted() throws Exception {
    var error = handleWith(ErrorCategory.BUSINESS);
    assertThat(error.getErrorType()).isEqualTo(ErrorType.ExecutionAborted);
  }

  @Test
  void handleException_unauthorizedCategory_mapsToValidationError() throws Exception {
    var error = handleWith(ErrorCategory.UNAUTHORIZED);
    assertThat(error.getErrorType()).isEqualTo(ErrorType.ValidationError);
  }

  @Test
  void handleException_forbiddenCategory_mapsToValidationError() throws Exception {
    var error = handleWith(ErrorCategory.FORBIDDEN);
    assertThat(error.getErrorType()).isEqualTo(ErrorType.ValidationError);
  }

  @Test
  void handleException_technicalCategory_mapsToDataFetchingException() throws Exception {
    var error = handleWith(ErrorCategory.TECHNICAL);
    assertThat(error.getErrorType()).isEqualTo(ErrorType.DataFetchingException);
  }

  // ── AuthDirective: UnauthorizedException ─────────────────────────────────

  @Test
  void unauthorizedException_hasCorrectMessage() {
    AuthDirective.UnauthorizedException ex = new AuthDirective.UnauthorizedException("denied");
    assertThat(ex.getMessage()).isEqualTo("denied");
  }

  // ── AuthDirective.onField(): method body coverage ─────────────────────────

  @Test
  void onField_registersWrappedDataFetcherAndReturnsField() {
    AuthDirective.AuthorizationService authService = mock(AuthDirective.AuthorizationService.class);
    AuthDirective directive = new AuthDirective(authService);

    SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env =
        mock(SchemaDirectiveWiringEnvironment.class);
    GraphQLFieldDefinition field = mock(GraphQLFieldDefinition.class);
    GraphQLCodeRegistry.Builder codeRegistry = mock(GraphQLCodeRegistry.Builder.class);
    GraphQLFieldsContainer container = mock(GraphQLFieldsContainer.class);

    when(field.getName()).thenReturn("myField");
    when(container.getName()).thenReturn("Query");
    when(env.getElement()).thenReturn(field);
    when(env.getCodeRegistry()).thenReturn(codeRegistry);
    when(env.getFieldsContainer()).thenReturn(container);

    GraphQLAppliedDirective appliedDirective = mock(GraphQLAppliedDirective.class);
    GraphQLAppliedDirectiveArgument argument = mock(GraphQLAppliedDirectiveArgument.class);
    when(argument.getValue()).thenReturn(List.of("ROLE_ADMIN"));
    when(appliedDirective.getArgument("requires")).thenReturn(argument);
    when(env.getAppliedDirective("auth")).thenReturn(appliedDirective);

    @SuppressWarnings("rawtypes")
    DataFetcher rawFetcher = mock(DataFetcher.class);
    @SuppressWarnings("unchecked")
    DataFetcher originalFetcher = rawFetcher;
    when(codeRegistry.getDataFetcher(
            any(FieldCoordinates.class), any(GraphQLFieldDefinition.class)))
        .thenAnswer(inv -> originalFetcher);

    GraphQLFieldDefinition result = directive.onField(env);

    assertThat(result).isSameAs(field);
    verify(codeRegistry).dataFetcher(any(FieldCoordinates.class), any(DataFetcher.class));
  }

  // ── GraphQLSubscriptionManager.subscribeWithHeartbeat ─────────────────────

  @Test
  void subscribeWithHeartbeat_emitsHeartbeatEvents() {
    GraphQLSubscriptionManager<String> manager = new GraphQLSubscriptionManager<>();
    var publisher = manager.subscribeWithHeartbeat(Duration.ofMillis(50), "ping");

    StepVerifier.create(publisher).expectNext("ping").thenCancel().verify(Duration.ofSeconds(2));
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  private static GraphQLError handleWith(ErrorCategory category) throws Exception {
    Problem problem = Problem.of(ErrorCode.of("ERR"), category, Severity.ERROR, "test");
    DomainException ex = new DomainException(problem);

    DataFetcherExceptionHandlerParameters params =
        mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(ex);
    when(params.getSourceLocation()).thenReturn(new SourceLocation(1, 1));
    when(params.getPath()).thenReturn(ResultPath.parse("/test"));

    return new GraphQLExceptionHandler().handleException(params).get().getErrors().get(0);
  }
}
