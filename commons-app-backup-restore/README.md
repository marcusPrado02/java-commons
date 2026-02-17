# Commons App Backup & Restore

Backup and restore utilities for databases, filesystems, and cloud storage with support for full, incremental, and differential backups.

## Features

- **Multiple Backup Types**: Full, incremental, and differential backups
- **Filesystem Backup**: Backup directories and files to ZIP archives
- **Compression**: Optional ZIP compression for space savings
- **Integrity Verification**: SHA-256 checksums for backup validation
- **Incremental Backups**: Only backup changed files since last backup
- **Restore Operations**: Full restore with integrity verification
- **Metadata Tracking**: Complete backup metadata with timestamps
- **Result Pattern**: Consistent error handling with Result<T>

## Installation

### Maven

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-backup-restore</artifactId>
</dependency>
```

### Gradle

```gradle
implementation 'com.marcusprado02.commons:commons-app-backup-restore'
```

## Quick Start

### 1. Filesystem Backup

```java
// Create backup service for a directory
var service = FilesystemBackupService.forSource(Paths.get("/data/myapp"));

// Configure backup
var config = BackupConfiguration.builder()
    .destinationPath("/backups")
    .compressionEnabled(true)
    .build();

// Create full backup
var result = service.createFullBackup("Daily Backup", config);

if (result.isOk()) {
    var metadata = result.getOrNull();
    logger.info("Backup created: {} ({} bytes)",
        metadata.id(), metadata.size());
}
```

### 2. Incremental Backup

```java
// Create full backup first
var fullBackup = service.createFullBackup("Full Backup", config);
var parentId = fullBackup.getOrNull().id();

// Later, create incremental backup with only changed files
var incrementalResult = service.createIncrementalBackup(
    "Incremental Backup",
    parentId,
    config
);

if (incrementalResult.isOk()) {
    var metadata = incrementalResult.getOrNull();
    logger.info("Incremental backup: {} files changed",
        Long.parseLong(metadata.metadata().get("fileCount")));
}
```

### 3. Restore from Backup

```java
var restoreConfig = RestoreConfiguration.builder()
    .targetPath("/restore/myapp")
    .overwriteExisting(true)
    .verifyIntegrity(true)  // Verify SHA-256 checksum
    .build();

var restoreResult = service.restore(backupId, restoreConfig);

if (restoreResult.isOk()) {
    var result = restoreResult.getOrNull();
    logger.info("Restored {} files in {}",
        result.filesRestored(), result.duration());
}
```

## Backup Types

### Full Backup

Complete copy of all data:

```java
var fullBackup = service.createFullBackup("Full Backup", config);
```

**Use when:**
- Starting a new backup chain
- Need complete data copy
- First backup of a data source

### Incremental Backup

Only backs up files changed since the last backup (full or incremental):

```java
var incrementalBackup = service.createIncrementalBackup(
    "Incremental Backup",
    parentBackupId,
    config
);
```

**Use when:**
- Frequent backups needed
- Want to minimize backup size
- Storage space is limited

**Benefits:**
- Faster backup creation
- Smaller backup size
- Less storage consumption

**Drawbacks:**
- Requires parent backup
- Restore needs full chain
- More complex restore process

### Differential Backup

Backs up changes since the last full backup (not yet implemented):

```java
// TODO: Differential backup support
var differentialBackup = service.createDifferentialBackup(
    "Differential Backup",
    lastFullBackupId,
    config
);
```

## Configuration

### BackupConfiguration

```java
var config = BackupConfiguration.builder()
    .destinationPath("/backups")          // Where to store backups
    .compressionEnabled(true)             // Enable ZIP compression
    .encryptionEnabled(false)             // Enable AES encryption
    .encryptionKey("secret-key")          // Encryption key
    .parallel(false)                      // Parallel processing
    .option("custom-option", "value")     // Provider-specific options
    .build();
```

### RestoreConfiguration

```java
var restoreConfig = RestoreConfiguration.builder()
    .targetPath("/restore")               // Where to restore data
    .overwriteExisting(true)              // Overwrite existing files
    .verifyIntegrity(true)                // Verify checksum before restore
    .decryptionKey("secret-key")          // Decryption key
    .option("custom-option", "value")     // Provider-specific options
    .build();
