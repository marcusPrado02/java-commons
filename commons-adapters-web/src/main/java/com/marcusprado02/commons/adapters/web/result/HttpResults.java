package com.marcusprado02.commons.adapters.web.result;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Objects;

public final class HttpResults {

  private HttpResults() {}

  public static <T> HttpResultResponse map(Result<T> result, HttpResultMapper mapper) {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(mapper, "mapper");

    if (result.isOk()) {
      @SuppressWarnings("unchecked")
      Result.Ok<T> ok = (Result.Ok<T>) result;
      return mapper.mapOk(ok);
    }

    return mapper.mapFail((Result.Fail<?>) result);
  }
}
