# API Reference: Scheduler

## Visão Geral

`commons-app-scheduler` fornece agendamento de tarefas distribuído com suporte a cron expressions, execução única, tarefas recorrentes, e coordenação distribuída.

**Quando usar:**
- Executar tarefas periódicas (limpeza, relatórios, sincronização)
- Agendar jobs com cron expressions
- Processar tarefas em background
- Coordenar execução distribuída (um nó por vez)
- Agendar tarefas únicas no futuro

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-scheduler</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### ScheduledTask

Interface para implementar tarefas agendadas.

```java
public interface ScheduledTask {
    
    /**
     * Nome único da tarefa.
     */
    String name();
    
    /**
     * Executa a tarefa.
     */
    Result<Void> execute(TaskContext context);
    
    /**
     * Configura retry policy (opcional).
     */
    default RetryPolicy retryPolicy() {
        return RetryPolicy.disabled();
    }
    
    /**
     * Configura timeout (opcional).
     */
    default Duration timeout() {
        return Duration.ofMinutes(5);
    }
}
```

### TaskScheduler

Agenda e gerencia execução de tarefas.

```java
public interface TaskScheduler {
    
    /**
     * Agenda tarefa com cron expression.
     */
    void scheduleCron(ScheduledTask task, String cronExpression);
    
    /**
     * Agenda tarefa com intervalo fixo.
     */
    void scheduleFixedRate(
        ScheduledTask task,
        Duration initialDelay,
        Duration period
    );
    
    /**
     * Agenda tarefa única no futuro.
     */
    void scheduleOnce(ScheduledTask task, Instant executeAt);
    
    /**
     * Cancela tarefa agendada.
     */
    void cancel(String taskName);
    
    /**
     * Lista todas as tarefas agendadas.
     */
    List<TaskInfo> listScheduled();
}
```

### DistributedLock

Garante execução única em cluster.

```java
public interface DistributedLock {
    
    /**
     * Tenta adquirir lock.
     */
    boolean tryLock(String lockKey, Duration duration);
    
    /**
     * Libera lock.
     */
    void unlock(String lockKey);
    
    /**
     * Executa com lock (adquire, executa, libera).
     */
    <T> Result<T> executeWithLock(
        String lockKey,
        Duration lockDuration,
        Supplier<Result<T>> action
    );
}
```

---

## 💡 Uso Básico

### Tarefa com Cron Expression

```java
@Component
public class DailyCleanupTask implements ScheduledTask {
    
    private final OrderRepository orderRepository;
    private final StructuredLog log;
    
    @Override
    public String name() {
        return "daily-cleanup";
    }
    
    @Override
    public Result<Void> execute(TaskContext context) {
        log.info("Starting daily cleanup")
            .field("executionId", context.executionId())
            .log();
        
        try {
            // Deletar pedidos cancelados há mais de 30 dias
            Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
            int deleted = orderRepository.deleteCancelledBefore(cutoff);
            
            log.info("Cleanup completed")
                .field("deletedOrders", deleted)
                .log();
            
            return Result.ok();
            
        } catch (Exception e) {
            log.error("Cleanup failed", e).log();
            return Result.fail(Problem.of(
                "CLEANUP.FAILED",
                "Failed to cleanup orders: " + e.getMessage()
            ));
        }
    }
    
    @Override
    public RetryPolicy retryPolicy() {
        return RetryPolicy.builder()
            .maxAttempts(3)
            .waitDuration(Duration.ofMinutes(1))
            .build();
    }
    
    @Override
    public Duration timeout() {
        return Duration.ofMinutes(10);
    }
}
```

### Agendamento

