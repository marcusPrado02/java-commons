package com.marcusprado02.commons.app.configuration;

/**
 * Listener for configuration changes.
 *
 * <p>Implementations are notified when configuration values change, allowing them to react
 * accordingly.
 *
 * <p>Example:
 *
 * <pre>{@code
 * public class DatabaseConfigListener implements ConfigurationChangeListener {
 *
 *     @Override
 *     public void onConfigurationChange(ConfigurationChangeEvent event) {
 *         if (event.key().startsWith("database.")) {
 *             logger.info("Database config changed: {} = {}", event.key(), event.newValue());
 *             reconfigureConnectionPool();
 *         }
 *     }
 * }
 * }</pre>
 *
 * @see ConfigurationChangeEvent
 * @see DynamicConfiguration
 */
@FunctionalInterface
public interface ConfigurationChangeListener {

  /**
   * Called when a configuration value changes.
   *
   * @param event the change event
   */
  void onConfigurationChange(ConfigurationChangeEvent event);
}
