# Commons App Scheduler

Scheduled jobs with Quartz and distributed locking using ShedLock.

## Overview

This module provides a high-level abstraction for scheduling jobs with support for:
- **Cron expressions** for complex scheduling patterns
- **Fixed delay** and **fixed rate** execution
- **Distributed locking** to prevent concurrent execution across instances
- **Job persistence** for surviving application restarts
- **Type-safe** API using Result pattern

## Features

- ✅ Quartz integration for powerful scheduling
- ✅ ShedLock for distributed coordination
- ✅ Cron expression support
- ✅ Job persistence with JDBC
- ✅ Distributed locking to prevent duplicate executions
- ✅ Type-safe Result-based API
- ✅ Configurable lock duration and intervals
- ✅ Pause/resume/unschedule jobs
- ✅ Query next execution times
- ✅ Spring integration (optional)

## Dependencies

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-app-scheduler</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Core Concepts

### ScheduledJob

Interface for defining jobs:

```java
public interface ScheduledJob<T> {
  Result<T> execute(ScheduledJobContext context);

  default String getJobId() { return getClass().getSimpleName(); }
  default boolean requiresDistributedLock() { return true; }
  default long getLockDurationMillis() { return 10 * 60 * 1000; } // 10 min
}
```

### JobScheduler

Main API for scheduling:

```java
public interface JobScheduler {
  <T> Result<String> scheduleOnce(ScheduledJob<T> job, Instant fireTime);
  <T> Result<String> scheduleWithFixedDelay(ScheduledJob<T> job, Duration initialDelay, Duration interval);
  <T> Result<String> scheduleWithCron(ScheduledJob<T> job, String cronExpression);

  Result<Void> unschedule(String jobId);
  Result<Void> pause(String jobId);
  Result<Void> resume(String jobId);
}
```

### DistributedLock

Coordination across instances:

```java
public interface DistributedLock {
  <T> Result<T> executeWithLock(String lockName, Duration lockDuration, Supplier<Result<T>> task);
  Result<Boolean> tryLock(String lockName, Duration lockDuration);
  Result<Void> unlock(String lockName);
}
```

## Usage Examples

### Simple Scheduled Job

```java
public class DataCleanupJob implements ScheduledJob<Integer> {

  @Override
  public Result<Integer> execute(ScheduledJobContext context) {
    int deletedRecords = cleanupOldRecords();
    return Result.ok(deletedRecords);
  }

  @Override
  public String getJobId() {
    return "data-cleanup";
  }

  @Override
  public boolean requiresDistributedLock() {
    return true; // Only one instance should run this job
  }

  private int cleanupOldRecords() {
    // Implementation
    return 42;
  }
}
```

### Schedule with Cron

```java
JobScheduler scheduler = // ...

// Every day at 2 AM
scheduler.scheduleWithCron(
    new DataCleanupJob(),
    "0 0 2 * * ?"
);

// Every 15 minutes
scheduler.scheduleWithCron(
    new HealthCheckJob(),
    "0 */15 * * * ?"
);

// Every Monday at 9 AM
scheduler.scheduleWithCron(
    new WeeklyReportJob(),
    "0 0 9 ? * MON"
);
```

### Schedule with Fixed Delay

```java
// Run every 5 minutes, starting after 1 minute
scheduler.scheduleWithFixedDelay(
    new MetricsCollectionJob(),
    Duration.ofMinutes(1),  // initial delay
    Duration.ofMinutes(5)   // interval
);
```

### Schedule One-time Job

```java
// Run at specific time
scheduler.scheduleOnce(
    new BackupJob(),
    Instant.now().plus(Duration.ofHours(2))
);
```

### Job with Parameters

```java
Map<String, Object> params = Map.of(
    "batchSize", 1000,
    "targetDate", LocalDate.now()
);

scheduler.scheduleWithCron(
    new BatchProcessingJob(),
    "0 0 1 * * ?",
    params
);
```

Access parameters in job:

```java
@Override
public Result<Void> execute(ScheduledJobContext context) {
  int batchSize = context.getParameter("batchSize", 100);
  LocalDate targetDate = context.<LocalDate>getParameter("targetDate")
      .orElse(LocalDate.now());

  // Process...
  return Result.ok(null);
}
```

### Managing Jobs

```java
// Pause a job
scheduler.pause("data-cleanup");

// Resume a paused job
scheduler.resume("data-cleanup");

// Unschedule a job
scheduler.unschedule("data-cleanup");

// Check if scheduled
boolean scheduled = scheduler.isScheduled("data-cleanup")
    .getOrDefault(false);

// Get next execution time
Instant nextRun = scheduler.getNextFireTime("data-cleanup")
    .getOrNull();
```

### Distributed Locking

