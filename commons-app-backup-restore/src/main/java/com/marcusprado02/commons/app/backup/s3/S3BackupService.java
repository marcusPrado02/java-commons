package com.marcusprado02.commons.app.backup.s3;

import com.marcusprado02.commons.app.backup.BackupConfiguration;
import com.marcusprado02.commons.app.backup.BackupMetadata;
import com.marcusprado02.commons.app.backup.BackupMetadata.BackupStatus;
import com.marcusprado02.commons.app.backup.BackupMetadata.BackupType;
import com.marcusprado02.commons.app.backup.BackupService;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * S3-backed backup service that stores backups as objects in an AWS S3 bucket.
 *
 * <p>This service first delegates to a local {@link BackupService} (e.g., {@code FilesystemBackupService}
 * or {@code DatabaseBackupService}) to produce a local backup file, then uploads the result to S3.
 * The local staging file is deleted after a successful upload.
 *
 * <p>Required configuration options (via {@link BackupConfiguration#options()}):
 *
 * <ul>
 *   <li>{@code s3.bucket} — S3 bucket name (required)
 *   <li>{@code s3.prefix} — object key prefix, e.g. {@code "backups/prod/"} (optional)
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
 * BackupService local = new FilesystemBackupService();
 *
 * S3BackupService service = S3BackupService.create(s3, local);
 *
 * BackupConfiguration config = BackupConfiguration.builder()
 *     .destinationPath("/tmp/backup-staging")
 *     .option("s3.bucket", "my-backups")
 *     .option("s3.prefix", "prod/")
 *     .build();
 *
 * service.createFullBackup("nightly", config)
 *     .peek(meta -> log.info("Uploaded to S3: {}", meta.location()));
 * }</pre>
 */
public class S3BackupService implements BackupService {

  private static final Logger logger = LoggerFactory.getLogger(S3BackupService.class);

  private static final String OPT_BUCKET = "s3.bucket";
  private static final String OPT_PREFIX = "s3.prefix";

  private final S3Client s3;
  private final BackupService localDelegate;

  /** Index of S3-uploaded backup metadata, keyed by backup ID. */
  private final Map<String, BackupMetadata> store = new ConcurrentHashMap<>();

  private S3BackupService(S3Client s3, BackupService localDelegate) {
    this.s3 = Objects.requireNonNull(s3, "s3");
    this.localDelegate = Objects.requireNonNull(localDelegate, "localDelegate");
  }

  /**
   * Creates a new S3BackupService.
   *
   * @param s3 AWS S3 client
   * @param localDelegate local backup service used for staging before upload
   * @return new service instance
   */
  public static S3BackupService create(S3Client s3, BackupService localDelegate) {
    return new S3BackupService(s3, localDelegate);
  }

  @Override
  public Result<BackupMetadata> createFullBackup(String name, BackupConfiguration config) {
    return localDelegate
        .createFullBackup(name, config)
        .flatMap(meta -> uploadToS3(meta, config));
  }

  @Override
  public Result<BackupMetadata> createIncrementalBackup(
      String name, String parentBackupId, BackupConfiguration config) {
    return localDelegate
        .createIncrementalBackup(name, parentBackupId, config)
        .flatMap(meta -> uploadToS3(meta, config));
  }

  @Override
  public Result<BackupMetadata> createDifferentialBackup(
      String name, String fullBackupId, BackupConfiguration config) {
    return localDelegate
        .createDifferentialBackup(name, fullBackupId, config)
        .flatMap(meta -> uploadToS3(meta, config));
  }

  @Override
  public Result<List<BackupMetadata>> listBackups() {
    try {
      // List all objects tracked in our local store
      return Result.ok(List.copyOf(store.values()));
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("S3_BACKUP.LIST_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to list S3 backups: " + e.getMessage()));
    }
  }

  /**
   * Lists backups directly from S3 using the given bucket and prefix.
   *
   * @param bucket S3 bucket name
   * @param prefix object key prefix
   * @return list of S3 object keys representing backups
   */
  public Result<List<String>> listBackupsFromS3(String bucket, String prefix) {
    try {
      var request = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
      var response = s3.listObjectsV2(request);
      var keys = response.contents().stream().map(S3Object::key).toList();
      return Result.ok(keys);
    } catch (S3Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("S3_BACKUP.LIST_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to list S3 objects: " + e.getMessage()));
    }
  }

  @Override
  public Result<BackupMetadata> getBackup(String backupId) {
    var meta = store.get(backupId);
    if (meta == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("S3_BACKUP.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Backup not found: " + backupId));
    }
    return Result.ok(meta);
  }

  @Override
  public Result<Void> deleteBackup(String backupId) {
    var meta = store.get(backupId);
    if (meta == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("S3_BACKUP.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Backup not found: " + backupId));
    }

    try {
      // Parse bucket and key from s3://bucket/key format
      var s3Location = parseS3Location(meta.location());
      s3.deleteObject(
          DeleteObjectRequest.builder().bucket(s3Location[0]).key(s3Location[1]).build());
      store.remove(backupId);
      logger.info("Deleted S3 backup: {} at {}", backupId, meta.location());
      return Result.ok(null);
    } catch (S3Exception e) {
      logger.error("Failed to delete S3 backup {}: {}", backupId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("S3_BACKUP.DELETE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to delete S3 backup: " + e.getMessage()));
    }
  }

  @Override
  public Result<Boolean> verifyBackup(String backupId) {
    var meta = store.get(backupId);
    if (meta == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("S3_BACKUP.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Backup not found: " + backupId));
    }

    try {
      var s3Location = parseS3Location(meta.location());
      var response = s3.headObject(
          HeadObjectRequest.builder().bucket(s3Location[0]).key(s3Location[1]).build());

      // Verify checksum via ETag if available
      boolean valid = response.contentLength() > 0;
      if (meta.checksum().isPresent() && response.checksumSHA256() != null) {
        valid = meta.checksum().get().equals(response.checksumSHA256());
      }
      return Result.ok(valid);

    } catch (NoSuchKeyException e) {
      return Result.ok(false);
    } catch (S3Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("S3_BACKUP.VERIFY_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to verify S3 backup: " + e.getMessage()));
    }
  }

  // -------------------------------------------------------------------------
  // Internal
  // -------------------------------------------------------------------------

  private Result<BackupMetadata> uploadToS3(BackupMetadata localMeta, BackupConfiguration config) {
    String bucket = config.options().get(OPT_BUCKET);
    if (bucket == null || bucket.isBlank()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("S3_BACKUP.MISSING_BUCKET"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Missing required option: s3.bucket"));
    }

    String prefix = config.options().getOrDefault(OPT_PREFIX, "");
    String fileName = Paths.get(localMeta.location()).getFileName().toString();
    String objectKey = prefix + fileName;

    try {
      Path localFile = Paths.get(localMeta.location());

      PutObjectRequest putRequest =
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(objectKey)
              .contentLength(Files.size(localFile))
              .metadata(
                  Map.of(
                      "backup-id", localMeta.id(),
                      "backup-name", localMeta.name(),
                      "backup-type", localMeta.type().name()))
              .build();

      s3.putObject(putRequest, RequestBody.fromFile(localFile));

      String s3Uri = "s3://" + bucket + "/" + objectKey;
      logger.info("Uploaded backup to S3: {} -> {}", localMeta.id(), s3Uri);

      // Delete local staging file
      Files.deleteIfExists(localFile);

      // Create S3-location metadata
      String newId = UUID.randomUUID().toString();
      var s3Meta =
          BackupMetadata.builder()
              .id(newId)
              .name(localMeta.name())
              .type(localMeta.type())
              .source(localMeta.source())
              .location(s3Uri)
              .size(localMeta.size())
              .status(BackupStatus.COMPLETED)
              .createdAt(localMeta.createdAt())
              .completedAt(Instant.now())
              .parentBackupId(localMeta.parentBackupId().orElse(null))
              .checksum(localMeta.checksum().orElse(null))
              .build();

      store.put(newId, s3Meta);
      return Result.ok(s3Meta);

    } catch (S3Exception e) {
      logger.error("Failed to upload backup to S3: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("S3_BACKUP.UPLOAD_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to upload backup to S3: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error uploading backup to S3: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("S3_BACKUP.UPLOAD_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error uploading to S3: " + e.getMessage()));
    }
  }

  /**
   * Parses an {@code s3://bucket/key} URI into {@code [bucket, key]}.
   *
   * @param s3Uri S3 URI
   * @return two-element array: {@code [bucket, key]}
   */
  private static String[] parseS3Location(String s3Uri) {
    // Expected format: s3://bucket/key
    String path = s3Uri.substring("s3://".length());
    int slash = path.indexOf('/');
    if (slash < 0) {
      return new String[]{path, ""};
    }
    return new String[]{path.substring(0, slash), path.substring(slash + 1)};
  }
}
