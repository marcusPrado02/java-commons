package com.marcusprado02.commons.benchmarks;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmarks for {@code Result<T>} pattern operations. Tests performance of Result creation,
 * mapping, and error handling.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class ResultBenchmark {

  private Result<Integer> successResult;
  private Result<Integer> failureResult;
  private Problem problem;

  @Setup
  public void setup() {
    successResult = Result.ok(42);

    problem =
        Problem.of(
            ErrorCode.of("TEST_ERROR"),
            ErrorCategory.TECHNICAL,
            Severity.ERROR,
            "Test error message");

    failureResult = Result.fail(problem);
  }

  @Benchmark
  public Result<Integer> createSuccess() {
    return Result.ok(42);
  }

  @Benchmark
  public Result<Integer> createFailure() {
    return Result.fail(problem);
  }

  @Benchmark
  public Result<String> mapSuccess() {
    return successResult.map(value -> "Value: " + value);
  }

  @Benchmark
  public Result<String> mapFailure() {
    return failureResult.map(value -> "Value: " + value);
  }

  @Benchmark
  public Result<Integer> flatMapSuccess() {
    return successResult.flatMap(value -> Result.ok(value * 2));
  }

  @Benchmark
  public Result<Integer> flatMapFailure() {
    return failureResult.flatMap(value -> Result.ok(value * 2));
  }

  @Benchmark
  public Integer getOrElseSuccess() {
    return successResult.getOrElse(0);
  }

  @Benchmark
  public Integer getOrElseFailure() {
    return failureResult.getOrElse(0);
  }

  @Benchmark
  public boolean isSuccessCheck() {
    return successResult.isOk();
  }

  @Benchmark
  public boolean isFailureCheck() {
    return failureResult.isFail();
  }

  @Benchmark
  public Result<Integer> chainedOperations() {
    return Result.ok(10).map(v -> v * 2).flatMap(v -> Result.ok(v + 5)).map(v -> v * 3);
  }

  @Benchmark
  public Result<Integer> errorRecovery() {
    return failureResult.recover(error -> 0);
  }
}
