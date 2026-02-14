package com.marcusprado02.commons.app.configuration;

import java.util.Map;
import java.util.Set;

/**
 * Feature flags service for controlling feature rollout and A/B testing.
 *
 * <p>Feature flags (also known as feature toggles) allow enabling/disabling features at runtime
 * without code changes or deployments.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FeatureFlags flags = ...;
 *
 * // Simple boolean flag
 * if (flags.isEnabled("new-checkout-flow")) {
 *     return newCheckoutService.process(order);
 * } else {
 *     return legacyCheckoutService.process(order);
 * }
 *
 * // Context-aware flag (user-specific, percentage rollout, etc)
 * Map<String, Object> context = Map.of(
 *     "userId", "user-123",
 *     "region", "us-east",
 *     "subscriptionTier", "premium"
 * );
 *
 * if (flags.isEnabled("premium-analytics", context)) {
 *     return premiumAnalyticsService.generate(userId);
 * }
 * }</pre>
 *
 * @see ConfigurationProvider
 * @see DynamicConfiguration
 */
public interface FeatureFlags {

  /**
   * Checks if a feature flag is enabled.
   *
   * @param flagName the feature flag name
   * @return true if the flag is enabled
   */
  boolean isEnabled(String flagName);

  /**
   * Checks if a feature flag is enabled for a specific context.
   *
   * <p>Context can include: - User ID for user-specific targeting - Region for geo-targeting -
   * Subscription tier for tier-based features - Percentage for gradual rollout - Custom attributes
   * for advanced targeting
   *
   * @param flagName the feature flag name
   * @param context evaluation context
   * @return true if the flag is enabled for this context
   */
  boolean isEnabled(String flagName, Map<String, Object> context);

  /**
   * Gets the variant value for a feature flag.
   *
   * <p>Variants allow multiple variations of a feature (A/B/C testing).
   *
   * <p>Example:
   *
   * <pre>{@code
   * String buttonColor = flags.getVariant("button-color", "blue");
   * // Returns: "red", "green", "blue" (default) depending on configuration
   * }</pre>
   *
   * @param flagName the feature flag name
   * @param defaultValue default value if flag not found
   * @param <T> the variant type
   * @return the variant value
   */
  <T> T getVariant(String flagName, T defaultValue);

  /**
   * Gets the variant value for a feature flag with context.
   *
   * @param flagName the feature flag name
   * @param defaultValue default value if flag not found
   * @param context evaluation context
   * @param <T> the variant type
   * @return the variant value
   */
  <T> T getVariant(String flagName, T defaultValue, Map<String, Object> context);

  /**
   * Returns all defined feature flag names.
   *
   * @return set of all feature flag names
   */
  Set<String> getAllFlags();

  /**
   * Returns metadata about a feature flag.
   *
   * <p>Metadata may include: - Description - Owner/team - Creation date - Targeting rules - Rollout
   * percentage
   *
   * @param flagName the feature flag name
   * @return flag metadata
   */
  Map<String, Object> getFlagMetadata(String flagName);
}
