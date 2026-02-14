package com.marcusprado02.commons.ports.files;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Represents a file object with its metadata and content. */
public record FileObject(FileId id, InputStream content, FileMetadata metadata) {

  public FileObject {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(content, "content must not be null");
    Objects.requireNonNull(metadata, "metadata must not be null");
  }

  /** Metadata associated with a file. */
  public record FileMetadata(
      String contentType,
      Long contentLength,
      Instant lastModified,
      String etag,
      Map<String, String> customMetadata) {

    public FileMetadata {
      Objects.requireNonNull(contentType, "contentType must not be null");
      Objects.requireNonNull(contentLength, "contentLength must not be null");
      customMetadata = customMetadata != null ? Map.copyOf(customMetadata) : Map.of();
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private String contentType = "application/octet-stream";
      private Long contentLength = 0L;
      private Instant lastModified;
      private String etag;
      private Map<String, String> customMetadata = new HashMap<>();

      public Builder contentType(String contentType) {
        this.contentType = contentType;
        return this;
      }

      public Builder contentLength(Long contentLength) {
        this.contentLength = contentLength;
        return this;
      }

      public Builder lastModified(Instant lastModified) {
        this.lastModified = lastModified;
        return this;
      }

      public Builder etag(String etag) {
        this.etag = etag;
        return this;
      }

      public Builder customMetadata(Map<String, String> customMetadata) {
        this.customMetadata = new HashMap<>(customMetadata);
        return this;
      }

      public Builder addCustomMetadata(String key, String value) {
        this.customMetadata.put(key, value);
        return this;
      }

      public FileMetadata build() {
        return new FileMetadata(contentType, contentLength, lastModified, etag, customMetadata);
      }
    }
  }
}
