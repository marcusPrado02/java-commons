# Commons App Data Migration

Database migration framework with support for Flyway, Liquibase, and custom Java-based migrations.

## Features

- **Multiple Migration Engines**: Flyway, Liquibase, and custom migrations
- **Unified API**: Single `MigrationEngine` interface for all engines
- **Result-based Error Handling**: Uses `Result<T>` pattern for safe error handling
- **Transaction Support**: Configurable transactional execution
- **Migration History**: Tracks applied migrations with timestamps and execution times
- **Validation**: Validates migrations against the database
- **Baseline Support**: Creates baselines for existing databases

## Installation

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-data-migration</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- For Flyway support (optional) -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>10.7.2</version>
</dependency>

<!-- For Liquibase support (optional) -->
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
    <version>4.25.1</version>
</dependency>
```

## Quick Start

### 1. Flyway Migrations

Create migration files in `src/main/resources/db/migration/`:

```sql
-- V1__create_users.sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

-- V2__add_users_status.sql
ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';
```

Execute migrations:

```java
import com.marcusprado02.commons.app.datamigration.MigrationEngine;
import com.marcusprado02.commons.app.datamigration.MigrationResult;
import com.marcusprado02.commons.app.datamigration.flyway.FlywayMigrationEngine;
import com.marcusprado02.commons.kernel.result.Result;

MigrationEngine engine = FlywayMigrationEngine.create(dataSource);
Result<MigrationResult> result = engine.migrate();

if (result.isOk()) {
    MigrationResult migrationResult = result.getOrNull();
    System.out.println("Applied " + migrationResult.migrationsExecuted() + " migrations");
    System.out.println("Current version: " + migrationResult.targetVersion());
} else {
    System.err.println("Migration failed: " + result.getErrorOrNull().message());
}
```

### 2. Liquibase Migrations

Create changelog in `src/main/resources/db/changelog/db.changelog-master.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.25.xsd">

    <changeSet id="1" author="marcus">
        <createTable tableName="users">
            <column name="id" type="BIGINT">
                <constraints primaryKey="true"/>
            </column>
            <column name="name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="email" type="VARCHAR(255)">
                <constraints nullable="false" unique="true"/>
            </column>
        </createTable>
    </changeSet>

    <changeSet id="2" author="marcus">
        <addColumn tableName="users">
            <column name="status" type="VARCHAR(20)" defaultValue="ACTIVE"/>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

Execute migrations:

```java
import com.marcusprado02.commons.app.datamigration.liquibase.LiquibaseMigrationEngine;

MigrationEngine engine = LiquibaseMigrationEngine.builder()
    .dataSource(dataSource)
    .changeLogFile("db/changelog/db.changelog-master.xml")
    .build();

Result<MigrationResult> result = engine.migrate();
```

### 3. Custom Java Migrations

Create a custom migration:

```java
import com.marcusprado02.commons.app.datamigration.custom.CustomMigration;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class V1_2__InsertDefaultData implements CustomMigration {

    @Override
    public void migrate(Connection connection) throws Exception {
        String sql = "INSERT INTO users (id, name, email) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, 1L);
            stmt.setString(2, "Admin");
            stmt.setString(3, "admin@example.com");
            stmt.executeUpdate();
        }
    }

    @Override
    public String getVersion() {
        return "1.2";
    }

    @Override
    public String getDescription() {
        return "Insert default data";
    }

    @Override
    public boolean isTransactional() {
        return true; // Run in transaction
    }
}
```

Execute custom migrations:

```java
import com.marcusprado02.commons.app.datamigration.custom.CustomMigrationEngine;

MigrationEngine engine = CustomMigrationEngine.builder()
    .dataSource(dataSource)
    .migration(new V1_2__InsertDefaultData())
    .migration(new V1_3__CreateIndexes())
    .build();

Result<MigrationResult> result = engine.migrate();
```

## Advanced Usage

### Flyway with Custom Configuration

```java
MigrationEngine engine = FlywayMigrationEngine.builder()
    .dataSource(dataSource)
    .locations("classpath:db/migration", "classpath:db/seeds")
    .table("schema_version")
    .baselineVersion("1.0")
    .baselineDescription("Initial baseline")
    .validateOnMigrate(true)
    .outOfOrder(false)
    .cleanDisabled(true) // Disable clean in production
    .build();

// Execute migration
Result<MigrationResult> result = engine.migrate();

// Validate migrations
Result<Void> validationResult = engine.validate();

// Get migration info
List<MigrationInfo> info = engine.info();
for (MigrationInfo migration : info) {
    System.out.println(migration.version() + " - " + migration.state());
}

// Baseline existing database
Result<Void> baselineResult = engine.baseline("1.0", "Initial version");
```

