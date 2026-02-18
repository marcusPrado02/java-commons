# API Reference: Data Migration

## Visão Geral

`commons-app-data-migration` fornece framework para migrações de esquema e dados versionadas, rastreáveis e reversíveis.

**Quando usar:**
- Schema evolution em produção
- Database migrations versionadas
- Data transformations entre versões
- Migrações entre ambientes (dev → staging → prod)
- Database refactoring seguro

**Padrões suportados:**
- Versioned migrations (Flyway/Liquibase style)
- Up/Down migrations
- Checksum validation
- Rollback support

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-data-migration</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### MigrationService

Interface principal para migrations.

```java
public interface MigrationService {
    
    /**
     * Executa migrations pendentes.
     */
    Result<MigrationResult> migrate();
    
    /**
     * Migra até versão específica.
     */
    Result<MigrationResult> migrateTo(MigrationVersion targetVersion);
    
    /**
     * Rollback da última migration.
     */
    Result<Void> rollback();
    
    /**
     * Rollback até versão específica.
     */
    Result<Void> rollbackTo(MigrationVersion targetVersion);
    
    /**
     * Lista migrations aplicadas.
     */
    List<MigrationHistory> getHistory();
    
    /**
     * Valida migrations pendentes.
     */
    Result<ValidationResult> validate();
}
```

### Migration

Define uma migration.

```java
public interface Migration {
    
    /**
     * Versão da migration (e.g., "1.0.0", "2.1.0").
     */
    MigrationVersion version();
    
    /**
     * Descrição da migration.
     */
    String description();
    
    /**
     * Executa migration (up).
     */
    Result<Void> up(MigrationContext context);
    
    /**
     * Reverte migration (down).
     */
    Result<Void> down(MigrationContext context);
    
    /**
     * Checksum para detectar mudanças.
     */
    default String checksum() {
        return calculateChecksum();
    }
}
```

### MigrationContext

Contexto de execução.

```java
public class MigrationContext {
    
    private final Connection connection;
    private final Map<String, Object> parameters;
    
    /**
     * Executa SQL statement.
     */
    public Result<Void> execute(String sql) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            return Result.ok();
        } catch (SQLException e) {
            return Result.fail(Problem.of(
                "MIGRATION.SQL_ERROR",
                e.getMessage()
            ));
        }
    }
    
    /**
     * Executa query e retorna resultados.
     */
    public Result<List<Map<String, Object>>> query(String sql) { ... }
    
    /**
     * Batch insert.
     */
    public Result<Void> batchInsert(
        String table,
        List<Map<String, Object>> rows
    ) { ... }
}
```

---

## 📝 SQL Migrations

### V1__CreateUsersTable.java

```java
@Component
public class V1__CreateUsersTable implements Migration {
    
    @Override
    public MigrationVersion version() {
        return MigrationVersion.of("1.0.0");
    }
    
    @Override
    public String description() {
        return "Create users table";
    }
    
    @Override
    public Result<Void> up(MigrationContext context) {
        String sql = """
            CREATE TABLE users (
                id VARCHAR(36) PRIMARY KEY,
                email VARCHAR(255) NOT NULL UNIQUE,
                name VARCHAR(255) NOT NULL,
                status VARCHAR(50) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                INDEX idx_users_email (email),
                INDEX idx_users_status (status)
            )
            """;
        
        return context.execute(sql);
    }
    
    @Override
    public Result<Void> down(MigrationContext context) {
        return context.execute("DROP TABLE users");
    }
}
```

### V2__AddUserRoles.java