```java
@Configuration
public class SchedulerConfig {
    
    private final TaskScheduler taskScheduler;
    
    @PostConstruct
    public void scheduleJobs() {
        // Diário às 2:00 AM
        taskScheduler.scheduleCron(
            new DailyCleanupTask(),
            "0 0 2 * * *"
        );
        
        // A cada 5 minutos
        taskScheduler.scheduleCron(
            new OutboxProcessorTask(),
            "0 */5 * * * *"
        );
        
        // A cada hora
        taskScheduler.scheduleFixedRate(
            new HealthCheckTask(),
            Duration.ZERO,           // Início imediato
            Duration.ofHours(1)      // Intervalo
        );
    }
}
```

---

## ⏰ Cron Expressions

### Formato

```
┌───────────── segundo (0-59)
│ ┌───────────── minuto (0-59)
│ │ ┌───────────── hora (0-23)
│ │ │ ┌───────────── dia do mês (1-31)
│ │ │ │ ┌───────────── mês (1-12)
│ │ │ │ │ ┌───────────── dia da semana (0-7, 0=domingo)
│ │ │ │ │ │
* * * * * *
```

### Exemplos

```java
// Todos os dias às 2:30 AM
"0 30 2 * * *"

// A cada 15 minutos
"0 */15 * * * *"

// Segunda a sexta às 9:00 AM
"0 0 9 * * MON-FRI"

// Primeiro dia de cada mês às 00:00
"0 0 0 1 * *"

// Últimos domingos do mês às 22:00
"0 0 22 * * SUN#-1"

// A cada 5 segundos
"*/5 * * * * *"
```

---

## 🌐 Distributed Execution

### Tarefa Distribuída com Lock

```java
@Component
public class ReportGenerationTask implements ScheduledTask {
    
    private final DistributedLock distributedLock;
    private final ReportService reportService;
    private final StructuredLog log;
    
    @Override
    public String name() {
        return "monthly-report";
    }
    
    @Override
    public Result<Void> execute(TaskContext context) {
        String lockKey = "task:" + name();
        Duration lockDuration = Duration.ofMinutes(30);
        
        // Executa apenas em um nó do cluster
        return distributedLock.executeWithLock(
            lockKey,
            lockDuration,
            () -> generateReport(context)
        );
    }
    
    private Result<Void> generateReport(TaskContext context) {
        log.info("Generating monthly report")
            .field("month", YearMonth.now())
            .field("nodeId", context.nodeId())
            .log();
        
        Result<Report> reportResult = reportService.generateMonthlyReport();
        
        return reportResult.flatMap(report -> 
            reportService.sendReportByEmail(report)
        );
    }
}
```

### Redis Distributed Lock

```java
@Component
public class RedisDistributedLock implements DistributedLock {
    
    private final StringRedisTemplate redisTemplate;
    
    @Override
    public boolean tryLock(String lockKey, Duration duration) {
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(
                lockKey,
                UUID.randomUUID().toString(),
                duration
            );
        
        return Boolean.TRUE.equals(acquired);
    }
    
    @Override
    public void unlock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
    
    @Override
    public <T> Result<T> executeWithLock(
        String lockKey,
        Duration lockDuration,
        Supplier<Result<T>> action
    ) {
        if (!tryLock(lockKey, lockDuration)) {
            return Result.fail(Problem.of(
                "LOCK.ACQUISITION_FAILED",
                "Could not acquire lock: " + lockKey
            ));
        }
        
        try {
            return action.get();
        } finally {
            unlock(lockKey);
        }
    }
}
```

---

## 📅 Scheduling Strategies

### Fixed Rate vs Fixed Delay

