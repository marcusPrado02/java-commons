package com.marcusprado02.commons.adapters.graphql.directive;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactories;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom directive for field-level authorization.
 *
 * <p>Example schema:
 *
 * <pre>{@code
 * directive @auth(
 *   requires: [String!]!
 * ) on FIELD_DEFINITION
 *
 * type Query {
 *   adminData: AdminData @auth(requires: ["ROLE_ADMIN"])
 *   userData: UserData @auth(requires: ["ROLE_USER"])
 * }
 * }</pre>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Bean
 * public RuntimeWiringConfigurer authDirective(AuthorizationService authService) {
 *     return builder -> builder.directive("auth", new AuthDirective(authService));
 * }
 * }</pre>
 */
public class AuthDirective implements SchemaDirectiveWiring {

  private final AuthorizationService authorizationService;
  private final Map<FieldCoordinates, DataFetcher<?>> originalDataFetchers =
      new ConcurrentHashMap<>();

  public AuthDirective(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Override
  public GraphQLFieldDefinition onField(
      SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {

    GraphQLFieldDefinition field = environment.getElement();
    GraphQLCodeRegistry.Builder codeRegistry = environment.getCodeRegistry();
    FieldCoordinates coordinates =
        FieldCoordinates.coordinates(environment.getFieldsContainer(), field);

    // Get required roles from directive argument
    var requiredRoles = (java.util.List<?>) environment.getAppliedDirective("auth").getArgument("requires").getValue();

    // Get original data fetcher
    DataFetcher<?> originalDataFetcher =
        codeRegistry.getDataFetcher(coordinates, field);

    // Create wrapped data fetcher with authorization
    DataFetcher<?> authDataFetcher =
        DataFetcherFactories.wrapDataFetcher(
            originalDataFetcher,
            (dataFetchingEnvironment, value) -> {
              // Check authorization
              if (!authorizationService.hasAnyRole(requiredRoles)) {
                throw new UnauthorizedException(
                    "Insufficient permissions. Required roles: " + requiredRoles);
              }
              return value;
            });

    // Register wrapped data fetcher
    codeRegistry.dataFetcher(coordinates, authDataFetcher);

    return field;
  }

  /**
   * Authorization service interface.
   */
  public interface AuthorizationService {
    /**
     * Checks if current user has any of the required roles.
     *
     * @param roles required roles
     * @return true if user has any role
     */
    boolean hasAnyRole(java.util.List<?> roles);
  }

  /**
   * Exception thrown when user is not authorized.
   */
  public static class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
      super(message);
    }
  }
}
