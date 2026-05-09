package com.marcusprado02.commons.app.backup.s3;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.backup.BackupConfiguration;
import com.marcusprado02.commons.app.backup.BackupMetadata;
import com.marcusprado02.commons.app.backup.BackupMetadata.BackupStatus;
import com.marcusprado02.commons.app.backup.BackupMetadata.BackupType;
import com.marcusprado02.commons.app.backup.BackupService;
import com.marcusprado02.commons.kernel.result.Result;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

class S3BackupServiceBranchTest {

  @Mock private S3Client s3Client;
  @Mock private BackupService localDelegate;

  private S3BackupService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = S3BackupService.create(s3Client, localDelegate);
  }

  private BackupMetadata buildMetaForFile(String id, Path file, BackupType type) throws Exception {
    return BackupMetadata.builder()
        .id(id)
        .name("backup")
        .type(type)
        .source("src")
        .location(file.toString())
        .size(Files.size(file))
        .status(BackupStatus.COMPLETED)
        .createdAt(Instant.now())
        .checksum("abc123")
        .build();
  }

  private String performSuccessfulUpload(Path tempDir) throws Exception {
    Path file = tempDir.resolve("backup.zip");
    Files.write(file, "data".getBytes());
    BackupMetadata localMeta = buildMetaForFile("local-id", file, BackupType.FULL);
    when(localDelegate.createFullBackup(eq("test"), any())).thenReturn(Result.ok(localMeta));
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(mock(PutObjectResponse.class));

    var config =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            .option("s3.bucket", "test-bucket")
            .build();
    var result = service.createFullBackup("test", config);
    assertThat(result.isOk()).isTrue();
    return result.getOrNull().id();
  }

  // --- successful upload: empty prefix, checksum propagated, file deleted ---

  @Test
  void shouldUploadToS3SuccessfullyOnFullBackup(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("upload.zip");
    Files.write(file, "data".getBytes());
    BackupMetadata localMeta = buildMetaForFile("local-1", file, BackupType.FULL);
    when(localDelegate.createFullBackup(eq("upload"), any())).thenReturn(Result.ok(localMeta));
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(mock(PutObjectResponse.class));

    var config =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            .option("s3.bucket", "my-bucket")
            .build();

    var result = service.createFullBackup("upload", config);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().location()).startsWith("s3://my-bucket/");
    assertThat(result.getOrNull().status()).isEqualTo(BackupStatus.COMPLETED);
    assertThat(result.getOrNull().checksum()).contains("abc123");
    assertThat(Files.exists(file)).isFalse();
  }

  // --- successful upload: non-empty prefix ---

  @Test
  void shouldUploadToS3SuccessfullyWithPrefix(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("prefixed.zip");
    Files.write(file, "content".getBytes());
    BackupMetadata localMeta = buildMetaForFile("local-2", file, BackupType.FULL);
    when(localDelegate.createFullBackup(eq("prefixed"), any())).thenReturn(Result.ok(localMeta));
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(mock(PutObjectResponse.class));

    var config =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            .option("s3.bucket", "bucket")
            .option("s3.prefix", "prod/")
            .build();

    var result = service.createFullBackup("prefixed", config);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().location()).contains("prod/");
  }

  // --- successful upload: parentBackupId propagated ---

  @Test
  void shouldUploadToS3SuccessfullyWithParentId(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("incremental.zip");
    Files.write(file, "inc".getBytes());
    BackupMetadata localMeta =
        BackupMetadata.builder()
            .id("local-3")
            .name("backup")
            .type(BackupType.INCREMENTAL)
            .source("src")
            .location(file.toString())
            .size(Files.size(file))
            .status(BackupStatus.COMPLETED)
            .createdAt(Instant.now())
            .checksum("cksum")
            .parentBackupId("parent-xyz")
            .build();
    when(localDelegate.createIncrementalBackup(eq("inc"), eq("parent-xyz"), any()))
        .thenReturn(Result.ok(localMeta));
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(mock(PutObjectResponse.class));

    var config =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            .option("s3.bucket", "bucket")
            .build();

    var result = service.createIncrementalBackup("inc", "parent-xyz", config);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().parentBackupId()).contains("parent-xyz");
  }

  // --- upload fails: S3Exception ---

  @Test
  void shouldReturnUploadFailedOnS3Exception(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("s3ex.zip");
    Files.write(file, "data".getBytes());
    BackupMetadata localMeta = buildMetaForFile("local-4", file, BackupType.FULL);
    when(localDelegate.createFullBackup(eq("s3ex"), any())).thenReturn(Result.ok(localMeta));
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(S3Exception.builder().message("access denied").build());

    var config =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            .option("s3.bucket", "bucket")
            .build();

    var result = service.createFullBackup("s3ex", config);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("S3_BACKUP.UPLOAD_FAILED");
  }

  // --- upload fails: generic Exception ---

  @Test
  void shouldReturnUploadFailedOnGenericException(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("ex.zip");
    Files.write(file, "data".getBytes());
    BackupMetadata localMeta = buildMetaForFile("local-5", file, BackupType.FULL);
    when(localDelegate.createFullBackup(eq("ex"), any())).thenReturn(Result.ok(localMeta));
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(new RuntimeException("unexpected"));

    var config =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            .option("s3.bucket", "bucket")
            .build();

    var result = service.createFullBackup("ex", config);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("S3_BACKUP.UPLOAD_FAILED");
  }

  // --- deleteBackup: success ---

  @Test
  void shouldDeleteExistingS3BackupSuccessfully(@TempDir Path tempDir) throws Exception {
    String uploadedId = performSuccessfulUpload(tempDir);

    when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(null);

    var deleteResult = service.deleteBackup(uploadedId);

    assertThat(deleteResult.isOk()).isTrue();
    assertThat(service.getBackup(uploadedId).isFail()).isTrue();
  }

  // --- deleteBackup: S3Exception ---

  @Test
  void shouldDeleteS3BackupFailsWithS3Exception(@TempDir Path tempDir) throws Exception {
    String uploadedId = performSuccessfulUpload(tempDir);

    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenThrow(S3Exception.builder().message("forbidden").build());

    var result = service.deleteBackup(uploadedId);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("S3_BACKUP.DELETE_FAILED");
  }

  // --- verifyBackup: object exists, contentLength > 0, no checksum match ---

  @Test
  void shouldVerifyS3BackupWhenObjectExists(@TempDir Path tempDir) throws Exception {
    String uploadedId = performSuccessfulUpload(tempDir);

    var headResponse = mock(HeadObjectResponse.class);
    when(headResponse.contentLength()).thenReturn(100L);
    when(headResponse.checksumSHA256()).thenReturn(null);
    when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

    var result = service.verifyBackup(uploadedId);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  // --- verifyBackup: checksum match ---

  @Test
  void shouldVerifyS3BackupChecksumMatch(@TempDir Path tempDir) throws Exception {
    String uploadedId = performSuccessfulUpload(tempDir);

    var headResponse = mock(HeadObjectResponse.class);
    when(headResponse.contentLength()).thenReturn(100L);
    when(headResponse.checksumSHA256()).thenReturn("abc123");
    when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

    var result = service.verifyBackup(uploadedId);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  // --- verifyBackup: checksum mismatch ---

  @Test
  void shouldVerifyS3BackupChecksumMismatch(@TempDir Path tempDir) throws Exception {
    String uploadedId = performSuccessfulUpload(tempDir);

    var headResponse = mock(HeadObjectResponse.class);
    when(headResponse.contentLength()).thenReturn(100L);
    when(headResponse.checksumSHA256()).thenReturn("wrong-checksum");
    when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

    var result = service.verifyBackup(uploadedId);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  // --- verifyBackup: NoSuchKeyException → false ---

  @Test
  void shouldVerifyS3BackupWhenObjectNotFound(@TempDir Path tempDir) throws Exception {
    String uploadedId = performSuccessfulUpload(tempDir);

    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().build());

    var result = service.verifyBackup(uploadedId);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  // --- verifyBackup: S3Exception ---

  @Test
  void shouldVerifyS3BackupFailsWithS3Exception(@TempDir Path tempDir) throws Exception {
    String uploadedId = performSuccessfulUpload(tempDir);

    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(S3Exception.builder().message("throttled").build());

    var result = service.verifyBackup(uploadedId);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("S3_BACKUP.VERIFY_FAILED");
  }

  // --- listBackupsFromS3: S3Exception ---

  @Test
  void shouldListBackupsFromS3FailsWithS3Exception() {
    when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenThrow(S3Exception.builder().message("access denied").build());

    var result = service.listBackupsFromS3("bucket", "prefix/");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("S3_BACKUP.LIST_FAILED");
  }
}
