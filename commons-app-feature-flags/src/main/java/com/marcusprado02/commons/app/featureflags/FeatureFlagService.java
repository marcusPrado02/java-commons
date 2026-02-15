package com.marcusprado02.commons.app.featureflags;

/**
 * High-level feature flag service.
 *
 * <p>Provides a simplified API for feature flag evaluation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Service
 * public class OrderService {
 *
 *     private final FeatureFlagService featureFlags;
 *
 *     public void processOrder(Order order) {
 *         FeatureFlagContext context = FeatureFlagContext.forUser(order.getUserId());
 *
 *         if (featureFlags.isEnabled("new-payment-flow", context)) {
 *             processWithNewFlow(order);
 *         } else {
 *             processWithOldFlow(order);
 *         }
 *     }
 * }
 * }</pre>
 */
public class FeatureFlagService {

  private final FeatureFlagProvider provider;

  public FeatureFlagService(FeatureFlagProvider provider) {
    this.provider = provider;
  }

  public boolean isEnabled(String featureKey) {
    return provider.isEnabled(featureKey, FeatureFlagContext.anonymous());
  }

  public boolean isEnabled(String featureKey, FeatureFlagContext context) {
    return provider.isEnabled(featureKey, context);
  }

  public boolean isEnabled(String featureKey, String userId) {
    return provider.isEnabled(featureKey, FeatureFlagContext.forUser(userId));
  }

  public FeatureFlagValue getValue(String featureKey) {
    return provider.getValue(featureKey, FeatureFlagContext.anonymous());
  }

  public FeatureFlagValue getValue(String featureKey, FeatureFlagContext context) {
    return provider.getValue(featureKey, context);
  }

  public FeatureFlagValue getValue(String featureKey, String userId) {
    return provider.getValue(featureKey, FeatureFlagContext.forUser(userId));
  }

  public FeatureFlagValue getValueOrDefault(
      String featureKey, FeatureFlagValue defaultValue) {
    return provider.getValue(featureKey, FeatureFlagContext.anonymous(), defaultValue);
  }

  public FeatureFlagValue getValueOrDefault(
      String featureKey, FeatureFlagContext context, FeatureFlagValue defaultValue) {
    return provider.getValue(featureKey, context, defaultValue);
  }
}
