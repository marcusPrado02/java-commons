# Commons Benchmarks

Performance benchmarks using JMH (Java Microbenchmark Harness) for critical components of the Commons Platform.

## Overview

This module contains microbenchmarks to measure and track performance of core Commons Platform components:

- **Serialization**: JSON serialization/deserialization with Jackson
- **Resilience**: Circuit Breaker and Retry patterns overhead
- **Result Pattern**: Result<T> monad operations performance
- **Repository**: In-memory repository CRUD operations

## Running Benchmarks

### Build Benchmark JAR

```bash
cd /path/to/java-commons
./mvnw clean package -pl commons-benchmarks -am
```

This creates an uber JAR at `commons-benchmarks/target/benchmarks.jar`.

### Run All Benchmarks

```bash
java -jar commons-benchmarks/target/benchmarks.jar
```

### Run Specific Benchmark

```bash
# Run only serialization benchmarks
java -jar commons-benchmarks/target/benchmarks.jar SerializationBenchmark

# Run only resilience benchmarks
java -jar commons-benchmarks/target/benchmarks.jar ResilienceBenchmark

# Run only Result benchmarks
java -jar commons-benchmarks/target/benchmarks.jar ResultBenchmark

# Run only repository benchmarks
java -jar commons-benchmarks/target/benchmarks.jar RepositoryBenchmark
```

### Run Specific Test Method

```bash
# Run only simple object serialization
java -jar commons-benchmarks/target/benchmarks.jar SerializationBenchmark.serializeSimpleObject

# Run only circuit breaker benchmarks
java -jar commons-benchmarks/target/benchmarks.jar ResilienceBenchmark.circuitBreaker
```

### Custom JMH Options

```bash
# Run with specific iterations
java -jar commons-benchmarks/target/benchmarks.jar -i 10 -wi 3

# Run with specific forks
java -jar commons-benchmarks/target/benchmarks.jar -f 3

# Output results to JSON
java -jar commons-benchmarks/target/benchmarks.jar -rf json -rff results.json

# Run with profiler (Linux perf)
java -jar commons-benchmarks/target/benchmarks.jar -prof perfasm

# List available profilers
java -jar commons-benchmarks/target/benchmarks.jar -lprof
```

## Benchmark Results Interpretation

### Time Units

- Most benchmarks use **nanoseconds (ns)** or **microseconds (μs)** as the time unit
- Lower is better
- Results show average time per operation

### Output Format

```
Benchmark                                   Mode  Cnt     Score     Error  Units
SerializationBenchmark.serializeSimpleObject avgt    5   1234.567 ± 45.678  ns/op
```

- **Mode**: avgt = Average Time
- **Cnt**: Number of measurement iterations
- **Score**: Average time per operation
- **Error**: Confidence interval
- **Units**: Time unit (ns/op = nanoseconds per operation)

### What to Look For

1. **Baseline Comparisons**: Compare with and without resilience patterns to see overhead
2. **Trends**: Track changes over time as code evolves
3. **Hotspots**: Identify slow operations that need optimization
4. **Regressions**: Detect performance degradation in new releases

## Available Benchmarks

### SerializationBenchmark

Tests Jackson JSON serialization performance:

- `serializeSimpleObject` - Serialize simple object to JSON
- `deserializeSimpleObject` - Deserialize JSON to simple object
- `serializeComplexObject` - Serialize complex nested object
- `deserializeComplexObject` - Deserialize complex nested object
- `serializeToBytes` - Serialize to byte array

### ResilienceBenchmark

Tests Resilience4j pattern overhead:

- `baselineOperation` - Baseline without any resilience
- `circuitBreakerSuccessful` - Circuit breaker on successful operation
- `retrySuccessful` - Retry on successful operation
- `circuitBreakerAndRetry` - Combined circuit breaker + retry
- `complexOperation` - More complex business logic baseline
- `circuitBreakerComplexOperation` - Circuit breaker on complex logic

### ResultBenchmark

Tests Result<T> monad performance:

- `createSuccess` - Create successful Result
- `createFailure` - Create failed Result
- `mapSuccess` - Map transformation on success
- `mapFailure` - Map transformation on failure
- `flatMapSuccess` - FlatMap on success
- `flatMapFailure` - FlatMap on failure
- `chainedOperations` - Chain multiple operations
- `errorRecovery` - Error recovery pattern

### RepositoryBenchmark

Tests repository operations:

- `saveOperation` - Save entity
- `findByIdExisting` - Find existing entity by ID
- `findByIdNonExisting` - Find non-existing entity
- `findAll` - Retrieve all entities
- `findByName` - Query by name field
- `findByGroup` - Query by group field
- `deleteOperation` - Delete entity
- `existsOperation` - Check entity existence

## JMH Configuration

Benchmarks use these default settings (configurable via annotations):

- **Mode**: Average Time (how long operation takes on average)
- **Time Unit**: Nanoseconds or Microseconds
- **Warmup Iterations**: 3 iterations of 1 second each
- **Measurement Iterations**: 5 iterations of 1 second each
- **Forks**: 1 (separate JVM process)

## Best Practices

### When to Run Benchmarks

1. **Before Release**: Ensure no performance regressions
2. **After Major Changes**: Validate performance impact
3. **Optimization Work**: Measure improvements
4. **Continuous Monitoring**: Track trends over time

### Tips for Accurate Results

1. **Close Background Applications**: Reduce system noise
2. **Run Multiple Times**: Check consistency of results
3. **Use Realistic Data**: Benchmark with production-like data sizes
4. **Check GC Impact**: Monitor garbage collection overhead
5. **Warm Up JVM**: Let JIT compiler optimize (done automatically by JMH)

### CI/CD Integration

Store benchmark results over time to track performance trends:

```bash
# Run benchmarks and save results
java -jar benchmarks.jar -rf json -rff results/benchmark-$(date +%Y%m%d).json

# Compare with previous results
jmh-compare results/benchmark-20260214.json results/benchmark-20260213.json
```

## Profiling

JMH supports various profilers to identify performance bottlenecks:

```bash
# GC profiler
java -jar benchmarks.jar -prof gc

# Stack profiler
java -jar benchmarks.jar -prof stack

# Class loader profiler
java -jar benchmarks.jar -prof cl

# Compiler profiler
java -jar benchmarks.jar -prof comp
```

## Adding New Benchmarks

1. Create a new class in `com.marcusprado02.commons.benchmarks`
2. Annotate with JMH annotations:
   ```java
   @BenchmarkMode(Mode.AverageTime)
   @OutputTimeUnit(TimeUnit.NANOSECONDS)
   @State(Scope.Thread)
   public class MyBenchmark {
     
     @Setup
     public void setup() {
       // Initialize test data
     }
     
     @Benchmark
     public void myOperation() {
       // Code to benchmark
     }
   }
   ```
3. Rebuild: `./mvnw clean package -pl commons-benchmarks`
4. Run: `java -jar commons-benchmarks/target/benchmarks.jar MyBenchmark`

## References

- [JMH Official Site](https://openjdk.org/projects/code-tools/jmh/)
- [JMH Samples](https://hg.openjdk.org/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)
- [Resilience4j Performance](https://resilience4j.readme.io/docs/performance)
- [Jackson Performance](https://github.com/FasterXML/jackson-docs/wiki/Performance)

## Continuous Performance Monitoring

Recommended approach for tracking performance over time:

1. Run benchmarks on every release
2. Store results in version control or artifact repository
3. Create dashboards showing trends
4. Set alerts for significant regressions (>10% degradation)
5. Document optimizations in CHANGELOG.md

## License

See root project LICENSE file.
