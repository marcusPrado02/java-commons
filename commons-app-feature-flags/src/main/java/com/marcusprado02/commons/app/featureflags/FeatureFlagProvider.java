package com.marcusprado02.commons.app.featureflags;

/**
 * Feature flag provider abstraction.
 *
 * <p>Implementations provide feature flag evaluation for different providers (LaunchDarkly,
 * Unleash, in-memory, etc.).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FeatureFlagProvider provider = new LaunchDarklyProvider(sdkKey);
 * FeatureFlagContext context = FeatureFlagContext.forUser("user123");
 *
 * // Check if feature is enabled
 * boolean enabled = provider.isEnabled("new-checkout", context);
 *
 * // Get feature value
 * FeatureFlagValue value = provider.getValue("max-items", context);
 * int maxItems = value.asInt();
 * }</pre>
 */
public interface FeatureFlagProvider {

  /**
   * Checks if a feature flag is enabled.
   *
   * @param featureKey feature key
   * @param context evaluation context
   * @return true if enabled, false otherwise
   */
  boolean isEnabled(String featureKey, FeatureFlagContext context);

  /**
   * Gets the value of a feature flag.
   *
   * @param featureKey feature key
   * @param context evaluation context
   * @return feature flag value
   */
  FeatureFlagValue getValue(String featureKey, FeatureFlagContext context);

  /**
   * Gets the value of a feature flag with a default fallback.
   *
   * @param featureKey feature key
   * @param context evaluation context
   * @param defaultValue default value if flag not found
   * @return feature flag value or default
   */
  default FeatureFlagValue getValue(
      String featureKey, FeatureFlagContext context, FeatureFlagValue defaultValue) {
    try {
      return getValue(featureKey, context);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Shuts down the provider and releases resources.
   */
  default void shutdown() {
    // Default: no-op
  }
}
