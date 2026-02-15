package com.marcusprado02.commons.ports.search;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregation results containing buckets and metrics.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Result<AggregationResult> aggResult = searchPort.aggregate(
 *     "products",
 *     SearchQuery.matchAll(),
 *     List.of(Aggregation.terms("categories", "category"))
 * );
 *
 * aggResult.ifSuccess(result -> {
 *     for (Bucket bucket : result.buckets()) {
 *         System.out.println(bucket.key() + ": " + bucket.docCount());
 *     }
 * });
 * }</pre>
 */
public record AggregationResult(
    String name,
    List<Bucket> buckets,
    Map<String, Double> metrics
) {

  public AggregationResult {
    Objects.requireNonNull(name, "Aggregation name cannot be null");
    buckets = buckets == null ? List.of() : List.copyOf(buckets);
    metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
  }

  /**
   * Creates an empty aggregation result.
   *
   * @param name aggregation name
   * @return empty result
   */
  public static AggregationResult empty(String name) {
    return new AggregationResult(name, List.of(), Map.of());
  }

  /**
   * Gets a metric value.
   *
   * @param metricName metric name
   * @return metric value or null
   */
  public Double getMetric(String metricName) {
    return metrics.get(metricName);
  }

  /**
   * Checks if there are any buckets.
   *
   * @return true if buckets exist
   */
  public boolean hasBuckets() {
    return !buckets.isEmpty();
  }

  /**
   * Gets the number of buckets.
   *
   * @return bucket count
   */
  public int bucketCount() {
    return buckets.size();
  }

  /**
   * Aggregation bucket (for terms, histogram, range aggregations).
   *
   * @param key bucket key
   * @param docCount number of documents in bucket
   * @param subAggregations sub-aggregation results
   */
  public record Bucket(
      String key,
      long docCount,
      Map<String, AggregationResult> subAggregations
  ) {
    public Bucket {
      Objects.requireNonNull(key, "Bucket key cannot be null");
      subAggregations = subAggregations == null ? Map.of() : Map.copyOf(subAggregations);

      if (docCount < 0) {
        throw new IllegalArgumentException("Doc count must be >= 0");
      }
    }

    /**
     * Creates a simple bucket without sub-aggregations.
     *
     * @param key bucket key
     * @param docCount document count
     * @return new Bucket
     */
    public static Bucket of(String key, long docCount) {
      return new Bucket(key, docCount, Map.of());
    }
  }
}
