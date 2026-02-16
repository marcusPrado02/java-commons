# Commons App Batch

Application layer batch processing abstractions and patterns.

## Overview

`commons-app-batch` provides technology-agnostic abstractions for batch processing operations. It defines core interfaces for chunk-oriented processing with built-in support for retry policies, skip logic, and restart capabilities.

### Key Features

- **Chunk-Oriented Processing**: Process items in configurable batches
- **Retry Policies**: Automatic retry on transient failures
- **Skip Policies**: Skip failed items up to a configurable threshold
- **Restart Capabilities**: Resume from the last successful position
- **Technology-Agnostic**: Can be implemented with Spring Batch or custom solutions
- **Type-Safe Error Handling**: Result pattern for predictable error management
- **Progress Tracking**: Monitor job execution with detailed metrics

## Core Concepts

### Batch Job

A batch job represents a long-running operation that processes data in chunks:

```java
public class CustomerImportJob implements BatchJob<CustomerRecord, ImportResult> {

  private final ItemReader<CustomerRecord> reader;
  private final ItemProcessor<CustomerRecord, ValidatedCustomer> processor;
  private final ItemWriter<ValidatedCustomer> writer;

  @Override
  public String getJobId() {
    return "customer-import";
  }

  @Override
  public Result<BatchJobExecution<ImportResult>, BatchJobError> execute(
      BatchJobContext context) {

    ChunkProcessor<CustomerRecord, ValidatedCustomer> chunkProcessor =
        new SimpleChunkProcessor<>(reader, processor, writer, getChunkSize());

    long totalRead = 0;
    long totalWritten = 0;
    long totalSkipped = 0;

    while (chunkProcessor.hasMoreItems()) {
      Result<ChunkProcessingResult, BatchJobError> result =
          chunkProcessor.processChunk(context);

      if (result.isFailure()) {
        return Result.failure(result.error());
      }

      ChunkProcessingResult chunkResult = result.value();
      totalRead += chunkResult.getItemsRead();
      totalWritten += chunkResult.getItemsWritten();
      totalSkipped += chunkResult.getItemsSkipped();
    }

    BatchJobExecution<ImportResult> execution = BatchJobExecution.<ImportResult>builder()
        .executionId(context.getExecutionId())
        .status(BatchJobStatus.COMPLETED)
        .startTime(context.getStartTime())
        .endTime(Instant.now())
        .itemsRead(totalRead)
        .itemsWritten(totalWritten)
        .itemsSkipped(totalSkipped)
        .result(new ImportResult(totalWritten))
        .build();

    return Result.success(execution);
  }

  @Override
  public boolean isRestartable() {
    return true;
  }

  @Override
  public int getChunkSize() {
    return 100;
  }

  @Override
  public int getMaxRetries() {
    return 3;
  }

  @Override
  public int getMaxSkips() {
    return 10;
  }

  @Override
  public boolean shouldSkip(Exception exception) {
    return exception instanceof ValidationException;
  }

  @Override
  public boolean shouldRetry(Exception exception) {
    return exception instanceof TransientDataAccessException;
  }
}
```

### Item Reader

Reads items from a data source:

```java
public class CsvFileReader implements ItemReader<CustomerRecord> {

  private final BufferedReader reader;
  private long position = 0;
  private boolean exhausted = false;

  public CsvFileReader(Path filePath) throws IOException {
    this.reader = Files.newBufferedReader(filePath);
  }

  @Override
  public Result<Optional<CustomerRecord>, BatchJobError> read() {
    if (exhausted) {
      return Result.success(Optional.empty());
    }

    try {
      String line = reader.readLine();
      if (line == null) {
        exhausted = true;
        return Result.success(Optional.empty());
      }

      position++;
      CustomerRecord record = parseLine(line);
      return Result.success(Optional.of(record));

    } catch (IOException e) {
      return Result.failure(BatchJobError.of(
          BatchJobError.ITEM_READ_FAILED,
          "Failed to read line " + position,
          e));
    }
  }

  @Override
  public long getCurrentPosition() {
    return position;
  }

  @Override
  public void seekTo(long position) {
    // Reset reader and skip to position
    // Implementation depends on data source
  }

  @Override
  public void close() {
    try {
      reader.close();
    } catch (IOException e) {
      // Log error
    }
  }

  private CustomerRecord parseLine(String line) {
    String[] fields = line.split(",");
    return new CustomerRecord(fields[0], fields[1], fields[2]);
  }
}
```

