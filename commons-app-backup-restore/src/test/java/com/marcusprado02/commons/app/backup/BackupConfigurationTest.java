package com.marcusprado02.commons.app.backup;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BackupConfigurationTest {

  @Test
  void shouldBuildWithRequiredFields() {
    var config = BackupConfiguration.builder().destinationPath("/tmp/backups").build();

    assertThat(config.destinationPath()).isEqualTo("/tmp/backups");
    assertThat(config.compressionEnabled()).isTrue(); // default
    assertThat(config.encryptionEnabled()).isFalse(); // default
    assertThat(config.encryptionKey()).isNull();
    assertThat(config.parallel()).isFalse(); // default
    assertThat(config.options()).isEmpty();
  }

  @Test
  void shouldBuildWithAllFields() {
    var options = Map.of("key1", "value1");
    var config =
        BackupConfiguration.builder()
            .destinationPath("/backups")
            .compressionEnabled(false)
            .encryptionEnabled(true)
            .encryptionKey("my-secret-key")
            .parallel(true)
            .options(options)
            .build();

    assertThat(config.destinationPath()).isEqualTo("/backups");
    assertThat(config.compressionEnabled()).isFalse();
    assertThat(config.encryptionEnabled()).isTrue();
    assertThat(config.encryptionKey()).isEqualTo("my-secret-key");
    assertThat(config.parallel()).isTrue();
    assertThat(config.options()).containsEntry("key1", "value1");
  }

  @Test
  void shouldAddSingleOptionWhenOptionsEmpty() {
    var config =
        BackupConfiguration.builder()
            .destinationPath("/backups")
            .option("db.schema", "public")
            .build();

    assertThat(config.options()).containsEntry("db.schema", "public");
  }

  @Test
  void shouldAddMultipleOptionsSequentially() {
    var config =
        BackupConfiguration.builder()
            .destinationPath("/backups")
            .option("key1", "value1")
            .option("key2", "value2")
            .build();

    assertThat(config.options()).containsEntry("key1", "value1");
    assertThat(config.options()).containsEntry("key2", "value2");
  }

  @Test
  void shouldHandleNullOptionsAsEmptyMap() {
    var config = BackupConfiguration.builder().destinationPath("/backups").options(null).build();

    assertThat(config.options()).isEmpty();
  }

  @Test
  void shouldMakeOptionsImmutable() {
    var mutableMap = new HashMap<String, String>();
    mutableMap.put("k", "v");
    var config =
        BackupConfiguration.builder().destinationPath("/backups").options(mutableMap).build();

    assertThatThrownBy(() -> config.options().put("new", "entry"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldFailWhenDestinationPathIsNull() {
    assertThatThrownBy(() -> BackupConfiguration.builder().build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("destinationPath");
  }

  @Test
  void shouldEqualWhenSameData() {
    var config1 =
        BackupConfiguration.builder().destinationPath("/backups").compressionEnabled(true).build();
    var config2 =
        BackupConfiguration.builder().destinationPath("/backups").compressionEnabled(true).build();

    assertThat(config1).isEqualTo(config2);
    assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
  }

  @Test
  void shouldHaveToString() {
    var config = BackupConfiguration.builder().destinationPath("/tmp").build();

    assertThat(config.toString()).contains("/tmp");
  }
}