```java
@Component
public class V2__AddUserRoles implements Migration {
    
    @Override
    public MigrationVersion version() {
        return MigrationVersion.of("2.0.0");
    }
    
    @Override
    public String description() {
        return "Add user roles table and column";
    }
    
    @Override
    public Result<Void> up(MigrationContext context) {
        // Create roles table
        Result<Void> createRoles = context.execute("""
            CREATE TABLE roles (
                id VARCHAR(36) PRIMARY KEY,
                name VARCHAR(50) NOT NULL UNIQUE,
                permissions TEXT,
                created_at TIMESTAMP NOT NULL
            )
            """);
        
        if (createRoles.isFail()) {
            return createRoles;
        }
        
        // Add role_id column to users
        Result<Void> addColumn = context.execute("""
            ALTER TABLE users
            ADD COLUMN role_id VARCHAR(36),
            ADD CONSTRAINT fk_users_role
                FOREIGN KEY (role_id) REFERENCES roles(id)
            """);
        
        if (addColumn.isFail()) {
            return addColumn;
        }
        
        // Insert default roles
        return context.batchInsert("roles", List.of(
            Map.of(
                "id", UUID.randomUUID().toString(),
                "name", "USER",
                "permissions", "[]",
                "created_at", Instant.now()
            ),
            Map.of(
                "id", UUID.randomUUID().toString(),
                "name", "ADMIN",
                "permissions", "[\"*\"]",
                "created_at", Instant.now()
            )
        ));
    }
    
    @Override
    public Result<Void> down(MigrationContext context) {
        // Remove foreign key
        Result<Void> dropFk = context.execute("""
            ALTER TABLE users
            DROP FOREIGN KEY fk_users_role
            """);
        
        if (dropFk.isFail()) {
            return dropFk;
        }
        
        // Remove column
        Result<Void> dropColumn = context.execute("""
            ALTER TABLE users
            DROP COLUMN role_id
            """);
        
        if (dropColumn.isFail()) {
            return dropColumn;
        }
        
        // Drop roles table
        return context.execute("DROP TABLE roles");
    }
}
```

---

## 🔄 Data Migrations

### V3__MigrateUserStatus.java

```java
@Component
public class V3__MigrateUserStatus implements Migration {
    
    @Override
    public MigrationVersion version() {
        return MigrationVersion.of("3.0.0");
    }
    
    @Override
    public String description() {
        return "Migrate user status from enum to table";
    }
    
    @Override
    public Result<Void> up(MigrationContext context) {
        // Step 1: Create status table
        Result<Void> createTable = context.execute("""
            CREATE TABLE user_statuses (
                id VARCHAR(36) PRIMARY KEY,
                code VARCHAR(50) NOT NULL UNIQUE,
                description VARCHAR(255)
            )
            """);
        
        if (createTable.isFail()) {
            return createTable;
        }
        
        // Step 2: Insert statuses from enum
        Result<Void> insertStatuses = context.batchInsert(
            "user_statuses",
            List.of(
                Map.of(
                    "id", UUID.randomUUID().toString(),
                    "code", "ACTIVE",
                    "description", "User is active"
                ),
                Map.of(
                    "id", UUID.randomUUID().toString(),
                    "code", "INACTIVE",
                    "description", "User is inactive"
                ),
                Map.of(
                    "id", UUID.randomUUID().toString(),
                    "code", "SUSPENDED",
                    "description", "User is suspended"
                )
            )
        );
        
        if (insertStatuses.isFail()) {
            return insertStatuses;
        }
        
        // Step 3: Add status_id column
        Result<Void> addColumn = context.execute("""
            ALTER TABLE users
            ADD COLUMN status_id VARCHAR(36)
            """);
        
        if (addColumn.isFail()) {
            return addColumn;
        }
        
        // Step 4: Migrate data (status enum -> status_id)
        Result<List<Map<String, Object>>> statuses = 
            context.query("SELECT id, code FROM user_statuses");
        
        if (statuses.isFail()) {
            return Result.fail(statuses.problemOrNull());
        }
        
        Map<String, String> statusMap = statuses.getOrThrow().stream()
            .collect(Collectors.toMap(
                row -> (String) row.get("code"),
                row -> (String) row.get("id")
            ));
        
        for (Map.Entry<String, String> entry : statusMap.entrySet()) {
            String statusCode = entry.getKey();
            String statusId = entry.getValue();
            
            Result<Void> update = context.execute(
                String.format(
                    "UPDATE users SET status_id = '%s' WHERE status = '%s'",
                    statusId,
                    statusCode
                )
            );
            
            if (update.isFail()) {
                return update;
            }
        }
        
        // Step 5: Add foreign key constraint
        Result<Void> addFk = context.execute("""
            ALTER TABLE users
            ADD CONSTRAINT fk_users_status
                FOREIGN KEY (status_id) REFERENCES user_statuses(id)
            """);
        
        if (addFk.isFail()) {
            return addFk;
        }
        
        // Step 6: Drop old status column
        return context.execute("ALTER TABLE users DROP COLUMN status");
    }
    
    @Override
    public Result<Void> down(MigrationContext context) {
        // Reverse migration
        
        // Step 1: Add status column back
        Result<Void> addColumn = context.execute("""
            ALTER TABLE users
            ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
            """);
        
        if (addColumn.isFail()) {
            return addColumn;
        }
        
        // Step 2: Migrate data back (status_id -> status)
        Result<Void> migrateBack = context.execute("""
            UPDATE users u
            INNER JOIN user_statuses s ON u.status_id = s.id
            SET u.status = s.code
            """);
        
        if (migrateBack.isFail()) {
            return migrateBack;
        }
        
        // Step 3: Drop foreign key
        Result<Void> dropFk = context.execute("""
            ALTER TABLE users
            DROP FOREIGN KEY fk_users_status
            """);
        
        if (dropFk.isFail()) {
            return dropFk;
        }
        
        // Step 4: Drop status_id column
        Result<Void> dropColumn = context.execute("""
            ALTER TABLE users
            DROP COLUMN status_id
            """);
        
        if (dropColumn.isFail()) {
            return dropColumn;
        }
        
        // Step 5: Drop user_statuses table
        return context.execute("DROP TABLE user_statuses");
    }
}
```

