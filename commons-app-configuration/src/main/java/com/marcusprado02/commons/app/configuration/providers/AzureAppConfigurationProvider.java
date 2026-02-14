package com.marcusprado02.commons.app.configuration.providers;

import com.marcusprado02.commons.app.configuration.ConfigurationProvider;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.*;

/**
 * Configuration provider for Azure App Configuration.
 *
 * <p>Integrates with Azure App Configuration service to fetch configuration values.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create provider
 * AzureAppConfigurationProvider provider = new AzureAppConfigurationProvider(
 *     "Endpoint=https://myappconfig.azconfig.io;Id=xxx;Secret=yyy"
 * );
 *
 * // Optional: Add label filter
 * provider.setLabel("production");
 *
 * // Use the provider
 * Optional<String> value = provider.getString("my.config.key");
 *
 * // Refresh from Azure
 * provider.refresh();
 * }</pre>
 *
 * <p><b>Note:</b> Requires azure-data-appconfiguration dependency.
 *
 * <p>Dependencies:
 *
 * <pre>{@code
 * <dependency>
 *     <groupId>com.azure</groupId>
 *     <artifactId>azure-data-appconfiguration</artifactId>
 * </dependency>
 * }</pre>
 *
 * @see ConfigurationProvider
 */
public class AzureAppConfigurationProvider implements ConfigurationProvider {

  private static final String PROVIDER_NAME = "azure-app-configuration";
  private final String connectionString;
  private final Map<String, String> cache;
  private String label;

  /**
   * Creates an Azure App Configuration provider.
   *
   * @param connectionString Azure App Configuration connection string
   */
  public AzureAppConfigurationProvider(String connectionString) {
    Objects.requireNonNull(connectionString, "connectionString cannot be null");
    this.connectionString = connectionString;
    this.cache = new HashMap<>();
    loadConfiguration();
  }

  /**
   * Sets the label filter for configuration keys.
   *
   * <p>Labels allow different configurations for different environments.
   *
   * @param label the label (e.g., "production", "staging")
   */
  public void setLabel(String label) {
    this.label = label;
    loadConfiguration();
  }

  @Override
  public Optional<String> getString(String key) {
    return Optional.ofNullable(cache.get(key));
  }

  @Override
  public Optional<Integer> getInt(String key) {
    return getString(key).flatMap(this::parseInteger);
  }

  @Override
  public Optional<Long> getLong(String key) {
    return getString(key).flatMap(this::parseLong);
  }

  @Override
  public Optional<Boolean> getBoolean(String key) {
    return getString(key).map(Boolean::parseBoolean);
  }

  @Override
  public Optional<Double> getDouble(String key) {
    return getString(key).flatMap(this::parseDouble);
  }

  @Override
  public Map<String, String> getProperties(String prefix) {
    String prefixWithDot = prefix.endsWith(".") ? prefix : prefix + ".";
    Map<String, String> result = new HashMap<>();

    cache.forEach(
        (key, value) -> {
          if (key.startsWith(prefixWithDot)) {
            String keyWithoutPrefix = key.substring(prefixWithDot.length());
            result.put(keyWithoutPrefix, value);
          }
        });

    return Collections.unmodifiableMap(result);
  }

  @Override
  public Result<Void> refresh() {
    try {
      loadConfiguration();
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(
          com.marcusprado02.commons.kernel.errors.Problem.of(
              com.marcusprado02.commons.kernel.errors.ErrorCode.of("CONFIG.REFRESH_FAILED"),
              com.marcusprado02.commons.kernel.errors.ErrorCategory.TECHNICAL,
              com.marcusprado02.commons.kernel.errors.Severity.ERROR,
              "Failed to refresh Azure App Configuration: " + e.getMessage()));
    }
  }

  @Override
  public boolean containsKey(String key) {
    return cache.containsKey(key);
  }

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  private void loadConfiguration() {
    try {
      /*
       * Real implementation would use Azure SDK:
       *
       * ConfigurationClient client = new ConfigurationClientBuilder()
       *     .connectionString(connectionString)
       *     .buildClient();
       *
       * SettingSelector selector = new SettingSelector()
       *     .setLabelFilter(label);
       *
       * client.listConfigurationSettings(selector).forEach(setting -> {
       *     cache.put(setting.getKey(), setting.getValue());
       * });
       */

      // Stub implementation - in production, use actual Azure SDK
      System.err.println(
          "WARNING: AzureAppConfigurationProvider is a stub. "
              + "Add azure-data-appconfiguration dependency for real implementation.");
    } catch (Exception e) {
      throw new RuntimeException("Failed to load Azure App Configuration", e);
    }
  }

  private Optional<Integer> parseInteger(String value) {
    try {
      return Optional.of(Integer.parseInt(value));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private Optional<Long> parseLong(String value) {
    try {
      return Optional.of(Long.parseLong(value));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private Optional<Double> parseDouble(String value) {
    try {
      return Optional.of(Double.parseDouble(value));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
