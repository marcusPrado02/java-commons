package com.marcusprado02.commons.app.featureflags.provider.inmemory;

import com.marcusprado02.commons.app.featureflags.FeatureFlagContext;
import com.marcusprado02.commons.app.featureflags.FeatureFlagProvider;
import com.marcusprado02.commons.app.featureflags.FeatureFlagValue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * In-memory feature flag provider for testing and development.
 *
 * <p>Supports:
 *
 * <ul>
 *   <li>Simple boolean flags
 *   <li>Multi-variant flags (string, number, JSON)
 *   <li>User targeting with predicates
 *   <li>Percentage rollouts
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * InMemoryFeatureFlagProvider provider = InMemoryFeatureFlagProvider.builder()
 *     .flag("new-ui", true)
 *     .flag("max-items", 100)
 *     .flag("theme", "dark")
 *     .flagWithTargeting("beta-feature",
 *         context -> context.getAttribute("plan").equals("premium"),
 *         true,
 *         false)
 *     .build();
 * }</pre>
 */
public class InMemoryFeatureFlagProvider implements FeatureFlagProvider {

  private final Map<String, FlagConfig> flags;

  private InMemoryFeatureFlagProvider(Map<String, FlagConfig> flags) {
    this.flags = new ConcurrentHashMap<>(flags);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean isEnabled(String featureKey, FeatureFlagContext context) {
    FlagConfig config = flags.get(featureKey);
    if (config == null) {
      return false;
    }
    return config.evaluate(context).asBoolean();
  }

  @Override
  public FeatureFlagValue getValue(String featureKey, FeatureFlagContext context) {
    FlagConfig config = flags.get(featureKey);
    if (config == null) {
      throw new IllegalArgumentException("Feature flag not found: " + featureKey);
    }
    return config.evaluate(context);
  }

  /**
   * Updates a feature flag value.
   *
   * @param featureKey feature key
   * @param value new value
   */
  public void updateFlag(String featureKey, FeatureFlagValue value) {
    flags.put(featureKey, new SimpleFlagConfig(value));
  }

  /**
   * Removes a feature flag.
   *
   * @param featureKey feature key
   */
  public void removeFlag(String featureKey) {
    flags.remove(featureKey);
  }

  /**
   * Clears all feature flags.
   */
  public void clear() {
    flags.clear();
  }

  private sealed interface FlagConfig {
    FeatureFlagValue evaluate(FeatureFlagContext context);
  }

  private record SimpleFlagConfig(FeatureFlagValue value) implements FlagConfig {
    @Override
    public FeatureFlagValue evaluate(FeatureFlagContext context) {
      return value;
    }
  }

  private record TargetedFlagConfig(
      Predicate<FeatureFlagContext> predicate,
      FeatureFlagValue enabledValue,
      FeatureFlagValue disabledValue)
      implements FlagConfig {
    @Override
    public FeatureFlagValue evaluate(FeatureFlagContext context) {
      return predicate.test(context) ? enabledValue : disabledValue;
    }
  }

  private record PercentageRolloutConfig(
      int percentage, FeatureFlagValue enabledValue, FeatureFlagValue disabledValue)
      implements FlagConfig {
    @Override
    public FeatureFlagValue evaluate(FeatureFlagContext context) {
      // Simple hash-based percentage rollout
      String key = context.userId() != null ? context.userId() : context.sessionId();
      if (key == null) {
        return disabledValue;
      }
      int hash = Math.abs(key.hashCode());
      int bucket = hash % 100;
      return bucket < percentage ? enabledValue : disabledValue;
    }
  }

  public static class Builder {
    private final Map<String, FlagConfig> flags = new ConcurrentHashMap<>();

    public Builder flag(String featureKey, boolean value) {
      flags.put(featureKey, new SimpleFlagConfig(FeatureFlagValue.of(value)));
      return this;
    }

    public Builder flag(String featureKey, String value) {
      flags.put(featureKey, new SimpleFlagConfig(FeatureFlagValue.of(value)));
      return this;
    }

    public Builder flag(String featureKey, int value) {
      flags.put(featureKey, new SimpleFlagConfig(FeatureFlagValue.of(value)));
      return this;
    }

    public Builder flag(String featureKey, long value) {
      flags.put(featureKey, new SimpleFlagConfig(FeatureFlagValue.of(value)));
      return this;
    }

    public Builder flag(String featureKey, double value) {
      flags.put(featureKey, new SimpleFlagConfig(FeatureFlagValue.of(value)));
      return this;
    }

    public Builder flag(String featureKey, FeatureFlagValue value) {
      flags.put(featureKey, new SimpleFlagConfig(value));
      return this;
    }

    public Builder flagWithTargeting(
        String featureKey,
        Predicate<FeatureFlagContext> predicate,
        FeatureFlagValue enabledValue,
        FeatureFlagValue disabledValue) {
      flags.put(featureKey, new TargetedFlagConfig(predicate, enabledValue, disabledValue));
      return this;
    }

    public Builder flagWithPercentageRollout(
        String featureKey,
        int percentage,
        FeatureFlagValue enabledValue,
        FeatureFlagValue disabledValue) {
      if (percentage < 0 || percentage > 100) {
        throw new IllegalArgumentException("Percentage must be between 0 and 100");
      }
      flags.put(featureKey, new PercentageRolloutConfig(percentage, enabledValue, disabledValue));
      return this;
    }

    public InMemoryFeatureFlagProvider build() {
      return new InMemoryFeatureFlagProvider(flags);
    }
  }
}
