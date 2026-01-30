package com.marcusprado02.commons.adapters.web.spring.result;

import com.marcusprado02.commons.adapters.web.envelope.ApiEnvelopeWithContext;
import com.marcusprado02.commons.adapters.web.result.HttpResultMapperWithContext;
import com.marcusprado02.commons.adapters.web.result.HttpResultResponseWithContext;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.Map;

public final class SpringHttpResultMapperWithContext implements HttpResultMapperWithContext {

  @Override
  public <T> HttpResultResponseWithContext mapOk(
      Result.Ok<T> ok, String correlationId, String tenantId, String actorId) {

    ApiEnvelopeWithContext.Success<T> body =
        ApiEnvelopeWithContext.Success.of(ok.value(), correlationId, tenantId, actorId);

    return new HttpResultResponseWithContext(200, body);
  }

  @Override
  public HttpResultResponseWithContext mapFail(
      Result.Fail<?> fail, String correlationId, String tenantId, String actorId) {
    return mapProblem(fail.problem(), correlationId, tenantId, actorId);
  }

  @Override
  public HttpResultResponseWithContext mapProblem(
      Problem problem, String correlationId, String tenantId, String actorId) {

    ApiEnvelopeWithContext.Failure body =
        ApiEnvelopeWithContext.Failure.of(
            new ProblemResponse(problem), correlationId, tenantId, actorId);

    int status =
        switch (problem.category()) {
          case VALIDATION -> 400;
          case BUSINESS -> 422;
          case NOT_FOUND -> 404;
          case CONFLICT -> 409;
          case UNAUTHORIZED -> 401;
          case FORBIDDEN -> 403;
          case TECHNICAL -> 500;
        };

    return new HttpResultResponseWithContext(status, body);
  }

  private record ProblemResponse(String code, String message, Map<String, Object> details) {
    ProblemResponse(Problem problem) {
      this(
          problem.code().value(),
          problem.message(),
          Map.of(
              "category", problem.category().name(),
              "severity", problem.severity().name()));
    }
  }
}
