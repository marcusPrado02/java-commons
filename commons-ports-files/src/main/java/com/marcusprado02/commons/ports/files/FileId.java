package com.marcusprado02.commons.ports.files;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique file identifier.
 * Combines bucket/container name with a key/path.
 */
public record FileId(String bucket, String key) {

  public FileId {
    Objects.requireNonNull(bucket, "bucket must not be null");
    Objects.requireNonNull(key, "key must not be null");
    if (bucket.isBlank()) {
      throw new IllegalArgumentException("bucket must not be blank");
    }
    if (key.isBlank()) {
      throw new IllegalArgumentException("key must not be blank");
    }
  }

  /**
   * Create a FileId with a generated UUID key.
   */
  public static FileId generate(String bucket, String prefix) {
    String key = prefix.endsWith("/") ? prefix + UUID.randomUUID() : prefix + "/" + UUID.randomUUID();
    return new FileId(bucket, key);
  }

  /**
   * Create a FileId from a full path (bucket/key).
   */
  public static FileId fromPath(String path) {
    Objects.requireNonNull(path, "path must not be null");
    int index = path.indexOf('/');
    if (index <= 0 || index == path.length() - 1) {
      throw new IllegalArgumentException("Invalid path format, expected: bucket/key");
    }
    return new FileId(path.substring(0, index), path.substring(index + 1));
  }

  /**
   * Get the full path (bucket/key).
   */
  public String toPath() {
    return bucket + "/" + key;
  }

  /**
   * Get the file extension.
   */
  public String getExtension() {
    int lastDot = key.lastIndexOf('.');
    return lastDot > 0 ? key.substring(lastDot + 1) : "";
  }

  /**
   * Get the file name (last segment of the key).
   */
  public String getFileName() {
    int lastSlash = key.lastIndexOf('/');
    return lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
  }

  @Override
  public String toString() {
    return toPath();
  }
}
