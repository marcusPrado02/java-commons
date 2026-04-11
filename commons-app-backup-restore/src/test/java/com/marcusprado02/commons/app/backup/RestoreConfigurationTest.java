package com.marcusprado02.commons.app.backup;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RestoreConfigurationTest {

  @Test
  void shouldBuildWithRequiredFields() {
    var config = RestoreConfiguration.builder().targetPath("/tmp/restore").build();

    assertThat(config.targetPath()).isEqualTo("/tmp/restore");
    assertThat(config.overwriteExisting()).isFalse(); // default
    assertThat(config.verifyIntegrity()).isTrue(); // default
    assertThat(config.decryptionKey()).isNull();
    assertThat(config.options()).isEmpty();
  }

  @Test
  void shouldBuildWithAllFields() {
    var options = Map.of("option1", "value1");
    var config =
        RestoreConfiguration.builder()
            .targetPath("/restore")
            .overwriteExisting(true)
            .verifyIntegrity(false)
            .decryptionKey("my-decrypt-key")
            .options(options)
            .build();

    assertThat(config.targetPath()).isEqualTo("/restore");
    assertThat(config.overwriteExisting()).isTrue();
    assertThat(config.verifyIntegrity()).isFalse();
    assertThat(config.decryptionKey()).isEqualTo("my-decrypt-key");
    assertThat(config.options()).containsEntry("option1", "value1");
  }

  @Test
  void shouldAddSingleOptionWhenOptionsEmpty() {
    var config =
        RestoreConfiguration.builder()
            .targetPath("/restore")
            .option("restore.mode", "full")
            .build();

    assertThat(config.options()).containsEntry("restore.mode", "full");
  }

  @Test
  void shouldAddMultipleOptionsSequentially() {
    var config =
        RestoreConfiguration.builder()
            .targetPath("/restore")
            .option("key1", "val1")
            .option("key2", "val2")
            .build();

    assertThat(config.options()).containsEntry("key1", "val1");
    assertThat(config.options()).containsEntry("key2", "val2");
  }

  @Test
  void shouldHandleNullOptionsAsEmptyMap() {
    var config = RestoreConfiguration.builder().targetPath("/restore").options(null).build();

    assertThat(config.options()).isEmpty();
  }

  @Test
  void shouldMakeOptionsImmutable() {
    var mutableMap = new HashMap<String, String>();
    mutableMap.put("k", "v");
    var config = RestoreConfiguration.builder().targetPath("/restore").options(mutableMap).build();

    assertThatThrownBy(() -> config.options().put("new", "entry"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldFailWhenTargetPathIsNull() {
    assertThatThrownBy(() -> RestoreConfiguration.builder().build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("targetPath");
  }

  @Test
  void shouldEqualWhenSameData() {
    var config1 =
        RestoreConfiguration.builder().targetPath("/restore").overwriteExisting(true).build();
    var config2 =
        RestoreConfiguration.builder().targetPath("/restore").overwriteExisting(true).build();

    assertThat(config1).isEqualTo(config2);
    assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
  }

  @Test
  void shouldHaveToString() {
    var config = RestoreConfiguration.builder().targetPath("/tmp/restore").build();

    assertThat(config.toString()).contains("/tmp/restore");
  }
}