### Liquibase with Contexts and Labels

```java
MigrationEngine engine = LiquibaseMigrationEngine.builder()
    .dataSource(dataSource)
    .changeLogFile("db/changelog/db.changelog-master.xml")
    .contexts("production", "security")
    .labels("v1.0", "critical")
    .build();

Result<MigrationResult> result = engine.migrate();
```

Changelog with contexts and labels:

```xml
<changeSet id="3" author="marcus" context="production" labels="v1.0">
    <createIndex tableName="users" indexName="idx_users_email">
        <column name="email"/>
    </createIndex>
</changeSet>

<changeSet id="4" author="marcus" context="development" labels="test-data">
    <insert tableName="users">
        <column name="id" valueNumeric="999"/>
        <column name="name" value="Test User"/>
        <column name="email" value="test@example.com"/>
    </insert>
</changeSet>
```

### Custom Migrations with Non-Transactional Support

```java
public class V2_0__CreateConcurrentIndex implements CustomMigration {

    @Override
    public void migrate(Connection connection) throws Exception {
        // PostgreSQL concurrent index creation
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE INDEX CONCURRENTLY idx_users_name ON users(name)");
        }
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public String getDescription() {
        return "Create concurrent index on users.name";
    }

    @Override
    public boolean isTransactional() {
        return false; // Must run outside transaction
    }
}
```

### Migration Validation

```java
MigrationEngine engine = FlywayMigrationEngine.create(dataSource);

// Validate before migrating
Result<Void> validationResult = engine.validate();
if (validationResult.isFail()) {
    System.err.println("Validation failed: " + validationResult.getErrorOrNull().message());
    return;
}

// Proceed with migration
Result<MigrationResult> result = engine.migrate();
```

### Getting Migration Information

```java
MigrationEngine engine = FlywayMigrationEngine.create(dataSource);

List<MigrationInfo> migrations = engine.info();

for (MigrationInfo migration : migrations) {
    System.out.println("Version: " + migration.version());
    System.out.println("Description: " + migration.description());
    System.out.println("Type: " + migration.type());
    System.out.println("State: " + migration.state());

    if (migration.isApplied()) {
        System.out.println("Installed on: " + migration.installedOn());
        System.out.println("Execution time: " + migration.executionTime() + "ms");
    }

    System.out.println("---");
}

// Check for pending migrations
if (engine.hasPendingMigrations()) {
    System.out.println("There are pending migrations to apply");
}

// Get current version
String version = engine.currentVersion();
System.out.println("Current database version: " + version);
```

### Baseline Existing Database

```java
MigrationEngine engine = FlywayMigrationEngine.builder()
    .dataSource(dataSource)
    .baselineVersion("1.5")
    .baselineDescription("Production baseline")
    .build();

// Create baseline
Result<Void> result = engine.baseline("1.5", "Production baseline");

if (result.isOk()) {
    System.out.println("Baseline created at version 1.5");
}
```

### Clean Database (Development Only)

```java
MigrationEngine engine = FlywayMigrationEngine.builder()
    .dataSource(dataSource)
    .cleanDisabled(false) // Allow clean
    .build();

// WARNING: This drops all database objects
Result<Void> result = engine.clean();

if (result.isOk()) {
    System.out.println("Database cleaned");

    // Re-apply migrations
    engine.migrate();
}
```

### Repair Migration History

```java
MigrationEngine engine = FlywayMigrationEngine.create(dataSource);

// Repair removes failed migrations and realigns checksums
Result<Void> result = engine.repair();

if (result.isOk()) {
    System.out.println("Migration history repaired");
}
```

## Migration States

Migrations can be in the following states:

- **PENDING**: Not yet applied
- **SUCCESS**: Successfully applied
- **FAILED**: Failed during execution
- **BASELINE**: Part of the baseline
- **OUTDATED**: Applied but newer version exists
- **ROLLED_BACK**: Rolled back (not supported by all engines)

## Best Practices

### 1. Naming Conventions

