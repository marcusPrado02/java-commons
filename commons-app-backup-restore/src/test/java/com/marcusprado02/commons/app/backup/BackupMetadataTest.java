package com.marcusprado02.commons.app.backup;

import static org.assertj.core.api.Assertions.*;

import com.marcusprado02.commons.app.backup.BackupMetadata.BackupStatus;
import com.marcusprado02.commons.app.backup.BackupMetadata.BackupType;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BackupMetadataTest {

  @Test
  void shouldBuildWithMinimalFields() {
    var meta =
        BackupMetadata.builder()
            .id("id1")
            .name("nightly")
            .source("db")
            .location("/tmp/backup.zip")
            .build();

    assertThat(meta.id()).isEqualTo("id1");
    assertThat(meta.name()).isEqualTo("nightly");
    assertThat(meta.type()).isEqualTo(BackupType.FULL);
    assertThat(meta.status()).isEqualTo(BackupStatus.IN_PROGRESS);
    assertThat(meta.completedAt()).isEmpty();
    assertThat(meta.parentBackupId()).isEmpty();
    assertThat(meta.checksum()).isEmpty();
    assertThat(meta.metadata()).isEmpty();
    assertThat(meta.error()).isEmpty();
  }

  @Test
  void shouldBuildWithAllFields() {
    var now = Instant.now();
    var meta =
        BackupMetadata.builder()
            .id("id1")
            .name("test")
            .type(BackupType.INCREMENTAL)
            .source("mydb")
            .location("s3://bucket/key")
            .size(1024L)
            .status(BackupStatus.COMPLETED)
            .createdAt(now)
            .completedAt(now)
            .parentBackupId("parent-id")
            .checksum("abc123")
            .metadata(Map.of("env", "prod"))
            .error(null)
            .build();

    assertThat(meta.type()).isEqualTo(BackupType.INCREMENTAL);
    assertThat(meta.size()).isEqualTo(1024L);
    assertThat(meta.status()).isEqualTo(BackupStatus.COMPLETED);
    assertThat(meta.completedAt()).isPresent();
    assertThat(meta.parentBackupId()).contains("parent-id");
    assertThat(meta.checksum()).contains("abc123");
    assertThat(meta.metadata()).containsEntry("env", "prod");
    assertThat(meta.error()).isEmpty();
  }

  @Test
  void shouldBuildDifferentialType() {
    var meta =
        BackupMetadata.builder()
            .id("id")
            .name("diff")
            .type(BackupType.DIFFERENTIAL)
            .source("s")
            .location("l")
            .build();

    assertThat(meta.type()).isEqualTo(BackupType.DIFFERENTIAL);
  }

  @Test
  void shouldSetErrorMessage() {
    var meta =
        BackupMetadata.builder()
            .id("id")
            .name("failed")
            .source("s")
            .location("l")
            .status(BackupStatus.FAILED)
            .error("disk full")
            .build();

    assertThat(meta.error()).contains("disk full");
    assertThat(meta.isFailed()).isTrue();
  }

  @Test
  void shouldReturnTrueForIsComplete() {
    var meta =
        BackupMetadata.builder()
            .id("id")
            .name("n")
            .source("s")
            .location("l")
            .status(BackupStatus.COMPLETED)
            .build();

    assertThat(meta.isComplete()).isTrue();
    assertThat(meta.isInProgress()).isFalse();
    assertThat(meta.isFailed()).isFalse();
  }

  @Test
  void shouldReturnTrueForIsInProgress() {
    var meta =
        BackupMetadata.builder()
            .id("id")
            .name("n")
            .source("s")
            .location("l")
            .status(BackupStatus.IN_PROGRESS)
            .build();

    assertThat(meta.isInProgress()).isTrue();
    assertThat(meta.isComplete()).isFalse();
    assertThat(meta.isFailed()).isFalse();
  }

  @Test
  void shouldHandleNullNormalizationInCompactConstructor() {
    var meta =
        new BackupMetadata(
            "id",
            "name",
            BackupType.FULL,
            "src",
            "loc",
            0,
            BackupStatus.COMPLETED,
            Instant.now(),
            null,
            null,
            null,
            null,
            null);

    assertThat(meta.completedAt()).isEmpty();
    assertThat(meta.parentBackupId()).isEmpty();
    assertThat(meta.checksum()).isEmpty();
    assertThat(meta.metadata()).isEmpty();
    assertThat(meta.error()).isEmpty();
  }

  @Test
  void shouldThrowWhenRequiredFieldMissing() {
    assertThatThrownBy(
            () ->
                new BackupMetadata(
                    null,
                    "name",
                    BackupType.FULL,
                    "src",
                    "loc",
                    0,
                    BackupStatus.COMPLETED,
                    Instant.now(),
                    null,
                    null,
                    null,
                    null,
                    null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldSetCancelledStatus() {
    var meta =
        BackupMetadata.builder()
            .id("id")
            .name("n")
            .source("s")
            .location("l")
            .status(BackupStatus.CANCELLED)
            .build();

    assertThat(meta.status()).isEqualTo(BackupStatus.CANCELLED);
    assertThat(meta.isComplete()).isFalse();
    assertThat(meta.isInProgress()).isFalse();
    assertThat(meta.isFailed()).isFalse();
  }
}
