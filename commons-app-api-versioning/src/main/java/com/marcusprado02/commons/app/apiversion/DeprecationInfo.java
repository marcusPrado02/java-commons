package com.marcusprado02.commons.app.apiversion;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Metadata about a deprecated API version.
 *
 * <p>Contains information about when an API version was deprecated, when it will be removed, and
 * guidance for migrating to newer versions.
 */
public final class DeprecationInfo {

  private final ApiVersion version;
  private final LocalDate deprecatedOn;
  private final LocalDate sunsetDate;
  private final String reason;
  private final ApiVersion replacementVersion;
  private final String migrationGuide;

  private DeprecationInfo(Builder builder) {
    this.version = Objects.requireNonNull(builder.version, "version cannot be null");
    this.deprecatedOn = Objects.requireNonNull(builder.deprecatedOn, "deprecatedOn cannot be null");
    this.sunsetDate = builder.sunsetDate;
    this.reason = builder.reason;
    this.replacementVersion = builder.replacementVersion;
    this.migrationGuide = builder.migrationGuide;
  }

  public static Builder builder(ApiVersion version) {
    return new Builder(version);
  }

  public ApiVersion getVersion() {
    return version;
  }

  public LocalDate getDeprecatedOn() {
    return deprecatedOn;
  }

  public Optional<LocalDate> getSunsetDate() {
    return Optional.ofNullable(sunsetDate);
  }

  public Optional<String> getReason() {
    return Optional.ofNullable(reason);
  }

  public Optional<ApiVersion> getReplacementVersion() {
    return Optional.ofNullable(replacementVersion);
  }

  public Optional<String> getMigrationGuide() {
    return Optional.ofNullable(migrationGuide);
  }

  /**
   * Checks if this version is currently sunset (removal date has passed).
   *
   * @return true if sunset
   */
  public boolean isSunset() {
    return sunsetDate != null && LocalDate.now().isAfter(sunsetDate);
  }

  /**
   * Checks if this version will be sunset soon (within the specified days).
   *
   * @param daysThreshold the number of days to check
   * @return true if sunset is approaching
   */
  public boolean isSunsetApproaching(int daysThreshold) {
    if (sunsetDate == null) {
      return false;
    }
    LocalDate threshold = LocalDate.now().plusDays(daysThreshold);
    return sunsetDate.isBefore(threshold);
  }

  public static final class Builder {
    private final ApiVersion version;
    private LocalDate deprecatedOn;
    private LocalDate sunsetDate;
    private String reason;
    private ApiVersion replacementVersion;
    private String migrationGuide;

    private Builder(ApiVersion version) {
      this.version = version;
      this.deprecatedOn = LocalDate.now();
    }

    public Builder deprecatedOn(LocalDate deprecatedOn) {
      this.deprecatedOn = deprecatedOn;
      return this;
    }

    public Builder sunsetDate(LocalDate sunsetDate) {
      this.sunsetDate = sunsetDate;
      return this;
    }

    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public Builder replacementVersion(ApiVersion replacementVersion) {
      this.replacementVersion = replacementVersion;
      return this;
    }

    public Builder migrationGuide(String migrationGuide) {
      this.migrationGuide = migrationGuide;
      return this;
    }

    public DeprecationInfo build() {
      return new DeprecationInfo(this);
    }
  }
}