```java
@Configuration
public class SchedulingStrategies {
    
    private final TaskScheduler scheduler;
    
    /**
     * FIXED RATE: Executa a cada X tempo desde o INÍCIO da execução anterior.
     * Se tarefa demorar mais que o intervalo, próxima inicia imediatamente.
     */
    public void scheduleFixedRate() {
        scheduler.scheduleFixedRate(
            new DataSyncTask(),
            Duration.ZERO,
            Duration.ofMinutes(5)  // Inicia a cada 5 min
        );
    }
    
    /**
     * FIXED DELAY: Executa X tempo após o FIM da execução anterior.
     * Aguarda conclusão antes de contar próximo intervalo.
     */
    public void scheduleFixedDelay() {
        scheduler.scheduleFixedDelay(
            new HeavyProcessingTask(),
            Duration.ZERO,
            Duration.ofMinutes(5)  // 5 min APÓS conclusão
        );
    }
    
    /**
     * CRON: Executa em horários específicos.
     */
    public void scheduleCron() {
        scheduler.scheduleCron(
            new DailyReportTask(),
            "0 0 2 * * *"  // 2:00 AM todos os dias
        );
    }
}
```

---

## 🔄 Task Execution Flow

### Complete Example

```java
@Component
public class OutboxProcessorTask implements ScheduledTask {
    
    private final OutboxRepository outboxRepository;
    private final MessagePublisher messagePublisher;
    private final MetricsFacade metrics;
    private final StructuredLog log;
    
    @Override
    public String name() {
        return "outbox-processor";
    }
    
    @Override
    public Result<Void> execute(TaskContext context) {
        Instant start = Instant.now();
        
        log.info("Processing outbox messages")
            .field("executionId", context.executionId())
            .field("nodeId", context.nodeId())
            .log();
        
        try {
            // Buscar mensagens pendentes
            List<OutboxMessage> pending = outboxRepository
                .findPendingMessages(100);
            
            if (pending.isEmpty()) {
                log.debug("No pending messages").log();
                return Result.ok();
            }
            
            // Processar mensagens
            int processed = 0;
            int failed = 0;
            
            for (OutboxMessage message : pending) {
                Result<Void> publishResult = publishMessage(message);
                
                if (publishResult.isOk()) {
                    outboxRepository.markAsPublished(message.id());
                    processed++;
                } else {
                    outboxRepository.incrementRetryCount(message.id());
                    failed++;
                    
                    log.warn("Failed to publish message")
                        .field("messageId", message.id())
                        .field("attempt", message.retryCount() + 1)
                        .field("error", publishResult.problemOrNull().detail())
                        .log();
                }
            }
            
            // Métricas
            Duration duration = Duration.between(start, Instant.now());
            metrics.recordTimer("outbox.processing.duration", duration);
            metrics.recordGauge("outbox.processed", processed);
            metrics.recordGauge("outbox.failed", failed);
            
            log.info("Outbox processing completed")
                .field("processed", processed)
                .field("failed", failed)
                .field("duration", duration)
                .log();
            
            return Result.ok();
            
        } catch (Exception e) {
            metrics.incrementCounter("outbox.processing.error");
            log.error("Outbox processing failed", e).log();
            
            return Result.fail(Problem.of(
                "OUTBOX.PROCESSING_FAILED",
                "Failed to process outbox: " + e.getMessage()
            ));
        }
    }
    
    private Result<Void> publishMessage(OutboxMessage message) {
        return messagePublisher.publish(
            message.topic(),
            message.key(),
            message.payload()
        );
    }
    
    @Override
    public RetryPolicy retryPolicy() {
        return RetryPolicy.builder()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(30))
            .backoffMultiplier(2.0)
            .build();
    }
    
    @Override
    public Duration timeout() {
        return Duration.ofMinutes(5);
    }
}
```

---

## 📊 Monitoring

### Task Metrics

```java
@Component
public class TaskMetricsCollector {
    
    private final MetricsFacade metrics;
    
    @EventListener
    public void onTaskStarted(TaskStartedEvent event) {
        metrics.incrementCounter(
            "task.started",
            "taskName", event.taskName()
        );
    }
    
    @EventListener
    public void onTaskCompleted(TaskCompletedEvent event) {
        metrics.recordTimer(
            "task.duration",
            event.duration(),
            "taskName", event.taskName(),
            "status", event.status().name()
        );
        
        if (event.status() == TaskStatus.SUCCESS) {
            metrics.incrementCounter(
                "task.success",
                "taskName", event.taskName()
            );
        } else {
            metrics.incrementCounter(
                "task.failure",
                "taskName", event.taskName()
            );
        }
    }
    
    @EventListener
    public void onTaskRetrying(TaskRetryingEvent event) {
        metrics.incrementCounter(
            "task.retry",
            "taskName", event.taskName(),
            "attempt", String.valueOf(event.attemptNumber())
        );
    }
}
```

