# API Reference: Backup & Restore

## Visão Geral

`commons-app-backup-restore` fornece estratégias de backup e restore de dados para disaster recovery e business continuity.

**Quando usar:**
- Backup automatizado de dados críticos
- Point-in-time recovery (PITR)
- Disaster recovery plans
- Migrações de dados entre ambientes
- Compliance e auditoria (retenção de dados)

**Estratégias suportadas:**
- Full Backup
- Incremental Backup
- Differential Backup
- Continuous Backup (CDC)

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-backup-restore</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### BackupService

Interface principal para backup.

```java
public interface BackupService {
    
    /**
     * Cria backup completo.
     */
    Result<BackupId> createFullBackup(BackupRequest request);
    
    /**
     * Cria backup incremental (apenas mudanças desde último backup).
     */
    Result<BackupId> createIncrementalBackup(BackupRequest request);
    
    /**
     * Lista backups disponíveis.
     */
    List<BackupMetadata> listBackups(BackupFilter filter);
    
    /**
     * Deleta backup.
     */
    Result<Void> deleteBackup(BackupId backupId);
    
    /**
     * Valida integridade de backup.
     */
    Result<BackupValidation> validateBackup(BackupId backupId);
}
```

### RestoreService

Interface para restore de dados.

```java
public interface RestoreService {
    
    /**
     * Restaura dados de backup.
     */
    Result<RestoreId> restore(RestoreRequest request);
    
    /**
     * Restaura para point-in-time específico.
     */
    Result<RestoreId> restoreToPointInTime(
        Instant pointInTime,
        RestoreRequest request
    );
    
    /**
     * Cancela restore em andamento.
     */
    Result<Void> cancelRestore(RestoreId restoreId);
    
    /**
     * Verifica status de restore.
     */
    Optional<RestoreStatus> getStatus(RestoreId restoreId);
}
```

### BackupMetadata

Informações sobre backup.

```java
public record BackupMetadata(
    BackupId id,
    String name,
    BackupType type,
    Instant createdAt,
    BackupStatus status,
    long sizeBytes,
    String checksum,
    Map<String, String> tags,
    BackupLocation location
) {
    public Duration age() {
        return Duration.between(createdAt, Instant.now());
    }
    
    public boolean isExpired(Duration retentionPeriod) {
        return age().compareTo(retentionPeriod) > 0;
    }
}

public enum BackupType {
    FULL,
    INCREMENTAL,
    DIFFERENTIAL,
    CONTINUOUS
}

public enum BackupStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    EXPIRED
}
```

---

## 💾 Full Backup

### Implementation

```java
@Service
public class DatabaseBackupService implements BackupService {
    
    private final DataSource dataSource;
    private final FileStorage fileStorage;
    private final BackupRepository backupRepository;
    private final StructuredLog log;
    
    @Override
    public Result<BackupId> createFullBackup(BackupRequest request) {
        BackupId backupId = BackupId.generate();
        Instant start = Instant.now();
        
        log.info("Starting full backup")
            .field("backupId", backupId.value())
            .field("database", request.database())
            .log();
        
        try (Connection conn = dataSource.getConnection()) {
            // Get all tables
            List<String> tables = getTables(conn, request.database());
            
            // Create backup file
            Path backupPath = createBackupFile(backupId);
            
            try (
                FileOutputStream fos = new FileOutputStream(backupPath.toFile());
                GZIPOutputStream gzos = new GZIPOutputStream(fos);
                PrintWriter writer = new PrintWriter(gzos)
            ) {
                // Write metadata
                writer.println("-- Full Backup");
                writer.println("-- Created: " + Instant.now());
                writer.println("-- Database: " + request.database());
                writer.println();
                
                long totalRows = 0;
                
                // Backup each table
                for (String table : tables) {
                    log.info("Backing up table")
                        .field("backupId", backupId.value())
                        .field("table", table)
                        .log();
                    
                    // Write CREATE TABLE
                    String createStatement = getCreateTableStatement(conn, table);
                    writer.println(createStatement);
                    writer.println();
                    
                    // Write INSERT statements
                    try (
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)
                    ) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int columnCount = meta.getColumnCount();
                        
                        while (rs.next()) {
                            writer.print("INSERT INTO " + table + " VALUES (");
                            
                            for (int i = 1; i <= columnCount; i++) {
                                Object value = rs.getObject(i);
                                
                                if (value == null) {
                                    writer.print("NULL");
                                } else if (value instanceof String || value instanceof Instant) {
                                    writer.print("'" + escapeString(value.toString()) + "'");
                                } else {
                                    writer.print(value);
                                }
                                
                                if (i < columnCount) {
                                    writer.print(", ");
                                }
                            }
                            
                            writer.println(");");
                            totalRows++;
                        }
                    }
                    
                    writer.println();
                }
                
                writer.flush();
            }
            
            Duration duration = Duration.between(start, Instant.now());
            long sizeBytes = Files.size(backupPath);
            String checksum = calculateChecksum(backupPath);
            
            // Upload to cloud storage
            BackupLocation location = uploadBackup(backupId, backupPath);
            
            // Save metadata
            BackupMetadata metadata = new BackupMetadata(
                backupId,
                request.name(),
                BackupType.FULL,
                start,
                BackupStatus.COMPLETED,
                sizeBytes,
                checksum,
                request.tags(),
                location
            );
            
            backupRepository.save(metadata);
            
            // Delete local file
            Files.delete(backupPath);
            
            log.info("Full backup completed")
                .field("backupId", backupId.value())
                .field("duration", duration)
                .field("sizeBytes", sizeBytes)
                .field("totalRows", totalRows)
                .log();
            
            return Result.ok(backupId);
            
        } catch (Exception e) {
            log.error("Full backup failed", e)
                .field("backupId", backupId.value())
                .log();
            
            return Result.fail(Problem.of(
                "BACKUP.FAILED",
                "Backup failed: " + e.getMessage()
            ));
        }
    }
    
    private String calculateChecksum(Path file) throws IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        
        byte[] hash = digest.digest();
        return Base64.getEncoder().encodeToString(hash);
    }
}
```

