package com.marcusprado02.commons.platform.featureflag;

public interface FeatureFlagProvider {

  boolean isEnabled(FeatureFlag flag);
}