### Health Check

```java
@Component
public class SchedulerHealthCheck implements HealthCheck {
    
    private final TaskScheduler scheduler;
    
    @Override
    public String name() {
        return "scheduler";
    }
    
    @Override
    public HealthStatus check() {
        try {
            List<TaskInfo> tasks = scheduler.listScheduled();
            
            long runningTasks = tasks.stream()
                .filter(t -> t.status() == TaskStatus.RUNNING)
                .count();
            
            long failedTasks = tasks.stream()
                .filter(t -> t.status() == TaskStatus.FAILED)
                .count();
            
            if (failedTasks > 3) {
                return HealthStatus.down()
                    .withDetail("failedTasks", failedTasks)
                    .withMessage("Too many failed tasks");
            }
            
            return HealthStatus.up()
                .withDetail("totalTasks", tasks.size())
                .withDetail("runningTasks", runningTasks)
                .withDetail("failedTasks", failedTasks);
                
        } catch (Exception e) {
            return HealthStatus.down()
                .withException(e);
        }
    }
}
```

---

## 🎯 Advanced Patterns

### Conditional Execution

```java
@Component
public class ConditionalReportTask implements ScheduledTask {
    
    private final FeatureFlagService featureFlags;
    private final ReportService reportService;
    
    @Override
    public String name() {
        return "conditional-report";
    }
    
    @Override
    public Result<Void> execute(TaskContext context) {
        // Executa apenas se feature flag ativada
        if (!featureFlags.isEnabled("daily-report")) {
            return Result.ok();  // Skip silently
        }
        
        // Executa apenas em dias úteis
        if (isWeekend()) {
            return Result.ok();
        }
        
        return reportService.generateReport();
    }
    
    private boolean isWeekend() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY;
    }
}
```

### Batch Processing

```java
@Component
public class BatchProcessorTask implements ScheduledTask {
    
    private final DataRepository repository;
    private final BatchProcessor processor;
    
    @Override
    public String name() {
        return "batch-processor";
    }
    
    @Override
    public Result<Void> execute(TaskContext context) {
        int batchSize = 1000;
        int totalProcessed = 0;
        
        while (true) {
            // Buscar próximo lote
            List<DataRecord> batch = repository.findPendingBatch(batchSize);
            
            if (batch.isEmpty()) {
                break;  // Sem mais dados
            }
            
            // Processar lote
            Result<Void> processResult = processor.process(batch);
            
            if (processResult.isFail()) {
                return processResult;  // Falha, para execução
            }
            
            totalProcessed += batch.size();
            
            // Checkpoint - salvar progresso
            context.updateProgress(totalProcessed);
        }
        
        return Result.ok();
    }
}
```

### Dynamic Scheduling

```java
@Service
public class DynamicSchedulerService {
    
    private final TaskScheduler scheduler;
    private final ConfigurationProvider config;
    
    /**
     * Reagenda tarefa baseado em configuração dinâmica.
     */
    public void updateSchedule(String taskName) {
        // Cancelar agendamento atual
        scheduler.cancel(taskName);
        
        // Buscar nova configuração
        String cronExpression = config.getString(
            "tasks." + taskName + ".cron"
        ).orElse("0 0 * * * *");  // Default: hourly
        
        // Reagendar
        ScheduledTask task = findTask(taskName);
        scheduler.scheduleCron(task, cronExpression);
    }
}
```

---

## 🧪 Testing

### Unit Tests

