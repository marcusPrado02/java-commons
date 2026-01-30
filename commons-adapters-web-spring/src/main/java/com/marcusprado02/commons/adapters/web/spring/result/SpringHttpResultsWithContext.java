package com.marcusprado02.commons.adapters.web.spring.result;

import com.marcusprado02.commons.adapters.web.result.HttpResultsWithContext;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.platform.context.RequestContext;
import org.springframework.http.ResponseEntity;

public final class SpringHttpResultsWithContext {

  private SpringHttpResultsWithContext() {}

  public static <T> ResponseEntity<?> from(
      Result<T> result, SpringHttpResultMapperWithContext mapper, RequestContext requestContext) {
    String correlationId = requestContext.correlationId().orElse(null);
    String tenantId = requestContext.tenantId().map(Object::toString).orElse(null);
    String actorId = requestContext.actor().map(Object::toString).orElse(null);

    var httpResp = HttpResultsWithContext.map(result, mapper, correlationId, tenantId, actorId);

    return ResponseEntity.status(httpResp.status()).body(httpResp.body());
  }
}
