package com.marcusprado02.commons.app.backup;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RestoreResultTest {

  @Test
  void shouldBuildWithRequiredFields() {
    var result =
        RestoreResult.builder()
            .backupId("backup1")
            .targetPath("/restore")
            .duration(Duration.ofSeconds(10))
            .build();

    assertThat(result.backupId()).isEqualTo("backup1");
    assertThat(result.targetPath()).isEqualTo("/restore");
    assertThat(result.filesRestored()).isEqualTo(0L);
    assertThat(result.bytesRestored()).isEqualTo(0L);
    assertThat(result.duration()).isEqualTo(Duration.ofSeconds(10));
    assertThat(result.error()).isEmpty();
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void shouldBuildWithAllFields() {
    var now = Instant.now();
    var result =
        RestoreResult.builder()
            .backupId("b1")
            .targetPath("/data")
            .filesRestored(100L)
            .bytesRestored(204800L)
            .duration(Duration.ofMinutes(2))
            .completedAt(now)
            .metadata(Map.of("env", "staging"))
            .error(null)
            .build();

    assertThat(result.filesRestored()).isEqualTo(100L);
    assertThat(result.bytesRestored()).isEqualTo(204800L);
    assertThat(result.completedAt()).isEqualTo(now);
    assertThat(result.metadata()).containsEntry("env", "staging");
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void shouldReportFailureWhenErrorPresent() {
    var result =
        RestoreResult.builder()
            .backupId("b1")
            .targetPath("/data")
            .duration(Duration.ZERO)
            .error("permission denied")
            .build();

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.error()).contains("permission denied");
  }

  @Test
  void shouldHandleNullNormalizationInCompactConstructor() {
    var result = new RestoreResult("b1", "/path", 0L, 0L, Duration.ZERO, Instant.now(), null, null);

    assertThat(result.metadata()).isEmpty();
    assertThat(result.error()).isEmpty();
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void shouldThrowWhenBackupIdIsNull() {
    assertThatThrownBy(
            () ->
                new RestoreResult(null, "/path", 0L, 0L, Duration.ZERO, Instant.now(), null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowWhenDurationIsNull() {
    assertThatThrownBy(
            () -> new RestoreResult("id", "/path", 0L, 0L, null, Instant.now(), null, null))
        .isInstanceOf(NullPointerException.class);
  }
}
