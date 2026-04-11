package com.marcusprado02.commons.platform.featureflag;

/** Checks whether a given {@link FeatureFlag} is currently enabled. */
public interface FeatureFlagProvider {

  boolean isEnabled(FeatureFlag flag);
}