### Item Processor

Transforms or validates items:

```java
public class CustomerValidator implements ItemProcessor<CustomerRecord, ValidatedCustomer> {

  private final ValidationService validationService;

  @Override
  public Result<Optional<ValidatedCustomer>, BatchJobError> process(CustomerRecord item) {
    // Validate the record
    if (!validationService.isValidEmail(item.email())) {
      // Filter out invalid items
      return Result.success(Optional.empty());
    }

    if (!validationService.isValidPhone(item.phone())) {
      return Result.success(Optional.empty());
    }

    // Transform to validated customer
    ValidatedCustomer validated = new ValidatedCustomer(
        item.name(),
        item.email(),
        item.phone(),
        validationService.normalizePhone(item.phone())
    );

    return Result.success(Optional.of(validated));
  }
}
```

### Item Writer

Writes processed items in chunks:

```java
public class CustomerDatabaseWriter implements ItemWriter<ValidatedCustomer> {

  private final CustomerRepository repository;

  @Override
  public Result<Void, BatchJobError> write(List<ValidatedCustomer> items) {
    try {
      repository.saveAll(items);
      return Result.success(null);

    } catch (DataAccessException e) {
      return Result.failure(BatchJobError.of(
          BatchJobError.ITEM_WRITE_FAILED,
          "Failed to write " + items.size() + " customers",
          e));
    }
  }

  @Override
  public void close() {
    // Clean up resources if needed
  }
}
```

### Chunk Processor

Orchestrates reading, processing, and writing:

```java
public class SimpleChunkProcessor<I, O> implements ChunkProcessor<I, O> {

  private final ItemReader<I> reader;
  private final ItemProcessor<I, O> processor;
  private final ItemWriter<O> writer;
  private final int chunkSize;
  private boolean hasMore = true;

  public SimpleChunkProcessor(
      ItemReader<I> reader,
      ItemProcessor<I, O> processor,
      ItemWriter<O> writer,
      int chunkSize) {
    this.reader = reader;
    this.processor = processor;
    this.writer = writer;
    this.chunkSize = chunkSize;
  }

  @Override
  public Result<ChunkProcessingResult, BatchJobError> processChunk(BatchJobContext context) {
    List<O> processedItems = new ArrayList<>();
    int itemsRead = 0;
    int itemsFiltered = 0;
    int itemsSkipped = 0;

    // Read chunk
    for (int i = 0; i < chunkSize && hasMore; i++) {
      Result<Optional<I>, BatchJobError> readResult = reader.read();

      if (readResult.isFailure()) {
        return Result.failure(readResult.error());
      }

      Optional<I> itemOpt = readResult.value();
      if (itemOpt.isEmpty()) {
        hasMore = false;
        break;
      }

      itemsRead++;

      // Process item
      Result<Optional<O>, BatchJobError> processResult = processor.process(itemOpt.get());

      if (processResult.isFailure()) {
        return Result.failure(processResult.error());
      }

      Optional<O> processedOpt = processResult.value();
      if (processedOpt.isEmpty()) {
        itemsFiltered++;
      } else {
        processedItems.add(processedOpt.get());
      }
    }

    // Write chunk
    if (!processedItems.isEmpty()) {
      Result<Void, BatchJobError> writeResult = writer.write(processedItems);
      if (writeResult.isFailure()) {
        return Result.failure(writeResult.error());
      }
    }

    ChunkProcessingResult result = ChunkProcessingResult.builder()
        .itemsRead(itemsRead)
        .itemsProcessed(itemsRead - itemsFiltered)
        .itemsFiltered(itemsFiltered)
        .itemsWritten(processedItems.size())
        .itemsSkipped(itemsSkipped)
        .hasMoreItems(hasMore)
        .build();

    return Result.success(result);
  }

  @Override
  public int getChunkSize() {
    return chunkSize;
  }

  @Override
  public boolean hasMoreItems() {
    return hasMore;
  }

  @Override
  public void close() {
    reader.close();
    writer.close();
  }
}
```

