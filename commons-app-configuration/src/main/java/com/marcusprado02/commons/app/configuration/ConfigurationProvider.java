package com.marcusprado02.commons.app.configuration;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Map;
import java.util.Optional;

/**
 * Generic configuration provider interface.
 *
 * <p>Implementations provide access to configuration values from different sources such as: -
 * Spring Cloud Config Server - Azure App Configuration - AWS AppConfig - Environment variables -
 * Property files
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ConfigurationProvider provider = ...;
 *
 * // Get single value
 * Optional<String> dbUrl = provider.getString("database.url");
 *
 * // Get with default
 * int timeout = provider.getInt("http.timeout").orElse(5000);
 *
 * // Get all properties with prefix
 * Map<String, String> dbConfig = provider.getProperties("database");
 * }</pre>
 *
 * @see FeatureFlags
 * @see DynamicConfiguration
 */
public interface ConfigurationProvider {

  /**
   * Retrieves a string configuration value.
   *
   * @param key the configuration key
   * @return optional containing the value if present
   */
  Optional<String> getString(String key);

  /**
   * Retrieves an integer configuration value.
   *
   * @param key the configuration key
   * @return optional containing the parsed integer value if present and valid
   */
  Optional<Integer> getInt(String key);

  /**
   * Retrieves a long configuration value.
   *
   * @param key the configuration key
   * @return optional containing the parsed long value if present and valid
   */
  Optional<Long> getLong(String key);

  /**
   * Retrieves a boolean configuration value.
   *
   * @param key the configuration key
   * @return optional containing the parsed boolean value if present and valid
   */
  Optional<Boolean> getBoolean(String key);

  /**
   * Retrieves a double configuration value.
   *
   * @param key the configuration key
   * @return optional containing the parsed double value if present and valid
   */
  Optional<Double> getDouble(String key);

  /**
   * Retrieves all configuration properties with the specified prefix.
   *
   * <p>Example:
   *
   * <pre>{@code
   * // Given properties:
   * // database.url=jdbc:...
   * // database.username=admin
   * // database.password=secret
   *
   * Map<String, String> dbConfig = provider.getProperties("database");
   * // Returns: {url=jdbc:..., username=admin, password=secret}
   * }</pre>
   *
   * @param prefix the property prefix (without trailing dot)
   * @return map of property keys (without prefix) to values
   */
  Map<String, String> getProperties(String prefix);

  /**
   * Refreshes the configuration from the underlying source.
   *
   * @return result indicating success or failure
   */
  Result<Void> refresh();

  /**
   * Checks if a configuration key exists.
   *
   * @param key the configuration key
   * @return true if the key exists
   */
  boolean containsKey(String key);

  /**
   * Returns the name of this configuration provider.
   *
   * @return provider name
   */
  String getProviderName();
}
