package com.marcusprado02.commons.benchmarks;

import com.marcusprado02.commons.app.ratelimiting.RateLimitConfig;
import com.marcusprado02.commons.app.ratelimiting.RateLimitResult;
import com.marcusprado02.commons.app.ratelimiting.RateLimiter;
import com.marcusprado02.commons.app.ratelimiting.RateLimiterFactory;
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
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for the in-memory token bucket rate limiter.
 *
 * <p>Measures:
 *
 * <ul>
 *   <li>Single-key {@code tryConsume} throughput
 *   <li>Multi-key fan-out (simulating per-user rate limiting)
 *   <li>High-load scenario where most calls are rejected
 * </ul>
 *
 * <p>To run:
 *
 * <pre>{@code
 * mvn package -pl commons-benchmarks -am -DskipTests
 * java -jar commons-benchmarks/target/benchmarks.jar RateLimiterBenchmark
 * }</pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class RateLimiterBenchmark {

  private RateLimiter unlimitedLimiter;
  private RateLimiter restrictedLimiter;
  private int keyCounter;

  /** Creates a high-capacity and a restricted rate limiter for the benchmark scenarios. */
  @Setup
  public void setup() {
    // High-capacity limiter — nearly all requests succeed
    unlimitedLimiter =
        RateLimiterFactory.inMemory().withConfig(RateLimitConfig.perSecond(1_000_000)).build();

    // Restricted limiter — most requests will be rejected (1 req/sec)
    restrictedLimiter =
        RateLimiterFactory.inMemory().withConfig(RateLimitConfig.perSecond(1)).build();

    keyCounter = 0;
  }

  /**
   * Benchmark: tryConsume on a single key with a high-capacity limiter. Models the happy path —
   * nearly all tokens are available.
   */
  @Benchmark
  public RateLimitResult singleKey_highCapacity(Blackhole bh) {
    return unlimitedLimiter.tryConsume("benchmark-key");
  }

  /**
   * Benchmark: tryConsume with fan-out across many distinct keys. Each call uses a different key
   * (round-robin over 1000 keys), simulating per-user rate limiting at scale.
   */
  @Benchmark
  public RateLimitResult multiKey_fanOut(Blackhole bh) {
    String key = "user-" + (keyCounter++ % 1000);
    return unlimitedLimiter.tryConsume(key);
  }

  /**
   * Benchmark: tryConsume on a heavily throttled limiter. Measures rejection overhead when the
   * bucket is exhausted.
   */
  @Benchmark
  public RateLimitResult singleKey_heavilyThrottled(Blackhole bh) {
    return restrictedLimiter.tryConsume("throttled-key");
  }

  /**
   * Benchmark: tryConsume with burst consumption (2 tokens at once). Measures cost of multi-token
   * requests.
   */
  @Benchmark
  public RateLimitResult singleKey_burstConsumption(Blackhole bh) {
    return unlimitedLimiter.tryConsume("burst-key", 2);
  }
}
