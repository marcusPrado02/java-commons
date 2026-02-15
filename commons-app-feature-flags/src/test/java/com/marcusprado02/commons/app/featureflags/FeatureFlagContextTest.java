package com.marcusprado02.commons.app.featureflags;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FeatureFlagContextTest {

  @Test
  void shouldCreateAnonymousContext() {
    FeatureFlagContext context = FeatureFlagContext.anonymous();

    assertThat(context.getUserId()).isEmpty();
    assertThat(context.getSessionId()).isEmpty();
    assertThat(context.attributes()).isEmpty();
  }

  @Test
  void shouldCreateUserContext() {
    FeatureFlagContext context = FeatureFlagContext.forUser("user123");

    assertThat(context.getUserId()).hasValue("user123");
    assertThat(context.getSessionId()).isEmpty();
    assertThat(context.attributes()).isEmpty();
  }

  @Test
  void shouldBuildContextWithAttributes() {
    FeatureFlagContext context =
        FeatureFlagContext.builder()
            .userId("user123")
            .sessionId("session456")
            .attribute("email", "user@example.com")
            .attribute("plan", "premium")
            .build();

    assertThat(context.getUserId()).hasValue("user123");
    assertThat(context.getSessionId()).hasValue("session456");
    assertThat(context.getAttribute("email")).hasValue("user@example.com");
    assertThat(context.getAttribute("plan")).hasValue("premium");
    assertThat(context.getAttribute("missing")).isEmpty();
  }

  @Test
  void shouldCreateImmutableAttributes() {
    FeatureFlagContext context =
        FeatureFlagContext.builder().attribute("key", "value").build();

    assertThatThrownBy(() -> context.attributes().put("new", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
