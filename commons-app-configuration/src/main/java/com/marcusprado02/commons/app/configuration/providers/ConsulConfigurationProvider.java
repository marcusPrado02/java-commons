package com.marcusprado02.commons.app.configuration.providers;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.marcusprado02.commons.app.configuration.ConfigurationChangeEvent;
import com.marcusprado02.commons.app.configuration.ConfigurationChangeListener;
import com.marcusprado02.commons.app.configuration.DynamicConfiguration;
import com.marcusprado02.commons.kernel.result.Result;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration provider that integrates with HashiCorp Consul KV store.
 *
 * <p>Supports:
 *
 * <ul>
 *   <li>Reading configuration from Consul KV store
 *   <li>Dynamic refresh with long polling
 *   <li>Prefix-based configuration grouping
 *   <li>Automatic change notifications
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create configuration
 * ConsulConfigurationProvider.Configuration config =
 *     ConsulConfigurationProvider.Configuration.builder()
 *         .host("localhost")
 *         .port(8500)
 *         .keyPrefix("config/myapp")
 *         .pollingInterval(Duration.ofSeconds(30))
 *         .build();
 *
 * // Create provider
 * ConfigurationProvider provider = new ConsulConfigurationProvider(config);
 *
 * // Listen for changes
 * provider.addChangeListener(event -> {
 *     System.out.println("Configuration changed: " + event.getChangedKeys());
 * });
 *
 * // Get values
 * Optional<String> value = provider.getString("database.url");
 * }</pre>
 *
 * <p><b>Consul KV Structure:</b>
 *
 * <pre>
 * config/
 *   myapp/
 *     database.url = "jdbc:postgresql://localhost/db"
 *     database.username = "user"
 *     feature.newUI = "true"
 * </pre>
 *
 * @see DynamicConfiguration
 */