```

## Backup Metadata

Each backup includes comprehensive metadata:

```java
var metadata = backupResult.getOrNull();

// Basic information
String id = metadata.id();
String name = metadata.name();
BackupType type = metadata.type();          // FULL, INCREMENTAL, DIFFERENTIAL
BackupStatus status = metadata.status();    // IN_PROGRESS, COMPLETED, FAILED, CANCELLED

// Location and size
String source = metadata.source();
String location = metadata.location();
long size = metadata.size();

// Timestamps
Instant createdAt = metadata.createdAt();
Optional<Instant> completedAt = metadata.completedAt();

// Integrity
Optional<String> checksum = metadata.checksum();

// Incremental backup chain
Optional<String> parentBackupId = metadata.parentBackupId();

// Additional metadata
Map<String, String> customMetadata = metadata.metadata();

// Status checks
boolean isComplete = metadata.isComplete();
boolean isInProgress = metadata.isInProgress();
boolean isFailed = metadata.isFailed();
```

## Backup Operations

### List All Backups

```java
var result = service.listBackups();

if (result.isOk()) {
    var backups = result.getOrNull();
    for (var backup : backups) {
        logger.info("Backup: {} - {} - {} bytes",
            backup.name(),
            backup.type(),
            backup.size());
    }
}
```

### Get Backup by ID

```java
var result = service.getBackup(backupId);

if (result.isOk()) {
    var metadata = result.getOrNull();
    logger.info("Backup status: {}", metadata.status());
}
```

### Verify Backup Integrity

```java
var result = service.verifyBackup(backupId);

if (result.isOk() && result.getOrNull()) {
    logger.info("Backup integrity verified");
} else {
    logger.error("Backup integrity check failed");
}
```

### Delete Backup

```java
var result = service.deleteBackup(backupId);

if (result.isOk()) {
    logger.info("Backup deleted successfully");
}
```

## Restore Operations

### Full Restore

```java
var restoreConfig = RestoreConfiguration.builder()
    .targetPath("/restore/path")
    .overwriteExisting(true)
    .verifyIntegrity(true)
    .build();

var result = service.restore(backupId, restoreConfig);

if (result.isOk()) {
    var restore = result.getOrNull();
    logger.info("Restored {} files ({} bytes) in {}",
        restore.filesRestored(),
        restore.bytesRestored(),
        restore.duration());
}
```

### Validate Restore (Dry Run)

```java
var result = service.validateRestore(backupId, restoreConfig);

if (result.isOk()) {
    logger.info("Restore validation successful");
    // Proceed with actual restore
} else {
    logger.error("Restore validation failed: {}",
        result.problemOrNull().message());
}
```

### Point-in-Time Restore

```java
// Restore to specific timestamp (requires incremental backup chain)
var result = service.restorePointInTime(
    "2026-02-17T10:00:00Z",
    restoreConfig
);
```

## Error Handling

All methods return `Result<T>` for consistent error handling:

```java
var result = service.createFullBackup("Backup", config);

