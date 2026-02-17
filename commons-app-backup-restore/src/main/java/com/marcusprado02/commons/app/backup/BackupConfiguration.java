package com.marcusprado02.commons.app.backup;

import java.util.Map;
import java.util.Objects;

/**
 * Configuration for backup operations.
 *
 * @param destinationPath destination path for backup storage
 * @param compressionEnabled enable compression
 * @param encryptionEnabled enable encryption
 * @param encryptionKey encryption key (if encryption enabled)
 * @param parallel use parallel processing
 * @param options additional provider-specific options
 */
public record BackupConfiguration(
    String destinationPath,
    boolean compressionEnabled,
    boolean encryptionEnabled,
    String encryptionKey,
    boolean parallel,
    Map<String, String> options) {

  public BackupConfiguration {
    Objects.requireNonNull(destinationPath, "destinationPath");
    options = options == null ? Map.of() : Map.copyOf(options);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String destinationPath;
    private boolean compressionEnabled = true;
    private boolean encryptionEnabled = false;
    private String encryptionKey;
    private boolean parallel = false;
    private Map<String, String> options = Map.of();

    public Builder destinationPath(String destinationPath) {
      this.destinationPath = destinationPath;
      return this;
    }

    public Builder compressionEnabled(boolean compressionEnabled) {
      this.compressionEnabled = compressionEnabled;
      return this;
    }

    public Builder encryptionEnabled(boolean encryptionEnabled) {
      this.encryptionEnabled = encryptionEnabled;
      return this;
    }

    public Builder encryptionKey(String encryptionKey) {
      this.encryptionKey = encryptionKey;
      return this;
    }

    public Builder parallel(boolean parallel) {
      this.parallel = parallel;
      return this;
    }

    public Builder options(Map<String, String> options) {
      this.options = options;
      return this;
    }

    public Builder option(String key, String value) {
      if (this.options.isEmpty()) {
        this.options = Map.of(key, value);
      } else {
        var mutable = new java.util.HashMap<>(this.options);
        mutable.put(key, value);
        this.options = mutable;
      }
      return this;
    }

    public BackupConfiguration build() {
      return new BackupConfiguration(
          destinationPath, compressionEnabled, encryptionEnabled, encryptionKey, parallel, options);
    }
  }
}
