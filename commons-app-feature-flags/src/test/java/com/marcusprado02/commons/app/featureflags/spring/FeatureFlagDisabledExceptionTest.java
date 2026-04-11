package com.marcusprado02.commons.app.featureflags.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FeatureFlagDisabledExceptionTest {

  @Test
  void constructorWithKeyOnly_setsMessageAndKey() {
    FeatureFlagDisabledException ex = new FeatureFlagDisabledException("my-feature");

    assertThat(ex.getFeatureKey()).isEqualTo("my-feature");
    assertThat(ex.getMessage()).isEqualTo("Feature flag is disabled: my-feature");
  }

  @Test
  void constructorWithKeyAndMessage_setsCustomMessageAndKey() {
    FeatureFlagDisabledException ex =
        new FeatureFlagDisabledException("my-feature", "custom message");

    assertThat(ex.getFeatureKey()).isEqualTo("my-feature");
    assertThat(ex.getMessage()).isEqualTo("custom message");
  }

  @Test
  void isInstanceOfRuntimeException() {
    FeatureFlagDisabledException ex = new FeatureFlagDisabledException("flag");

    assertThat(ex).isInstanceOf(RuntimeException.class);
  }
}
