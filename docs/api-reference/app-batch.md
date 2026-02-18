# API Reference: Batch Processing

## Visão Geral

`commons-app-batch` fornece framework para processamento em lote eficiente com suporte a particionamento, checkpoints, retry, processamento paralelo e monitoring.

**Quando usar:**
- Processar grandes volumes de dados
- ETL (Extract, Transform, Load)
- Relatórios e agregações
- Migrações de dados
- Limpeza e manutenção periódica

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-batch</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### BatchJob

Define um job de processamento em lote.

```java
public interface BatchJob<I, O> {
    
    /**
     * Nome único do job.
     */
    String name();
    
    /**
     * Lê próximo lote de itens.
     */
    List<I> read(BatchContext context);
    
    /**
     * Processa um item.
     */
    Result<O> process(I item, BatchContext context);
    
    /**
     * Escreve lote de resultados.
     */
    Result<Void> write(List<O> items, BatchContext context);
    
    /**
     * Configuração do batch.
     */
    default BatchConfiguration configuration() {
        return BatchConfiguration.defaults();
    }
}
```

### BatchExecutor

Executa jobs em lote.

```java
public interface BatchExecutor {
    
    /**
     * Executa batch job.
     */
    <I, O> Result<BatchExecutionResult> execute(BatchJob<I, O> job);
    
    /**
     * Executa job com parâmetros.
     */
    <I, O> Result<BatchExecutionResult> execute(
        BatchJob<I, O> job,
        Map<String, Object> parameters
    );
    
    /**
     * Para execução em andamento.
     */
    void stop(String jobName);
}
```

### BatchContext

Contexto de execução do batch.

```java
public class BatchContext {
    
    private final String jobName;
    private final String executionId;
    private final Map<String, Object> parameters;
    private final AtomicLong itemsProcessed;
    private final AtomicLong itemsFailed;
    
    /**
     * Total de itens processados até agora.
     */
    public long getItemsProcessed() {
        return itemsProcessed.get();
    }
    
    /**
     * Salva checkpoint.
     */
    public void checkpoint(Object state);
    
    /**
     * Recupera último checkpoint.
     */
    public Optional<Object> getLastCheckpoint();
}
```

---

## 💡 Uso Básico

### Simple Batch Job

```java
@Component
public class OrderCleanupJob implements BatchJob<Order, Void> {
    
    private final OrderRepository orderRepository;
    private final StructuredLog log;
    
    @Override
    public String name() {
        return "order-cleanup";
    }
    
    @Override
    public List<Order> read(BatchContext context) {
        // Buscar pedidos cancelados há mais de 30 dias
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        
        return orderRepository.findCancelledBefore(
            cutoff,
            100  // Batch size
        );
    }
    
    @Override
    public Result<Void> process(Order order, BatchContext context) {
        log.info("Deleting cancelled order")
            .field("orderId", order.id().value())
            .field("cancelledAt", order.cancelledAt())
            .log();
        
        try {
            orderRepository.delete(order.id());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(Problem.of(
                "ORDER.DELETE_FAILED",
                "Failed to delete order: " + e.getMessage()
            ));
        }
    }
    
    @Override
    public Result<Void> write(List<Void> items, BatchContext context) {
        // Nada para escrever (delete)
        return Result.ok();
    }
    
    @Override
    public BatchConfiguration configuration() {
        return BatchConfiguration.builder()
            .batchSize(100)
            .maxRetries(3)
            .skipOnError(true)
            .build();
    }
}
```

### Executing Batch

```java
@Service
public class BatchService {
    
    private final BatchExecutor batchExecutor;
    private final OrderCleanupJob cleanupJob;
    
    public Result<BatchExecutionResult> runCleanup() {
        return batchExecutor.execute(cleanupJob);
    }
}
```

---

## 🔄 Read-Process-Write Pattern

### ETL Job Example

