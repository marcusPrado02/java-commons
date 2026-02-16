package com.marcusprado02.commons.app.apiversion;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing API versions and their deprecation status.
 *
 * <p>Maintains information about which API versions are supported, deprecated, and sunset.
 */
public final class VersionRegistry {

  private final Map<ApiVersion, DeprecationInfo> deprecations = new ConcurrentHashMap<>();
  private final ApiVersion defaultVersion;
  private final ApiVersion latestVersion;

  private VersionRegistry(Builder builder) {
    this.defaultVersion =
        Objects.requireNonNull(builder.defaultVersion, "defaultVersion cannot be null");
    this.latestVersion =
        Objects.requireNonNull(builder.latestVersion, "latestVersion cannot be null");
    this.deprecations.putAll(builder.deprecations);
  }

  public static Builder builder() {
    return new Builder();
  }

  public ApiVersion getDefaultVersion() {
    return defaultVersion;
  }

  public ApiVersion getLatestVersion() {
    return latestVersion;
  }

  /**
   * Checks if a version is deprecated.
   *
   * @param version the version to check
   * @return true if deprecated
   */
  public boolean isDeprecated(ApiVersion version) {
    return deprecations.containsKey(version);
  }

  /**
   * Gets deprecation information for a version.
   *
   * @param version the version
   * @return optional containing deprecation info if deprecated
   */
  public Optional<DeprecationInfo> getDeprecationInfo(ApiVersion version) {
    return Optional.ofNullable(deprecations.get(version));
  }

  /**
   * Checks if a version is sunset (removed).
   *
   * @param version the version to check
   * @return true if sunset
   */
  public boolean isSunset(ApiVersion version) {
    return deprecations.containsKey(version) && deprecations.get(version).isSunset();
  }

  /**
   * Checks if a version is supported (not sunset).
   *
   * @param version the version to check
   * @return true if supported
   */
  public boolean isSupported(ApiVersion version) {
    return !isSunset(version);
  }

  /**
   * Registers a deprecated version.
   *
   * @param deprecationInfo the deprecation information
   */
  public void registerDeprecation(DeprecationInfo deprecationInfo) {
    deprecations.put(deprecationInfo.getVersion(), deprecationInfo);
  }

  public static final class Builder {
    private ApiVersion defaultVersion;
    private ApiVersion latestVersion;
    private final Map<ApiVersion, DeprecationInfo> deprecations = new ConcurrentHashMap<>();

    private Builder() {}

    public Builder defaultVersion(ApiVersion version) {
      this.defaultVersion = version;
      return this;
    }

    public Builder latestVersion(ApiVersion version) {
      this.latestVersion = version;
      return this;
    }

    public Builder deprecate(DeprecationInfo deprecationInfo) {
      this.deprecations.put(deprecationInfo.getVersion(), deprecationInfo);
      return this;
    }

    public VersionRegistry build() {
      return new VersionRegistry(this);
    }
  }
}
