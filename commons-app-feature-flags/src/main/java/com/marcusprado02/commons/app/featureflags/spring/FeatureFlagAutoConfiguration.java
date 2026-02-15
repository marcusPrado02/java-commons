package com.marcusprado02.commons.app.featureflags.spring;

import com.marcusprado02.commons.app.featureflags.FeatureFlagProvider;
import com.marcusprado02.commons.app.featureflags.FeatureFlagService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for feature flags with Spring.
 *
 * <p>Registers:
 *
 * <ul>
 *   <li>{@link FeatureFlagService} bean
 *   <li>{@link FeatureFlagAspect} for @FeatureFlag annotation support
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Configuration
 * @Import(FeatureFlagAutoConfiguration.class)
 * public class AppConfig {
 *
 *     @Bean
 *     public FeatureFlagProvider featureFlagProvider() {
 *         return new LaunchDarklyFeatureFlagProvider("sdk-key");
 *     }
 * }
 * }</pre>
 */
@Configuration
@EnableAspectJAutoProxy
public class FeatureFlagAutoConfiguration {

  @Bean
  public FeatureFlagService featureFlagService(FeatureFlagProvider provider) {
    return new FeatureFlagService(provider);
  }

  @Bean
  public FeatureFlagAspect featureFlagAspect(FeatureFlagService featureFlagService) {
    return new FeatureFlagAspect(featureFlagService);
  }
}
