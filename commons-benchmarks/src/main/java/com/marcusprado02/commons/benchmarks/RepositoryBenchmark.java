package com.marcusprado02.commons.benchmarks;

import com.marcusprado02.commons.adapters.persistence.inmemory.InMemoryPageableRepository;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Benchmarks for repository operations.
 * Tests performance of CRUD operations and queries.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class RepositoryBenchmark {

  private TestRepository repository;
  private List<TestEntity> testEntities;
  private String existingId;

  @Setup
  public void setup() {
    repository = new TestRepository();

    // Pre-populate with 1000 entities
    testEntities = IntStream.range(0, 1000)
        .mapToObj(i -> new TestEntity(
            UUID.randomUUID().toString(),
            "Entity-" + i,
            i % 100 // Group entities for search testing
        ))
        .toList();

    testEntities.forEach(entity -> repository.save(entity));
    existingId = testEntities.get(500).getId();
  }

  @Benchmark
  public TestEntity saveOperation() {
    TestEntity newEntity = new TestEntity(
        UUID.randomUUID().toString(),
        "New Entity",
        999
    );
    return repository.save(newEntity);
  }

  @Benchmark
  public Optional<TestEntity> findByIdExisting() {
    return repository.findById(existingId);
  }

  @Benchmark
  public Optional<TestEntity> findByIdNonExisting() {
    return repository.findById("non-existing-id");
  }

  @Benchmark
  public List<TestEntity> findAll() {
    return repository.findAll();
  }

  @Benchmark
  public List<TestEntity> findByName() {
    return repository.findByName("Entity-500");
  }

  @Benchmark
  public List<TestEntity> findByGroup() {
    return repository.findByGroup(50);
  }

  @Benchmark
  public void deleteOperation() {
    // Delete and re-add to keep data consistent
    String idToDelete = testEntities.get(0).getId();
    repository.deleteById(idToDelete);

    // Re-add for next iteration
    repository.save(testEntities.get(0));
  }

  @Benchmark
  public boolean existsOperation() {
    return repository.findById(existingId).isPresent();
  }

  // Test entity
  public static class TestEntity {
    private final String id;
    private final String name;
    private final int group;

    public TestEntity(String id, String name, int group) {
      this.id = id;
      this.name = name;
      this.group = group;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public int getGroup() {
      return group;
    }
  }

  // Test repository
  public static class TestRepository extends InMemoryPageableRepository<TestEntity, String> {

    public TestRepository() {
      super(TestEntity::getId);
    }

    public List<TestEntity> findAll() {
      return List.copyOf(storage.values());
    }

    public List<TestEntity> findByName(String name) {
      return findAll().stream()
          .filter(e -> e.getName().equals(name))
          .toList();
    }

    public List<TestEntity> findByGroup(int group) {
      return findAll().stream()
          .filter(e -> e.getGroup() == group)
          .toList();
    }
  }
}
