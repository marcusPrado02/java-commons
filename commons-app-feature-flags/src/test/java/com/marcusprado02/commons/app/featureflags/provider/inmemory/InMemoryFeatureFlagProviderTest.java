package com.marcusprado02.commons.app.featureflags.provider.inmemory;

import static org.assertj.core.api.Assertions.*;

import com.marcusprado02.commons.app.featureflags.FeatureFlagContext;
import com.marcusprado02.commons.app.featureflags.FeatureFlagValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryFeatureFlagProviderTest {

  private InMemoryFeatureFlagProvider provider;

  @BeforeEach
  void setUp() {
    provider =
        InMemoryFeatureFlagProvider.builder()
            .flag("simple-flag", true)
            .flag("string-flag", "dark-theme")
            .flag("number-flag", 100)
            .flagWithTargeting(
                "targeted-flag",
                context -> "premium".equals(context.getAttribute("plan").orElse(null)),
                FeatureFlagValue.of(true),
                FeatureFlagValue.of(false))
            .flagWithPercentageRollout(
                "rollout-flag",
                50,
                FeatureFlagValue.of(true),
                FeatureFlagValue.of(false))
            .build();
  }

  @Test
  void shouldReturnSimpleFlag() {
    boolean enabled =
        provider.isEnabled("simple-flag", FeatureFlagContext.anonymous());

    assertThat(enabled).isTrue();
  }

  @Test
  void shouldReturnStringFlag() {
    FeatureFlagValue value =
        provider.getValue("string-flag", FeatureFlagContext.anonymous());

    assertThat(value.asString()).isEqualTo("dark-theme");
  }

  @Test
  void shouldReturnNumberFlag() {
    FeatureFlagValue value =
        provider.getValue("number-flag", FeatureFlagContext.anonymous());

    assertThat(value.asInt()).isEqualTo(100);
  }

  @Test
  void shouldHandleTargetedFlag() {
    FeatureFlagContext premiumContext =
        FeatureFlagContext.builder().userId("user1").attribute("plan", "premium").build();

    FeatureFlagContext freeContext =
        FeatureFlagContext.builder().userId("user2").attribute("plan", "free").build();

    assertThat(provider.isEnabled("targeted-flag", premiumContext)).isTrue();
    assertThat(provider.isEnabled("targeted-flag", freeContext)).isFalse();
  }

  @Test
  void shouldHandlePercentageRollout() {
    // Test with multiple users to verify distribution
    int enabledCount = 0;
    for (int i = 0; i < 100; i++) {
      FeatureFlagContext context = FeatureFlagContext.forUser("user" + i);
      if (provider.isEnabled("rollout-flag", context)) {
        enabledCount++;
      }
    }

    // Should be roughly 50% enabled (allow some variance)
    assertThat(enabledCount).isBetween(40, 60);
  }

  @Test
  void shouldUpdateFlag() {
    provider.updateFlag("simple-flag", FeatureFlagValue.of(false));

    boolean enabled =
        provider.isEnabled("simple-flag", FeatureFlagContext.anonymous());
    assertThat(enabled).isFalse();
  }

  @Test
  void shouldRemoveFlag() {
    provider.removeFlag("simple-flag");

    assertThatThrownBy(
            () -> provider.getValue("simple-flag", FeatureFlagContext.anonymous()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Feature flag not found");
  }

  @Test
  void shouldClearAllFlags() {
    provider.clear();

    assertThat(provider.isEnabled("simple-flag", FeatureFlagContext.anonymous()))
        .isFalse();
  }

  @Test
  void shouldReturnFalseForMissingFlag() {
    boolean enabled =
        provider.isEnabled("missing-flag", FeatureFlagContext.anonymous());

    assertThat(enabled).isFalse();
  }

  @Test
  void shouldThrowForMissingFlagWhenGettingValue() {
    assertThatThrownBy(
            () -> provider.getValue("missing-flag", FeatureFlagContext.anonymous()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
