package com.marcusprado02.commons.app.configuration;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.*;

/**
 * Composite configuration provider that delegates to multiple providers with precedence order.
 *
 * <p>Providers are queried in order until a value is found. The first provider that returns a value
 * wins.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create composite with precedence: environment > cloud config > defaults
 * ConfigurationProvider composite = new CompositeConfigurationProvider(
 *     environmentProvider,      // Highest priority
 *     azureAppConfigProvider,
 *     defaultsProvider          // Lowest priority
 * );
 *
 * // Query will check providers in order
 * Optional<String> dbUrl = composite.getString("database.url");
 * // Returns first non-empty value from: env > azure > defaults
 * }</pre>
 *
 * @see ConfigurationProvider
 */
public class CompositeConfigurationProvider implements ConfigurationProvider {

  private final List<ConfigurationProvider> providers;
  private final String providerName;

  /**
   * Creates a composite provider with the given providers in precedence order.
   *
   * @param providers the providers in order (highest to lowest priority)
   */
  public CompositeConfigurationProvider(ConfigurationProvider... providers) {
    this(Arrays.asList(providers));
  }

  /**
   * Creates a composite provider with the given providers in precedence order.
   *
   * @param providers the providers in order (highest to lowest priority)
   */
  public CompositeConfigurationProvider(List<ConfigurationProvider> providers) {
    if (providers == null || providers.isEmpty()) {
      throw new IllegalArgumentException("At least one provider is required");
    }
    this.providers = new ArrayList<>(providers);
    this.providerName =
        "composite["
            + String.join(
                ",", providers.stream().map(ConfigurationProvider::getProviderName).toList())
            + "]";
  }

  @Override
  public Optional<String> getString(String key) {
    for (ConfigurationProvider provider : providers) {
      Optional<String> value = provider.getString(key);
      if (value.isPresent()) {
        return value;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Integer> getInt(String key) {
    for (ConfigurationProvider provider : providers) {
      Optional<Integer> value = provider.getInt(key);
      if (value.isPresent()) {
        return value;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Long> getLong(String key) {
    for (ConfigurationProvider provider : providers) {
      Optional<Long> value = provider.getLong(key);
      if (value.isPresent()) {
        return value;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Boolean> getBoolean(String key) {
    for (ConfigurationProvider provider : providers) {
      Optional<Boolean> value = provider.getBoolean(key);
      if (value.isPresent()) {
        return value;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Double> getDouble(String key) {
    for (ConfigurationProvider provider : providers) {
      Optional<Double> value = provider.getDouble(key);
      if (value.isPresent()) {
        return value;
      }
    }
    return Optional.empty();
  }

  @Override
  public Map<String, String> getProperties(String prefix) {
    Map<String, String> result = new HashMap<>();

    // Iterate in reverse order so higher priority providers overwrite lower ones
    for (int i = providers.size() - 1; i >= 0; i--) {
      Map<String, String> properties = providers.get(i).getProperties(prefix);
      result.putAll(properties);
    }

    return Collections.unmodifiableMap(result);
  }

  @Override
  public Result<Void> refresh() {
    List<String> errors = new ArrayList<>();

    for (ConfigurationProvider provider : providers) {
      Result<Void> result = provider.refresh();
      if (result.isFail()) {
        errors.add(provider.getProviderName() + ": " + result.problemOrNull().message());
      }
    }

    if (!errors.isEmpty()) {
      return Result.fail(
          com.marcusprado02.commons.kernel.errors.Problem.of(
              com.marcusprado02.commons.kernel.errors.ErrorCode.of("CONFIG.REFRESH_FAILED"),
              com.marcusprado02.commons.kernel.errors.ErrorCategory.TECHNICAL,
              com.marcusprado02.commons.kernel.errors.Severity.ERROR,
              "One or more providers failed to refresh: " + String.join("; ", errors)));
    }

    return Result.ok(null);
  }

  @Override
  public boolean containsKey(String key) {
    return providers.stream().anyMatch(p -> p.containsKey(key));
  }

  @Override
  public String getProviderName() {
    return providerName;
  }

  /**
   * Returns all providers in precedence order.
   *
   * @return unmodifiable list of providers
   */
  public List<ConfigurationProvider> getProviders() {
    return Collections.unmodifiableList(providers);
  }
}