---

## 🚀 Migration Service Implementation

### Default Migration Service

```java
@Service
public class DefaultMigrationService implements MigrationService {
    
    private final DataSource dataSource;
    private final List<Migration> migrations;
    private final MigrationHistoryRepository historyRepository;
    private final StructuredLog log;
    
    @Override
    public Result<MigrationResult> migrate() {
        log.info("Starting database migration").log();
        
        try (Connection conn = dataSource.getConnection()) {
            // Ensure migration history table exists
            ensureMigrationHistoryTable(conn);
            
            // Get applied migrations
            List<MigrationHistory> history = historyRepository.findAll();
            Set<MigrationVersion> appliedVersions = history.stream()
                .map(MigrationHistory::version)
                .collect(Collectors.toSet());
            
            // Find pending migrations
            List<Migration> pending = migrations.stream()
                .filter(m -> !appliedVersions.contains(m.version()))
                .sorted(Comparator.comparing(Migration::version))
                .toList();
            
            if (pending.isEmpty()) {
                log.info("No pending migrations").log();
                return Result.ok(new MigrationResult(0, Duration.ZERO));
            }
            
            log.info("Found pending migrations")
                .field("count", pending.size())
                .log();
            
            Instant start = Instant.now();
            int appliedCount = 0;
            
            // Apply each migration in transaction
            for (Migration migration : pending) {
                Result<Void> result = applyMigration(conn, migration);
                
                if (result.isFail()) {
                    log.error("Migration failed")
                        .field("version", migration.version().value())
                        .field("error", result.problemOrNull().detail())
                        .log();
                    
                    return Result.fail(result.problemOrNull());
                }
                
                appliedCount++;
            }
            
            Duration duration = Duration.between(start, Instant.now());
            
            log.info("Database migration completed")
                .field("appliedCount", appliedCount)
                .field("duration", duration)
                .log();
            
            return Result.ok(new MigrationResult(appliedCount, duration));
            
        } catch (Exception e) {
            log.error("Migration failed", e).log();
            return Result.fail(Problem.of(
                "MIGRATION.FAILED",
                e.getMessage()
            ));
        }
    }
    
    private Result<Void> applyMigration(
        Connection conn,
        Migration migration
    ) {
        log.info("Applying migration")
            .field("version", migration.version().value())
            .field("description", migration.description())
            .log();
        
        Instant start = Instant.now();
        
        try {
            // Start transaction
            conn.setAutoCommit(false);
            
            // Create migration context
            MigrationContext context = new MigrationContext(conn, Map.of());
            
            // Execute migration
            Result<Void> result = migration.up(context);
            
            if (result.isFail()) {
                conn.rollback();
                return result;
            }
            
            // Record in history
            Duration duration = Duration.between(start, Instant.now());
            
            MigrationHistory history = new MigrationHistory(
                migration.version(),
                migration.description(),
                migration.checksum(),
                start,
                duration,
                MigrationStatus.SUCCESS
            );
            
            historyRepository.save(history);
            
            // Commit transaction
            conn.commit();
            
            log.info("Migration applied successfully")
                .field("version", migration.version().value())
                .field("duration", duration)
                .log();
            
            return Result.ok();
            
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                log.error("Rollback failed", rollbackEx).log();
            }
            
            return Result.fail(Problem.of(
                "MIGRATION.EXECUTION_FAILED",
                e.getMessage()
            ));
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                log.error("Failed to reset autocommit", e).log();
            }
        }
    }
    
    @Override
    public Result<Void> rollback() {
        // Find last applied migration
        Optional<MigrationHistory> lastOpt = historyRepository.findLast();
        
        if (lastOpt.isEmpty()) {
            return Result.fail(Problem.of(
                "MIGRATION.NO_HISTORY",
                "No migrations to rollback"
            ));
        }
        
        MigrationHistory last = lastOpt.get();
        
        // Find migration instance
        Optional<Migration> migrationOpt = migrations.stream()
            .filter(m -> m.version().equals(last.version()))
            .findFirst();
        
        if (migrationOpt.isEmpty()) {
            return Result.fail(Problem.of(
                "MIGRATION.NOT_FOUND",
                "Migration not found: " + last.version()
            ));
        }
        
        Migration migration = migrationOpt.get();
        
        log.info("Rolling back migration")
            .field("version", migration.version().value())
            .log();
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            MigrationContext context = new MigrationContext(conn, Map.of());
            Result<Void> result = migration.down(context);
            
            if (result.isFail()) {
                conn.rollback();
                return result;
            }
            
            // Remove from history
            historyRepository.delete(last.version());
            
            conn.commit();
            
            log.info("Migration rolled back successfully")
                .field("version", migration.version().value())
                .log();
            
            return Result.ok();
            
        } catch (Exception e) {
            return Result.fail(Problem.of(
                "MIGRATION.ROLLBACK_FAILED",
                e.getMessage()
            ));
        }
    }
    
    @Override
    public Result<ValidationResult> validate() {
        List<String> errors = new ArrayList<>();
        
        // Check for checksum changes
        List<MigrationHistory> history = historyRepository.findAll();
        
        for (MigrationHistory record : history) {
            Optional<Migration> migrationOpt = migrations.stream()
                .filter(m -> m.version().equals(record.version()))
                .findFirst();
            
            if (migrationOpt.isEmpty()) {
                errors.add("Migration removed: " + record.version());
                continue;
            }
            
            Migration migration = migrationOpt.get();
            
            if (!migration.checksum().equals(record.checksum())) {
                errors.add(String.format(
                    "Checksum mismatch for %s: expected %s, got %s",
                    migration.version(),
                    record.checksum(),
                    migration.checksum()
                ));
            }
        }
        
        return Result.ok(new ValidationResult(errors));
    }
}
```

