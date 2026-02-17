package com.marcusprado02.commons.app.backup;

import java.util.Map;
import java.util.Objects;

/**
 * Configuration for restore operations.
 *
 * @param targetPath target path for restored data
 * @param overwriteExisting overwrite existing files/data
 * @param verifyIntegrity verify backup integrity before restore
 * @param decryptionKey decryption key (if backup is encrypted)
 * @param options additional provider-specific options
 */
public record RestoreConfiguration(
    String targetPath,
    boolean overwriteExisting,
    boolean verifyIntegrity,
    String decryptionKey,
    Map<String, String> options) {

  public RestoreConfiguration {
    Objects.requireNonNull(targetPath, "targetPath");
    options = options == null ? Map.of() : Map.copyOf(options);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String targetPath;
    private boolean overwriteExisting = false;
    private boolean verifyIntegrity = true;
    private String decryptionKey;
    private Map<String, String> options = Map.of();

    public Builder targetPath(String targetPath) {
      this.targetPath = targetPath;
      return this;
    }

    public Builder overwriteExisting(boolean overwriteExisting) {
      this.overwriteExisting = overwriteExisting;
      return this;
    }

    public Builder verifyIntegrity(boolean verifyIntegrity) {
      this.verifyIntegrity = verifyIntegrity;
      return this;
    }

    public Builder decryptionKey(String decryptionKey) {
      this.decryptionKey = decryptionKey;
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

    public RestoreConfiguration build() {
      return new RestoreConfiguration(
          targetPath, overwriteExisting, verifyIntegrity, decryptionKey, options);
    }
  }
}
