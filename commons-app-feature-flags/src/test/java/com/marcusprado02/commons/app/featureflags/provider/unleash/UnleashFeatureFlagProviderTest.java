package com.marcusprado02.commons.app.featureflags.provider.unleash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.app.featureflags.FeatureFlagContext;
import com.marcusprado02.commons.app.featureflags.FeatureFlagValue;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.Variant;
import io.getunleash.variant.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnleashFeatureFlagProviderTest {

  @Mock private Unleash unleash;

  private UnleashFeatureFlagProvider provider;

  @BeforeEach
  void setUp() {
    provider = new UnleashFeatureFlagProvider(unleash);
  }

  // --- isEnabled tests ---

  @Test
  void isEnabled_returnsTrueWhenUnleashReturnsTrue() {
    when(unleash.isEnabled(eq("my-flag"), any(UnleashContext.class))).thenReturn(true);

    boolean result = provider.isEnabled("my-flag", FeatureFlagContext.forUser("user1"));

    assertThat(result).isTrue();
  }

  @Test
  void isEnabled_returnsFalseWhenUnleashReturnsFalse() {
    when(unleash.isEnabled(eq("my-flag"), any(UnleashContext.class))).thenReturn(false);

    boolean result = provider.isEnabled("my-flag", FeatureFlagContext.forUser("user1"));

    assertThat(result).isFalse();
  }

  @Test
  void isEnabled_withAnonymousContext() {
    when(unleash.isEnabled(eq("flag"), any(UnleashContext.class))).thenReturn(false);

    boolean result = provider.isEnabled("flag", FeatureFlagContext.anonymous());

    assertThat(result).isFalse();
  }

  @Test
  void isEnabled_withSessionIdAndAttributes() {
    FeatureFlagContext context =
        FeatureFlagContext.builder()
            .userId("user1")
            .sessionId("sess123")
            .attribute("plan", "premium")
            .build();
    when(unleash.isEnabled(eq("flag"), any(UnleashContext.class))).thenReturn(true);

    boolean result = provider.isEnabled("flag", context);

    assertThat(result).isTrue();
  }

  // --- getValue: null/disabled variant ---

  @Test
  void getValue_returnsDisabledFlagWhenVariantIsNull() {
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(null);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asBoolean()).isFalse();
  }

  @Test
  void getValue_returnsDisabledFlagWhenVariantIsDisabled() {
    Variant disabled = new Variant("disabled", (Payload) null, false);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(disabled);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asBoolean()).isFalse();
  }

  // --- getValue: enabled variant with empty payload ---

  @Test
  void getValue_returnsEnabledTrueWhenVariantEnabledAndNoPayload() {
    Variant variant = new Variant("enabled", (Payload) null, true);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(variant);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asBoolean()).isTrue();
  }

  // --- getValue: payload parsing ---

  @Test
  void getValue_parsesPayloadAsTrue() {
    Payload payload = new Payload("string", "true");
    Variant variant = new Variant("v", payload, true);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(variant);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asBoolean()).isTrue();
  }

  @Test
  void getValue_parsesPayloadAsFalse() {
    Payload payload = new Payload("string", "false");
    Variant variant = new Variant("v", payload, true);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(variant);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asBoolean()).isFalse();
  }

  @Test
  void getValue_parsesPayloadAsTrueCaseInsensitive() {
    Payload payload = new Payload("string", "TRUE");
    Variant variant = new Variant("v", payload, true);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(variant);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("u"));

    assertThat(value.asBoolean()).isTrue();
  }

  @Test
  void getValue_parsesPayloadAsLongNumber() {
    Payload payload = new Payload("number", "12345");
    Variant variant = new Variant("v", payload, true);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(variant);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asLong()).isEqualTo(12345L);
  }

  @Test
  void getValue_parsesPayloadAsNegativeLong() {
    Payload payload = new Payload("number", "-99");
    Variant variant = new Variant("v", payload, true);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(variant);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asLong()).isEqualTo(-99L);
  }

  @Test
  void getValue_parsesPayloadAsDouble() {
    Payload payload = new Payload("number", "2.71");
    Variant variant = new Variant("v", payload, true);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(variant);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asDouble()).isEqualTo(2.71);
  }

  @Test
  void getValue_parsesPayloadAsNegativeDouble() {
    Payload payload = new Payload("number", "-1.5");
    Variant variant = new Variant("v", payload, true);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(variant);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asDouble()).isEqualTo(-1.5);
  }

  @Test
  void getValue_parsesPayloadAsJsonObject() {
    Payload payload = new Payload("json", "{\"enabled\":true}");
    Variant variant = new Variant("v", payload, true);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(variant);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asJson()).isPresent();
  }

  @Test
  void getValue_parsesPayloadAsJsonArray() {
    Payload payload = new Payload("json", "[1,2,3]");
    Variant variant = new Variant("v", payload, true);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(variant);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asJson()).isPresent();
  }

  @Test
  void getValue_parsesPayloadAsString() {
    Payload payload = new Payload("string", "blue");
    Variant variant = new Variant("v", payload, true);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(variant);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.forUser("user1"));

    assertThat(value.asString()).isEqualTo("blue");
  }

  @Test
  void getValue_withContextHavingNoUserId() {
    Variant disabled = new Variant("disabled", (Payload) null, false);
    when(unleash.getVariant(eq("flag"), any(UnleashContext.class))).thenReturn(disabled);

    FeatureFlagValue value = provider.getValue("flag", FeatureFlagContext.anonymous());

    assertThat(value.asBoolean()).isFalse();
  }
}
