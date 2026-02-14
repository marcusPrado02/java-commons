package com.marcusprado02.commons.app.configuration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of FeatureFlags for testing and development.
 *
 * <p>Supports: - Boolean flags - Variant flags (A/B/C testing) - Percentage-based rollout -
 * User-specific targeting - Metadata
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create and configure
 * InMemoryFeatureFlags flags = new InMemoryFeatureFlags();
 *
 * // Simple boolean flag
 * flags.enable("new-checkout");
 *
 * // Percentage rollout (50%)
 * flags.setRolloutPercentage("beta-feature", 50);
 *
 * // Variant flag
 * flags.setVariants("button-color", Map.of(
 *     "control", 40,  // 40% get "control"
 *     "red", 30,      // 30% get "red"
 *     "blue", 30      // 30% get "blue"
 * ));
 *
 * // Check flag
 * if (flags.isEnabled("new-checkout")) {
 *     // ...
 * }
 *
 * // Get variant
 * String color = flags.getVariant("button-color", "control",
 *     Map.of("userId", "user-123"));
 * }</pre>
 *
 * @see FeatureFlags
 */
public class InMemoryFeatureFlags implements FeatureFlags {

  private final Map<String, FlagConfiguration> flags;

  public InMemoryFeatureFlags() {
    this.flags = new ConcurrentHashMap<>();
  }

  /**
   * Enables a feature flag.
   *
   * @param flagName the flag name
   */
  public void enable(String flagName) {
    flags.put(flagName, new FlagConfiguration(true, 100, null, Map.of()));
  }

  /**
   * Disables a feature flag.
   *
   * @param flagName the flag name
   */
  public void disable(String flagName) {
    flags.put(flagName, new FlagConfiguration(false, 0, null, Map.of()));
  }

  /**
   * Sets the rollout percentage for a feature flag.
   *
   * <p>The percentage (0-100) determines what portion of users will see the flag as enabled.
   *
   * @param flagName the flag name
   * @param percentage the rollout percentage (0-100)
   */
  public void setRolloutPercentage(String flagName, int percentage) {
    if (percentage < 0 || percentage > 100) {
      throw new IllegalArgumentException("Percentage must be between 0 and 100");
    }
    flags.put(flagName, new FlagConfiguration(true, percentage, null, Map.of()));
  }

  /**
   * Sets variant distribution for a feature flag.
   *
   * <p>Variants allow A/B/C testing with multiple variations.
   *
   * @param flagName the flag name
   * @param variants map of variant name to percentage
   */
  public void setVariants(String flagName, Map<String, Integer> variants) {
    int totalPercentage = variants.values().stream().mapToInt(Integer::intValue).sum();
    if (totalPercentage != 100) {
      throw new IllegalArgumentException("Variant percentages must sum to 100");
    }
    flags.put(flagName, new FlagConfiguration(true, 100, variants, Map.of()));
  }

  /**
   * Sets metadata for a feature flag.
   *
   * @param flagName the flag name
   * @param metadata the metadata
   */
  public void setMetadata(String flagName, Map<String, Object> metadata) {
    FlagConfiguration existing =
        flags.getOrDefault(flagName, new FlagConfiguration(false, 0, null, Map.of()));
    flags.put(
        flagName,
        new FlagConfiguration(existing.enabled, existing.percentage, existing.variants, metadata));
  }

  /**
   * Removes a feature flag.
   *
   * @param flagName the flag name
   */
  public void remove(String flagName) {
    flags.remove(flagName);
  }

  /** Removes all feature flags. */
  public void clear() {
    flags.clear();
  }

  @Override
  public boolean isEnabled(String flagName) {
    return isEnabled(flagName, Map.of());
  }

  @Override
  public boolean isEnabled(String flagName, Map<String, Object> context) {
    FlagConfiguration config = flags.get(flagName);
    if (config == null || !config.enabled) {
      return false;
    }

    // If 100% rollout, always enabled
    if (config.percentage == 100) {
      return true;
    }

    // Use user ID for consistent percentage rollout
    Object userId = context.get("userId");
    if (userId != null) {
      int hash = Math.abs(userId.hashCode());
      int bucket = hash % 100;
      return bucket < config.percentage;
    }

    // Without user ID, use random (not recommended for production)
    Random random = new Random();
    return random.nextInt(100) < config.percentage;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getVariant(String flagName, T defaultValue) {
    return getVariant(flagName, defaultValue, Map.of());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getVariant(String flagName, T defaultValue, Map<String, Object> context) {
    FlagConfiguration config = flags.get(flagName);
    if (config == null || !config.enabled || config.variants == null) {
      return defaultValue;
    }

    // Determine variant based on user ID for consistency
    Object userId = context.get("userId");
    int hash = userId != null ? Math.abs(userId.hashCode()) : new Random().nextInt(100);
    int bucket = hash % 100;

    // Find which variant bucket falls into
    int cumulativePercentage = 0;
    for (Map.Entry<String, Integer> entry : config.variants.entrySet()) {
      cumulativePercentage += entry.getValue();
      if (bucket < cumulativePercentage) {
        return (T) entry.getKey();
      }
    }

    return defaultValue;
  }

  @Override
  public Set<String> getAllFlags() {
    return Collections.unmodifiableSet(flags.keySet());
  }

  @Override
  public Map<String, Object> getFlagMetadata(String flagName) {
    FlagConfiguration config = flags.get(flagName);
    if (config == null) {
      return Map.of();
    }

    Map<String, Object> metadata = new HashMap<>(config.metadata);
    metadata.put("enabled", config.enabled);
    metadata.put("percentage", config.percentage);
    if (config.variants != null) {
      metadata.put("variants", config.variants);
    }
    return Collections.unmodifiableMap(metadata);
  }

  private record FlagConfiguration(
      boolean enabled,
      int percentage,
      Map<String, Integer> variants,
      Map<String, Object> metadata) {}
}
