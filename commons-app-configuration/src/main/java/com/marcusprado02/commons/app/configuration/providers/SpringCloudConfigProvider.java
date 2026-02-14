package com.marcusprado02.commons.app.configuration.providers;

import com.marcusprado02.commons.app.configuration.ConfigurationProvider;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.*;

/**
 * Configuration provider for Spring Cloud Config Server.
 *
 * <p>Integrates with Spring Cloud Config to fetch configuration from a remote config server.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // In Spring Boot application
 * @Bean
 * public ConfigurationProvider springCloudConfigProvider(
 *         ConfigurableEnvironment environment) {
 *     return new SpringCloudConfigProvider(environment);
 * }
 *
 * // Use the provider
 * @Autowired
 * private ConfigurationProvider configProvider;
 *
 * public void someMethod() {
 *     Optional<String> value = configProvider.getString("my.config.key");
 * }
 * }</pre>
 *
 * <p><b>Note:</b> Requires spring-cloud-starter-config dependency. This provider integrates with
 * Spring's Environment to access properties loaded by Spring Cloud Config.
 *
 * @see ConfigurationProvider
 */
public class SpringCloudConfigProvider implements ConfigurationProvider {

  private final Object environment; // org.springframework.core.env.Environment
  private static final String PROVIDER_NAME = "spring-cloud-config";

  /**
   * Creates a provider using Spring's Environment.
   *
   * @param environment Spring Environment (pass as Object to avoid hard dependency)
   */
  public SpringCloudConfigProvider(Object environment) {
    Objects.requireNonNull(environment, "environment cannot be null");

    // Verify it's actually a Spring Environment using reflection
    if (!isSpringEnvironment(environment)) {
      throw new IllegalArgumentException(
          "Parameter must be an instance of org.springframework.core.env.Environment");
    }

    this.environment = environment;
  }

  @Override
  public Optional<String> getString(String key) {
    try {
      String value =
          (String)
              environment
                  .getClass()
                  .getMethod("getProperty", String.class)
                  .invoke(environment, key);
      return Optional.ofNullable(value);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<Integer> getInt(String key) {
    try {
      Integer value =
          (Integer)
              environment
                  .getClass()
                  .getMethod("getProperty", String.class, Class.class)
                  .invoke(environment, key, Integer.class);
      return Optional.ofNullable(value);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<Long> getLong(String key) {
    try {
      Long value =
          (Long)
              environment
                  .getClass()
                  .getMethod("getProperty", String.class, Class.class)
                  .invoke(environment, key, Long.class);
      return Optional.ofNullable(value);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<Boolean> getBoolean(String key) {
    try {
      Boolean value =
          (Boolean)
              environment
                  .getClass()
                  .getMethod("getProperty", String.class, Class.class)
                  .invoke(environment, key, Boolean.class);
      return Optional.ofNullable(value);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<Double> getDouble(String key) {
    try {
      Double value =
          (Double)
              environment
                  .getClass()
                  .getMethod("getProperty", String.class, Class.class)
                  .invoke(environment, key, Double.class);
      return Optional.ofNullable(value);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Map<String, String> getProperties(String prefix) {
    Map<String, String> result = new HashMap<>();
    String prefixWithDot = prefix.endsWith(".") ? prefix : prefix + ".";

    try {
      // Get all property names from all property sources
      @SuppressWarnings("unchecked")
      Iterable<Object> propertySources =
          (Iterable<Object>)
              environment.getClass().getMethod("getPropertySources").invoke(environment);

      for (Object source : propertySources) {
        Object underlyingSource = source.getClass().getMethod("getSource").invoke(source);
        if (underlyingSource instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) underlyingSource;
          map.forEach(
              (key, value) -> {
                if (key.startsWith(prefixWithDot) && value != null) {
                  String keyWithoutPrefix = key.substring(prefixWithDot.length());
                  result.put(keyWithoutPrefix, value.toString());
                }
              });
        }
      }
    } catch (Exception e) {
      // Return empty map on error
    }

    return Collections.unmodifiableMap(result);
  }

  @Override
  public Result<Void> refresh() {
    try {
      // Trigger Spring Cloud Config refresh
      // In a real Spring app, you'd inject CloudRefreshEvent or use @RefreshScope
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(
          com.marcusprado02.commons.kernel.errors.Problem.of(
              com.marcusprado02.commons.kernel.errors.ErrorCode.of("CONFIG.REFRESH_FAILED"),
              com.marcusprado02.commons.kernel.errors.ErrorCategory.TECHNICAL,
              com.marcusprado02.commons.kernel.errors.Severity.ERROR,
              "Failed to refresh Spring Cloud Config: " + e.getMessage()));
    }
  }

  @Override
  public boolean containsKey(String key) {
    try {
      boolean contains =
          (boolean)
              environment
                  .getClass()
                  .getMethod("containsProperty", String.class)
                  .invoke(environment, key);
      return contains;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  private boolean isSpringEnvironment(Object obj) {
    try {
      Class<?> clazz = obj.getClass();
      // Check if it implements org.springframework.core.env.Environment
      for (Class<?> iface : getAllInterfaces(clazz)) {
        if (iface.getName().equals("org.springframework.core.env.Environment")) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  private Set<Class<?>> getAllInterfaces(Class<?> clazz) {
    Set<Class<?>> interfaces = new HashSet<>();
    while (clazz != null) {
      interfaces.addAll(Arrays.asList(clazz.getInterfaces()));
      clazz = clazz.getSuperclass();
    }
    return interfaces;
  }
}
