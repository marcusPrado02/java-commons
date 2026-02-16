package com.marcusprado02.commons.app.datamigration;

/** State of a database migration. */
public enum MigrationState {
  /** Migration is pending and has not been applied. */
  PENDING,

  /** Migration was successfully applied. */
  SUCCESS,

  /** Migration failed during execution. */
  FAILED,

  /** Migration was manually applied or skipped. */
  BASELINE,

  /** Migration checksum mismatch - file was modified after application. */
  OUTDATED,

  /** Migration was rolled back. */
  ROLLED_BACK
}
