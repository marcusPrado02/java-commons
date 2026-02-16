package com.marcusprado02.commons.app.configuration.providers;

import com.marcusprado02.commons.app.configuration.ConfigurationChangeEvent;
import com.marcusprado02.commons.app.configuration.ConfigurationChangeListener;
import com.marcusprado02.commons.app.configuration.DynamicConfiguration;
import com.marcusprado02.commons.kernel.result.Result;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration provider that integrates with etcd distributed key-value store.
 *
 * <p>Supports:
 *
 * <ul>
 *   <li>Reading configuration from etcd
 *   <li>Real-time change notifications via etcd watch
 *   <li>Prefix-based configuration grouping
 *   <li>Distributed consistency guarantees
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create configuration
 * EtcdConfigurationProvider.Configuration config =
 *     EtcdConfigurationProvider.Configuration.builder()
 *         .endpoints("http://localhost:2379")
 *         .keyPrefix("/config/myapp")
 *         .build();
 *
 * // Create provider
 * ConfigurationProvider provider = new EtcdConfigurationProvider(config);
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
 * <p><b>etcd Key Structure:</b>
 *
 * <pre>
 * /config/
 *   myapp/
 *     database.url = "jdbc:postgresql://localhost/db"
 *     database.username = "user"
 *     feature.newUI = "true"
 * </pre>
 *
 * @see DynamicConfiguration
 */
public class EtcdConfigurationProvider implements DynamicConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(EtcdConfigurationProvider.class);
  private static final String PROVIDER_NAME = "etcd";

  private final Configuration configuration;
  private final Client etcdClient;
  private final KV kvClient;
  private final Watch watchClient;
  private final Map<String, String> cache = new ConcurrentHashMap<>();
  private final List<ConfigurationChangeListener> listeners = new CopyOnWriteArrayList<>();
  private final Map<String, List<Consumer<ConfigurationChangeEvent>>> keyListeners =
      new ConcurrentHashMap<>();
  private Watch.Watcher watcher;

  /**
   * Creates an etcd configuration provider.
   *
   * @param configuration etcd configuration
   */
  public EtcdConfigurationProvider(Configuration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
    this.etcdClient = Client.builder().endpoints(configuration.endpoints).build();
    this.kvClient = etcdClient.getKVClient();
    this.watchClient = etcdClient.getWatchClient();
    initializeCache();
    if (configuration.enableWatch) {
      startWatching();
    }
  }

  /** Initializes the configuration cache from etcd. */
  private void initializeCache() {
    try {
      ByteSequence prefixBytes = toByteSequence(configuration.keyPrefix);
      GetOption option = GetOption.builder().isPrefix(true).build();

      CompletableFuture<io.etcd.jetcd.kv.GetResponse> future = kvClient.get(prefixBytes, option);
      io.etcd.jetcd.kv.GetResponse response = future.get();

      for (KeyValue kv : response.getKvs()) {
        String key = removePrefix(toString(kv.getKey()));
        String value = toString(kv.getValue());
        cache.put(key, value);
      }

      logger.info("Initialized etcd configuration cache with {} keys", cache.size());
    } catch (Exception e) {
      logger.error("Failed to initialize etcd configuration cache", e);
    }
  }

  /** Starts watching for configuration changes in etcd. */
  private void startWatching() {
    try {
      ByteSequence prefixBytes = toByteSequence(configuration.keyPrefix);
      WatchOption option = WatchOption.builder().isPrefix(true).build();

      watcher =
          watchClient.watch(
              prefixBytes,
              option,
              watchResponse -> {
                Set<String> changedKeys = new HashSet<>();

                for (WatchEvent event : watchResponse.getEvents()) {
                  String key = removePrefix(toString(event.getKeyValue().getKey()));

                  switch (event.getEventType()) {
                    case PUT:
                      String value = toString(event.getKeyValue().getValue());
                      cache.put(key, value);
                      changedKeys.add(key);
                      break;
                    case DELETE:
                      cache.remove(key);
                      changedKeys.add(key);
                      break;
                    default:
                      break;
                  }
                }

                if (!changedKeys.isEmpty()) {
                  // Notify listeners for each changed key
                  for (String changedKey : changedKeys) {
                    String oldValue = null; // Could track old values if needed
                    String newValue = cache.get(changedKey);
                    ConfigurationChangeEvent.ChangeType type =
                        newValue != null
                            ? ConfigurationChangeEvent.ChangeType.UPDATED
                            : ConfigurationChangeEvent.ChangeType.REMOVED;

                    ConfigurationChangeEvent changeEvent =
                        new ConfigurationChangeEvent(
                            changedKey,
                            oldValue,
                            newValue,
                            type,
                            java.time.Instant.now(),
                            PROVIDER_NAME);
                    notifyListeners(changeEvent);
                  }
                  logger.info("Configuration changed: {} keys updated", changedKeys.size());
                }
              });

      logger.info("Started watching etcd for configuration changes");
    } catch (Exception e) {
      logger.error("Failed to start watching etcd", e);
    }
  }

  private ByteSequence toByteSequence(String str) {
    return ByteSequence.from(str, StandardCharsets.UTF_8);
  }

  private String toString(ByteSequence bytes) {
    return bytes.toString(StandardCharsets.UTF_8);
  }

  private String removePrefix(String key) {
    if (key.startsWith(configuration.keyPrefix + "/")) {
      return key.substring(configuration.keyPrefix.length() + 1);
    }
    if (key.startsWith(configuration.keyPrefix)) {
      return key.substring(configuration.keyPrefix.length());
    }
    return key;
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
      cache.clear();
      initializeCache();
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(
          com.marcusprado02.commons.kernel.errors.Problem.of(
              com.marcusprado02.commons.kernel.errors.ErrorCode.of("CONFIG_REFRESH_FAILED"),
              com.marcusprado02.commons.kernel.errors.ErrorCategory.INFRASTRUCTURE,
              com.marcusprado02.commons.kernel.errors.Severity.ERROR,
              "Failed to refresh etcd configuration: " + e.getMessage()));
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
    // Auto-refresh already enabled via watch
    return Result.ok(null);
  }

  @Override
  public void disableAutoRefresh() {
    // Controlled via enableWatch configuration
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

  /** Shuts down the etcd client and watcher. */
  public void shutdown() {
    if (watcher != null) {
      watcher.close();
    }
    if (etcdClient != null) {
      etcdClient.close();
    }
  }

  /** Configuration for etcd provider. */
  public static class Configuration {
    private final String[] endpoints;
    private final String keyPrefix;
    private final boolean enableWatch;

    private Configuration(Builder builder) {
      this.endpoints = builder.endpoints;
      this.keyPrefix = builder.keyPrefix;
      this.enableWatch = builder.enableWatch;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private String[] endpoints = new String[] {"http://localhost:2379"};
      private String keyPrefix = "/config";
      private boolean enableWatch = true;

      public Builder endpoints(String... endpoints) {
        this.endpoints = endpoints;
        return this;
      }

      public Builder keyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
        return this;
      }

      public Builder enableWatch(boolean enableWatch) {
        this.enableWatch = enableWatch;
        return this;
      }

      public Configuration build() {
        return new Configuration(this);
      }
    }
  }
}
