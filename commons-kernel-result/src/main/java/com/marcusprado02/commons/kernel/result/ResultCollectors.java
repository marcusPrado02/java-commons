package com.marcusprado02.commons.kernel.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Utility collectors for aggregating lists of {@link Result} values. */
public final class ResultCollectors {

  private ResultCollectors() {}

  /**
   * Joins a list of {@code Result<Void>}.
   *
   * <p>Returns Ok if all are Ok, or the first failure encountered.
   *
   * @param results list of results to join
   * @return first failure, or Ok if all succeed
   */
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
   * Joins a list of {@code Result<T>}.
   *
   * <p>If all are Ok, returns a list of values. If any fail, returns the first failure.
   *
   * @param results list of results to join
   * @param <T> value type
   * @return list of values or the first failure
   */
  public static <T> Result<List<T>> allOrFirstFailure(List<Result<T>> results) {
    Objects.requireNonNull(results, "results");
    List<T> values = new ArrayList<>(results.size());
    for (Result<T> r : results) {
      if (r == null) {
        continue;
      }
      if (r.isFail()) {
        return Result.fail(r.problemOrNull());
      }
      values.add(r.getOrNull());
    }
    return Result.ok(List.copyOf(values));
  }
}
