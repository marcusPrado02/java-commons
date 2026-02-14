package com.marcusprado02.commons.ports.files;

import com.marcusprado02.commons.kernel.result.Result;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Port for file storage operations. Supports various storage backends (S3, Azure Blob, GCS, etc.).
 */
public interface FileStorePort {

  /**
   * Upload a file to storage.
   *
   * @param fileId the file identifier
   * @param content the file content
   * @param options upload options
   * @return Result with upload result or problem
   */
  Result<UploadResult> upload(FileId fileId, InputStream content, UploadOptions options);

  /** Upload a file with default options. */
  default Result<UploadResult> upload(FileId fileId, InputStream content) {
    return upload(fileId, content, UploadOptions.defaults());
  }

  /**
   * Download a file from storage.
   *
   * @param fileId the file identifier
   * @return Result with file object or problem
   */
  Result<FileObject> download(FileId fileId);

  /**
   * Delete a file from storage.
   *
   * @param fileId the file identifier
   * @return Result with void or problem
   */
  Result<Void> delete(FileId fileId);

  /**
   * Delete multiple files from storage.
   *
   * @param fileIds list of file identifiers
   * @return Result with delete result or problem
   */
  Result<DeleteResult> deleteAll(List<FileId> fileIds);

  /**
   * Check if a file exists.
   *
   * @param fileId the file identifier
   * @return Result with boolean or problem
   */
  Result<Boolean> exists(FileId fileId);

  /**
   * Get file metadata without downloading content.
   *
   * @param fileId the file identifier
   * @return Result with metadata or problem
   */
  Result<FileObject.FileMetadata> getMetadata(FileId fileId);

  /**
   * List files with a given prefix.
   *
   * @param bucket the bucket name
   * @param prefix the key prefix
   * @param options list options
   * @return Result with list of file IDs or problem
   */
  Result<ListResult> list(String bucket, String prefix, ListOptions options);

  /** List files with default options. */
  default Result<ListResult> list(String bucket, String prefix) {
    return list(bucket, prefix, ListOptions.defaults());
  }

  /**
   * Generate a presigned URL for temporary access.
   *
   * @param fileId the file identifier
   * @param operation the operation type (GET/PUT)
   * @param duration URL validity duration
   * @return Result with presigned URL or problem
   */
  Result<URL> generatePresignedUrl(FileId fileId, PresignedOperation operation, Duration duration);

  /**
   * Copy a file to a new location.
   *
   * @param source the source file ID
   * @param destination the destination file ID
   * @return Result with void or problem
   */
  Result<Void> copy(FileId source, FileId destination);

  /** Represents upload operation result. */
  record UploadResult(FileId fileId, String etag, Long contentLength) {
    public UploadResult {
      Objects.requireNonNull(fileId, "fileId must not be null");
      Objects.requireNonNull(etag, "etag must not be null");
      Objects.requireNonNull(contentLength, "contentLength must not be null");
    }
  }

  /** Represents delete operation result. */
  record DeleteResult(int deletedCount, List<FileId> failedDeletes) {
    public DeleteResult {
      Objects.requireNonNull(failedDeletes, "failedDeletes must not be null");
    }
  }

  /** Represents list operation result. */
  record ListResult(List<FileId> files, String continuationToken, boolean hasMore) {
    public ListResult {
      Objects.requireNonNull(files, "files must not be null");
    }
  }

  /** Options for upload operations. */
  record UploadOptions(
      String contentType,
      java.util.Map<String, String> metadata,
      StorageClass storageClass,
      ServerSideEncryption encryption) {

    public static UploadOptions defaults() {
      return new UploadOptions(
          "application/octet-stream", java.util.Map.of(), StorageClass.STANDARD, null);
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private String contentType = "application/octet-stream";
      private java.util.Map<String, String> metadata = java.util.Map.of();
      private StorageClass storageClass = StorageClass.STANDARD;
      private ServerSideEncryption encryption;

      public Builder contentType(String contentType) {
        this.contentType = contentType;
        return this;
      }

      public Builder metadata(java.util.Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
      }

      public Builder storageClass(StorageClass storageClass) {
        this.storageClass = storageClass;
        return this;
      }

      public Builder encryption(ServerSideEncryption encryption) {
        this.encryption = encryption;
        return this;
      }

      public UploadOptions build() {
        return new UploadOptions(contentType, metadata, storageClass, encryption);
      }
    }
  }

  /** Options for list operations. */
  record ListOptions(int maxKeys, String continuationToken) {

    public static ListOptions defaults() {
      return new ListOptions(1000, null);
    }

    public static ListOptions withMaxKeys(int maxKeys) {
      return new ListOptions(maxKeys, null);
    }

    public static ListOptions withContinuation(String token) {
      return new ListOptions(1000, token);
    }
  }

  /** Storage class options. */
  enum StorageClass {
    STANDARD,
    REDUCED_REDUNDANCY,
    STANDARD_IA,
    ONEZONE_IA,
    INTELLIGENT_TIERING,
    GLACIER,
    GLACIER_IR,
    DEEP_ARCHIVE
  }

  /** Server-side encryption options. */
  enum ServerSideEncryption {
    AES256,
    AWS_KMS
  }

  /** Presigned URL operation types. */
  enum PresignedOperation {
    GET,
    PUT
  }
}
