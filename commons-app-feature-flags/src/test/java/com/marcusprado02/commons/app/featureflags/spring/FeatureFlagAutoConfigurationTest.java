package com.marcusprado02.commons.app.featureflags.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.marcusprado02.commons.app.featureflags.FeatureFlagProvider;
import com.marcusprado02.commons.app.featureflags.FeatureFlagService;
import org.junit.jupiter.api.Test;

class FeatureFlagAutoConfigurationTest {

  private final FeatureFlagAutoConfiguration config = new FeatureFlagAutoConfiguration();

  @Test
  void featureFlagService_createsBean() {
    FeatureFlagProvider provider = mock(FeatureFlagProvider.class);

    FeatureFlagService service = config.featureFlagService(provider);

    assertThat(service).isNotNull();
  }

  @Test
  void featureFlagAspect_createsBean() {
    FeatureFlagProvider provider = mock(FeatureFlagProvider.class);
    FeatureFlagService service = config.featureFlagService(provider);

    FeatureFlagAspect aspect = config.featureFlagAspect(service);

    assertThat(aspect).isNotNull();
  }
}