```java
public class ReportGenerationJob implements ScheduledJob<Report> {

  @Override
  public Result<Report> execute(ScheduledJobContext context) {
    Report report = generateReport();
    return Result.ok(report);
  }

  @Override
  public boolean requiresDistributedLock() {
    return true; // Prevent concurrent execution
  }

  @Override
  public long getLockDurationMillis() {
    return 30 * 60 * 1000; // 30 minutes max execution time
  }

  @Override
  public long getMinLockIntervalMillis() {
    return 5 * 1000; // 5 seconds minimum between attempts
  }
}
```

### Custom Lock Usage

```java
DistributedLock lock = // ...

lock.executeWithLock(
    "critical-section",
    Duration.ofMinutes(5),
    () -> {
      // Only one instance executes this
      processCriticalOperation();
      return Result.ok(null);
    }
);
```

## Cron Expression Format

Quartz cron expressions have 6 or 7 fields:

```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12 or JAN-DEC)
│ │ │ │ │ ┌───────────── day of week (1-7 or SUN-SAT)
│ │ │ │ │ │ ┌───────────── year (optional, 1970-2099)
│ │ │ │ │ │ │
* * * * * * *
```

### Common Examples

| Expression | Description |
|------------|-------------|
| `0 0 * * * ?` | Every hour at minute 0 |
| `0 */15 * * * ?` | Every 15 minutes |
| `0 0 2 * * ?` | Daily at 2 AM |
| `0 0 9 ? * MON-FRI` | Weekdays at 9 AM |
| `0 0 0 1 * ?` | First day of month at midnight |
| `0 0 12 ? * SUN` | Every Sunday at noon |

## Configuration

### Quartz Configuration

```properties
# quartz.properties
org.quartz.scheduler.instanceName = MyScheduler
org.quartz.scheduler.instanceId = AUTO

# Thread pool
org.quartz.threadPool.class = org.quartz.simpl.SimpleThreadPool
org.quartz.threadPool.threadCount = 10

# Job store (JDBC)
org.quartz.jobStore.class = org.quartz.impl.jdbcjobstore.JobStorePersistenceDelegate
org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.StdJDBCDelegate
org.quartz.jobStore.dataSource = myDS
org.quartz.jobStore.tablePrefix = QRTZ_
org.quartz.jobStore.isClustered = true

# Data source
org.quartz.dataSource.myDS.driver = org.postgresql.Driver
org.quartz.dataSource.myDS.URL = jdbc:postgresql://localhost:5432/mydb
org.quartz.dataSource.myDS.user = user
org.quartz.dataSource.myDS.password = password
```

### ShedLock Configuration

```java
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class SchedulerConfig {

  @Bean
  public LockProvider lockProvider(DataSource dataSource) {
    return new JdbcTemplateLockProvider(dataSource);
  }
}
```

Database schema (PostgreSQL):

```sql
CREATE TABLE shedlock (
  name VARCHAR(64) PRIMARY KEY,
  lock_until TIMESTAMP NOT NULL,
  locked_at TIMESTAMP NOT NULL,
  locked_by VARCHAR(255) NOT NULL
);
```

## Database Schema

### Quartz Tables

Quartz requires several tables for job persistence. Use the schema scripts provided by Quartz for your database:

- PostgreSQL: `tables_postgres.sql`
- MySQL: `tables_mysql_innodb.sql`
- Oracle: `tables_oracle.sql`
- SQL Server: `tables_sqlServer.sql`

Tables created:
- `QRTZ_JOB_DETAILS` - Job definitions
- `QRTZ_TRIGGERS` - Trigger definitions
- `QRTZ_CRON_TRIGGERS` - Cron trigger details
- `QRTZ_SIMPLE_TRIGGERS` - Simple trigger details
- `QRTZ_FIRED_TRIGGERS` - Currently firing triggers
- `QRTZ_CALENDARS` - Calendar exclusions
- `QRTZ_PAUSED_TRIGGER_GRPS` - Paused trigger groups
- `QRTZ_SCHEDULER_STATE` - Scheduler instance state
- `QRTZ_LOCKS` - Distributed locks

## Best Practices

### Job Design

1. **Stateless Jobs**: Jobs should be stateless and idempotent
2. **Short Execution**: Keep job execution time reasonable (< lock duration)
3. **Exception Handling**: Always handle exceptions inside jobs
4. **Logging**: Use structured logging with job ID and context
5. **Idempotency**: Design jobs to be safely re-runnable

```java
@Override
public Result<Void> execute(ScheduledJobContext context) {
  try {
    logger.info("Starting job: {}", context.getJobId());

    // Idempotent operation
    processRecords(context);

    logger.info("Completed job: {}", context.getJobId());
    return Result.ok(null);

  } catch (Exception e) {
    logger.error("Job failed: {}", context.getJobId(), e);
    return Result.fail(
        Problem.of(
            ErrorCode.of("JOB_EXECUTION_FAILED"),
            ErrorCategory.TECHNICAL,
            Severity.ERROR,
            "Job execution failed: " + e.getMessage()
        )
    );
  }
}
```

