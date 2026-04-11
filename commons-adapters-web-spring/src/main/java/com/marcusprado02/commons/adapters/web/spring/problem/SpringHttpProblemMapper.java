package com.marcusprado02.commons.adapters.web.spring.problem;

import com.marcusprado02.commons.adapters.web.problem.HttpProblemMapper;
import com.marcusprado02.commons.adapters.web.problem.HttpProblemResponse;
import com.marcusprado02.commons.kernel.errors.Problem;
import java.util.Map;

/**
 * Maps {@link com.marcusprado02.commons.kernel.errors.Problem} instances to HTTP problem responses.
 */
public final class SpringHttpProblemMapper implements HttpProblemMapper {

  @Override
  @SuppressWarnings("checkstyle:indentation")
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
        Map.of(
            "category", problem.category().name(),
            "severity", problem.severity().name()),
        null);
  }
}
