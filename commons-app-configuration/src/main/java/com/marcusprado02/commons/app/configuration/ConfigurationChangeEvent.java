package com.marcusprado02.commons.app.configuration;

import java.time.Instant;
import java.util.Objects;

/**
 * Event representing a configuration change.
 *
 * <p>Contains information about what changed, when, and the old/new values.
 *
 * @param key the configuration key that changed
 * @param oldValue the previous value (null if key was added)
 * @param newValue the new value (null if key was removed)
 * @param changeType the type of change (ADDED, UPDATED, REMOVED)
 * @param timestamp when the change occurred
 * @param source the source of the change (e.g., "azure-app-config", "spring-cloud-config")
 */
public record ConfigurationChangeEvent(
    String key,
    String oldValue,
    String newValue,
    ChangeType changeType,
    Instant timestamp,
    String source) {

  public ConfigurationChangeEvent {
    Objects.requireNonNull(key, "key cannot be null");
    Objects.requireNonNull(changeType, "changeType cannot be null");
    Objects.requireNonNull(timestamp, "timestamp cannot be null");
    Objects.requireNonNull(source, "source cannot be null");
  }

  /**
   * Creates a new change event for an added configuration.
   *
   * @param key the configuration key
   * @param newValue the new value
   * @param source the source of the change
   * @return change event
   */
  public static ConfigurationChangeEvent added(String key, String newValue, String source) {
    return new ConfigurationChangeEvent(
        key, null, newValue, ChangeType.ADDED, Instant.now(), source);
  }

  /**
   * Creates a new change event for an updated configuration.
   *
   * @param key the configuration key
   * @param oldValue the old value
   * @param newValue the new value
   * @param source the source of the change
   * @return change event
   */
  public static ConfigurationChangeEvent updated(
      String key, String oldValue, String newValue, String source) {
    return new ConfigurationChangeEvent(
        key, oldValue, newValue, ChangeType.UPDATED, Instant.now(), source);
  }

  /**
   * Creates a new change event for a removed configuration.
   *
   * @param key the configuration key
   * @param oldValue the old value
   * @param source the source of the change
   * @return change event
   */
  public static ConfigurationChangeEvent removed(String key, String oldValue, String source) {
    return new ConfigurationChangeEvent(
        key, oldValue, null, ChangeType.REMOVED, Instant.now(), source);
  }

  /** Type of configuration change. */
  public enum ChangeType {
    /** A new configuration key was added. */
    ADDED,
    /** An existing configuration value was updated. */
    UPDATED,
    /** A configuration key was removed. */
    REMOVED
  }
}