---

## 📈 Incremental Backup

### Change Tracking

```java
@Service
public class IncrementalBackupService {
    
    private final DataSource dataSource;
    private final BackupRepository backupRepository;
    
    @Override
    public Result<BackupId> createIncrementalBackup(BackupRequest request) {
        BackupId backupId = BackupId.generate();
        
        // Find last backup
        Optional<BackupMetadata> lastBackup = backupRepository
            .findLatestCompleted(request.database());
        
        if (lastBackup.isEmpty()) {
            // No previous backup - create full backup
            return createFullBackup(request);
        }
        
        Instant since = lastBackup.get().createdAt();
        
        log.info("Starting incremental backup")
            .field("backupId", backupId.value())
            .field("since", since)
            .log();
        
        try (Connection conn = dataSource.getConnection()) {
            Path backupPath = createBackupFile(backupId);
            
            try (
                FileOutputStream fos = new FileOutputStream(backupPath.toFile());
                GZIPOutputStream gzos = new GZIPOutputStream(fos);
                PrintWriter writer = new PrintWriter(gzos)
            ) {
                writer.println("-- Incremental Backup");
                writer.println("-- Created: " + Instant.now());
                writer.println("-- Since: " + since);
                writer.println("-- Base Backup: " + lastBackup.get().id());
                writer.println();
                
                // Get tables with updated_at column
                List<String> tables = getTablesWithTimestamp(conn);
                
                long totalChanges = 0;
                
                for (String table : tables) {
                    // Find changes since last backup
                    String query = String.format(
                        "SELECT * FROM %s WHERE updated_at > ?",
                        table
                    );
                    
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setTimestamp(1, Timestamp.from(since));
                        
                        try (ResultSet rs = stmt.executeQuery()) {
                            ResultSetMetaData meta = rs.getMetaData();
                            int columnCount = meta.getColumnCount();
                            
                            while (rs.next()) {
                                // Write REPLACE or INSERT statement
                                writer.print("REPLACE INTO " + table + " VALUES (");
                                
                                for (int i = 1; i <= columnCount; i++) {
                                    Object value = rs.getObject(i);
                                    writeValue(writer, value);
                                    
                                    if (i < columnCount) {
                                        writer.print(", ");
                                    }
                                }
                                
                                writer.println(");");
                                totalChanges++;
                            }
                        }
                    }
                }
                
                writer.flush();
            }
            
            long sizeBytes = Files.size(backupPath);
            String checksum = calculateChecksum(backupPath);
            BackupLocation location = uploadBackup(backupId, backupPath);
            
            BackupMetadata metadata = new BackupMetadata(
                backupId,
                request.name(),
                BackupType.INCREMENTAL,
                Instant.now(),
                BackupStatus.COMPLETED,
                sizeBytes,
                checksum,
                request.tags(),
                location
            );
            
            backupRepository.save(metadata);
            Files.delete(backupPath);
            
            log.info("Incremental backup completed")
                .field("backupId", backupId.value())
                .field("changes", totalChanges)
                .field("sizeBytes", sizeBytes)
                .log();
            
            return Result.ok(backupId);
            
        } catch (Exception e) {
            return Result.fail(Problem.of(
                "BACKUP.FAILED",
                e.getMessage()
            ));
        }
    }
}
```

