package com.marcusprado02.commons.app.featureflags;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class FeatureFlagValueTest {

  @Test
  void shouldHandleBooleanValue() {
    FeatureFlagValue value = FeatureFlagValue.of(true);

    assertThat(value.asBoolean()).isTrue();
    assertThat(value.asString()).isEqualTo("true");
    assertThat(value.asInt()).isEqualTo(1);
    assertThat(value.asLong()).isEqualTo(1L);
    assertThat(value.asDouble()).isEqualTo(1.0);
    assertThat(value.asJson()).isEmpty();
  }

  @Test
  void shouldHandleStringValue() {
    FeatureFlagValue value = FeatureFlagValue.of("hello");

    assertThat(value.asString()).isEqualTo("hello");
  }

  @Test
  void shouldHandleIntValue() {
    FeatureFlagValue value = FeatureFlagValue.of(42);

    assertThat(value.asInt()).isEqualTo(42);
    assertThat(value.asLong()).isEqualTo(42L);
    assertThat(value.asDouble()).isEqualTo(42.0);
    assertThat(value.asString()).isEqualTo("42");
  }

  @Test
  void shouldHandleLongValue() {
    FeatureFlagValue value = FeatureFlagValue.of(123456789L);

    assertThat(value.asLong()).isEqualTo(123456789L);
    assertThat(value.asInt()).isEqualTo(123456789);
    assertThat(value.asString()).isEqualTo("123456789");
  }

  @Test
  void shouldHandleDoubleValue() {
    FeatureFlagValue value = FeatureFlagValue.of(3.14);

    assertThat(value.asDouble()).isEqualTo(3.14);
    assertThat(value.asString()).isEqualTo("3.14");
  }

  @Test
  void shouldHandleJsonValue() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode json = mapper.readTree("{\"key\":\"value\"}");

    FeatureFlagValue value = FeatureFlagValue.of(json);

    assertThat(value.asJson()).hasValue(json);
    assertThat(value.asString()).isEqualTo("value");
  }
}
