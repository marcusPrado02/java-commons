package com.marcusprado02.commons.adapters.web.spring.result;

import com.marcusprado02.commons.adapters.web.envelope.ApiEnvelope;
import com.marcusprado02.commons.adapters.web.problem.HttpProblemResponse;
import com.marcusprado02.commons.adapters.web.result.HttpResultMapper;
import com.marcusprado02.commons.adapters.web.result.HttpResultResponse;
import com.marcusprado02.commons.adapters.web.spring.envelope.SpringApiMetaFactory;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.Map;

public final class SpringHttpResultMapper implements HttpResultMapper {

  @Override
  public <T> HttpResultResponse mapOk(Result.Ok<T> ok) {
    return new HttpResultResponse(
        200, ApiEnvelope.Success.of(ok.value(), SpringApiMetaFactory.current()));
  }

  @Override
  public HttpResultResponse mapFail(Result.Fail<?> fail) {
    HttpProblemResponse httpProblem = mapProblem(fail.problem());
    return new HttpResultResponse(
        httpProblem.status(), ApiEnvelope.Failure.of(httpProblem, SpringApiMetaFactory.current()));
  }

  @Override
  public HttpProblemResponse mapProblem(Problem problem) {
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

    return new HttpProblemResponse(
        status,
        problem.code().value(),
        problem.message(),
        Map.of(
            "category", problem.category().name(),
            "severity", problem.severity().name()),
        SpringApiMetaFactory.current());
  }
}
