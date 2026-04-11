package com.marcusprado02.commons.adapters.web.spring.result;

import com.marcusprado02.commons.adapters.web.result.HttpResultsWithContext;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.platform.context.RequestContext;
import org.springframework.http.ResponseEntity;

/**
 * Utility for converting {@link com.marcusprado02.commons.kernel.result.Result} to context-enriched
 * response entities.
 */
public final class SpringHttpResultsWithContext {

  private SpringHttpResultsWithContext() {}

  /**
   * Converts the result to a context-enriched response entity.
   *
   * @param result the result to convert
   * @param mapper the mapper to use
   * @param requestContext the current request context
   * @param <T> the result value type
   * @return the response entity
   */
  public static <T> ResponseEntity<?> from(
      Result<T> result, SpringHttpResultMapperWithContext mapper, RequestContext requestContext) {
    String correlationId = requestContext.correlationId().orElse(null);
    String tenantId = requestContext.tenantId().map(Object::toString).orElse(null);
    String actorId = requestContext.actor().map(Object::toString).orElse(null);

    var httpResp = HttpResultsWithContext.map(result, mapper, correlationId, tenantId, actorId);

    return ResponseEntity.status(httpResp.status()).body(httpResp.body());
  }
}
