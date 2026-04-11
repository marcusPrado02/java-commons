package com.marcusprado02.commons.app.backup.s3;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

class S3BackupServiceTest {

  @Mock private S3Client s3Client;
  @Mock private BackupService localDelegate;

  private S3BackupService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = S3BackupService.create(s3Client, localDelegate);
  }

  @Test
  void shouldCreateServiceViaFactory() {
    assertThat(service).isNotNull();
  }

  @Test
  void shouldListBackupsWhenEmpty() {
    var result = service.listBackups();

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }

  @Test
  void shouldReturnFailWhenBackupNotFound() {
    var result = service.getBackup("nonexistent");

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnDeleteWhenBackupNotFound() {
    var result = service.deleteBackup("nonexistent");

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnVerifyWhenBackupNotFound() {
    var result = service.verifyBackup("nonexistent");

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnFailWhenBucketOptionMissing() {
    var localMeta = buildCompletedMeta("local-backup");
    when(localDelegate.createFullBackup(eq("test"), any())).thenReturn(Result.ok(localMeta));

    var config = BackupConfiguration.builder().destinationPath("/tmp").build();
    var result = service.createFullBackup("test", config);

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnFailWhenLocalDelegateFails() {
    var problem =
        Problem.of(
            ErrorCode.of("TEST.FAIL"),
            ErrorCategory.TECHNICAL,
            Severity.ERROR,
            "local delegate failed");
    when(localDelegate.createFullBackup(eq("test"), any())).thenReturn(Result.fail(problem));

    var config =
        BackupConfiguration.builder()
            .destinationPath("/tmp")
            .option("s3.bucket", "my-bucket")
            .build();

    var result = service.createFullBackup("test", config);

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnFailWhenIncrementalDelegateFails() {
    var problem =
        Problem.of(
            ErrorCode.of("TEST.FAIL"),
            ErrorCategory.TECHNICAL,
            Severity.ERROR,
            "incremental failed");
    when(localDelegate.createIncrementalBackup(eq("inc"), eq("parent"), any()))
        .thenReturn(Result.fail(problem));

    var config =
        BackupConfiguration.builder()
            .destinationPath("/tmp")
            .option("s3.bucket", "my-bucket")
            .build();

    var result = service.createIncrementalBackup("inc", "parent", config);

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnFailWhenDifferentialDelegateFails() {
    var problem =
        Problem.of(
            ErrorCode.of("TEST.FAIL"), ErrorCategory.TECHNICAL, Severity.ERROR, "diff failed");
    when(localDelegate.createDifferentialBackup(eq("diff"), eq("full"), any()))
        .thenReturn(Result.fail(problem));

    var config =
        BackupConfiguration.builder()
            .destinationPath("/tmp")
            .option("s3.bucket", "my-bucket")
            .build();

    var result = service.createDifferentialBackup("diff", "full", config);

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldListBackupsFromS3() {
    var response = ListObjectsV2Response.builder().contents(List.of()).build();
    when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

    var result = service.listBackupsFromS3("my-bucket", "prefix/");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }

  private static BackupMetadata buildCompletedMeta(String id) {
    return BackupMetadata.builder()
        .id(id)
        .name("backup")
        .type(BackupType.FULL)
        .source("src")
        .location("/tmp/" + id + ".zip")
        .size(0)
        .status(BackupStatus.COMPLETED)
        .createdAt(Instant.now())
        .build();
  }
}
