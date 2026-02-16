package com.marcusprado02.commons.adapters.metrics.prometheus;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.*;

/**
 * Utility class for building metric labels (tags) in a type-safe manner.
 *
 * <p>Prometheus labels are key-value pairs that allow filtering and aggregation of metrics.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Simple labels
 * MetricLabels labels = MetricLabels.of("method", "GET", "status", "200");
 *
 * // Builder pattern
 * MetricLabels labels = MetricLabels.builder()
 *     .label("method", "GET")
 *     .label("status", "200")
 *     .label("endpoint", "/api/users")
 *     .build();
 *
 * // Combining labels
 * MetricLabels baseLabels = MetricLabels.of("service", "user-service");
 * MetricLabels combined = baseLabels.with("method", "GET");
 * }</pre>
 */
public final class MetricLabels {

  private final Tags tags;

  private MetricLabels(Tags tags) {
    this.tags = tags;
  }

  /**
   * Creates empty labels.
   *
   * @return empty labels
   */
  public static MetricLabels empty() {
    return new MetricLabels(Tags.empty());
  }

  /**
   * Creates labels from key-value pairs.
   *
   * @param keyValues key-value pairs (key1, value1, key2, value2, ...)
   * @return metric labels
   * @throws IllegalArgumentException if keyValues has odd length
   */
  public static MetricLabels of(String... keyValues) {
    if (keyValues.length % 2 != 0) {
      throw new IllegalArgumentException("keyValues must have even length");
    }
    return new MetricLabels(Tags.of(keyValues));
  }

  /**
   * Creates labels from a single key-value pair.
   *
   * @param key the label key
   * @param value the label value
   * @return metric labels
   */
  public static MetricLabels of(String key, String value) {
    return new MetricLabels(Tags.of(key, value));
  }

  /**
   * Creates labels from a map.
   *
   * @param labels the labels map
   * @return metric labels
   */
  public static MetricLabels of(Map<String, String> labels) {
    List<Tag> tagList = new ArrayList<>();
    labels.forEach((k, v) -> tagList.add(Tag.of(k, v)));
    return new MetricLabels(Tags.of(tagList));
  }

  /**
   * Creates a builder for constructing labels.
   *
   * @return labels builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns a new MetricLabels instance with an additional label.
   *
   * @param key the label key
   * @param value the label value
   * @return new metric labels with the additional label
   */
  public MetricLabels with(String key, String value) {
    return new MetricLabels(tags.and(key, value));
  }

  /**
   * Returns a new MetricLabels instance with additional labels.
   *
   * @param other other labels to combine
   * @return new metric labels with combined labels
   */
  public MetricLabels with(MetricLabels other) {
    return new MetricLabels(tags.and(other.tags));
  }

  /**
   * Converts labels to Micrometer Tags.
   *
   * @return Micrometer tags
   */
  public Tags toTags() {
    return tags;
  }

  /**
   * Converts labels to a map.
   *
   * @return labels as map
   */
  public Map<String, String> toMap() {
    Map<String, String> map = new HashMap<>();
    tags.forEach(tag -> map.put(tag.getKey(), tag.getValue()));
    return map;
  }

  /**
   * Returns the number of labels.
   *
   * @return label count
   */
  public int size() {
    int count = 0;
    for (Tag ignored : tags) {
      count++;
    }
    return count;
  }

  /**
   * Checks if labels are empty.
   *
   * @return true if no labels, false otherwise
   */
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public String toString() {
    return tags.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MetricLabels that = (MetricLabels) o;
    return Objects.equals(tags, that.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tags);
  }

  /** Builder for constructing metric labels. */
  public static final class Builder {
    private final List<Tag> tags = new ArrayList<>();

    private Builder() {}

    /**
     * Adds a label.
     *
     * @param key the label key
     * @param value the label value
     * @return this builder
     */
    public Builder label(String key, String value) {
      tags.add(Tag.of(key, value));
      return this;
    }

    /**
     * Adds a label if the value is not null.
     *
     * @param key the label key
     * @param value the label value (may be null)
     * @return this builder
     */
    public Builder labelIfPresent(String key, String value) {
      if (value != null) {
        tags.add(Tag.of(key, value));
      }
      return this;
    }

    /**
     * Adds multiple labels from a map.
     *
     * @param labels the labels map
     * @return this builder
     */
    public Builder labels(Map<String, String> labels) {
      labels.forEach((k, v) -> tags.add(Tag.of(k, v)));
      return this;
    }

    /**
     * Builds the metric labels.
     *
     * @return metric labels
     */
    public MetricLabels build() {
      return new MetricLabels(Tags.of(tags));
    }
  }

  // ===== Common Label Factories =====

  /**
   * Creates labels for HTTP metrics.
   *
   * @param method HTTP method (GET, POST, etc.)
   * @param status HTTP status code
   * @return metric labels
   */
  public static MetricLabels http(String method, int status) {
    return of("method", method, "status", String.valueOf(status));
  }

  /**
   * Creates labels for HTTP metrics with endpoint.
   *
   * @param method HTTP method
   * @param status HTTP status code
   * @param endpoint the endpoint path
   * @return metric labels
   */
  public static MetricLabels http(String method, int status, String endpoint) {
    return of("method", method, "status", String.valueOf(status), "endpoint", endpoint);
  }

  /**
   * Creates labels for database metrics.
   *
   * @param operation database operation (SELECT, INSERT, UPDATE, DELETE)
   * @param table table name
   * @return metric labels
   */
  public static MetricLabels database(String operation, String table) {
    return of("operation", operation, "table", table);
  }

  /**
   * Creates labels for messaging metrics.
   *
   * @param topic topic or queue name
   * @param operation operation (send, receive, process)
   * @return metric labels
   */
  public static MetricLabels messaging(String topic, String operation) {
    return of("topic", topic, "operation", operation);
  }

  /**
   * Creates labels for cache metrics.
   *
   * @param cache cache name
   * @param result cache result (hit, miss)
   * @return metric labels
   */
  public static MetricLabels cache(String cache, String result) {
    return of("cache", cache, "result", result);
  }

  /**
   * Creates labels for service-to-service call metrics.
   *
   * @param service target service name
   * @param operation operation name
   * @return metric labels
   */
  public static MetricLabels service(String service, String operation) {
    return of("service", service, "operation", operation);
  }
}
