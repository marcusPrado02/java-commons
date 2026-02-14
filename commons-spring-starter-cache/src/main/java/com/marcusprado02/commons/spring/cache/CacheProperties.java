package com.marcusprado02.commons.spring.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Commons Cache.
 *
 * <p>Properties prefix: {@code commons.cache}
 */
@ConfigurationProperties(prefix = "commons.cache")
public class CacheProperties {

  /** Cache type: redis, memcached, or none. */
  private CacheType type = CacheType.REDIS;

  /** Redis-specific configuration. */
  private final Redis redis = new Redis();

  /** Memcached-specific configuration. */
  private final Memcached memcached = new Memcached();

  public CacheType getType() {
    return type;
  }

  public void setType(CacheType type) {
    this.type = type;
  }

  public Redis getRedis() {
    return redis;
  }

  public Memcached getMemcached() {
    return memcached;
  }

  public enum CacheType {
    REDIS,
    MEMCACHED,
    NONE
  }

  /** Redis configuration. */
  public static class Redis {

    /** Redis host. */
    private String host = "localhost";

    /** Redis port. */
    private int port = 6379;

    /** Redis password (optional). */
    private String password;

    /** Key prefix for multi-tenancy. */
    private String keyPrefix = "";

    /** Enable Redis cache. */
    private boolean enabled = true;

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getKeyPrefix() {
      return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
      this.keyPrefix = keyPrefix;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  /** Memcached configuration. */
  public static class Memcached {

    /** Memcached host. */
    private String host = "localhost";

    /** Memcached port. */
    private int port = 11211;

    /** Key prefix for multi-tenancy. */
    private String keyPrefix = "";

    /** Enable Memcached cache. */
    private boolean enabled = true;

    /** Connection timeout. */
    private Duration timeout = Duration.ofSeconds(3);

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public String getKeyPrefix() {
      return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
      this.keyPrefix = keyPrefix;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }
  }
}
