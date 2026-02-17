package com.marcusprado02.commons.app.apigateway;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import java.util.Random;

/** Weighted random load balancer implementation. */
final class WeightedRandomLoadBalancer implements LoadBalancer {

  private final List<Integer> weights;
  private final Random random = new Random();

  WeightedRandomLoadBalancer(List<Integer> weights) {
    this.weights = List.copyOf(weights);
  }

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

    if (weights.size() != instances.size()) {
      return Result.fail(
          Problem.of(
              new ErrorCode("WEIGHT_MISMATCH"),
              ErrorCategory.BUSINESS,
              Severity.ERROR,
              "Weights list size must match instances list size"));
    }

    int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
    if (totalWeight <= 0) {
      return Result.fail(
          Problem.of(
              new ErrorCode("INVALID_WEIGHTS"),
              ErrorCategory.BUSINESS,
              Severity.ERROR,
              "Total weight must be greater than zero"));
    }

    int randomWeight = random.nextInt(totalWeight);
    int currentWeight = 0;

    for (int i = 0; i < instances.size(); i++) {
      currentWeight += weights.get(i);
      if (randomWeight < currentWeight) {
        return Result.ok(instances.get(i));
      }
    }

    // Fallback (should never reach here)
    return Result.ok(instances.get(instances.size() - 1));
  }
}
