package com.marcusprado02.commons.platform.featureflag;

public record FeatureFlag(String key) {
  public static FeatureFlag of(String key) {
    return new FeatureFlag(key);
  }
}
