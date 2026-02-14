package com.marcusprado02.commons.adapters.files.azureblob;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.files.FileId;
import com.marcusprado02.commons.ports.files.FileObject;
import com.marcusprado02.commons.ports.files.FileStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Azure Blob Storage implementation of FileStorePort.
 * Provides file storage operations using Azure Blob Storage.
 */
public class AzureBlobFileStoreAdapter implements FileStorePort {

  private static final Logger log = LoggerFactory.getLogger(AzureBlobFileStoreAdapter.class);

  private final BlobServiceClient blobServiceClient;
  private final AzureBlobConfiguration configuration;

  public AzureBlobFileStoreAdapter(BlobServiceClient blobServiceClient, AzureBlobConfiguration configuration) {
    this.blobServiceClient = Objects.requireNonNull(blobServiceClient, "blobServiceClient must not be null");
    this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
  }

  @Override
  public Result<UploadResult> upload(FileId fileId, InputStream content, UploadOptions options) {
    try {
      log.debug("Uploading blob: {}", fileId);

      BlobContainerClient containerClient = getContainerClient(fileId.bucket());
      BlobClient blobClient = containerClient.getBlobClient(fileId.key());

      byte[] bytes = content.readAllBytes();

      BlobParallelUploadOptions uploadOptions = new BlobParallelUploadOptions(new ByteArrayInputStream(bytes), bytes.length);

      BlobHttpHeaders headers = new BlobHttpHeaders()
          .setContentType(options.contentType());
      uploadOptions.setHeaders(headers);
      uploadOptions.setMetadata(options.metadata());

      if (options.storageClass() != null) {
        uploadOptions.setTier(mapAccessTier(options.storageClass()));
      }

      BlockBlobItem response = blobClient.uploadWithResponse(uploadOptions, null, Context.NONE).getValue();

      log.info("Blob uploaded successfully: {} (etag: {})", fileId, response.getETag());

      return Result.ok(new UploadResult(fileId, response.getETag(), (long) bytes.length));

    } catch (BlobStorageException e) {
      log.error("Azure Blob error uploading blob: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("AZURE_UPLOAD_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to upload blob to Azure: " + e.getMessage()));
    } catch (IOException e) {
      log.error("I/O error reading file content: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("IO_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to read file content: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error uploading blob: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("UPLOAD_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<FileObject> download(FileId fileId) {
    try {
      log.debug("Downloading blob: {}", fileId);

      BlobContainerClient containerClient = getContainerClient(fileId.bucket());
      BlobClient blobClient = containerClient.getBlobClient(fileId.key());

      InputStream inputStream = blobClient.openInputStream();
      BlobProperties properties = blobClient.getProperties();

      FileObject.FileMetadata metadata = FileObject.FileMetadata.builder()
          .contentType(properties.getContentType())
          .contentLength(properties.getBlobSize())
          .lastModified(properties.getLastModified().toInstant())
          .etag(properties.getETag())
          .customMetadata(properties.getMetadata())
          .build();

      log.info("Blob downloaded successfully: {}", fileId);

      return Result.ok(new FileObject(fileId, inputStream, metadata));

    } catch (BlobStorageException e) {
      if (e.getErrorCode() == BlobErrorCode.BLOB_NOT_FOUND) {
        log.warn("Blob not found: {}", fileId);
        return Result.fail(Problem.of(
            ErrorCode.of("FILE_NOT_FOUND"),
            ErrorCategory.NOT_FOUND,
            Severity.ERROR,
            "Blob does not exist: " + fileId));
      }
      log.error("Azure Blob error downloading blob: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("AZURE_DOWNLOAD_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to download blob from Azure: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error downloading blob: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("DOWNLOAD_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> delete(FileId fileId) {
    try {
      log.debug("Deleting blob: {}", fileId);

      BlobContainerClient containerClient = getContainerClient(fileId.bucket());
      BlobClient blobClient = containerClient.getBlobClient(fileId.key());

      blobClient.delete();

      log.info("Blob deleted successfully: {}", fileId);
      return Result.ok(null);

    } catch (BlobStorageException e) {
      log.error("Azure Blob error deleting blob: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("AZURE_DELETE_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to delete blob from Azure: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error deleting blob: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("DELETE_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<DeleteResult> deleteAll(List<FileId> fileIds) {
    try {
      log.debug("Deleting {} blobs", fileIds.size());

      Map<String, List<FileId>> containerGroups = new HashMap<>();
      for (FileId fileId : fileIds) {
        containerGroups.computeIfAbsent(fileId.bucket(), k -> new ArrayList<>()).add(fileId);
      }

      int totalDeleted = 0;
      List<FileId> failedDeletes = new ArrayList<>();

      for (Map.Entry<String, List<FileId>> entry : containerGroups.entrySet()) {
        String container = entry.getKey();
        List<FileId> files = entry.getValue();

        BlobContainerClient containerClient = getContainerClient(container);

        for (FileId fileId : files) {
          try {
            BlobClient blobClient = containerClient.getBlobClient(fileId.key());
            blobClient.delete();
            totalDeleted++;
          } catch (BlobStorageException e) {
            failedDeletes.add(fileId);
            log.warn("Failed to delete blob {}: {}", fileId, e.getMessage());
          }
        }
      }

      log.info("Deleted {} blobs, {} failed", totalDeleted, failedDeletes.size());

      return Result.ok(new DeleteResult(totalDeleted, failedDeletes));

    } catch (Exception e) {
      log.error("Unexpected error deleting blobs", e);
      return Result.fail(Problem.of(
          ErrorCode.of("DELETE_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<Boolean> exists(FileId fileId) {
    try {
      log.debug("Checking if blob exists: {}", fileId);

      BlobContainerClient containerClient = getContainerClient(fileId.bucket());
      BlobClient blobClient = containerClient.getBlobClient(fileId.key());

      return Result.ok(blobClient.exists());

    } catch (BlobStorageException e) {
      log.error("Azure Blob error checking blob existence: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("AZURE_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to check blob existence: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error checking blob existence: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<FileObject.FileMetadata> getMetadata(FileId fileId) {
    try {
      log.debug("Getting metadata for blob: {}", fileId);

      BlobContainerClient containerClient = getContainerClient(fileId.bucket());
      BlobClient blobClient = containerClient.getBlobClient(fileId.key());

      BlobProperties properties = blobClient.getProperties();

      FileObject.FileMetadata metadata = FileObject.FileMetadata.builder()
          .contentType(properties.getContentType())
          .contentLength(properties.getBlobSize())
          .lastModified(properties.getLastModified().toInstant())
          .etag(properties.getETag())
          .customMetadata(properties.getMetadata())
          .build();

      log.info("Retrieved metadata for blob: {}", fileId);
      return Result.ok(metadata);

    } catch (BlobStorageException e) {
      if (e.getErrorCode() == BlobErrorCode.BLOB_NOT_FOUND) {
        log.warn("Blob not found: {}", fileId);
        return Result.fail(Problem.of(
            ErrorCode.of("FILE_NOT_FOUND"),
            ErrorCategory.NOT_FOUND,
            Severity.ERROR,
            "Blob does not exist: " + fileId));
      }
      log.error("Azure Blob error getting blob metadata: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("AZURE_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to get blob metadata: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error getting blob metadata: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<ListResult> list(String container, String prefix, ListOptions options) {
    try {
      log.debug("Listing blobs in container: {}, prefix: {}", container, prefix);

      BlobContainerClient containerClient = getContainerClient(container);

      ListBlobsOptions listOptions = new ListBlobsOptions()
          .setPrefix(prefix)
          .setMaxResultsPerPage(options.maxKeys());

      PagedIterable<BlobItem> blobs = containerClient.listBlobs(listOptions, null);

      List<FileId> files = new ArrayList<>();
      int count = 0;
      for (BlobItem blob : blobs) {
        files.add(new FileId(container, blob.getName()));
        count++;
        if (count >= options.maxKeys()) {
          break;
        }
      }

      log.info("Listed {} blobs in container: {}", files.size(), container);

      // Azure Blob doesn't provide continuation token in the same way as S3
      // For simplicity, we'll return hasMore=false
      return Result.ok(new ListResult(files, null, false));

    } catch (BlobStorageException e) {
      log.error("Azure Blob error listing blobs in container: {}", container, e);
      return Result.fail(Problem.of(
          ErrorCode.of("AZURE_LIST_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to list blobs: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error listing blobs in container: {}", container, e);
      return Result.fail(Problem.of(
          ErrorCode.of("LIST_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<URL> generatePresignedUrl(FileId fileId, PresignedOperation operation, Duration duration) {
    try {
      log.debug("Generating SAS URL for blob: {}, operation: {}, duration: {}", fileId, operation, duration);

      BlobContainerClient containerClient = getContainerClient(fileId.bucket());
      BlobClient blobClient = containerClient.getBlobClient(fileId.key());

      BlobSasPermission permissions = new BlobSasPermission();
      switch (operation) {
        case GET -> permissions.setReadPermission(true);
        case PUT -> permissions.setWritePermission(true).setCreatePermission(true);
      }

      OffsetDateTime expiryTime = OffsetDateTime.now().plus(duration);

      BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, permissions);

      String sasToken = blobClient.generateSas(sasValues);
      String sasUrl = blobClient.getBlobUrl() + "?" + sasToken;

      log.info("Generated SAS URL for blob: {}", fileId);
      return Result.ok(new URL(sasUrl));

    } catch (BlobStorageException e) {
      log.error("Azure Blob error generating SAS URL: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("AZURE_SAS_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to generate SAS URL: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error generating SAS URL: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("SAS_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> copy(FileId source, FileId destination) {
    try {
      log.debug("Copying blob from {} to {}", source, destination);

      BlobContainerClient sourceContainerClient = getContainerClient(source.bucket());
      BlobClient sourceBlobClient = sourceContainerClient.getBlobClient(source.key());

      BlobContainerClient destContainerClient = getContainerClient(destination.bucket());
      BlobClient destBlobClient = destContainerClient.getBlobClient(destination.key());

      String sourceUrl = sourceBlobClient.getBlobUrl();
      destBlobClient.copyFromUrl(sourceUrl);

      log.info("Blob copied successfully from {} to {}", source, destination);
      return Result.ok(null);

    } catch (BlobStorageException e) {
      log.error("Azure Blob error copying blob from {} to {}", source, destination, e);
      return Result.fail(Problem.of(
          ErrorCode.of("AZURE_COPY_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to copy blob: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error copying blob from {} to {}", source, destination, e);
      return Result.fail(Problem.of(
          ErrorCode.of("COPY_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  // Helper methods

  private BlobContainerClient getContainerClient(String containerName) {
    return blobServiceClient.getBlobContainerClient(containerName);
  }

  private AccessTier mapAccessTier(StorageClass storageClass) {
    return switch (storageClass) {
      case STANDARD -> AccessTier.HOT;
      case STANDARD_IA -> AccessTier.COOL;
      case GLACIER, DEEP_ARCHIVE -> AccessTier.ARCHIVE;
      default -> AccessTier.HOT;
    };
  }
}
