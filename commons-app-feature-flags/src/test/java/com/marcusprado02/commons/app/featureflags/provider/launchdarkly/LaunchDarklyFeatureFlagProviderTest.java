package com.marcusprado02.commons.app.featureflags.provider.launchdarkly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.LDClient;
import com.marcusprado02.commons.app.featureflags.FeatureFlagContext;
import com.marcusprado02.commons.app.featureflags.FeatureFlagValue;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LaunchDarklyFeatureFlagProviderTest {

  @Mock private LDClient ldClient;

  private LaunchDarklyFeatureFlagProvider provider;

  @BeforeEach
  void setUp() {
    provider = new LaunchDarklyFeatureFlagProvider(ldClient);
  }

  @Test
  void isEnabled_returnsTrueWhenClientReturnsTrue() {
    when(ldClient.boolVariation(eq("my-flag"), any(LDContext.class), eq(false))).thenReturn(true);

    boolean result = provider.isEnabled("my-flag", FeatureFlagContext.forUser("user1"));

    assertThat(result).isTrue();
  }

  @Test
  void isEnabled_returnsFalseWhenClientReturnsFalse() {
    when(ldClient.boolVariation(eq("my-flag"), any(LDContext.class), eq(false))).thenReturn(false);

    boolean result = provider.isEnabled("my-flag", FeatureFlagContext.forUser("user1"));

    assertThat(result).isFalse();
  }

  @Test
  void isEnabled_withAnonymousContext() {
    when(ldClient.boolVariation(eq("flag"), any(LDContext.class), eq(false))).thenReturn(false);

    boolean result = provider.isEnabled("flag", FeatureFlagContext.anonymous());

    assertThat(result).isFalse();
  }

  @Test
  void isEnabled_withSessionId() {
    FeatureFlagContext context =
        FeatureFlagContext.builder().userId("user1").sessionId("sess123").build();
    when(ldClient.boolVariation(eq("flag"), any(LDContext.class), eq(false))).thenReturn(true);

    boolean result = provider.isEnabled("flag", context);

    assertThat(result).isTrue();
  }

  @Test
  void isEnabled_withAttributes_string() {
    FeatureFlagContext context =
        FeatureFlagContext.builder()
            .userId("user1")
            .attribute("plan", "premium")
            .attribute("active", true)
            .attribute("score", 42)
            .build();
    when(ldClient.boolVariation(eq("flag"), any(LDContext.class), eq(false))).thenReturn(true);

    boolean result = provider.isEnabled("flag", context);

    assertThat(result).isTrue();
  }

  @Test
  void getValue_returnsBooleanValueTrue() {
    when(ldClient.jsonValueVariation(eq("flag"), any(LDContext.class), any(LDValue.class)))
        .thenReturn(LDValue.of(true));

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asBoolean()).isTrue();
  }

  @Test
  void getValue_returnsBooleanValueFalse() {
    when(ldClient.jsonValueVariation(eq("flag"), any(LDContext.class), any(LDValue.class)))
        .thenReturn(LDValue.of(false));

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asBoolean()).isFalse();
  }

  @Test
  void getValue_returnsIntValue() {
    when(ldClient.jsonValueVariation(eq("flag"), any(LDContext.class), any(LDValue.class)))
        .thenReturn(LDValue.of(42));

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asInt()).isEqualTo(42);
  }

  @Test
  void getValue_returnsDoubleValue() {
    when(ldClient.jsonValueVariation(eq("flag"), any(LDContext.class), any(LDValue.class)))
        .thenReturn(LDValue.of(3.14));

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asDouble()).isEqualTo(3.14);
  }

  @Test
  void getValue_returnsStringValue() {
    when(ldClient.jsonValueVariation(eq("flag"), any(LDContext.class), any(LDValue.class)))
        .thenReturn(LDValue.of("blue"));

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asString()).isEqualTo("blue");
  }

  @Test
  void getValue_returnsJsonObjectValue() {
    when(ldClient.jsonValueVariation(eq("flag"), any(LDContext.class), any(LDValue.class)))
        .thenReturn(LDValue.parse("{\"key\":\"val\"}"));

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asJson()).isPresent();
  }

  @Test
  void getValue_returnsJsonArrayValue() {
    when(ldClient.jsonValueVariation(eq("flag"), any(LDContext.class), any(LDValue.class)))
        .thenReturn(LDValue.parse("[1,2,3]"));

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asJson()).isPresent();
  }

  @Test
  void getValue_returnsDefaultFalseOnNullLDValue() {
    when(ldClient.jsonValueVariation(eq("flag"), any(LDContext.class), any(LDValue.class)))
        .thenReturn(LDValue.ofNull());

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asBoolean()).isFalse();
  }

  @Test
  void shutdown_closesClientSilently() throws Exception {
    provider.shutdown();
    verify(ldClient).close();
  }

  @Test
  void shutdown_ignoresExceptionFromClose() throws Exception {
    doThrow(new RuntimeException("close error")).when(ldClient).close();

    // Should not throw
    provider.shutdown();
  }

  @Test
  void isEnabled_withContextHavingNoAttributes() {
    FeatureFlagContext context = FeatureFlagContext.builder().userId("user1").build();
    when(ldClient.boolVariation(eq("flag"), any(LDContext.class), eq(false))).thenReturn(false);

    boolean result = provider.isEnabled("flag", context);

    assertThat(result).isFalse();
  }

  @Test
  void isEnabled_withCustomAttributeNumber() {
    FeatureFlagContext context =
        FeatureFlagContext.builder()
            .userId("user1")
            .attribute("score", 99.5)
            .attribute("count", 3)
            .build();
    when(ldClient.boolVariation(eq("flag"), any(LDContext.class), eq(false))).thenReturn(true);

    boolean result = provider.isEnabled("flag", context);

    assertThat(result).isTrue();
  }

  @Test
  void isEnabled_withObjectAttribute() {
    FeatureFlagContext context =
        FeatureFlagContext.builder()
            .userId("user1")
            .attribute("metadata", Map.of("k", "v"))
            .build();
    when(ldClient.boolVariation(eq("flag"), any(LDContext.class), eq(false))).thenReturn(false);

    boolean result = provider.isEnabled("flag", context);

    assertThat(result).isFalse();
  }
}