---

## 🔍 Migration History

### History Model

```java
public record MigrationHistory(
    MigrationVersion version,
    String description,
    String checksum,
    Instant appliedAt,
    Duration duration,
    MigrationStatus status
) {}

public enum MigrationStatus {
    SUCCESS,
    FAILED,
    ROLLED_BACK
}
```

### History Repository

```java
public interface MigrationHistoryRepository {
    
    void save(MigrationHistory history);
    
    List<MigrationHistory> findAll();
    
    Optional<MigrationHistory> findLast();
    
    Optional<MigrationHistory> findByVersion(MigrationVersion version);
    
    void delete(MigrationVersion version);
}
```

---

## 🧪 Testing

### Migration Tests

```java
class MigrationServiceTest {
    
    private DataSource dataSource;
    private MigrationService migrationService;
    
    @BeforeEach
    void setUp() {
        dataSource = createTestDataSource();
        
        List<Migration> migrations = List.of(
            new V1__CreateUsersTable(),
            new V2__AddUserRoles(),
            new V3__MigrateUserStatus()
        );
        
        migrationService = new DefaultMigrationService(
            dataSource,
            migrations,
            new JdbcMigrationHistoryRepository(dataSource),
            StructuredLog.noop()
        );
    }
    
    @Test
    void shouldApplyAllPendingMigrations() {
        // When
        Result<MigrationResult> result = migrationService.migrate();
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        MigrationResult migrationResult = result.getOrThrow();
        assertThat(migrationResult.appliedCount()).isEqualTo(3);
        
        // Verify tables created
        assertThat(tableExists("users")).isTrue();
        assertThat(tableExists("roles")).isTrue();
        assertThat(tableExists("user_statuses")).isTrue();
    }
    
    @Test
    void shouldRollbackLastMigration() {
        // Given: Migrations applied
        migrationService.migrate();
        
        // When: Rollback
        Result<Void> result = migrationService.rollback();
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        // V3 tables should be gone
        assertThat(tableExists("user_statuses")).isFalse();
        assertThat(columnExists("users", "status")).isTrue();
        assertThat(columnExists("users", "status_id")).isFalse();
    }
    
    @Test
    void shouldDetectChecksumChanges() {
        // Given: Migration applied
        migrationService.migrate();
        
        // When: Migration changed
        V1__CreateUsersTable migration = new V1__CreateUsersTable() {
            @Override
            public String checksum() {
                return "different-checksum";
            }
        };
        
        // Then: Validation fails
        Result<ValidationResult> result = migrationService.validate();
        
        assertThat(result.isOk()).isTrue();
        ValidationResult validation = result.getOrThrow();
        assertThat(validation.hasErrors()).isTrue();
        assertThat(validation.errors()).contains("Checksum mismatch");
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use versionamento semântico
@Override
public MigrationVersion version() {
    return MigrationVersion.of("1.2.3");
}

// ✅ Nomeie migrations descritivamente
public class V5__AddEmailVerificationToUsers implements Migration

// ✅ Use transações
conn.setAutoCommit(false);
try {
    migration.up(context);
    conn.commit();
} catch (Exception e) {
    conn.rollback();
}

// ✅ Teste rollbacks
@Test
void shouldRollbackSuccessfully() {
    migrationService.migrate();
    Result<Void> result = migrationService.rollback();
    assertThat(result.isOk()).isTrue();
}

// ✅ Valide migrations antes de deploy
Result<ValidationResult> validation = migrationService.validate();
if (validation.getOrThrow().hasErrors()) {
    throw new RuntimeException("Migration validation failed");
}
```

### ❌ DON'T

```java
// ❌ NÃO modifique migrations aplicadas
public class V1__CreateUsersTable {
    @Override
    public Result<Void> up(MigrationContext ctx) {
        return ctx.execute("CREATE TABLE users...");  // ❌ Mudou!
    }
}

// ❌ NÃO use migrations para dados variáveis
public class V5__InsertAdminUser {  // ❌ Use seed data!
    @Override
    public Result<Void> up(MigrationContext ctx) {
        return ctx.execute("INSERT INTO users...");
    }
}

// ❌ NÃO ignore falhas
Result<Void> result = migration.up(context);
// ❌ Sem verificar result!

// ❌ NÃO faça migrations sem rollback
@Override
public Result<Void> down(MigrationContext ctx) {
    return Result.ok();  // ❌ Não implementado!
}

// ❌ NÃO delete migrations antigas
// git rm V1__CreateUsersTable.java  // ❌ NUNCA deletar!
```

---

## Ver Também

- [Backup & Restore](app-backup-restore.md) - Database backups
- [JPA Adapter](../../commons-adapters-persistence-jpa/) - ORM integration
- [Scheduler](app-scheduler.md) - Scheduled migrations
- [Observability](../guides/observability.md) - Migration monitoring