```java
@Component
public class CustomerMigrationJob implements BatchJob<CustomerEntity, Customer> {
    
    private final LegacyCustomerRepository legacyRepo;
    private final CustomerRepository newRepo;
    private final CustomerMapper mapper;
    
    @Override
    public String name() {
        return "customer-migration";
    }
    
    @Override
    public List<CustomerEntity> read(BatchContext context) {
        // READ: Buscar da base legada
        Long lastId = (Long) context.getLastCheckpoint().orElse(0L);
        
        List<CustomerEntity> customers = legacyRepo.findBatch(
            lastId,
            1000  // Batch size
        );
        
        // Salvar checkpoint
        if (!customers.isEmpty()) {
            Long newLastId = customers.get(customers.size() - 1).getId();
            context.checkpoint(newLastId);
        }
        
        return customers;
    }
    
    @Override
    public Result<Customer> process(
        CustomerEntity entity,
        BatchContext context
    ) {
        // PROCESS: Transformar para novo modelo
        try {
            Customer customer = mapper.toDomain(entity);
            
            // Aplicar regras de negócio
            Result<Void> validation = customer.validate();
            if (validation.isFail()) {
                return Result.fail(validation.problemOrNull());
            }
            
            return Result.ok(customer);
            
        } catch (Exception e) {
            return Result.fail(Problem.of(
                "CUSTOMER.MAPPING_FAILED",
                "Failed to map customer: " + e.getMessage()
            ));
        }
    }
    
    @Override
    public Result<Void> write(List<Customer> customers, BatchContext context) {
        // WRITE: Salvar na nova base
        try {
            newRepo.saveAll(customers);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(Problem.of(
                "CUSTOMER.SAVE_FAILED",
                "Failed to save customers: " + e.getMessage()
            ));
        }
    }
    
    @Override
    public BatchConfiguration configuration() {
        return BatchConfiguration.builder()
            .batchSize(1000)
            .maxRetries(3)
            .skipOnError(false)  // Falha para tudo em erro
            .enableCheckpoint(true)
            .checkpointInterval(Duration.ofMinutes(5))
            .build();
    }
}
```

---

## 📊 Partitioning

### Partitioned Job

```java
@Component
public class ReportGenerationJob implements PartitionedBatchJob<Order, Report> {
    
    private final OrderRepository orderRepository;
    private final ReportGenerator reportGenerator;
    
    @Override
    public String name() {
        return "monthly-report";
    }
    
    @Override
    public List<Partition> createPartitions(BatchContext context) {
        YearMonth month = YearMonth.from(
            (LocalDate) context.getParameter("reportMonth")
        );
        
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        
        // Particionar por semana
        List<Partition> partitions = new ArrayList<>();
        LocalDate current = start;
        int partitionId = 0;
        
        while (current.isBefore(end)) {
            LocalDate weekEnd = current.plusDays(6);
            if (weekEnd.isAfter(end)) {
                weekEnd = end;
            }
            
            partitions.add(new Partition(
                partitionId++,
                Map.of(
                    "startDate", current,
                    "endDate", weekEnd
                )
            ));
            
            current = weekEnd.plusDays(1);
        }
        
        return partitions;
    }
    
    @Override
    public List<Order> read(Partition partition, BatchContext context) {
        LocalDate start = (LocalDate) partition.getParameter("startDate");
        LocalDate end = (LocalDate) partition.getParameter("endDate");
        
        return orderRepository.findByDateRange(start, end);
    }
    
    @Override
    public Result<Report> process(Order order, BatchContext context) {
        return reportGenerator.generateOrderReport(order);
    }
    
    @Override
    public Result<Void> write(
        List<Report> reports,
        Partition partition,
        BatchContext context
    ) {
        // Consolidar relatórios da partição
        Report consolidated = Report.consolidate(reports);
        
        // Salvar relatório da semana
        return reportGenerator.savePartitionReport(
            partition.getId(),
            consolidated
        );
    }
    
    @Override
    public BatchConfiguration configuration() {
        return BatchConfiguration.builder()
            .batchSize(500)
            .parallelPartitions(4)  // 4 threads
            .build();
    }
}
```

---

## 🔁 Checkpoint & Restart

### Checkpoint Strategy

```java
@Component
public class CheckpointManager {
    
    private final CheckpointRepository repository;
    
    public void saveCheckpoint(
        String jobName,
        String executionId,
        Object state
    ) {
        Checkpoint checkpoint = new Checkpoint(
            jobName,
            executionId,
            state,
            Instant.now()
        );
        
        repository.save(checkpoint);
    }
    
    public Optional<Checkpoint> getLastCheckpoint(
        String jobName,
        String executionId
    ) {
        return repository.findLatest(jobName, executionId);
    }
    
    public void clearCheckpoints(String jobName, String executionId) {
        repository.deleteByJobAndExecution(jobName, executionId);
    }
}
```

### Restartable Job