---

## 🔄 Restore Service

### Point-in-Time Recovery

```java
@Service
public class DatabaseRestoreService implements RestoreService {
    
    private final DataSource dataSource;
    private final BackupRepository backupRepository;
    private final FileStorage fileStorage;
    
    @Override
    public Result<RestoreId> restoreToPointInTime(
        Instant pointInTime,
        RestoreRequest request
    ) {
        log.info("Starting point-in-time restore")
            .field("pointInTime", pointInTime)
            .field("database", request.targetDatabase())
            .log();
        
        // Find base full backup before point-in-time
        Optional<BackupMetadata> baseBackup = backupRepository
            .findLatestFullBackupBefore(
                request.sourceDatabase(),
                pointInTime
            );
        
        if (baseBackup.isEmpty()) {
            return Result.fail(Problem.of(
                "RESTORE.NO_BACKUP",
                "No backup found before " + pointInTime
            ));
        }
        
        // Find all incremental backups between base and point-in-time
        List<BackupMetadata> incrementalBackups = backupRepository
            .findIncrementalBackupsBetween(
                request.sourceDatabase(),
                baseBackup.get().createdAt(),
                pointInTime
            );
        
        RestoreId restoreId = RestoreId.generate();
        
        try (Connection conn = dataSource.getConnection()) {
            // Step 1: Restore full backup
            Result<Void> fullRestore = restoreBackup(
                conn,
                baseBackup.get(),
                request
            );
            
            if (fullRestore.isFail()) {
                return Result.fail(fullRestore.problemOrNull());
            }
            
            // Step 2: Apply incremental backups in order
            for (BackupMetadata incremental : incrementalBackups) {
                Result<Void> incrementalRestore = restoreBackup(
                    conn,
                    incremental,
                    request
                );
                
                if (incrementalRestore.isFail()) {
                    return Result.fail(incrementalRestore.problemOrNull());
                }
            }
            
            log.info("Point-in-time restore completed")
                .field("restoreId", restoreId.value())
                .field("pointInTime", pointInTime)
                .log();
            
            return Result.ok(restoreId);
            
        } catch (Exception e) {
            return Result.fail(Problem.of(
                "RESTORE.FAILED",
                e.getMessage()
            ));
        }
    }
    
    private Result<Void> restoreBackup(
        Connection conn,
        BackupMetadata backup,
        RestoreRequest request
    ) {
        try {
            // Download backup file
            Path backupFile = downloadBackup(backup);
            
            // Validate checksum
            String actualChecksum = calculateChecksum(backupFile);
            if (!actualChecksum.equals(backup.checksum())) {
                return Result.fail(Problem.of(
                    "RESTORE.CHECKSUM_MISMATCH",
                    "Backup file corrupted"
                ));
            }
            
            // Execute SQL statements
            try (
                FileInputStream fis = new FileInputStream(backupFile.toFile());
                GZIPInputStream gzis = new GZIPInputStream(fis);
                BufferedReader reader = new BufferedReader(new InputStreamReader(gzis))
            ) {
                StringBuilder sql = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    // Skip comments
                    if (line.startsWith("--") || line.trim().isEmpty()) {
                        continue;
                    }
                    
                    sql.append(line);
                    
                    // Execute when statement complete
                    if (line.endsWith(";")) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(sql.toString());
                        }
                        sql.setLength(0);
                    }
                }
            }
            
            // Delete temp file
            Files.delete(backupFile);
            
            return Result.ok();
            
        } catch (Exception e) {
            return Result.fail(Problem.of(
                "RESTORE.FAILED",
                e.getMessage()
            ));
        }
    }
}
```

---

## ⏱️ Automated Backup

### Scheduled Backup

