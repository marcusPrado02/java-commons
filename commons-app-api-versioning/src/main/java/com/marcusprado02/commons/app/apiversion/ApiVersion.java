package com.marcusprado02.commons.app.apiversion;

import java.util.Objects;

/**
 * Represents an API version.
 *
 * <p>Versions follow semantic versioning with major and minor components. The patch level is
 * typically not used for API versioning as it represents backward-compatible bug fixes.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * ApiVersion v1 = ApiVersion.of(1, 0);
 * ApiVersion v2 = ApiVersion.of(2, 0);
 *
 * if (v2.isNewerThan(v1)) {
 *   // Use newer API features
 * }
 *
 * boolean compatible = v1.isCompatibleWith(ApiVersion.of(1, 5));
 * }</pre>
 */
public final class ApiVersion implements Comparable<ApiVersion> {

  private final int major;
  private final int minor;

  private ApiVersion(int major, int minor) {
    if (major < 0) {
      throw new IllegalArgumentException("Major version must be >= 0");
    }
    if (minor < 0) {
      throw new IllegalArgumentException("Minor version must be >= 0");
    }
    this.major = major;
    this.minor = minor;
  }

  /**
   * Creates an API version with the specified major and minor components.
   *
   * @param major the major version
   * @param minor the minor version
   * @return the API version
   */
  public static ApiVersion of(int major, int minor) {
    return new ApiVersion(major, minor);
  }

  /**
   * Creates an API version with only a major component (minor defaults to 0).
   *
   * @param major the major version
   * @return the API version
   */
  public static ApiVersion of(int major) {
    return new ApiVersion(major, 0);
  }

  /**
   * Parses a version string in the format "v1" or "v1.2" or "1" or "1.2".
   *
   * @param version the version string
   * @return the parsed API version
   * @throws IllegalArgumentException if the version string is invalid
   */
  public static ApiVersion parse(String version) {
    Objects.requireNonNull(version, "version cannot be null");

    String normalized = version.trim().toLowerCase();
    if (normalized.startsWith("v")) {
      normalized = normalized.substring(1);
    }

    String[] parts = normalized.split("\\.");
    if (parts.length == 0 || parts.length > 2) {
      throw new IllegalArgumentException("Invalid version format: " + version);
    }

    try {
      int major = Integer.parseInt(parts[0]);
      int minor = parts.length == 2 ? Integer.parseInt(parts[1]) : 0;
      return new ApiVersion(major, minor);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid version format: " + version, e);
    }
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  /**
   * Checks if this version is newer than the specified version.
   *
   * @param other the version to compare with
   * @return true if this version is newer
   */
  public boolean isNewerThan(ApiVersion other) {
    return compareTo(other) > 0;
  }

  /**
   * Checks if this version is older than the specified version.
   *
   * @param other the version to compare with
   * @return true if this version is older
   */
  public boolean isOlderThan(ApiVersion other) {
    return compareTo(other) < 0;
  }

  /**
   * Checks if this version is compatible with the specified version.
   *
   * <p>Two versions are compatible if they have the same major version. Minor version differences
   * are considered backward compatible within the same major version.
   *
   * @param other the version to check compatibility with
   * @return true if compatible
   */
  public boolean isCompatibleWith(ApiVersion other) {
    return this.major == other.major;
  }

  /**
   * Returns the version as a string in the format "v1" or "v1.2".
   *
   * @return the version string
   */
  public String toVersionString() {
    return minor == 0 ? "v" + major : "v" + major + "." + minor;
  }

  /**
   * Returns the version path segment (e.g., "v1", "v2").
   *
   * @return the path segment
   */
  public String toPathSegment() {
    return "v" + major;
  }

  @Override
  public int compareTo(ApiVersion other) {
    int majorComparison = Integer.compare(this.major, other.major);
    if (majorComparison != 0) {
      return majorComparison;
    }
    return Integer.compare(this.minor, other.minor);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ApiVersion that = (ApiVersion) o;
    return major == that.major && minor == that.minor;
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor);
  }

  @Override
  public String toString() {
    return toVersionString();
  }

  // Convenience constants for common versions
  public static final ApiVersion V1 = ApiVersion.of(1, 0);
  public static final ApiVersion V2 = ApiVersion.of(2, 0);
  public static final ApiVersion V3 = ApiVersion.of(3, 0);
}