```java
@Component
public class DataSyncJob implements BatchJob<DataRecord, DataRecord> {
    
    @Override
    public List<DataRecord> read(BatchContext context) {
        // Recuperar último checkpoint
        Long lastId = context.getLastCheckpoint()
            .map(state -> ((CheckpointState) state).lastProcessedId())
            .orElse(0L);
        
        List<DataRecord> records = repository.findAfter(lastId, 1000);
        
        // Salvar novo checkpoint
        if (!records.isEmpty()) {
            Long newLastId = records.get(records.size() - 1).getId();
            context.checkpoint(new CheckpointState(newLastId));
        }
        
        return records;
    }
    
    @Override
    public Result<DataRecord> process(DataRecord record, BatchContext context) {
        // Processar...
        return Result.ok(record);
    }
    
    @Override
    public Result<Void> write(List<DataRecord> records, BatchContext context) {
        // Escrever...
        return Result.ok();
    }
    
    private record CheckpointState(Long lastProcessedId) {}
}
```

---

## ⚡ Parallel Processing

### Thread Pool Configuration

```java
@Configuration
public class BatchConfig {
    
    @Bean
    public BatchExecutor batchExecutor() {
        return BatchExecutor.builder()
            .threadPoolSize(10)
            .queueCapacity(1000)
            .keepAliveTime(Duration.ofMinutes(1))
            .build();
    }
}
```

### Parallel Job

```java
@Component
public class ImageProcessingJob implements BatchJob<Image, ProcessedImage> {
    
    @Override
    public String name() {
        return "image-processing";
    }
    
    @Override
    public List<Image> read(BatchContext context) {
        return imageRepository.findPendingProcessing(100);
    }
    
    @Override
    public Result<ProcessedImage> process(Image image, BatchContext context) {
        // Processamento pesado (redimensionar, otimizar, etc.)
        try {
            ProcessedImage processed = imageProcessor.process(image);
            return Result.ok(processed);
        } catch (Exception e) {
            return Result.fail(Problem.of(
                "IMAGE.PROCESSING_FAILED",
                e.getMessage()
            ));
        }
    }
    
    @Override
    public Result<Void> write(
        List<ProcessedImage> images,
        BatchContext context
    ) {
        return imageRepository.saveAll(images);
    }
    
    @Override
    public BatchConfiguration configuration() {
        return BatchConfiguration.builder()
            .batchSize(20)
            .parallelProcessing(true)  // Processar em paralelo
            .maxConcurrency(5)         // Máximo 5 threads
            .build();
    }
}
```

---

## 🎯 Error Handling

### Skip Strategy

```java
@Override
public BatchConfiguration configuration() {
    return BatchConfiguration.builder()
        .skipOnError(true)              // Continua mesmo com erros
        .skipLimit(100)                 // Máximo 100 erros
        .skipExceptions(                // Exceções para skip
            TimeoutException.class,
            TemporaryException.class
        )
        .build();
}
```

### Retry Strategy

```java
@Override
public BatchConfiguration configuration() {
    return BatchConfiguration.builder()
        .maxRetries(3)
        .retryDelay(Duration.ofSeconds(5))
        .retryBackoffMultiplier(2.0)
        .retryExceptions(
            NetworkException.class,
            DatabaseException.class
        )
        .build();
}
```

### Error Listener

```java
@Component
public class BatchErrorListener implements BatchExecutionListener {
    
    private final StructuredLog log;
    private final MetricsFacade metrics;
    
    @Override
    public void onItemError(
        String jobName,
        Object item,
        Throwable error,
        BatchContext context
    ) {
        log.error("Batch item processing failed")
            .field("jobName", jobName)
            .field("executionId", context.getExecutionId())
            .field("itemsProcessed", context.getItemsProcessed())
            .field("error", error.getMessage())
            .log();
        
        metrics.incrementCounter(
            "batch.item.error",
            "jobName", jobName,
            "errorType", error.getClass().getSimpleName()
        );
    }
    
    @Override
    public void onJobFailed(
        String jobName,
        Throwable error,
        BatchContext context
    ) {
        log.error("Batch job failed")
            .field("jobName", jobName)
            .field("executionId", context.getExecutionId())
            .field("itemsProcessed", context.getItemsProcessed())
            .field("itemsFailed", context.getItemsFailed())
            .log();
        
        metrics.incrementCounter(
            "batch.job.failed",
            "jobName", jobName
        );
    }
}
```

---

## 📈 Monitoring

### Metrics

```java
@Component
public class BatchMetricsCollector implements BatchExecutionListener {
    
    private final MetricsFacade metrics;
    
    @Override
    public void onJobStarted(String jobName, BatchContext context) {
        metrics.incrementCounter("batch.job.started", "jobName", jobName);
    }
    
    @Override
    public void onJobCompleted(
        String jobName,
        BatchExecutionResult result,
        BatchContext context
    ) {
        metrics.recordTimer(
            "batch.job.duration",
            result.getDuration(),
            "jobName", jobName,
            "status", result.getStatus().name()
        );
        
        metrics.recordGauge(
            "batch.items.processed",
            result.getItemsProcessed(),
            "jobName", jobName
        );
        
        metrics.recordGauge(
            "batch.items.failed",
            result.getItemsFailed(),
            "jobName", jobName
        );
    }
}
```

