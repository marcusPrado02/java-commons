package com.marcusprado02.commons.adapters.web.spring.cors;

import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Helper class to configure CORS (Cross-Origin Resource Sharing) using {@link CorsProperties}.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * @Configuration
 * public class WebConfig {
 *
 *   @Bean
 *   public WebMvcConfigurer corsConfigurer(CorsProperties corsProperties) {
 *     return CorsConfigurationHelper.fromProperties(corsProperties);
 *   }
 * }
 * }</pre>
 *
 * <p>Or manually:
 *
 * <pre>{@code
 * @Configuration
 * public class WebConfig implements WebMvcConfigurer {
 *
 *   @Override
 *   public void addCorsMappings(CorsRegistry registry) {
 *     CorsConfigurationHelper.builder()
 *         .allowedOrigins("http://localhost:3000", "https://app.example.com")
 *         .allowedMethods("GET", "POST", "PUT", "DELETE")
 *         .allowCredentials(true)
 *         .maxAge(3600)
 *         .applyTo(registry, "/**");
 *   }
 * }
 * }</pre>
 */
public final class CorsConfigurationHelper {

  private CorsConfigurationHelper() {}

  /**
   * Creates a {@link WebMvcConfigurer} from {@link CorsProperties}.
   *
   * @param properties CORS properties
   * @return configured WebMvcConfigurer
   */
  public static WebMvcConfigurer fromProperties(CorsProperties properties) {
    if (!properties.isEnabled()) {
      return new WebMvcConfigurer() {};
    }

    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        for (String pattern : properties.getPathPatterns()) {
          CorsRegistration registration = registry.addMapping(pattern);

          if (!properties.getAllowedOrigins().isEmpty()) {
            registration.allowedOrigins(properties.getAllowedOrigins().toArray(new String[0]));
          }

          if (!properties.getAllowedOriginPatterns().isEmpty()) {
            registration.allowedOriginPatterns(
                properties.getAllowedOriginPatterns().toArray(new String[0]));
          }

          if (!properties.getAllowedMethods().isEmpty()) {
            registration.allowedMethods(properties.getAllowedMethods().toArray(new String[0]));
          }

          if (!properties.getAllowedHeaders().isEmpty()) {
            registration.allowedHeaders(properties.getAllowedHeaders().toArray(new String[0]));
          }

          if (!properties.getExposedHeaders().isEmpty()) {
            registration.exposedHeaders(properties.getExposedHeaders().toArray(new String[0]));
          }

          registration.allowCredentials(properties.isAllowCredentials());

          if (properties.getMaxAge() != null) {
            registration.maxAge(properties.getMaxAge().getSeconds());
          }
        }
      }
    };
  }

  /**
   * Creates a fluent builder for manual CORS configuration.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String[] allowedOrigins;
    private String[] allowedOriginPatterns;
    private String[] allowedMethods;
    private String[] allowedHeaders;
    private String[] exposedHeaders;
    private Boolean allowCredentials;
    private Long maxAge;

    private Builder() {}

    public Builder allowedOrigins(String... origins) {
      this.allowedOrigins = origins;
      return this;
    }

    public Builder allowedOriginPatterns(String... patterns) {
      this.allowedOriginPatterns = patterns;
      return this;
    }

    public Builder allowedMethods(String... methods) {
      this.allowedMethods = methods;
      return this;
    }

    public Builder allowedHeaders(String... headers) {
      this.allowedHeaders = headers;
      return this;
    }

    public Builder exposedHeaders(String... headers) {
      this.exposedHeaders = headers;
      return this;
    }

    public Builder allowCredentials(boolean allow) {
      this.allowCredentials = allow;
      return this;
    }

    public Builder maxAge(long seconds) {
      this.maxAge = seconds;
      return this;
    }

    /**
     * Applies the configuration to the given registry and path pattern.
     *
     * @param registry CORS registry
     * @param pathPattern path pattern (e.g., "/**")
     */
    public void applyTo(CorsRegistry registry, String pathPattern) {
      CorsRegistration registration = registry.addMapping(pathPattern);

      if (allowedOrigins != null) {
        registration.allowedOrigins(allowedOrigins);
      }

      if (allowedOriginPatterns != null) {
        registration.allowedOriginPatterns(allowedOriginPatterns);
      }

      if (allowedMethods != null) {
        registration.allowedMethods(allowedMethods);
      }

      if (allowedHeaders != null) {
        registration.allowedHeaders(allowedHeaders);
      }

      if (exposedHeaders != null) {
        registration.exposedHeaders(exposedHeaders);
      }

      if (allowCredentials != null) {
        registration.allowCredentials(allowCredentials);
      }

      if (maxAge != null) {
        registration.maxAge(maxAge);
      }
    }
  }
}
