package com.marcusprado02.commons.adapters.files.s3;

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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AWS S3 implementation of FileStorePort.
 * Provides file storage operations using Amazon S3.
 */
public class S3FileStoreAdapter implements FileStorePort {

  private static final Logger log = LoggerFactory.getLogger(S3FileStoreAdapter.class);

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final S3Configuration configuration;

  public S3FileStoreAdapter(S3Client s3Client, S3Presigner s3Presigner, S3Configuration configuration) {
    this.s3Client = Objects.requireNonNull(s3Client, "s3Client must not be null");
    this.s3Presigner = Objects.requireNonNull(s3Presigner, "s3Presigner must not be null");
    this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
  }

  @Override
  public Result<UploadResult> upload(FileId fileId, InputStream content, UploadOptions options) {
    try {
      log.debug("Uploading file: {}", fileId);

      byte[] bytes = content.readAllBytes();

      PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
          .bucket(fileId.bucket())
          .key(fileId.key())
          .contentType(options.contentType())
          .metadata(options.metadata());

      if (options.storageClass() != null) {
        requestBuilder.storageClass(mapStorageClass(options.storageClass()));
      }

      if (options.encryption() != null) {
        requestBuilder.serverSideEncryption(mapEncryption(options.encryption()));
      }

      PutObjectResponse response = s3Client.putObject(
          requestBuilder.build(),
          RequestBody.fromBytes(bytes)
      );

      log.info("File uploaded successfully: {} (etag: {})", fileId, response.eTag());

      return Result.ok(new UploadResult(fileId, response.eTag(), (long) bytes.length));

    } catch (S3Exception e) {
      log.error("S3 error uploading file: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("S3_UPLOAD_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to upload file to S3: " + e.awsErrorDetails().errorMessage()));
    } catch (IOException e) {
      log.error("I/O error reading file content: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("IO_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to read file content: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error uploading file: {}", fileId, e);
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
      log.debug("Downloading file: {}", fileId);

      GetObjectRequest request = GetObjectRequest.builder()
          .bucket(fileId.bucket())
          .key(fileId.key())
          .build();

      var response = s3Client.getObject(request);

      FileObject.FileMetadata metadata = FileObject.FileMetadata.builder()
          .contentType(response.response().contentType())
          .contentLength(response.response().contentLength())
          .lastModified(response.response().lastModified())
          .etag(response.response().eTag())
          .customMetadata(response.response().metadata())
          .build();

      log.info("File downloaded successfully: {}", fileId);

      return Result.ok(new FileObject(fileId, response, metadata));

    } catch (NoSuchKeyException e) {
      log.warn("File not found: {}", fileId);
      return Result.fail(Problem.of(
          ErrorCode.of("FILE_NOT_FOUND"),
          ErrorCategory.NOT_FOUND,
          Severity.ERROR,
          "File does not exist: " + fileId));
    } catch (S3Exception e) {
      log.error("S3 error downloading file: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("S3_DOWNLOAD_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to download file from S3: " + e.awsErrorDetails().errorMessage()));
    } catch (Exception e) {
      log.error("Unexpected error downloading file: {}", fileId, e);
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
      log.debug("Deleting file: {}", fileId);

      DeleteObjectRequest request = DeleteObjectRequest.builder()
          .bucket(fileId.bucket())
          .key(fileId.key())
          .build();

      s3Client.deleteObject(request);

      log.info("File deleted successfully: {}", fileId);
      return Result.ok(null);

    } catch (S3Exception e) {
      log.error("S3 error deleting file: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("S3_DELETE_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to delete file from S3: " + e.awsErrorDetails().errorMessage()));
    } catch (Exception e) {
      log.error("Unexpected error deleting file: {}", fileId, e);
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
      log.debug("Deleting {} files", fileIds.size());

      Map<String, List<ObjectIdentifier>> bucketGroups = new HashMap<>();
      for (FileId fileId : fileIds) {
        bucketGroups.computeIfAbsent(fileId.bucket(), k -> new ArrayList<>())
            .add(ObjectIdentifier.builder().key(fileId.key()).build());
      }

      int totalDeleted = 0;
      List<FileId> failedDeletes = new ArrayList<>();

      for (Map.Entry<String, List<ObjectIdentifier>> entry : bucketGroups.entrySet()) {
        String bucket = entry.getKey();
        List<ObjectIdentifier> objects = entry.getValue();

        Delete delete = Delete.builder().objects(objects).build();

        DeleteObjectsRequest request = DeleteObjectsRequest.builder()
            .bucket(bucket)
            .delete(delete)
            .build();

        DeleteObjectsResponse response = s3Client.deleteObjects(request);

        totalDeleted += response.deleted().size();

        for (S3Error s3Error : response.errors()) {
          failedDeletes.add(new FileId(bucket, s3Error.key()));
          log.warn("Failed to delete file {}/{}: {}", bucket, s3Error.key(), s3Error.message());
        }
      }

      log.info("Deleted {} files, {} failed", totalDeleted, failedDeletes.size());

      return Result.ok(new DeleteResult(totalDeleted, failedDeletes));

    } catch (S3Exception e) {
      log.error("S3 error deleting files", e);
      return Result.fail(Problem.of(
          ErrorCode.of("S3_DELETE_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to delete files from S3: " + e.awsErrorDetails().errorMessage()));
    } catch (Exception e) {
      log.error("Unexpected error deleting files", e);
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
      log.debug("Checking if file exists: {}", fileId);

      HeadObjectRequest request = HeadObjectRequest.builder()
          .bucket(fileId.bucket())
          .key(fileId.key())
          .build();

      s3Client.headObject(request);

      return Result.ok(true);

    } catch (NoSuchKeyException e) {
      return Result.ok(false);
    } catch (S3Exception e) {
      log.error("S3 error checking file existence: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("S3_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to check file existence: " + e.awsErrorDetails().errorMessage()));
    } catch (Exception e) {
      log.error("Unexpected error checking file existence: {}", fileId, e);
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
      log.debug("Getting metadata for file: {}", fileId);

      HeadObjectRequest request = HeadObjectRequest.builder()
          .bucket(fileId.bucket())
          .key(fileId.key())
          .build();

      HeadObjectResponse response = s3Client.headObject(request);

      FileObject.FileMetadata metadata = FileObject.FileMetadata.builder()
          .contentType(response.contentType())
          .contentLength(response.contentLength())
          .lastModified(response.lastModified())
          .etag(response.eTag())
          .customMetadata(response.metadata())
          .build();

      log.info("Retrieved metadata for file: {}", fileId);
      return Result.ok(metadata);

    } catch (NoSuchKeyException e) {
      log.warn("File not found: {}", fileId);
      return Result.fail(Problem.of(
          ErrorCode.of("FILE_NOT_FOUND"),
          ErrorCategory.NOT_FOUND,
          Severity.ERROR,
          "File does not exist: " + fileId));
    } catch (S3Exception e) {
      log.error("S3 error getting file metadata: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("S3_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to get file metadata: " + e.awsErrorDetails().errorMessage()));
    } catch (Exception e) {
      log.error("Unexpected error getting file metadata: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<ListResult> list(String bucket, String prefix, ListOptions options) {
    try {
      log.debug("Listing files in bucket: {}, prefix: {}", bucket, prefix);

      ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
          .bucket(bucket)
          .prefix(prefix)
          .maxKeys(options.maxKeys());

      if (options.continuationToken() != null) {
        requestBuilder.continuationToken(options.continuationToken());
      }

      ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

      List<FileId> files = response.contents().stream()
          .map(s3Object -> new FileId(bucket, s3Object.key()))
          .toList();

      log.info("Listed {} files in bucket: {}", files.size(), bucket);

      return Result.ok(new ListResult(
          files,
          response.nextContinuationToken(),
          response.isTruncated()
      ));

    } catch (S3Exception e) {
      log.error("S3 error listing files in bucket: {}", bucket, e);
      return Result.fail(Problem.of(
          ErrorCode.of("S3_LIST_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to list files: " + e.awsErrorDetails().errorMessage()));
    } catch (Exception e) {
      log.error("Unexpected error listing files in bucket: {}", bucket, e);
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
      log.debug("Generating presigned URL for file: {}, operation: {}, duration: {}", fileId, operation, duration);

      URL presignedUrl = switch (operation) {
        case GET -> {
          GetObjectRequest getRequest = GetObjectRequest.builder()
              .bucket(fileId.bucket())
              .key(fileId.key())
              .build();

          GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
              .getObjectRequest(getRequest)
              .signatureDuration(duration)
              .build();

          yield s3Presigner.presignGetObject(presignRequest).url();
        }
        case PUT -> {
          PutObjectRequest putRequest = PutObjectRequest.builder()
              .bucket(fileId.bucket())
              .key(fileId.key())
              .build();

          PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
              .putObjectRequest(putRequest)
              .signatureDuration(duration)
              .build();

          yield s3Presigner.presignPutObject(presignRequest).url();
        }
      };

      log.info("Generated presigned URL for file: {}", fileId);
      return Result.ok(presignedUrl);

    } catch (S3Exception e) {
      log.error("S3 error generating presigned URL: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("S3_PRESIGN_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to generate presigned URL: " + e.awsErrorDetails().errorMessage()));
    } catch (Exception e) {
      log.error("Unexpected error generating presigned URL: {}", fileId, e);
      return Result.fail(Problem.of(
          ErrorCode.of("PRESIGN_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> copy(FileId source, FileId destination) {
    try {
      log.debug("Copying file from {} to {}", source, destination);

      CopyObjectRequest request = CopyObjectRequest.builder()
          .sourceBucket(source.bucket())
          .sourceKey(source.key())
          .destinationBucket(destination.bucket())
          .destinationKey(destination.key())
          .build();

      s3Client.copyObject(request);

      log.info("File copied successfully from {} to {}", source, destination);
      return Result.ok(null);

    } catch (S3Exception e) {
      log.error("S3 error copying file from {} to {}", source, destination, e);
      return Result.fail(Problem.of(
          ErrorCode.of("S3_COPY_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to copy file: " + e.awsErrorDetails().errorMessage()));
    } catch (Exception e) {
      log.error("Unexpected error copying file from {} to {}", source, destination, e);
      return Result.fail(Problem.of(
          ErrorCode.of("COPY_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Unexpected error: " + e.getMessage()));
    }
  }

  // Helper methods

  private software.amazon.awssdk.services.s3.model.StorageClass mapStorageClass(StorageClass storageClass) {
    return switch (storageClass) {
      case STANDARD -> software.amazon.awssdk.services.s3.model.StorageClass.STANDARD;
      case REDUCED_REDUNDANCY -> software.amazon.awssdk.services.s3.model.StorageClass.REDUCED_REDUNDANCY;
      case STANDARD_IA -> software.amazon.awssdk.services.s3.model.StorageClass.STANDARD_IA;
      case ONEZONE_IA -> software.amazon.awssdk.services.s3.model.StorageClass.ONEZONE_IA;
      case INTELLIGENT_TIERING -> software.amazon.awssdk.services.s3.model.StorageClass.INTELLIGENT_TIERING;
      case GLACIER -> software.amazon.awssdk.services.s3.model.StorageClass.GLACIER;
      case GLACIER_IR -> software.amazon.awssdk.services.s3.model.StorageClass.GLACIER_IR;
      case DEEP_ARCHIVE -> software.amazon.awssdk.services.s3.model.StorageClass.DEEP_ARCHIVE;
    };
  }

  private software.amazon.awssdk.services.s3.model.ServerSideEncryption mapEncryption(FileStorePort.ServerSideEncryption encryption) {
    return switch (encryption) {
      case AES256 -> software.amazon.awssdk.services.s3.model.ServerSideEncryption.AES256;
      case AWS_KMS -> software.amazon.awssdk.services.s3.model.ServerSideEncryption.AWS_KMS;
    };
  }
}
