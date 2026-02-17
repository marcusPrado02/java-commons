package com.marcusprado02.commons.app.backup;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a restore operation.
 *
 * @param backupId backup identifier
 * @param targetPath target path where data was restored
 * @param filesRestored number of files/records restored
 * @param bytesRestored total bytes restored
 * @param duration restore duration
 * @param completedAt restore completion timestamp
 * @param metadata additional metadata
 * @param error error message if failed
 */
public record RestoreResult(
    String backupId,
    String targetPath,
    long filesRestored,
    long bytesRestored,
    Duration duration,
    Instant completedAt,
    Map<String, String> metadata,
    Optional<String> error) {

  public RestoreResult {
    Objects.requireNonNull(backupId, "backupId");
    Objects.requireNonNull(targetPath, "targetPath");
    Objects.requireNonNull(duration, "duration");
    Objects.requireNonNull(completedAt, "completedAt");
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    error = error == null ? Optional.empty() : error;
  }

  public boolean isSuccess() {
    return error.isEmpty();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String backupId;
    private String targetPath;
    private long filesRestored;
    private long bytesRestored;
    private Duration duration;
    private Instant completedAt = Instant.now();
    private Map<String, String> metadata = Map.of();
    private Optional<String> error = Optional.empty();

    public Builder backupId(String backupId) {
      this.backupId = backupId;
      return this;
    }

    public Builder targetPath(String targetPath) {
      this.targetPath = targetPath;
      return this;
    }

    public Builder filesRestored(long filesRestored) {
      this.filesRestored = filesRestored;
      return this;
    }

    public Builder bytesRestored(long bytesRestored) {
      this.bytesRestored = bytesRestored;
      return this;
    }

    public Builder duration(Duration duration) {
      this.duration = duration;
      return this;
    }

    public Builder completedAt(Instant completedAt) {
      this.completedAt = completedAt;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder error(String error) {
      this.error = Optional.ofNullable(error);
      return this;
    }

    public RestoreResult build() {
      return new RestoreResult(
          backupId,
          targetPath,
          filesRestored,
          bytesRestored,
          duration,
          completedAt,
          metadata,
          error);
    }
  }
}
