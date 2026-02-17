package com.marcusprado02.commons.app.backup;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Port interface for restore operations.
 *
 * <p>This interface defines the contract for restoring data from backups. Implementations should
 * handle:
 *
 * <ul>
 *   <li>Full restore from a complete backup
 *   <li>Point-in-time restore from incremental backups
 *   <li>Selective restore (specific files/records)
 *   <li>Restore validation and verification
 *   <li>Rollback on failure
 * </ul>
 */
public interface RestoreService {

  /**
   * Restores data from a backup.
   *
   * @param backupId backup identifier
   * @param configuration restore configuration options
   * @return restore result
   */
  Result<RestoreResult> restore(String backupId, RestoreConfiguration configuration);

  /**
   * Restores data from a specific point in time using incremental backups.
   *
   * @param pointInTime ISO-8601 timestamp
   * @param configuration restore configuration options
   * @return restore result
   */
  Result<RestoreResult> restorePointInTime(String pointInTime, RestoreConfiguration configuration);

  /**
   * Validates a restore operation without actually performing it.
   *
   * @param backupId backup identifier
   * @param configuration restore configuration options
   * @return validation result
   */
  Result<Void> validateRestore(String backupId, RestoreConfiguration configuration);
}