if (result.isFail()) {
    var problem = result.problemOrNull();
    logger.error("Backup failed: {} - {}",
        problem.code().value(),
        problem.message());

    switch (problem.category()) {
        case NOT_FOUND -> handleNotFound();
        case BUSINESS -> handleBusinessError();
        case TECHNICAL -> handleTechnicalError();
    }
}
```

### Common Error Codes

| Error Code | Category | Description |
|------------|----------|-------------|
| `BACKUP.FULL_BACKUP_FAILED` | TECHNICAL | Failed to create full backup |
| `BACKUP.INCREMENTAL_BACKUP_FAILED` | TECHNICAL | Failed to create incremental backup |
| `BACKUP.PARENT_NOT_FOUND` | NOT_FOUND | Parent backup not found |
| `BACKUP.NO_PARENT_METADATA` | BUSINESS | Parent backup metadata missing |
| `BACKUP.NOT_FOUND` | NOT_FOUND | Backup not found |
| `BACKUP.DELETE_FAILED` | TECHNICAL | Failed to delete backup |
| `BACKUP.VERIFICATION_FAILED` | TECHNICAL | Failed to verify backup integrity |
| `RESTORE.INTEGRITY_CHECK_FAILED` | BUSINESS | Backup integrity check failed |
| `RESTORE.FAILED` | TECHNICAL | Failed to restore backup |
| `RESTORE.BACKUP_FILE_NOT_FOUND` | NOT_FOUND | Backup file not found |

## Best Practices

### 1. Regular Full Backups

Create full backups periodically as anchors for incremental chains:

```java
// Full backup every Sunday
if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.SUNDAY) {
    service.createFullBackup("Weekly Full Backup", config);
} else {
    service.createIncrementalBackup("Daily Incremental", lastBackupId, config);
}
```

### 2. Always Verify Integrity

Verify backup integrity before restore:

```java
var restoreConfig = RestoreConfiguration.builder()
    .targetPath(restorePath)
    .verifyIntegrity(true)  // Always enable
    .build();
```

### 3. Keep Backup Metadata

Store backup metadata externally for disaster recovery:

```java
var metadata = backupResult.getOrNull();
database.saveBackupMetadata(metadata);
```

### 4. Clean Up Old Backups

Implement retention policies:

```java
var backups = service.listBackups().getOrNull();
var oldBackups = backups.stream()
    .filter(b -> b.createdAt().isBefore(retentionCutoff))
    .toList();

for (var backup : oldBackups) {
    service.deleteBackup(backup.id());
}
```

### 5. Test Restores Regularly

Validate backups work:

```java
// Periodically test restore to temp location
var testRestore = service.restore(
    randomBackupId,
    RestoreConfiguration.builder()
        .targetPath(tempDir)
        .verifyIntegrity(true)
        .build()
);
```

## Advanced Features

### Custom Metadata

Add custom metadata to backups:

```java
var config = BackupConfiguration.builder()
    .destinationPath("/backups")
    .option("environment", "production")
    .option("application", "myapp")
    .option("version", "1.2.3")
    .build();
```

### Backup Chains

Track incremental backup chains:

```java
// Get full backup chain
var chain = new ArrayList<BackupMetadata>();
var current = latestIncremental;

while (current.parentBackupId().isPresent()) {
    chain.add(current);
    current = service.getBackup(current.parentBackupId().get()).getOrNull();
}
chain.add(current);  // Add full backup

Collections.reverse(chain);  // Full → Incremental 1 → Incremental 2 → ...
```

## Architecture

The backup & restore module follows port-adapter pattern:

```
commons-app-backup-restore/
├── BackupService              # Port interface
├── RestoreService             # Port interface
├── BackupConfiguration        # Configuration model
├── RestoreConfiguration       # Configuration model
├── BackupMetadata             # Domain model
├── RestoreResult              # Domain model
└── filesystem/
    └── FilesystemBackupService  # Adapter implementation
```

## Dependencies

- **commons-kernel-result**: Result<T> pattern
- **commons-kernel-errors**: Error handling
- **commons-kernel-core**: Core utilities
- **SLF4J**: Logging

## Limitations

Current implementation:
- ✅ Filesystem backup to ZIP archives
- ✅ Full and incremental backups
- ✅ Compression and integrity verification
- ✅ Restore with overwrite control
- ⏳ Database backup (planned)
- ⏳ S3 backup (planned)
- ⏳ Encryption (declared but not implemented)
- ⏳ Differential backups (planned)
- ⏳ Point-in-time restore (planned)
- ⏳ Parallel processing (planned)

## Future Enhancements

- Database backup adapters (PostgreSQL, MySQL, MongoDB)
- S3 backup adapter
- AES encryption support
- Differential backup implementation
- Point-in-time restore from incremental chain
- Parallel file processing
- Backup scheduling
- Retention policies
- Backup compression level control
- Progress callbacks
- Backup streaming for large files

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution guidelines.

## License

This project is licensed under the MIT License - see [LICENSE](../LICENSE) for details.