## Restart Capabilities

Jobs can be restarted from the last successful position:

```java
public class RestartableFileReader implements ItemReader<String> {

  private final BufferedReader reader;
  private long position = 0;

  @Override
  public Result<Optional<String>, BatchJobError> read() {
    // Read implementation
  }

  @Override
  public long getCurrentPosition() {
    return position;
  }

  @Override
  public void seekTo(long targetPosition) {
    try {
      // Reset to beginning
      reader.reset();
      position = 0;

      // Skip to target position
      while (position < targetPosition) {
        reader.readLine();
        position++;
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to seek to position " + targetPosition, e);
    }
  }
}

// When restarting a job
BatchJobContext context = BatchJobContext.builder()
    .jobId("customer-import")
    .executionId(UUID.randomUUID().toString())
    .isRestart(true)
    .restartFromPosition(15000L)  // Resume from item 15000
    .build();

// Reader will skip to position 15000
if (context.isRestart()) {
  context.getRestartFromPosition()
      .ifPresent(reader::seekTo);
}

Result<BatchJobExecution<ImportResult>, BatchJobError> result =
    job.execute(context);
```

## Retry and Skip Policies

Configure how to handle errors:

```java
public class CustomerImportJob implements BatchJob<CustomerRecord, ImportResult> {

  @Override
  public int getMaxRetries() {
    return 3;  // Retry up to 3 times
  }

  @Override
  public int getMaxSkips() {
    return 100;  // Skip up to 100 items before failing
  }

  @Override
  public boolean shouldRetry(Exception exception) {
    // Retry on transient errors
    return exception instanceof TemporaryDatabaseException
        || exception instanceof NetworkException;
  }

  @Override
  public boolean shouldSkip(Exception exception) {
    // Skip validation errors
    return exception instanceof ValidationException
        || exception instanceof MalformedDataException;
  }
}
```

## Job Execution Context

Access execution metadata and parameters:

```java
BatchJobContext context = BatchJobContext.builder()
    .jobId("customer-import")
    .executionId("exec-123")
    .parameters(Map.of(
        "inputFile", "/data/customers.csv",
        "outputFormat", "JSON",
        "strict", true
    ))
    .build();

// In job execution
String inputFile = context.getParameter("inputFile", "/default/path.csv");
boolean strict = context.getParameter("strict", false);

if (context.isRestart()) {
  long resumeFrom = context.getRestartFromPosition().orElse(0L);
  // Resume from position
}
```

## Job Status and Metrics

Track job execution progress:

```java
BatchJobExecution<ImportResult> execution = BatchJobExecution.<ImportResult>builder()
    .executionId(context.getExecutionId())
    .status(BatchJobStatus.COMPLETED)
    .startTime(context.getStartTime())
    .endTime(Instant.now())
    .itemsRead(50000)
    .itemsProcessed(48500)
    .itemsSkipped(1500)
    .itemsWritten(48500)
    .result(new ImportResult(48500))
    .build();

System.out.println("Duration: " + execution.getDuration());
System.out.println("Success: " + execution.isSuccessful());
System.out.println("Items written: " + execution.getItemsWritten());
```

## Error Handling

All batch errors use the Result pattern:

```java
Result<BatchJobExecution<ImportResult>, BatchJobError> result = job.execute(context);

result.match(
    execution -> {
      System.out.println("Job completed successfully");
      System.out.println("Processed: " + execution.getItemsWritten());
    },
    error -> {
      System.err.println("Job failed: " + error.message());
      if (error.getCause() != null) {
        error.getCause().printStackTrace();
      }
    }
);
```

Common error codes:
- `BATCH_JOB_INIT_FAILED` - Job initialization failed
- `BATCH_ITEM_READ_FAILED` - Failed to read item
- `BATCH_ITEM_PROCESS_FAILED` - Failed to process item
- `BATCH_ITEM_WRITE_FAILED` - Failed to write items
- `BATCH_MAX_SKIPS_EXCEEDED` - Too many items skipped
- `BATCH_MAX_RETRIES_EXCEEDED` - Retry limit exceeded
- `BATCH_JOB_INTERRUPTED` - Job was interrupted
- `BATCH_RESTART_FAILED` - Failed to restart job

## Best Practices

### 1. Choose Appropriate Chunk Size

```java
@Override
public int getChunkSize() {
  // Balance between performance and memory
  // Larger chunks = better performance but more memory
  // Smaller chunks = more granular restart but slower
  return 1000;  // Good default for most use cases
}
```

### 2. Make Readers and Writers Idempotent

```java
public class IdempotentWriter implements ItemWriter<Customer> {

  @Override
  public Result<Void, BatchJobError> write(List<Customer> items) {
    // Use upsert or check for duplicates
    repository.upsertAll(items);
    return Result.success(null);
  }
}
```

### 3. Handle Restarts Properly

```java
// Store checkpoint information
public class CheckpointingReader implements ItemReader<String> {

  @Override
  public long getCurrentPosition() {
    return position;
  }

  @Override
  public void seekTo(long position) {
    // Efficiently skip to position
    this.position = position;
  }
}
```

### 4. Use Skip for Business Validation

```java
@Override
public boolean shouldSkip(Exception exception) {
  // Skip business validation errors
  return exception instanceof ValidationException;
}

@Override
public boolean shouldRetry(Exception exception) {
  // Retry infrastructure errors
  return exception instanceof DatabaseException;
}
```

### 5. Monitor Job Progress

```java
ChunkProcessingResult result = chunkProcessor.processChunk(context);
if (result.getItemsSkipped() > threshold) {
  logger.warn("High skip rate: {}", result.getItemsSkipped());
}
```

## Dependencies

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-app-batch</artifactId>
</dependency>
```

Optional Spring Batch integration:
```xml
<dependency>
  <groupId>org.springframework.batch</groupId>
  <artifactId>spring-batch-core</artifactId>
</dependency>
```

## Integration with Spring Batch

This module can be used alongside Spring Batch for more advanced features:

```java
@Configuration
public class BatchConfiguration {

  @Bean
  public Job customerImportJob(JobRepository jobRepository, Step importStep) {
    return new JobBuilder("customerImport", jobRepository)
        .start(importStep)
        .build();
  }

  @Bean
  public Step importStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ItemReader<CustomerRecord> reader,
      ItemProcessor<CustomerRecord, ValidatedCustomer> processor,
      ItemWriter<ValidatedCustomer> writer) {

    return new StepBuilder("import", jobRepository)
        .<CustomerRecord, ValidatedCustomer>chunk(100, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .faultTolerant()
        .skipLimit(10)
        .skip(ValidationException.class)
        .retryLimit(3)
        .retry(TransientDataAccessException.class)
        .build();
  }
}
```

## See Also

- [commons-kernel-result](../commons-kernel-result/README.md) - Result type for error handling
- [commons-kernel-errors](../commons-kernel-errors/README.md) - Domain error abstractions
- [Spring Batch Documentation](https://docs.spring.io/spring-batch/docs/current/reference/html/)