```java
@Component
public class ScheduledBackupJob implements ScheduledTask {
    
    private final BackupService backupService;
    private final BackupConfiguration config;
    
    @Override
    public String name() {
        return "scheduled-backup";
    }
    
    @Override
    public String cronExpression() {
        return config.cronExpression();  // "0 0 2 * * ?" = 2 AM daily
    }
    
    @Override
    public Result<Void> execute() {
        log.info("Starting scheduled backup").log();
        
        BackupRequest request = BackupRequest.builder()
            .database(config.database())
            .name("scheduled-backup-" + LocalDate.now())
            .tags(Map.of(
                "type", "scheduled",
                "date", LocalDate.now().toString()
            ))
            .build();
        
        // Create incremental backup (falls back to full if needed)
        Result<BackupId> result = backupService.createIncrementalBackup(request);
        
        if (result.isOk()) {
            log.info("Scheduled backup completed")
                .field("backupId", result.getOrThrow().value())
                .log();
            
            // Clean up old backups
            cleanupOldBackups();
        } else {
            log.error("Scheduled backup failed")
                .field("error", result.problemOrNull().detail())
                .log();
        }
        
        return result.mapToVoid();
    }
    
    private void cleanupOldBackups() {
        Duration retention = config.retentionPeriod();  // e.g., 30 days
        
        List<BackupMetadata> expiredBackups = backupService
            .listBackups(BackupFilter.all())
            .stream()
            .filter(backup -> backup.isExpired(retention))
            .toList();
        
        for (BackupMetadata backup : expiredBackups) {
            log.info("Deleting expired backup")
                .field("backupId", backup.id().value())
                .field("age", backup.age())
                .log();
            
            backupService.deleteBackup(backup.id());
        }
    }
}
```

---

## 🧪 Testing

### Backup Tests

```java
class BackupServiceTest {
    
    private DataSource dataSource;
    private BackupService backupService;
    
    @BeforeEach
    void setUp() {
        dataSource = createTestDataSource();
        backupService = new DatabaseBackupService(
            dataSource,
            mock(FileStorage.class),
            mock(BackupRepository.class),
            StructuredLog.noop()
        );
    }
    
    @Test
    void shouldCreateFullBackup() {
        // Given: Database with data
        insertTestData();
        
        BackupRequest request = BackupRequest.builder()
            .database("test_db")
            .name("test-backup")
            .build();
        
        // When
        Result<BackupId> result = backupService.createFullBackup(request);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        BackupId backupId = result.getOrThrow();
        assertThat(backupId).isNotNull();
    }
    
    @Test
    void shouldRestoreFromBackup() {
        // Given: Backup exists
        BackupId backupId = createTestBackup();
        
        RestoreRequest request = RestoreRequest.builder()
            .sourceDatabase("test_db")
            .targetDatabase("restored_db")
            .build();
        
        // When
        Result<RestoreId> result = restoreService.restore(request);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        // Verify data restored
        assertThat(countRows("restored_db", "users")).isEqualTo(100);
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Calcule e valide checksums
String checksum = calculateChecksum(backupFile);
metadata = metadata.withChecksum(checksum);

// ✅ Comprima backups
GZIPOutputStream gzos = new GZIPOutputStream(fos);

// ✅ Teste restores regularmente
@Scheduled(cron = "0 0 0 * * SUN")  // Weekly
public void testRestore() {
    restoreToTestEnvironment();
}

// ✅ Monitore tamanho e duração de backups
metrics.recordTimer("backup.duration", duration);
metrics.recordGauge("backup.size_bytes", sizeBytes);

// ✅ Implemente retenção de backups
List<BackupMetadata> expired = backups.stream()
    .filter(b -> b.isExpired(retentionPeriod))
    .toList();
```

### ❌ DON'T

```java
// ❌ NÃO armazene backups no mesmo servidor
fileStorage.save(backupFile, "/local/path");  // ❌

// ❌ NÃO deixe backups sem validação
restore(backupId);  // ❌ Sem verificar checksum!

// ❌ NÃO faça backup sem compressão
FileOutputStream fos = new FileOutputStream(backupFile);  // ❌ Sem GZIP!

// ❌ NÃO ignore erros de backup
try {
    createBackup();
} catch (Exception e) {
    // ❌ NUNCA ignorar!
}

// ❌ NÃO faça backup durante horário de pico
@Scheduled(cron = "0 0 14 * * ?")  // ❌ 2 PM - horário de pico!
```

---

## Ver Também

- [Data Migration](app-data-migration.md) - Schema evolution
- [Scheduler](app-scheduler.md) - Backup scheduling
- [S3 Adapter](../../commons-adapters-files-s3/) - Cloud storage
- [Observability](../guides/observability.md) - Backup monitoring
