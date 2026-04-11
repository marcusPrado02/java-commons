package com.marcusprado02.commons.platform.featureflag;

/** Identifies a feature flag by its unique string key. */
public record FeatureFlag(String key) {
  public static FeatureFlag of(String key) {
    return new FeatureFlag(key);
  }
}
