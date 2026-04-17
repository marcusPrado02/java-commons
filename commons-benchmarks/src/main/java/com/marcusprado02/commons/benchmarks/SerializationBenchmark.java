package com.marcusprado02.commons.benchmarks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
 * Benchmarks for JSON serialization/deserialization using Jackson. Tests performance of different
 * JSON processing scenarios.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class SerializationBenchmark {

  private ObjectMapper objectMapper;
  private SimpleObject simpleObject;
  private ComplexObject complexObject;
  private String simpleJson;
  private String complexJson;

  /** Initializes Jackson ObjectMapper and pre-builds test objects and their JSON strings. */
  @Setup
  public void setup() throws Exception {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    // Simple object
    simpleObject = new SimpleObject("test-id", "Test Name", 42);
    simpleJson = objectMapper.writeValueAsString(simpleObject);

    // Complex nested object
    complexObject =
        new ComplexObject(
            "complex-id",
            "Complex Name",
            Instant.now(),
            List.of("tag1", "tag2", "tag3"),
            Map.of("key1", "value1", "key2", "value2"),
            new NestedObject("nested-1", 100),
            List.of(new NestedObject("nested-2", 200), new NestedObject("nested-3", 300)));
    complexJson = objectMapper.writeValueAsString(complexObject);
  }

  @Benchmark
  public String serializeSimpleObject() throws Exception {
    return objectMapper.writeValueAsString(simpleObject);
  }

  @Benchmark
  public SimpleObject deserializeSimpleObject() throws Exception {
    return objectMapper.readValue(simpleJson, SimpleObject.class);
  }

  @Benchmark
  public String serializeComplexObject() throws Exception {
    return objectMapper.writeValueAsString(complexObject);
  }

  @Benchmark
  public ComplexObject deserializeComplexObject() throws Exception {
    return objectMapper.readValue(complexJson, ComplexObject.class);
  }

  @Benchmark
  public byte[] serializeToBytes() throws Exception {
    return objectMapper.writeValueAsBytes(complexObject);
  }

  /** Simple flat object used as benchmark input. */
  public record SimpleObject(String id, String name, int value) {}

  /** Complex nested object with collections used as benchmark input. */
  public record ComplexObject(
      String id,
      String name,
      Instant timestamp,
      List<String> tags,
      Map<String, String> metadata,
      NestedObject nested,
      List<NestedObject> items) {}

  /** Nested object embedded inside {@link ComplexObject}. */
  public record NestedObject(String id, int count) {}
}
