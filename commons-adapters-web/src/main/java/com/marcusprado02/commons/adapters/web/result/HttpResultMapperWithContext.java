package com.marcusprado02.commons.adapters.web.result;

import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.result.Result;

public interface HttpResultMapperWithContext {

  <T> HttpResultResponseWithContext mapOk(
      Result.Ok<T> ok, String correlationId, String tenantId, String actorId);

  HttpResultResponseWithContext mapFail(
      Result.Fail<?> fail, String correlationId, String tenantId, String actorId);

  HttpResultResponseWithContext mapProblem(
      Problem problem, String correlationId, String tenantId, String actorId);
}
