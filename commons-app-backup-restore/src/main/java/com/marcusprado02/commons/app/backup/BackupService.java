package com.marcusprado02.commons.app.backup;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;

/**
 * Port interface for backup operations.
 *
 * <p>This interface defines the contract for backing up data sources like databases, filesystems,
 * or cloud storage. Implementations should handle:
 *
 * <ul>
 *   <li>Full backups (complete data copy)
 *   <li>Incremental backups (changes since last backup)
 *   <li>Differential backups (changes since last full backup)
 *   <li>Backup metadata tracking
 *   <li>Error handling and recovery
 * </ul>
 */
public interface BackupService {

  /**
   * Creates a full backup of the data source.
   *
   * @param name backup name/description
   * @param configuration backup configuration options
   * @return backup metadata
   */
  Result<BackupMetadata> createFullBackup(String name, BackupConfiguration configuration);

  /**
   * Creates an incremental backup (changes since last backup).
   *
   * @param name backup name/description
   * @param parentBackupId ID of the parent backup
   * @param configuration backup configuration options
   * @return backup metadata
   */
  Result<BackupMetadata> createIncrementalBackup(
      String name, String parentBackupId, BackupConfiguration configuration);

  /**
   * Lists all backups for the data source.
   *
   * @return list of backup metadata
   */
  Result<List<BackupMetadata>> listBackups();

  /**
   * Gets metadata for a specific backup.
   *
   * @param backupId backup identifier
   * @return backup metadata
   */
  Result<BackupMetadata> getBackup(String backupId);

  /**
   * Deletes a backup.
   *
   * @param backupId backup identifier
   * @return void result
   */
  Result<Void> deleteBackup(String backupId);

  /**
   * Verifies backup integrity using checksum.
   *
   * @param backupId backup identifier
   * @return true if backup is valid, false otherwise
   */
  Result<Boolean> verifyBackup(String backupId);
}
