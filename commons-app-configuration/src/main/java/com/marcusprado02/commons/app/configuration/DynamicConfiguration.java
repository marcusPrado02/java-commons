package com.marcusprado02.commons.app.configuration;

import com.marcusprado02.commons.kernel.result.Result;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Dynamic configuration service with change notification and automatic refresh.
 *
 * <p>Allows configuration to be updated at runtime without application restart.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * DynamicConfiguration config = ...;
 *
 * // Get current value with type safety
 * int maxConnections = config.getInt("database.max-connections", 10);
 *
 * // Listen for changes
 * config.addListener("database.max-connections", change -> {
 *     int oldValue = Integer.parseInt(change.oldValue());
 *     int newValue = Integer.parseInt(change.newValue());
 *     logger.info("Max connections changed from {} to {}", oldValue, newValue);
 *     connectionPool.resize(newValue);
 * });
 *
 * // Enable automatic refresh
 * config.enableAutoRefresh(Duration.ofMinutes(5));
 * }</pre>
 *
 * @see ConfigurationProvider
 * @see ConfigurationChangeEvent
 */
public interface DynamicConfiguration extends ConfigurationProvider {

  /**
   * Adds a listener for configuration changes.
   *
   * @param listener the change listener
   */
  void addListener(ConfigurationChangeListener listener);

  /**
   * Adds a listener for changes to a specific configuration key.
   *
   * @param key the configuration key to watch
   * @param listener the change listener
   */
  void addListener(String key, Consumer<ConfigurationChangeEvent> listener);

  /**
   * Removes a configuration change listener.
   *
   * @param listener the listener to remove
   */
  void removeListener(ConfigurationChangeListener listener);

  /**
   * Enables automatic configuration refresh at the specified interval.
   *
   * @param interval the refresh interval
   * @return result indicating success or failure
   */
  Result<Void> enableAutoRefresh(Duration interval);

  /** Disables automatic configuration refresh. */
  void disableAutoRefresh();

  /**
   * Checks if automatic refresh is enabled.
   *
   * @return true if auto-refresh is enabled
   */
  boolean isAutoRefreshEnabled();

  /**
   * Returns the current auto-refresh interval.
   *
   * @return refresh interval, or null if not enabled
   */
  Duration getRefreshInterval();
}
