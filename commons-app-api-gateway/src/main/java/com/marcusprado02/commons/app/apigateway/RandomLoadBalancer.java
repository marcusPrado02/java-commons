package com.marcusprado02.commons.app.apigateway;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import java.util.Random;

/** Random load balancer implementation. */
final class RandomLoadBalancer implements LoadBalancer {

  private final Random random = new Random();

  @Override
  public Result<String> choose(List<String> instances) {
    if (instances == null || instances.isEmpty()) {
      return Result.fail(
          Problem.of(
              new ErrorCode("NO_INSTANCES"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "No backend instances available"));
    }

    int index = random.nextInt(instances.size());
    return Result.ok(instances.get(index));
  }
}