**Flyway:**
- `V{version}__{description}.sql` - Versioned migration
- `U{version}__{description}.sql` - Undo migration
- `R__{description}.sql` - Repeatable migration

**Custom:**
- `V{version}__{description}` - Class name
- Version format: "1.0", "1.1", "2.0"

### 2. Migration Guidelines

- **Never modify applied migrations**: Create new migrations instead
- **Use transactions**: Enable for data consistency
- **Test migrations**: Always test on a copy of production data
- **Backwards compatible**: Don't break existing application code
- **Idempotent**: Migrations should be safe to re-run
- **Small changes**: Keep migrations focused and small

### 3. Version Control

- Store migrations in version control
- Review migrations in code reviews
- Tag releases with migration versions

### 4. Production Deployment

- Validate migrations in staging first
- Backup database before migrations
- Consider downtime requirements
- Monitor migration execution
- Have rollback plan ready

### 5. Security

- Disable `clean()` in production
- Use database users with appropriate permissions
- Audit migration execution
- Encrypt sensitive migration data

## Spring Boot Integration

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class MigrationConfig {

    @Bean
    public MigrationEngine migrationEngine(DataSource dataSource) {
        return FlywayMigrationEngine.builder()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .validateOnMigrate(true)
            .cleanDisabled(true)
            .build();
    }

    @Bean
    public ApplicationRunner runMigrations(MigrationEngine migrationEngine) {
        return args -> {
            Result<MigrationResult> result = migrationEngine.migrate();

            if (result.isFail()) {
                throw new IllegalStateException(
                    "Migration failed: " + result.getErrorOrNull().message()
                );
            }
        };
    }
}
```

## Comparison: Flyway vs Liquibase vs Custom

| Feature | Flyway | Liquibase | Custom |
|---------|--------|-----------|--------|
| SQL Migrations | ✅ | ✅ | ❌ |
| Java Migrations | ✅ | ⚠️ Limited | ✅ |
| Rollback | ⚠️ Paid | ✅ | ⚠️ Manual |
| Contexts/Labels | ❌ | ✅ | ❌ |
| Learning Curve | Easy | Medium | Easy |
| Flexibility | Medium | High | Highest |
| Community | Large | Large | N/A |

**Choose Flyway when:**
- Simple SQL migrations
- Prefer simplicity over features
- Java migrations needed

**Choose Liquibase when:**
- Need contexts and labels
- Require rollback support
- Database-agnostic migrations
- Complex change management

**Choose Custom when:**
- Complex Java logic required
- Need fine-grained control
- Specific transaction handling
- Integration with domain code

## Dependencies

```xml
<!-- Core dependency -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-data-migration</artifactId>
</dependency>

<!-- Flyway (optional) -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>10.7.2</version>
    <optional>true</optional>
</dependency>

<!-- Liquibase (optional) -->
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
    <version>4.25.1</version>
    <optional>true</optional>
</dependency>
```

## API Reference

### MigrationEngine

Core interface for all migration engines:

```java
public interface MigrationEngine {
    Result<MigrationResult> migrate();
    Result<Void> validate();
    List<MigrationInfo> info();
    Result<Void> clean();
    Result<Void> repair();
    Result<Void> baseline(String version, String description);

    default boolean hasPendingMigrations() { ... }
    default String currentVersion() { ... }
}
```

### MigrationResult

Result of migration execution:

```java
public record MigrationResult(
    int migrationsExecuted,
    String targetVersion,
    boolean success,
    List<String> warnings
) {
    public static MigrationResult success(int count, String version);
    public static MigrationResult successWithWarnings(int count, String version, List<String> warnings);
    public static MigrationResult failure();
}
```

### MigrationInfo

Information about a single migration:

```java
public record MigrationInfo(
    String version,
    String description,
    String type,
    MigrationState state,
    Instant installedOn,
    Integer executionTime,
    String checksum
) {
    public boolean isApplied();
    public boolean isPending();
    public boolean isFailed();
}
```

### MigrationState

```java
public enum MigrationState {
    PENDING,       // Not yet applied
    SUCCESS,       // Successfully applied
    FAILED,        // Failed during execution
    BASELINE,      // Part of baseline
    OUTDATED,      // Applied but outdated
    ROLLED_BACK    // Rolled back
}
```

## License

This module is part of Commons and is licensed under the same terms.