```java
class OutboxProcessorTaskTest {
    
    private OutboxRepository outboxRepository;
    private MessagePublisher messagePublisher;
    private OutboxProcessorTask task;
    
    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxRepository.class);
        messagePublisher = mock(MessagePublisher.class);
        task = new OutboxProcessorTask(
            outboxRepository,
            messagePublisher,
            MetricsFacade.noop(),
            StructuredLog.noop()
        );
    }
    
    @Test
    void shouldProcessPendingMessages() {
        // Given
        OutboxMessage msg1 = OutboxMessage.builder()
            .id(OutboxMessageId.generate())
            .topic("orders.events")
            .payload("{}")
            .build();
        
        when(outboxRepository.findPendingMessages(100))
            .thenReturn(List.of(msg1));
        
        when(messagePublisher.publish(any(), any(), any()))
            .thenReturn(Result.ok());
        
        // When
        Result<Void> result = task.execute(TaskContext.builder().build());
        
        // Then
        assertThat(result.isOk()).isTrue();
        verify(outboxRepository).markAsPublished(msg1.id());
    }
    
    @Test
    void shouldHandlePublishFailure() {
        // Given
        OutboxMessage msg = OutboxMessage.builder().build();
        when(outboxRepository.findPendingMessages(100))
            .thenReturn(List.of(msg));
        
        when(messagePublisher.publish(any(), any(), any()))
            .thenReturn(Result.fail(Problem.of("PUBLISH_FAILED", "Error")));
        
        // When
        Result<Void> result = task.execute(TaskContext.builder().build());
        
        // Then
        assertThat(result.isOk()).isTrue();  // Task completes
        verify(outboxRepository).incrementRetryCount(msg.id());
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class SchedulerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Autowired
    private TaskScheduler scheduler;
    
    @Autowired
    private TestTaskRepository taskRepository;
    
    @Test
    void shouldExecuteScheduledTask() throws Exception {
        // Given
        TestTask task = new TestTask(taskRepository);
        
        // When
        scheduler.scheduleOnce(task, Instant.now().plusSeconds(2));
        
        // Then
        await().atMost(Duration.ofSeconds(5))
            .until(() -> taskRepository.wasExecuted(task.name()));
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use distributed locks em clusters
distributedLock.executeWithLock(lockKey, duration, () -> {
    return processTask();
});

// ✅ Configure timeouts apropriados
@Override
public Duration timeout() {
    return Duration.ofMinutes(10);
}

// ✅ Implemente retry com backoff
RetryPolicy.builder()
    .maxAttempts(3)
    .waitDuration(Duration.ofSeconds(10))
    .backoffMultiplier(2.0)
    .build();

// ✅ Monitore execuções com métricas
metrics.recordTimer("task.duration", duration);
metrics.incrementCounter("task.success");

// ✅ Use batch processing para grandes volumes
while (hasMore) {
    List<Record> batch = repository.findBatch(1000);
    processBatch(batch);
}
```

### ❌ DON'T

```java
// ❌ NÃO execute tarefas longas sem timeout
@Override
public Duration timeout() {
    return Duration.ofHours(24);  // ❌ Muito longo
}

// ❌ NÃO ignore falhas silenciosamente
try {
    processTask();
} catch (Exception e) {
    // ❌ Não fazer nada!
}

// ❌ NÃO use fixed rate para tarefas lentas
scheduler.scheduleFixedRate(
    slowTask,
    Duration.ZERO,
    Duration.ofSeconds(30)  // ❌ Task demora 1 minuto!
);

// ❌ NÃO esqueça distributed lock em clusters
@Override
public Result<Void> execute(TaskContext context) {
    // ❌ Vai executar em TODOS os nós!
    return processTask();
}
```

---

## Ver Também

- [Resilience Guide](../guides/resilience.md) - Retry policies
- [Observability Guide](../guides/observability.md) - Métricas e logs
- [Outbox Pattern](app-outbox.md) - Processamento de outbox