### Locking Strategy

1. **Use locks for critical jobs**: Jobs that modify shared state should use locks
2. **Set appropriate duration**: Lock duration should exceed expected execution time
3. **Monitor stale locks**: Alert on locks held longer than expected
4. **Avoid unnecessary locks**: Read-only jobs may not need locking

### Monitoring

1. **Job Success Rate**: Track success/failure ratio
2. **Execution Time**: Monitor job duration trends
3. **Lock Wait Time**: Track time spent waiting for locks
4. **Missed Executions**: Alert on skipped job runs
5. **Queue Depth**: Monitor pending job count

## Error Handling

### Job Failures

Jobs return `Result<T>` for type-safe error handling:

```java
@Override
public Result<Integer> execute(ScheduledJobContext context) {
  if (shouldSkip()) {
    return Result.ok(0); // Successful skip
  }

  try {
    int processed = processData();
    return Result.ok(processed);
  } catch (ValidationException e) {
    return Result.fail(
        Problem.of(
            ErrorCode.of("VALIDATION_ERROR"),
            ErrorCategory.BUSINESS,
            Severity.WARNING,
            "Invalid data: " + e.getMessage()
        )
    );
  } catch (Exception e) {
    return Result.fail(
        Problem.of(
            ErrorCode.of("PROCESSING_ERROR"),
            ErrorCategory.TECHNICAL,
            Severity.ERROR,
            "Processing failed: " + e.getMessage()
        )
    );
  }
}
```

### Retry Configuration

Quartz supports retry through misfire instructions:

```java
// Configuration for handling mis fires
trigger.withMisfireHandlingInstructionFireAndProceed()
```

## Testing

### Unit Tests

```java
@Test
void shouldExecuteJob() {
  var job = new DataCleanupJob();
  var context = ScheduledJobContext.builder()
      .jobId("test-job")
      .scheduledFireTime(Instant.now())
      .actualFireTime(Instant.now())
      .build();

  Result<Integer> result = job.execute(context);

  assertThat(result.isOk()).isTrue();
  assertThat(result.getOrNull()).isGreaterThan(0);
}
```

### Integration Tests

```java
@Test
void shouldScheduleWithCron() {
  JobScheduler scheduler = createTestScheduler();

  Result<String> result = scheduler.scheduleWithCron(
      new TestJob(),
      "0 * * * * ?" // Every minute
  );

  assertThat(result.isOk()).isTrue();
  String jobId = result.getOrNull();

  await().atMost(65, SECONDS)
      .until(() -> getExecutionCount(jobId) > 0);
}
```

## Migration from Spring @Scheduled

Before (Spring @Scheduled):

```java
@Component
public class DataCleanupTask {

  @Scheduled(cron = "0 0 2 * * ?")
  public void cleanup() {
    cleanupOldRecords();
  }
}
```

After (Commons Scheduler):

```java
@Component
public class DataCleanupJob implements ScheduledJob<Integer> {

  @Override
  public Result<Integer> execute(ScheduledJobContext context) {
    int deleted = cleanupOldRecords();
    return Result.ok(deleted);
  }

  @Override
  public boolean requiresDistributedLock() {
    return true; // Now with distributed locking!
  }
}

// In configuration
@Bean
public void scheduleJobs(JobScheduler scheduler, DataCleanupJob job) {
  scheduler.scheduleWithCron(job, "0 0 2 * * ?");
}
```

## Troubleshooting

### Job Not Executing

1. **Check scheduler is started**: Verify scheduler.start() was called
2. **Check cron expression**: Validate with online cron parser
3. **Check locks**: Job may be waiting for lock
4. **Check logs**: Look for scheduling errors

### Duplicate Executions

1. **Verify distributed lock**: Ensure requiresDistributedLock() returns true
2. **Check ShedLock config**: Verify lock provider is configured
3. **Check clock skew**: Ensure server clocks are synchronized
4. **Check lock duration**: May be too short for job execution time

### Performance Issues

1. **Thread pool size**: Increase quartz.threadPool.threadCount
2. **Database connections**: Ensure adequate connection pool
3. **Job execution time**: Optimize long-running jobs
4. **Lock contention**: Reduce overlapping job schedules

## See Also

- [Quartz Scheduler Documentation](http://www.quartz-scheduler.org/documentation/)
- [ShedLock Documentation](https://github.com/lukas-krecan/ShedLock)
- [Cron Expression Guide](https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html)

## License

This module is part of the Commons Platform and follows the same license.
