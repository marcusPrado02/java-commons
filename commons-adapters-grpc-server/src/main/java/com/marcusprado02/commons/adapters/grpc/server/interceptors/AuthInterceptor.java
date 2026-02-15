package com.marcusprado02.commons.adapters.grpc.server.interceptors;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.util.Objects;
import java.util.function.Function;

/**
 * Server interceptor for authentication.
 *
 * <p>Validates authentication tokens from metadata and propagates authenticated principal to the
 * context. If authentication fails, the call is rejected with UNAUTHENTICATED status.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * AuthInterceptor authInterceptor = new AuthInterceptor(
 *     token -> validateToken(token) // Returns principal or null
 * );
 *
 * ServerBuilder.forPort(9090)
 *     .addService(myService)
 *     .intercept(authInterceptor)
 *     .build();
 * }</pre>
 */
public class AuthInterceptor implements ServerInterceptor {

  /**
   * Context key for the authenticated principal.
   */
  public static final Context.Key<String> PRINCIPAL_CONTEXT_KEY =
      Context.key("principal");

  /**
   * Metadata key for authorization header.
   */
  public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

  private final Function<String, String> tokenValidator;
  private final boolean required;

  /**
   * Creates a new auth interceptor.
   *
   * @param tokenValidator function that validates token and returns principal (null if invalid)
   */
  public AuthInterceptor(Function<String, String> tokenValidator) {
    this(tokenValidator, true);
  }

  /**
   * Creates a new auth interceptor.
   *
   * @param tokenValidator function that validates token and returns principal (null if invalid)
   * @param required       whether authentication is required
   */
  public AuthInterceptor(Function<String, String> tokenValidator, boolean required) {
    this.tokenValidator = Objects.requireNonNull(tokenValidator, "Token validator cannot be null");
    this.required = required;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
    String authHeader = headers.get(AUTHORIZATION_METADATA_KEY);

    if (authHeader == null) {
      if (required) {
        call.close(Status.UNAUTHENTICATED.withDescription("Missing authorization header"), headers);
        return new ServerCall.Listener<>() {
        };
      }
      // Authentication not required, proceed without principal
      return next.startCall(call, headers);
    }

    // Extract token (assuming "Bearer <token>" format)
    String token = extractToken(authHeader);
    if (token == null) {
      call.close(Status.UNAUTHENTICATED.withDescription("Invalid authorization header format"),
          headers);
      return new ServerCall.Listener<>() {
      };
    }

    // Validate token
    String principal = tokenValidator.apply(token);
    if (principal == null) {
      call.close(Status.UNAUTHENTICATED.withDescription("Invalid or expired token"), headers);
      return new ServerCall.Listener<>() {
      };
    }

    // Create context with principal and proceed
    Context context = Context.current().withValue(PRINCIPAL_CONTEXT_KEY, principal);
    return Contexts.interceptCall(context, call, headers, next);
  }

  /**
   * Extracts token from authorization header.
   *
   * @param authHeader authorization header value
   * @return token or null if format is invalid
   */
  private String extractToken(String authHeader) {
    if (authHeader == null || authHeader.isBlank()) {
      return null;
    }

    if (authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7).trim();
    }

    // Also support raw token without "Bearer" prefix
    return authHeader.trim();
  }

  /**
   * Gets the authenticated principal from the current context.
   *
   * @return principal or null if not authenticated
   */
  public static String getPrincipal() {
    return PRINCIPAL_CONTEXT_KEY.get();
  }

  /**
   * Checks if the current request is authenticated.
   *
   * @return true if a principal is present
   */
  public static boolean isAuthenticated() {
    return PRINCIPAL_CONTEXT_KEY.get() != null;
  }
}
