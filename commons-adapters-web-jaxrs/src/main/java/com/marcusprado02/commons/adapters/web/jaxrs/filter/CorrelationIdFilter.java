package com.marcusprado02.commons.adapters.web.jaxrs.filter;

import com.marcusprado02.commons.app.observability.ContextKeys;
import com.marcusprado02.commons.app.observability.CorrelationId;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import org.slf4j.MDC;

/**
 * JAX-RS filter that generates or propagates correlation IDs across requests.
 *
 * <p>Functionality:
 *
 * <ul>
 *   <li>Generates a new correlation ID if not present in request headers
 *   <li>Adds correlation ID to MDC for logging
 *   <li>Adds correlation ID to response headers
 *   <li>Cleans up MDC after request processing
 * </ul>
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * @ApplicationPath("/api")
 * public class RestApplication extends Application {
 *
 *   @Override
 *   public Set<Class<?>> getClasses() {
 *     return Set.of(
 *       CorrelationIdFilter.class,
 *       // other providers
 *     );
 *   }
 * }
 * }</pre>
 */
@Provider
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

  public static final String HEADER_NAME = "X-Correlation-Id";
  public static final String PROPERTY_NAME = "commons.correlation_id";

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String incoming = requestContext.getHeaderString(HEADER_NAME);
    String correlationId =
        (incoming == null || incoming.isBlank()) ? CorrelationId.newId() : incoming.trim();

    requestContext.setProperty(PROPERTY_NAME, correlationId);
    MDC.put(ContextKeys.CORRELATION_ID, correlationId);
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {

    String correlationId = (String) requestContext.getProperty(PROPERTY_NAME);
    if (correlationId != null) {
      responseContext.getHeaders().add(HEADER_NAME, correlationId);
    }

    MDC.remove(ContextKeys.CORRELATION_ID);
  }
}
