package com.marcusprado02.commons.adapters.web.jaxrs.client;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import org.slf4j.MDC;

/**
 * JAX-RS client filter that propagates context headers to downstream services.
 *
 * <p>Propagates headers from MDC (populated by server filters):
 *
 * <ul>
 *   <li>X-Correlation-Id
 *   <li>X-Tenant-Id (if present)
 *   <li>X-Actor-Id (if present)
 * </ul>
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * Client client = ClientBuilder.newClient();
 * client.register(ContextPropagationFilter.class);
 *
 * Response response = client.target("http://downstream-service/api/users")
 *     .request()
 *     .get();
 * }</pre>
 */
public class ContextPropagationFilter implements ClientRequestFilter {

  private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  private static final String TENANT_ID_HEADER = "X-Tenant-Id";
  private static final String ACTOR_ID_HEADER = "X-Actor-Id";

  private static final String CORRELATION_ID_MDC_KEY = "correlationId";
  private static final String TENANT_ID_MDC_KEY = "tenantId";
  private static final String ACTOR_ID_MDC_KEY = "actorId";

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
    if (correlationId != null && !correlationId.isBlank()) {
      requestContext.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
    }

    String tenantId = MDC.get(TENANT_ID_MDC_KEY);
    if (tenantId != null && !tenantId.isBlank()) {
      requestContext.getHeaders().add(TENANT_ID_HEADER, tenantId);
    }

    String actorId = MDC.get(ACTOR_ID_MDC_KEY);
    if (actorId != null && !actorId.isBlank()) {
      requestContext.getHeaders().add(ACTOR_ID_HEADER, actorId);
    }
  }
}
