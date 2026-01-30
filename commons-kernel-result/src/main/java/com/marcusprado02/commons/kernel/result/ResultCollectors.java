package com.marcusprado02.commons.kernel.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ResultCollectors {

  private ResultCollectors() {}

  /** Join a list of Result<Void>. Returns Ok if all are Ok, returns the first failure if any. */
  public static Result<Void> firstFailure(List<Result<Void>> results) {
    Objects.requireNonNull(results, "results");
    for (Result<Void> r : results) {
      if (r != null && r.isFail()) {
        return Result.fail(r.problemOrNull());
      }
    }
    return Result.ok(null);
  }

  /**
   * Join a list of Result<T>. If all are Ok, returns a list of values. If any fail, returns the
   * first failure.
   */
  public static <T> Result<List<T>> allOrFirstFailure(List<Result<T>> results) {
    Objects.requireNonNull(results, "results");
    List<T> values = new ArrayList<>(results.size());
    for (Result<T> r : results) {
      if (r == null) continue;
      if (r.isFail()) return Result.fail(r.problemOrNull());
      values.add(r.getOrNull());
    }
    return Result.ok(List.copyOf(values));
  }
}
