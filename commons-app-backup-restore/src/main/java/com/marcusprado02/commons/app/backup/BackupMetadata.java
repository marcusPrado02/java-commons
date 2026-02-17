package com.marcusprado02.commons.app.backup;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Metadata about a backup.
 *
 * @param id backup identifier
 * @param name backup name/description
 * @param type backup type (FULL, INCREMENTAL, DIFFERENTIAL)
 * @param source source identifier (database name, directory path, etc.)
 * @param location backup storage location
 * @param size backup size in bytes
 * @param status backup status
 * @param createdAt backup creation timestamp
 * @param completedAt backup completion timestamp
 * @param parentBackupId parent backup ID for incremental backups
 * @param checksum backup checksum for integrity verification
 * @param metadata additional metadata
 * @param error error message if failed
 */
public record BackupMetadata(
    String id,
    String name,
    BackupType type,
    String source,
    String location,
    long size,
    BackupStatus status,
    Instant createdAt,
    Optional<Instant> completedAt,
    Optional<String> parentBackupId,
    Optional<String> checksum,
    Map<String, String> metadata,
    Optional<String> error) {

  public BackupMetadata {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(location, "location");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(createdAt, "createdAt");
    completedAt = completedAt == null ? Optional.empty() : completedAt;
    parentBackupId = parentBackupId == null ? Optional.empty() : parentBackupId;
    checksum = checksum == null ? Optional.empty() : checksum;
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    error = error == null ? Optional.empty() : error;
  }

  /** Backup type. */
  public enum BackupType {
    /** Full backup of all data. */
    FULL,
    /** Incremental backup (changes since last backup). */
    INCREMENTAL,
    /** Differential backup (changes since last full backup). */
    DIFFERENTIAL
  }

  /** Backup status. */
  public enum BackupStatus {
    /** Backup is in progress. */
    IN_PROGRESS,
    /** Backup completed successfully. */
    COMPLETED,
    /** Backup failed. */
    FAILED,
    /** Backup was cancelled. */
    CANCELLED
  }

  /** Checks if backup is complete. */
  public boolean isComplete() {
    return status == BackupStatus.COMPLETED;
  }

  /** Checks if backup is in progress. */
  public boolean isInProgress() {
    return status == BackupStatus.IN_PROGRESS;
  }

  /** Checks if backup failed. */
  public boolean isFailed() {
    return status == BackupStatus.FAILED;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String id;
    private String name;
    private BackupType type = BackupType.FULL;
    private String source;
    private String location;
    private long size;
    private BackupStatus status = BackupStatus.IN_PROGRESS;
    private Instant createdAt = Instant.now();
    private Optional<Instant> completedAt = Optional.empty();
    private Optional<String> parentBackupId = Optional.empty();
    private Optional<String> checksum = Optional.empty();
    private Map<String, String> metadata = Map.of();
    private Optional<String> error = Optional.empty();

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder type(BackupType type) {
      this.type = type;
      return this;
    }

    public Builder source(String source) {
      this.source = source;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    public Builder size(long size) {
      this.size = size;
      return this;
    }

    public Builder status(BackupStatus status) {
      this.status = status;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder completedAt(Instant completedAt) {
      this.completedAt = Optional.ofNullable(completedAt);
      return this;
    }

    public Builder parentBackupId(String parentBackupId) {
      this.parentBackupId = Optional.ofNullable(parentBackupId);
      return this;
    }

    public Builder checksum(String checksum) {
      this.checksum = Optional.ofNullable(checksum);
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata == null ? Map.of() : metadata;
      return this;
    }

    public Builder error(String error) {
      this.error = Optional.ofNullable(error);
      return this;
    }

    public BackupMetadata build() {
      return new BackupMetadata(
          id,
          name,
          type,
          source,
          location,
          size,
          status,
          createdAt,
          completedAt,
          parentBackupId,
          checksum,
          metadata,
          error);
    }
  }
}
