package com.marcusprado02.commons.adapters.files.gcs;

import com.google.cloud.storage.*;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.files.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Google Cloud Storage implementation of FileStorePort.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Upload/download with streaming
 *   <li>Signed URLs for temporary access
 *   <li>Storage classes (STANDARD, NEARLINE, COLDLINE, ARCHIVE)
 *   <li>Custom metadata
 *   <li>Batch operations
 *   <li>Server-side copy
 * </ul>
 */
public class GCSFileStoreAdapter implements FileStorePort {

  private static final Logger log = LoggerFactory.getLogger(GCSFileStoreAdapter.class);

  private final Storage storage;

  public GCSFileStoreAdapter(Storage storage) {
    this.storage = Objects.requireNonNull(storage, "storage must not be null");
  }

  @Override
  public Result<UploadResult> upload(FileId fileId, InputStream content, UploadOptions options) {
    Objects.requireNonNull(fileId, "fileId must not be null");
    Objects.requireNonNull(content, "content must not be null");
    Objects.requireNonNull(options, "options must not be null");

    try {
      log.debug("Uploading file: bucket={}, key={}", fileId.bucket(), fileId.key());

      var blobInfoBuilder =
          BlobInfo.newBuilder(BlobId.of(fileId.bucket(), fileId.key()))
              .setContentType(options.contentType());

      // Add custom metadata
      if (options.metadata() != null && !options.metadata().isEmpty()) {
        blobInfoBuilder.setMetadata(new HashMap<>(options.metadata()));
      }

      // Set storage class
      if (options.storageClass() != null) {
        blobInfoBuilder.setStorageClass(mapStorageClass(options.storageClass()));
      }

      var blobInfo = blobInfoBuilder.build();

      // Upload with content
      var blob = storage.createFrom(blobInfo, content);

      log.info(
          "File uploaded successfully: bucket={}, key={}, size={}",
          fileId.bucket(),
          fileId.key(),
          blob.getSize());

      return Result.ok(new UploadResult(fileId, blob.getEtag(), blob.getSize()));

    } catch (StorageException e) {
      log.error("Failed to upload file: bucket={}, key={}", fileId.bucket(), fileId.key(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("GCS_UPLOAD_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to upload file to GCS: " + e.getMessage()));
    } catch (IOException e) {
      log.error("IO error uploading file: bucket={}, key={}", fileId.bucket(), fileId.key(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("IO_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Error reading file content: " + e.getMessage()));
    }
  }

  @Override
  public Result<FileObject> download(FileId fileId) {
    Objects.requireNonNull(fileId, "fileId must not be null");

    try {
      log.debug("Downloading file: bucket={}, key={}", fileId.bucket(), fileId.key());

      var blob = storage.get(BlobId.of(fileId.bucket(), fileId.key()));

      if (blob == null || !blob.exists()) {
        log.warn("File not found: bucket={}, key={}", fileId.bucket(), fileId.key());
        return Result.fail(
            Problem.of(
                ErrorCode.of("FILE_NOT_FOUND"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "File does not exist: " + fileId.key()));
      }

      var contentBytes = blob.getContent();
      var metadata =
          new FileObject.FileMetadata(
              blob.getContentType(),
              blob.getSize(),
              blob.getUpdateTimeOffsetDateTime().toInstant(),
              blob.getEtag(),
              blob.getMetadata() != null ? blob.getMetadata() : java.util.Map.of());

      log.info(
          "File downloaded successfully: bucket={}, key={}, size={}",
          fileId.bucket(),
          fileId.key(),
          blob.getSize());

      return Result.ok(new FileObject(fileId, new ByteArrayInputStream(contentBytes), metadata));

    } catch (StorageException e) {
      log.error("Failed to download file: bucket={}, key={}", fileId.bucket(), fileId.key(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("GCS_DOWNLOAD_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to download file from GCS: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> delete(FileId fileId) {
    Objects.requireNonNull(fileId, "fileId must not be null");

    try {
      log.debug("Deleting file: bucket={}, key={}", fileId.bucket(), fileId.key());

      var deleted = storage.delete(BlobId.of(fileId.bucket(), fileId.key()));

      if (!deleted) {
        log.warn("File not found for deletion: bucket={}, key={}", fileId.bucket(), fileId.key());
        return Result.fail(
            Problem.of(
                ErrorCode.of("FILE_NOT_FOUND"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "File does not exist: " + fileId.key()));
      }

      log.info("File deleted successfully: bucket={}, key={}", fileId.bucket(), fileId.key());
      return Result.ok(null);

    } catch (StorageException e) {
      log.error("Failed to delete file: bucket={}, key={}", fileId.bucket(), fileId.key(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("GCS_DELETE_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to delete file from GCS: " + e.getMessage()));
    }
  }

  @Override
  public Result<DeleteResult> deleteAll(List<FileId> fileIds) {
    Objects.requireNonNull(fileIds, "fileIds must not be null");

    log.debug("Batch deleting {} files", fileIds.size());

    var deletedCount = 0;
    var failedDeletes = new ArrayList<FileId>();

    for (var fileId : fileIds) {
      try {
        var deleted = storage.delete(BlobId.of(fileId.bucket(), fileId.key()));
        if (deleted) {
          deletedCount++;
        } else {
          log.warn("File not found for deletion: {}", fileId.key());
          failedDeletes.add(fileId);
        }
      } catch (StorageException e) {
        log.error("Failed to delete file: {}", fileId.key(), e);
        failedDeletes.add(fileId);
      }
    }

    log.info("Batch delete completed: {} deleted, {} failed", deletedCount, failedDeletes.size());

    return Result.ok(new DeleteResult(deletedCount, failedDeletes));
  }

  @Override
  public Result<Boolean> exists(FileId fileId) {
    Objects.requireNonNull(fileId, "fileId must not be null");

    try {
      var blob = storage.get(BlobId.of(fileId.bucket(), fileId.key()));
      var exists = blob != null && blob.exists();

      log.debug(
          "File existence check: bucket={}, key={}, exists={}",
          fileId.bucket(),
          fileId.key(),
          exists);

      return Result.ok(exists);

    } catch (StorageException e) {
      log.error(
          "Failed to check file existence: bucket={}, key={}", fileId.bucket(), fileId.key(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("GCS_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to check if file exists: " + e.getMessage()));
    }
  }

  @Override
  public Result<FileObject.FileMetadata> getMetadata(FileId fileId) {
    Objects.requireNonNull(fileId, "fileId must not be null");

    try {
      log.debug("Getting metadata: bucket={}, key={}", fileId.bucket(), fileId.key());

      var blob = storage.get(BlobId.of(fileId.bucket(), fileId.key()));

      if (blob == null || !blob.exists()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("FILE_NOT_FOUND"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "File does not exist: " + fileId.key()));
      }

      var metadata =
          new FileObject.FileMetadata(
              blob.getContentType(),
              blob.getSize(),
              blob.getUpdateTimeOffsetDateTime().toInstant(),
              blob.getEtag(),
              blob.getMetadata() != null ? blob.getMetadata() : java.util.Map.of());

      log.debug(
          "Metadata retrieved: bucket={}, key={}, size={}",
          fileId.bucket(),
          fileId.key(),
          blob.getSize());

      return Result.ok(metadata);

    } catch (StorageException e) {
      log.error("Failed to get metadata: bucket={}, key={}", fileId.bucket(), fileId.key(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("GCS_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to retrieve file metadata: " + e.getMessage()));
    }
  }

  @Override
  public Result<ListResult> list(String bucket, String prefix, ListOptions options) {
    Objects.requireNonNull(bucket, "bucket must not be null");
    Objects.requireNonNull(prefix, "prefix must not be null");
    Objects.requireNonNull(options, "options must not be null");

    try {
      log.debug("Listing files: bucket={}, prefix={}", bucket, prefix);

      var builder = Storage.BlobListOption.prefix(prefix);
      var pageSize = Storage.BlobListOption.pageSize(options.maxKeys());

      var listOptions = new ArrayList<Storage.BlobListOption>();
      listOptions.add(builder);
      listOptions.add(pageSize);

      if (options.continuationToken() != null) {
        listOptions.add(Storage.BlobListOption.pageToken(options.continuationToken()));
      }

      var page = storage.list(bucket, listOptions.toArray(new Storage.BlobListOption[0]));

      var files = new ArrayList<FileId>();
      for (var blob : page.iterateAll()) {
        files.add(new FileId(bucket, blob.getName()));
      }

      var nextPageToken = page.getNextPageToken();
      var hasMore = nextPageToken != null;

      log.info(
          "Listed {} files: bucket={}, prefix={}, hasMore={}",
          files.size(),
          bucket,
          prefix,
          hasMore);

      return Result.ok(new ListResult(files, nextPageToken, hasMore));

    } catch (StorageException e) {
      log.error("Failed to list files: bucket={}, prefix={}", bucket, prefix, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("GCS_LIST_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to list files: " + e.getMessage()));
    }
  }

  @Override
  public Result<URL> generatePresignedUrl(
      FileId fileId, PresignedOperation operation, Duration duration) {
    Objects.requireNonNull(fileId, "fileId must not be null");
    Objects.requireNonNull(operation, "operation must not be null");
    Objects.requireNonNull(duration, "duration must not be null");

    try {
      log.debug(
          "Generating signed URL: bucket={}, key={}, operation={}, duration={}",
          fileId.bucket(),
          fileId.key(),
          operation,
          duration);

      var blob = storage.get(BlobId.of(fileId.bucket(), fileId.key()));

      // For PUT operations, blob might not exist yet
      if (blob == null && operation == PresignedOperation.GET) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("FILE_NOT_FOUND"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "Cannot generate GET URL for non-existent file"));
      }

      var httpMethod =
          switch (operation) {
            case GET -> HttpMethod.GET;
            case PUT -> HttpMethod.PUT;
          };

      var blobInfo =
          blob != null
              ? blob
              : BlobInfo.newBuilder(BlobId.of(fileId.bucket(), fileId.key())).build();
      var url =
          storage.signUrl(
              blobInfo,
              duration.toMillis(),
              TimeUnit.MILLISECONDS,
              Storage.SignUrlOption.httpMethod(httpMethod));

      log.info(
          "Signed URL generated: bucket={}, key={}, operation={}",
          fileId.bucket(),
          fileId.key(),
          operation);

      return Result.ok(url);

    } catch (StorageException e) {
      log.error(
          "Failed to generate signed URL: bucket={}, key={}", fileId.bucket(), fileId.key(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("GCS_SIGNED_URL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to generate signed URL: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> copy(FileId source, FileId destination) {
    Objects.requireNonNull(source, "source must not be null");
    Objects.requireNonNull(destination, "destination must not be null");

    try {
      log.debug(
          "Copying file: source={}/{}, destination={}/{}",
          source.bucket(),
          source.key(),
          destination.bucket(),
          destination.key());

      var sourceBlob = storage.get(BlobId.of(source.bucket(), source.key()));

      if (sourceBlob == null || !sourceBlob.exists()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("FILE_NOT_FOUND"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "Source file does not exist: " + source.key()));
      }

      var request =
          Storage.CopyRequest.newBuilder()
              .setSource(BlobId.of(source.bucket(), source.key()))
              .setTarget(BlobId.of(destination.bucket(), destination.key()))
              .build();

      storage.copy(request);

      log.info(
          "File copied successfully: source={}/{}, destination={}/{}",
          source.bucket(),
          source.key(),
          destination.bucket(),
          destination.key());

      return Result.ok(null);

    } catch (StorageException e) {
      log.error("Failed to copy file: source={}/{}", source.bucket(), source.key(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("GCS_COPY_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to copy file: " + e.getMessage()));
    }
  }

  /** Map FileStorePort storage class to GCS storage class. */
  private com.google.cloud.storage.StorageClass mapStorageClass(
      FileStorePort.StorageClass storageClass) {
    return switch (storageClass) {
      case STANDARD -> com.google.cloud.storage.StorageClass.STANDARD;
      case REDUCED_REDUNDANCY ->
          com.google.cloud.storage.StorageClass.STANDARD; // Not available in GCS
      case STANDARD_IA -> com.google.cloud.storage.StorageClass.NEARLINE; // Monthly access
      case ONEZONE_IA -> com.google.cloud.storage.StorageClass.NEARLINE; // Map to NEARLINE
      case INTELLIGENT_TIERING ->
          com.google.cloud.storage.StorageClass.STANDARD; // Not available in GCS
      case GLACIER -> com.google.cloud.storage.StorageClass.COLDLINE; // Quarterly access
      case GLACIER_IR -> com.google.cloud.storage.StorageClass.COLDLINE; // Map to COLDLINE
      case DEEP_ARCHIVE -> com.google.cloud.storage.StorageClass.ARCHIVE; // Annual access
    };
  }
}