### Progress Tracking

```java
@Component
public class BatchProgressTracker {
    
    private final Map<String, BatchProgress> progressMap = new ConcurrentHashMap<>();
    
    public void updateProgress(
        String jobName,
        long itemsProcessed,
        long totalItems
    ) {
        BatchProgress progress = new BatchProgress(
            jobName,
            itemsProcessed,
            totalItems,
            calculatePercentage(itemsProcessed, totalItems),
            estimateRemainingTime(itemsProcessed, totalItems)
        );
        
        progressMap.put(jobName, progress);
    }
    
    public Optional<BatchProgress> getProgress(String jobName) {
        return Optional.ofNullable(progressMap.get(jobName));
    }
    
    private double calculatePercentage(long processed, long total) {
        if (total == 0) return 0;
        return (processed * 100.0) / total;
    }
}
```

---

## 🧪 Testing

### Unit Tests

```java
class OrderCleanupJobTest {
    
    private OrderRepository orderRepository;
    private OrderCleanupJob job;
    private BatchContext context;
    
    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        job = new OrderCleanupJob(orderRepository, StructuredLog.noop());
        context = BatchContext.builder()
            .jobName(job.name())
            .build();
    }
    
    @Test
    void shouldReadCancelledOrders() {
        // Given
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Order> cancelled = List.of(
            createCancelledOrder(cutoff.minus(10, ChronoUnit.DAYS))
        );
        
        when(orderRepository.findCancelledBefore(any(), anyInt()))
            .thenReturn(cancelled);
        
        // When
        List<Order> result = job.read(context);
        
        // Then
        assertThat(result).hasSize(1);
        verify(orderRepository).findCancelledBefore(any(), eq(100));
    }
    
    @Test
    void shouldDeleteOrder() {
        // Given
        Order order = createCancelledOrder(Instant.now().minus(40, ChronoUnit.DAYS));
        
        // When
        Result<Void> result = job.process(order, context);
        
        // Then
        assertThat(result.isOk()).isTrue();
        verify(orderRepository).delete(order.id());
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class BatchIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Autowired
    private BatchExecutor batchExecutor;
    
    @Autowired
    private OrderCleanupJob cleanupJob;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    void shouldExecuteBatchJob() {
        // Given: Create test data
        createCancelledOrders(50);
        
        // When: Execute batch
        Result<BatchExecutionResult> result = batchExecutor.execute(cleanupJob);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        BatchExecutionResult executionResult = result.getOrThrow();
        assertThat(executionResult.getItemsProcessed()).isEqualTo(50);
        assertThat(executionResult.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // Verify orders were deleted
        assertThat(orderRepository.count()).isZero();
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use batch size apropriado (100-1000)
.batchSize(500)

// ✅ Implemente checkpoints para jobs longos
context.checkpoint(lastProcessedId);

// ✅ Use particionamento para paralelizar
.parallelPartitions(4)

// ✅ Configure retry com backoff
.maxRetries(3)
.retryDelay(Duration.ofSeconds(5))
.retryBackoffMultiplier(2.0)

// ✅ Monitore progresso e métricas
metrics.recordGauge("batch.progress", percentage);

// ✅ Use skip strategy para erros não-críticos
.skipOnError(true)
.skipLimit(100)
```

### ❌ DON'T

```java
// ❌ NÃO use batch size muito grande
.batchSize(100000)  // ❌ Vai estourar memória

// ❌ NÃO processe tudo em memória
List<Order> allOrders = repository.findAll();  // ❌ OutOfMemoryError

// ❌ NÃO ignore checkpoints em jobs longos
// Se falhar após 3 horas, vai reprocessar tudo! ❌

// ❌ NÃO use skip para todos os erros
.skipOnError(true)
.skipLimit(Integer.MAX_VALUE)  // ❌ Vai mascarar problemas

// ❌ NÃO bloqueie tabelas inteiras
@Transactional  // ❌ Lock na tabela toda!
public void processBatch() {
    // ...
}
```

---

## Ver Também

- [Scheduler](app-scheduler.md) - Agendar batch jobs
- [Resilience Guide](../guides/resilience.md) - Retry policies
- [Observability Guide](../guides/observability.md) - Métricas
