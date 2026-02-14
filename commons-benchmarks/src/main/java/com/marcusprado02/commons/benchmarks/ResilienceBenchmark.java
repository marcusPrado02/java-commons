package com.marcusprado02.commons.benchmarks;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.openjdk.jmh.annotations.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmarks for resilience patterns (Circuit Breaker, Retry).
 * Tests performance overhead of resilience4j patterns.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class ResilienceBenchmark {

  private CircuitBreaker circuitBreaker;
  private Retry retry;
  private AtomicInteger counter;

  @Setup
  public void setup() {
    // Circuit breaker configuration
    CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .waitDurationInOpenState(Duration.ofMillis(1000))
        .slidingWindowSize(10)
        .build();

    circuitBreaker = CircuitBreaker.of("benchmark", circuitBreakerConfig);

    // Retry configuration
    RetryConfig retryConfig = RetryConfig.custom()
        .maxAttempts(3)
        .waitDuration(Duration.ofMillis(100))
        .build();

    retry = Retry.of("benchmark", retryConfig);

    counter = new AtomicInteger(0);
  }

  @Benchmark
  public int baselineOperation() {
    return successfulOperation();
  }

  @Benchmark
  public int circuitBreakerSuccessful() {
    return circuitBreaker.executeSupplier(this::successfulOperation);
  }

  @Benchmark
  public int retrySuccessful() {
    return retry.executeSupplier(this::successfulOperation);
  }

  @Benchmark
  public int circuitBreakerAndRetry() {
    return CircuitBreaker.decorateSupplier(
        circuitBreaker,
        Retry.decorateSupplier(retry, this::successfulOperation)
    ).get();
  }

  @Benchmark
  public int complexOperation() {
    // Simulate more complex business logic
    int result = counter.incrementAndGet();
    if (result % 2 == 0) {
      return result * 2;
    } else {
      return result + 10;
    }
  }

  @Benchmark
  public int circuitBreakerComplexOperation() {
    return circuitBreaker.executeSupplier(this::complexOperation);
  }

  private int successfulOperation() {
    return counter.incrementAndGet();
  }
}
