package com.marcusprado02.commons.app.featureflags;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Additional coverage for FeatureFlagValue record types — rawValue(), numeric conversions, etc. */
class FeatureFlagValueCoverageTest {

  // --- BooleanValue ---

  @Test
  void booleanValue_rawValueTrue() {
    FeatureFlagValue v = FeatureFlagValue.of(true);
    assertThat(v.rawValue()).isEqualTo(true);
  }

  @Test
  void booleanValue_rawValueFalse() {
    FeatureFlagValue v = FeatureFlagValue.of(false);
    assertThat(v.rawValue()).isEqualTo(false);
  }

  @Test
  void booleanValue_falseAsInt() {
    assertThat(FeatureFlagValue.of(false).asInt()).isEqualTo(0);
  }

  @Test
  void booleanValue_falseAsLong() {
    assertThat(FeatureFlagValue.of(false).asLong()).isEqualTo(0L);
  }

  @Test
  void booleanValue_falseAsDouble() {
    assertThat(FeatureFlagValue.of(false).asDouble()).isEqualTo(0.0);
  }

  @Test
  void booleanValue_falseAsString() {
    assertThat(FeatureFlagValue.of(false).asString()).isEqualTo("false");
  }

  @Test
  void booleanValue_falseAsBoolean() {
    assertThat(FeatureFlagValue.of(false).asBoolean()).isFalse();
  }

  @Test
  void booleanValue_trueAsInt() {
    assertThat(FeatureFlagValue.of(true).asInt()).isEqualTo(1);
  }

  @Test
  void booleanValue_trueAsLong() {
    assertThat(FeatureFlagValue.of(true).asLong()).isEqualTo(1L);
  }

  @Test
  void booleanValue_trueAsDouble() {
    assertThat(FeatureFlagValue.of(true).asDouble()).isEqualTo(1.0);
  }

  // --- StringValue ---

  @Test
  void stringValue_rawValue() {
    FeatureFlagValue v = FeatureFlagValue.of("hello");
    assertThat(v.rawValue()).isEqualTo("hello");
  }

  @Test
  void stringValue_asBoolean_true() {
    assertThat(FeatureFlagValue.of("true").asBoolean()).isTrue();
  }

  @Test
  void stringValue_asBoolean_false() {
    assertThat(FeatureFlagValue.of("false").asBoolean()).isFalse();
  }

  @Test
  void stringValue_asInt() {
    assertThat(FeatureFlagValue.of("7").asInt()).isEqualTo(7);
  }

  @Test
  void stringValue_asLong() {
    assertThat(FeatureFlagValue.of("999").asLong()).isEqualTo(999L);
  }

  @Test
  void stringValue_asDouble() {
    assertThat(FeatureFlagValue.of("1.5").asDouble()).isEqualTo(1.5);
  }

  @Test
  void stringValue_asJson_isEmpty() {
    assertThat(FeatureFlagValue.of("hello").asJson()).isEmpty();
  }

  // --- IntValue ---

  @Test
  void intValue_rawValue() {
    FeatureFlagValue v = FeatureFlagValue.of(10);
    assertThat(v.rawValue()).isEqualTo(10);
  }

  @Test
  void intValue_asBoolean_nonZero() {
    assertThat(FeatureFlagValue.of(1).asBoolean()).isTrue();
  }

  @Test
  void intValue_asBoolean_zero() {
    assertThat(FeatureFlagValue.of(0).asBoolean()).isFalse();
  }

  @Test
  void intValue_asLong() {
    assertThat(FeatureFlagValue.of(5).asLong()).isEqualTo(5L);
  }

  @Test
  void intValue_asDouble() {
    assertThat(FeatureFlagValue.of(5).asDouble()).isEqualTo(5.0);
  }

  @Test
  void intValue_asJson_isEmpty() {
    assertThat(FeatureFlagValue.of(42).asJson()).isEmpty();
  }

  // --- LongValue ---

  @Test
  void longValue_rawValue() {
    FeatureFlagValue v = FeatureFlagValue.of(100L);
    assertThat(v.rawValue()).isEqualTo(100L);
  }

  @Test
  void longValue_asBoolean_nonZero() {
    assertThat(FeatureFlagValue.of(1L).asBoolean()).isTrue();
  }

  @Test
  void longValue_asBoolean_zero() {
    assertThat(FeatureFlagValue.of(0L).asBoolean()).isFalse();
  }

  @Test
  void longValue_asInt() {
    assertThat(FeatureFlagValue.of(50L).asInt()).isEqualTo(50);
  }

  @Test
  void longValue_asDouble() {
    assertThat(FeatureFlagValue.of(50L).asDouble()).isEqualTo(50.0);
  }

  @Test
  void longValue_asString() {
    assertThat(FeatureFlagValue.of(123456789L).asString()).isEqualTo("123456789");
  }

  @Test
  void longValue_asJson_isEmpty() {
    assertThat(FeatureFlagValue.of(42L).asJson()).isEmpty();
  }

  // --- DoubleValue ---

  @Test
  void doubleValue_rawValue() {
    FeatureFlagValue v = FeatureFlagValue.of(2.5);
    assertThat(v.rawValue()).isEqualTo(2.5);
  }

  @Test
  void doubleValue_asBoolean_nonZero() {
    assertThat(FeatureFlagValue.of(1.0).asBoolean()).isTrue();
  }

  @Test
  void doubleValue_asBoolean_zero() {
    assertThat(FeatureFlagValue.of(0.0).asBoolean()).isFalse();
  }

  @Test
  void doubleValue_asInt() {
    assertThat(FeatureFlagValue.of(7.9).asInt()).isEqualTo(7);
  }

  @Test
  void doubleValue_asLong() {
    assertThat(FeatureFlagValue.of(100.6).asLong()).isEqualTo(100L);
  }

  @Test
  void doubleValue_asJson_isEmpty() {
    assertThat(FeatureFlagValue.of(3.14).asJson()).isEmpty();
  }

  // --- JsonValue ---

  @Test
  void jsonValue_rawValue() throws Exception {
    JsonNode node = new ObjectMapper().readTree("{\"a\":1}");
    FeatureFlagValue v = FeatureFlagValue.of(node);
    assertThat(v.rawValue()).isEqualTo(node);
  }

  @Test
  void jsonValue_asBoolean() throws Exception {
    JsonNode node = new ObjectMapper().readTree("true");
    assertThat(FeatureFlagValue.of(node).asBoolean()).isTrue();
  }

  @Test
  void jsonValue_asInt() throws Exception {
    JsonNode node = new ObjectMapper().readTree("42");
    assertThat(FeatureFlagValue.of(node).asInt()).isEqualTo(42);
  }

  @Test
  void jsonValue_asLong() throws Exception {
    JsonNode node = new ObjectMapper().readTree("999");
    assertThat(FeatureFlagValue.of(node).asLong()).isEqualTo(999L);
  }

  @Test
  void jsonValue_asDouble() throws Exception {
    JsonNode node = new ObjectMapper().readTree("1.5");
    assertThat(FeatureFlagValue.of(node).asDouble()).isEqualTo(1.5);
  }
}
