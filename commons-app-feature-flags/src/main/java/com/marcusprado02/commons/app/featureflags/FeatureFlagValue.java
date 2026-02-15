package com.marcusprado02.commons.app.featureflags;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.Optional;

/**
 * Value of a feature flag.
 *
 * <p>Supports different types: boolean, string, number, JSON.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FeatureFlagValue value = provider.getValue("feature-key", context);
 *
 * // Boolean flags
 * boolean enabled = value.asBoolean();
 *
 * // String flags (e.g., theme, variant)
 * String variant = value.asString();
 *
 * // Number flags (e.g., max-items, timeout-ms)
 * int maxItems = value.asInt();
 *
 * // JSON flags (complex configuration)
 * JsonNode config = value.asJson();
 * }</pre>
 */
public sealed interface FeatureFlagValue {

  Object rawValue();

  boolean asBoolean();

  String asString();

  int asInt();

  long asLong();

  double asDouble();

  Optional<JsonNode> asJson();

  static FeatureFlagValue of(boolean value) {
    return new BooleanValue(value);
  }

  static FeatureFlagValue of(String value) {
    return new StringValue(value);
  }

  static FeatureFlagValue of(int value) {
    return new IntValue(value);
  }

  static FeatureFlagValue of(long value) {
    return new LongValue(value);
  }

  static FeatureFlagValue of(double value) {
    return new DoubleValue(value);
  }

  static FeatureFlagValue of(JsonNode value) {
    return new JsonValue(value);
  }

  record BooleanValue(boolean value) implements FeatureFlagValue {
    @Override
    public Object rawValue() {
      return value;
    }

    @Override
    public boolean asBoolean() {
      return value;
    }

    @Override
    public String asString() {
      return String.valueOf(value);
    }

    @Override
    public int asInt() {
      return value ? 1 : 0;
    }

    @Override
    public long asLong() {
      return value ? 1L : 0L;
    }

    @Override
    public double asDouble() {
      return value ? 1.0 : 0.0;
    }

    @Override
    public Optional<JsonNode> asJson() {
      return Optional.empty();
    }
  }

  record StringValue(String value) implements FeatureFlagValue {
    public StringValue {
      Objects.requireNonNull(value, "value");
    }

    @Override
    public Object rawValue() {
      return value;
    }

    @Override
    public boolean asBoolean() {
      return Boolean.parseBoolean(value);
    }

    @Override
    public String asString() {
      return value;
    }

    @Override
    public int asInt() {
      return Integer.parseInt(value);
    }

    @Override
    public long asLong() {
      return Long.parseLong(value);
    }

    @Override
    public double asDouble() {
      return Double.parseDouble(value);
    }

    @Override
    public Optional<JsonNode> asJson() {
      return Optional.empty();
    }
  }

  record IntValue(int value) implements FeatureFlagValue {
    @Override
    public Object rawValue() {
      return value;
    }

    @Override
    public boolean asBoolean() {
      return value != 0;
    }

    @Override
    public String asString() {
      return String.valueOf(value);
    }

    @Override
    public int asInt() {
      return value;
    }

    @Override
    public long asLong() {
      return value;
    }

    @Override
    public double asDouble() {
      return value;
    }

    @Override
    public Optional<JsonNode> asJson() {
      return Optional.empty();
    }
  }

  record LongValue(long value) implements FeatureFlagValue {
    @Override
    public Object rawValue() {
      return value;
    }

    @Override
    public boolean asBoolean() {
      return value != 0;
    }

    @Override
    public String asString() {
      return String.valueOf(value);
    }

    @Override
    public int asInt() {
      return (int) value;
    }

    @Override
    public long asLong() {
      return value;
    }

    @Override
    public double asDouble() {
      return value;
    }

    @Override
    public Optional<JsonNode> asJson() {
      return Optional.empty();
    }
  }

  record DoubleValue(double value) implements FeatureFlagValue {
    @Override
    public Object rawValue() {
      return value;
    }

    @Override
    public boolean asBoolean() {
      return value != 0.0;
    }

    @Override
    public String asString() {
      return String.valueOf(value);
    }

    @Override
    public int asInt() {
      return (int) value;
    }

    @Override
    public long asLong() {
      return (long) value;
    }

    @Override
    public double asDouble() {
      return value;
    }

    @Override
    public Optional<JsonNode> asJson() {
      return Optional.empty();
    }
  }

  record JsonValue(JsonNode value) implements FeatureFlagValue {
    public JsonValue {
      Objects.requireNonNull(value, "value");
    }

    @Override
    public Object rawValue() {
      return value;
    }

    @Override
    public boolean asBoolean() {
      return value.asBoolean();
    }

    @Override
    public String asString() {
      return value.asText();
    }

    @Override
    public int asInt() {
      return value.asInt();
    }

    @Override
    public long asLong() {
      return value.asLong();
    }

    @Override
    public double asDouble() {
      return value.asDouble();
    }

    @Override
    public Optional<JsonNode> asJson() {
      return Optional.of(value);
    }
  }
}
