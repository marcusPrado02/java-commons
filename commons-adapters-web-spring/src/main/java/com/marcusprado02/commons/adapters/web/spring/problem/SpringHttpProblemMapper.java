package com.marcusprado02.commons.adapters.web.spring.problem;

import com.marcusprado02.commons.adapters.web.problem.HttpProblemMapper;
import com.marcusprado02.commons.adapters.web.problem.HttpProblemResponse;
import com.marcusprado02.commons.kernel.errors.*;
import java.util.Map;

public final class SpringHttpProblemMapper implements HttpProblemMapper {

  @Override
  public HttpProblemResponse map(Problem problem) {
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
        Map.of("severity", problem.severity().name()));
  }
}
