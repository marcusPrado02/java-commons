package com.marcusprado02.commons.app.configuration;

import com.marcusprado02.commons.kernel.result.Result;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * In-memory implementation of DynamicConfiguration for testing and development.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create and configure
 * InMemoryConfigurationProvider provider = new InMemoryConfigurationProvider();
 * provider.setProperty("app.name", "my-service");
 * provider.setProperty("app.timeout", "5000");
 *
 * // Use as ConfigurationProvider
 * Optional<String> name = provider.getString("app.name");
 * int timeout = provider.getInt("app.timeout").orElse(3000);
 *
 * // Listen for changes
 * provider.addListener("app.timeout", event -> {
 *     logger.info("Timeout changed to {}", event.newValue());
 * });
 *
 * // Update property (triggers listeners)
 * provider.setProperty("app.timeout", "10000");
 * }</pre>
 *
 * @see DynamicConfiguration
 * @see ConfigurationProvider
 */
public class InMemoryConfigurationProvider implements DynamicConfiguration {

  private final String providerName;
  private final Map<String, String> properties;
  private final List<ConfigurationChangeListener> listeners;
  private final Map<String, List<Consumer<ConfigurationChangeEvent>>> keyListeners;
  private ScheduledExecutorService refreshExecutor;
  private Duration refreshInterval;

  public InMemoryConfigurationProvider() {
    this("in-memory");
  }

  public InMemoryConfigurationProvider(String providerName) {
    this.providerName = providerName;
    this.properties = new ConcurrentHashMap<>();
    this.listeners = new CopyOnWriteArrayList<>();
    this.keyListeners = new ConcurrentHashMap<>();
  }

  /**
   * Sets a configuration property.
   *
   * <p>Triggers change listeners if the value changed.
   *
   * @param key the configuration key
   * @param value the configuration value
   */
  public void setProperty(String key, String value) {
    String oldValue = properties.put(key, value);

    ConfigurationChangeEvent event;
    if (oldValue == null) {
      event = ConfigurationChangeEvent.added(key, value, providerName);
    } else if (!oldValue.equals(value)) {
      event = ConfigurationChangeEvent.updated(key, oldValue, value, providerName);
    } else {
      return; // No change
    }

    notifyListeners(event);
  }

  /**
   * Removes a configuration property.
   *
   * @param key the configuration key
   */
  public void removeProperty(String key) {
    String oldValue = properties.remove(key);
    if (oldValue != null) {
      ConfigurationChangeEvent event =
          ConfigurationChangeEvent.removed(key, oldValue, providerName);
      notifyListeners(event);
    }
  }

  /**
   * Sets multiple properties at once.
   *
   * @param properties the properties to set
   */
  public void setProperties(Map<String, String> properties) {
    properties.forEach(this::setProperty);
  }

  /** Clears all properties. */
  public void clear() {
    List<String> keys = new ArrayList<>(properties.keySet());
    keys.forEach(this::removeProperty);
  }

  @Override
  public Optional<String> getString(String key) {
    return Optional.ofNullable(properties.get(key));
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

    properties.forEach(
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
    // In-memory provider has no external source to refresh from
    return Result.ok(null);
  }

  @Override
  public boolean containsKey(String key) {
    return properties.containsKey(key);
  }

  @Override
  public String getProviderName() {
    return providerName;
  }

  @Override
  public void addListener(ConfigurationChangeListener listener) {
    listeners.add(listener);
  }

  @Override
  public void addListener(String key, Consumer<ConfigurationChangeEvent> listener) {
    keyListeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
  }

  @Override
  public void removeListener(ConfigurationChangeListener listener) {
    listeners.remove(listener);
  }

  @Override
  public Result<Void> enableAutoRefresh(Duration interval) {
    if (refreshExecutor != null) {
      disableAutoRefresh();
    }

    this.refreshInterval = interval;
    this.refreshExecutor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread thread = new Thread(r, "config-refresh-" + providerName);
              thread.setDaemon(true);
              return thread;
            });

    refreshExecutor.scheduleAtFixedRate(
        this::refresh, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);

    return Result.ok(null);
  }

  @Override
  public void disableAutoRefresh() {
    if (refreshExecutor != null) {
      refreshExecutor.shutdown();
      refreshExecutor = null;
      refreshInterval = null;
    }
  }

  @Override
  public boolean isAutoRefreshEnabled() {
    return refreshExecutor != null && !refreshExecutor.isShutdown();
  }

  @Override
  public Duration getRefreshInterval() {
    return refreshInterval;
  }

  private void notifyListeners(ConfigurationChangeEvent event) {
    // Notify global listeners
    for (ConfigurationChangeListener listener : listeners) {
      try {
        listener.onConfigurationChange(event);
      } catch (Exception e) {
        // Log but don't fail
        System.err.println("Error in configuration change listener: " + e.getMessage());
      }
    }

    // Notify key-specific listeners
    List<Consumer<ConfigurationChangeEvent>> specificListeners = keyListeners.get(event.key());
    if (specificListeners != null) {
      for (Consumer<ConfigurationChangeEvent> listener : specificListeners) {
        try {
          listener.accept(event);
        } catch (Exception e) {
          System.err.println("Error in key-specific configuration listener: " + e.getMessage());
        }
      }
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