public class ConsulConfigurationProvider implements DynamicConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(ConsulConfigurationProvider.class);
  private static final String PROVIDER_NAME = "consul";

  private final Configuration configuration;
  private final ConsulClient consulClient;
  private final Map<String, String> cache = new ConcurrentHashMap<>();
  private final List<ConfigurationChangeListener> listeners = new CopyOnWriteArrayList<>();
  private final Map<String, List<Consumer<ConfigurationChangeEvent>>> keyListeners =
      new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private volatile long lastIndex = 0;

  /**
   * Creates a Consul configuration provider.
   *
   * @param configuration Consul configuration
   */
  public ConsulConfigurationProvider(Configuration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
    this.consulClient = new ConsulClient(configuration.host, configuration.port);
    initializeCache();
    if (configuration.enablePolling) {
      startPolling();
    }
  }

  /** Initializes the configuration cache from Consul. */
  private void initializeCache() {
    try {
      Response<List<GetValue>> response =
          consulClient.getKVValues(
              configuration.keyPrefix, configuration.token, QueryParams.DEFAULT);

      if (response.getValue() != null) {
        for (GetValue value : response.getValue()) {
          String key = removePrefix(value.getKey());
          String val = decodeValue(value.getDecodedValue());
          cache.put(key, val);
        }
      }
      lastIndex = response.getConsulIndex();
      logger.info("Initialized Consul configuration cache with {} keys", cache.size());
    } catch (Exception e) {
      logger.error("Failed to initialize Consul configuration cache", e);
    }
  }

  /** Starts polling for configuration changes. */
  private void startPolling() {
    scheduler.scheduleWithFixedDelay(
        this::pollForChanges,
        configuration.pollingInterval.toMillis(),
        configuration.pollingInterval.toMillis(),
        TimeUnit.MILLISECONDS);
  }

  /** Polls Consul for configuration changes using long polling. */
  private void pollForChanges() {
    try {
      QueryParams queryParams =
          QueryParams.Builder.builder()
              .setIndex(lastIndex)
              .setWaitTime(configuration.longPollTimeout.toSeconds())
              .build();

      Response<List<GetValue>> response =
          consulClient.getKVValues(configuration.keyPrefix, configuration.token, queryParams);

      if (response.getConsulIndex() > lastIndex) {
        processChanges(response);
        lastIndex = response.getConsulIndex();
      }
    } catch (Exception e) {
      logger.error("Error polling Consul for configuration changes", e);
    }
  }

  /** Processes configuration changes from Consul response. */
  private void processChanges(Response<List<GetValue>> response) {
    Map<String, String> newCache = new HashMap<>();
    Set<String> changedKeys = new HashSet<>();

    if (response.getValue() != null) {
      for (GetValue value : response.getValue()) {
        String key = removePrefix(value.getKey());
        String val = decodeValue(value.getDecodedValue());
        newCache.put(key, val);

        // Check if value changed
        String oldValue = cache.get(key);
        if (!Objects.equals(oldValue, val)) {
          changedKeys.add(key);
        }
      }
    }

    // Find deleted keys
    for (String key : cache.keySet()) {
      if (!newCache.containsKey(key)) {
        changedKeys.add(key);
      }
    }

    if (!changedKeys.isEmpty()) {
      // Track old values before updating cache
      Map<String, String> oldCache = new HashMap<>(cache);
      cache.clear();
      cache.putAll(newCache);

      // Notify listeners for each changed key
      for (String changedKey : changedKeys) {
        String oldValue = oldCache.get(changedKey);
        String newValue = newCache.get(changedKey);
        ConfigurationChangeEvent.ChangeType type;

        if (oldValue == null) {
          type = ConfigurationChangeEvent.ChangeType.ADDED;
        } else if (newValue == null) {
          type = ConfigurationChangeEvent.ChangeType.REMOVED;
        } else {
          type = ConfigurationChangeEvent.ChangeType.UPDATED;
        }

        ConfigurationChangeEvent event =
            new ConfigurationChangeEvent(
                changedKey, oldValue, newValue, type, java.time.Instant.now(), PROVIDER_NAME);
        notifyListeners(event);
      }

      logger.info("Configuration changed: {} keys updated", changedKeys.size());
    }
  }

  private String removePrefix(String key) {
    if (key.startsWith(configuration.keyPrefix + "/")) {
      return key.substring(configuration.keyPrefix.length() + 1);
    }
    return key;
  }

  private String decodeValue(byte[] bytes) {
    if (bytes == null) return null;
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Override
  public Optional<String> getString(String key) {
    return Optional.ofNullable(cache.get(key));
  }

  @Override
  public Optional<Integer> getInt(String key) {
    return getString(key).map(Integer::parseInt);
  }

  @Override
  public Optional<Long> getLong(String key) {
    return getString(key).map(Long::parseLong);
  }

  @Override
  public Optional<Double> getDouble(String key) {
    return getString(key).map(Double::parseDouble);
  }

  @Override
  public Optional<Boolean> getBoolean(String key) {
    return getString(key).map(Boolean::parseBoolean);
  }

  @Override
  public Map<String, String> getProperties(String prefix) {
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, String> entry : cache.entrySet()) {
      if (entry.getKey().startsWith(prefix + ".")) {
        String keyWithoutPrefix = entry.getKey().substring(prefix.length() + 1);
        result.put(keyWithoutPrefix, entry.getValue());
      }
    }
    return result;
  }

  @Override
  public Result<Void> refresh() {
    try {
      initializeCache();
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(
          com.marcusprado02.commons.kernel.errors.Problem.of(
              com.marcusprado02.commons.kernel.errors.ErrorCode.of("CONFIG_REFRESH_FAILED"),
              com.marcusprado02.commons.kernel.errors.ErrorCategory.INFRASTRUCTURE,
              com.marcusprado02.commons.kernel.errors.Severity.ERROR,
              "Failed to refresh Consul configuration: " + e.getMessage()));
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
  public Result<Void> enableAutoRefresh(java.time.Duration interval) {
    // Auto-refresh already enabled via polling
    return Result.ok(null);
  }

  @Override
  public void disableAutoRefresh() {
    // Controlled via enablePolling configuration
  }

  private void notifyListeners(ConfigurationChangeEvent event) {
    // Notify global listeners
    for (ConfigurationChangeListener listener : listeners) {
      try {
        listener.onConfigurationChange(event);
      } catch (Exception e) {
        logger.error("Error notifying configuration change listener", e);
      }
    }

    // Notify key-specific listeners
    List<Consumer<ConfigurationChangeEvent>> specificListeners = keyListeners.get(event.key());
    if (specificListeners != null) {
      for (Consumer<ConfigurationChangeEvent> listener : specificListeners) {
        try {
          listener.accept(event);
        } catch (Exception e) {
          logger.error("Error notifying key-specific listener for key: " + event.key(), e);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T convertValue(String value, Class<T> type) {
    if (type == String.class) return (T) value;
    if (type == Integer.class) return (T) Integer.valueOf(value);
    if (type == Long.class) return (T) Long.valueOf(value);
    if (type == Double.class) return (T) Double.valueOf(value);
    if (type == Boolean.class) return (T) Boolean.valueOf(value);
    throw new IllegalArgumentException("Unsupported type: " + type);
  }

  /** Shuts down the polling scheduler. */
  public void shutdown() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /** Configuration for Consul provider. */
  public static class Configuration {
    private final String host;
    private final int port;
    private final String keyPrefix;
    private final String token;
    private final boolean enablePolling;
    private final java.time.Duration pollingInterval;
    private final java.time.Duration longPollTimeout;

    private Configuration(Builder builder) {
      this.host = builder.host;
      this.port = builder.port;
      this.keyPrefix = builder.keyPrefix;
      this.token = builder.token;
      this.enablePolling = builder.enablePolling;
      this.pollingInterval = builder.pollingInterval;
      this.longPollTimeout = builder.longPollTimeout;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private String host = "localhost";
      private int port = 8500;
      private String keyPrefix = "config";
      private String token = null;
      private boolean enablePolling = true;
      private java.time.Duration pollingInterval = java.time.Duration.ofSeconds(30);
      private java.time.Duration longPollTimeout = java.time.Duration.ofSeconds(55);

      public Builder host(String host) {
        this.host = host;
        return this;
      }

      public Builder port(int port) {
        this.port = port;
        return this;
      }

      public Builder keyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
        return this;
      }

      public Builder token(String token) {
        this.token = token;
        return this;
      }

      public Builder enablePolling(boolean enablePolling) {
        this.enablePolling = enablePolling;
        return this;
      }

      public Builder pollingInterval(java.time.Duration pollingInterval) {
        this.pollingInterval = pollingInterval;
        return this;
      }

      public Builder longPollTimeout(java.time.Duration longPollTimeout) {
        this.longPollTimeout = longPollTimeout;
        return this;
      }

      public Configuration build() {
        return new Configuration(this);
      }
    }
  }
}
