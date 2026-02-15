package com.marcusprado02.commons.app.featureflags.spring;

/**
 * Exception thrown when a feature flag is disabled.
 *
 * <p>This is thrown by the {@link FeatureFlagAspect} when a method annotated with {@link
 * FeatureFlag} is called but the feature is disabled and the fallback strategy is {@link
 * FeatureFlag.FallbackStrategy#THROW_EXCEPTION}.
 */
public class FeatureFlagDisabledException extends RuntimeException {

  private final String featureKey;

  public FeatureFlagDisabledException(String featureKey) {
    super("Feature flag is disabled: " + featureKey);
    this.featureKey = featureKey;
  }

  public FeatureFlagDisabledException(String featureKey, String message) {
    super(message);
    this.featureKey = featureKey;
  }

  public String getFeatureKey() {
    return featureKey;
  }
}
