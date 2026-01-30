package com.marcusprado02.commons.adapters.web.result;

import com.marcusprado02.commons.adapters.web.problem.HttpProblemResponse;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.result.Result;

/**
 * Maps {@link Result} to a framework-agnostic HTTP representation.
 *
 * <p>Spring/JAX-RS/etc adapters should implement this interface.
 */
public interface HttpResultMapper {

  <T> HttpResultResponse mapOk(Result.Ok<T> ok);

  HttpResultResponse mapFail(Result.Fail<?> fail);

  HttpProblemResponse mapProblem(Problem problem);
}
