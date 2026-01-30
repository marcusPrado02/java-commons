package com.marcusprado02.commons.adapters.web.result;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Objects;

public final class HttpResultsWithContext {

  private HttpResultsWithContext() {}

  public static <T> HttpResultResponseWithContext map(
      Result<T> result,
      HttpResultMapperWithContext mapper,
      String correlationId,
      String tenantId,
      String actorId
  ) {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(mapper, "mapper");

    if (result.isOk()) {
      Result.Ok<T> ok = (Result.Ok<T>) result;
      return mapper.mapOk(ok, correlationId, tenantId, actorId);
    }

    return mapper.mapFail((Result.Fail<?>) result, correlationId, tenantId, actorId);
  }
}
