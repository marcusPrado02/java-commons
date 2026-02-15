package com.marcusprado02.commons.app.featureflags;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FeatureFlagServiceTest {

  @Mock private FeatureFlagProvider provider;

  private FeatureFlagService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new FeatureFlagService(provider);
  }

  @Test
  void shouldCheckIfEnabledWithAnonymousContext() {
    when(provider.isEnabled(eq("feature-key"), any(FeatureFlagContext.class)))
        .thenReturn(true);

    boolean enabled = service.isEnabled("feature-key");

    assertThat(enabled).isTrue();
    verify(provider).isEnabled(eq("feature-key"), any(FeatureFlagContext.class));
  }

  @Test
  void shouldCheckIfEnabledWithUserId() {
    when(provider.isEnabled(eq("feature-key"), any(FeatureFlagContext.class)))
        .thenReturn(true);

    boolean enabled = service.isEnabled("feature-key", "user123");

    assertThat(enabled).isTrue();
    verify(provider).isEnabled(eq("feature-key"), argThat(ctx -> "user123".equals(ctx.userId())));
  }

  @Test
  void shouldGetValueWithContext() {
    FeatureFlagValue expectedValue = FeatureFlagValue.of("dark-theme");
    when(provider.getValue(eq("theme"), any(FeatureFlagContext.class)))
        .thenReturn(expectedValue);

    FeatureFlagContext context = FeatureFlagContext.forUser("user123");
    FeatureFlagValue value = service.getValue("theme", context);

    assertThat(value).isEqualTo(expectedValue);
  }

  @Test
  void shouldGetValueOrDefault() {
    FeatureFlagValue defaultValue = FeatureFlagValue.of(10);
    when(provider.getValue(
            eq("max-items"), any(FeatureFlagContext.class), eq(defaultValue)))
        .thenReturn(defaultValue);

    FeatureFlagValue value = service.getValueOrDefault("max-items", defaultValue);

    assertThat(value).isEqualTo(defaultValue);
  }
}
