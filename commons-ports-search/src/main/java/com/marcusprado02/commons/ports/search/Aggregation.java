package com.marcusprado02.commons.ports.search;

import java.util.Objects;

/**
 * Represents an aggregation to execute on search results.
 *
 * <p>Aggregations allow grouping and computing metrics over search data.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Terms aggregation - group by category
 * Aggregation categoryAgg = Aggregation.terms("categories", "category");
 *
 * // Metric aggregation - average price
 * Aggregation avgPriceAgg = Aggregation.avg("avg_price", "price");
 *
 * // Range aggregation - price ranges
 * Aggregation priceRanges = Aggregation.builder()
 *     .name("price_ranges")
 *     .type(AggregationType.RANGE)
 *     .field("price")
 *     .build();
 * }</pre>
 */
public record Aggregation(
    String name,
    AggregationType type,
    String field,
    Integer size
) {

  public Aggregation {
    Objects.requireNonNull(name, "Aggregation name cannot be null");
    Objects.requireNonNull(type, "Aggregation type cannot be null");
  }

  /**
   * Creates a terms aggregation (group by field values).
   *
   * @param name aggregation name
   * @param field field to aggregate on
   * @return new Aggregation
   */
  public static Aggregation terms(String name, String field) {
    return new Aggregation(name, AggregationType.TERMS, field, 10);
  }

  /**
   * Creates a terms aggregation with custom size.
   *
   * @param name aggregation name
   * @param field field to aggregate on
   * @param size number of top terms to return
   * @return new Aggregation
   */
  public static Aggregation terms(String name, String field, int size) {
    return new Aggregation(name, AggregationType.TERMS, field, size);
  }

  /**
   * Creates an average metric aggregation.
   *
   * @param name aggregation name
   * @param field numeric field to compute average
   * @return new Aggregation
   */
  public static Aggregation avg(String name, String field) {
    return new Aggregation(name, AggregationType.AVG, field, null);
  }

  /**
   * Creates a sum metric aggregation.
   *
   * @param name aggregation name
   * @param field numeric field to sum
   * @return new Aggregation
   */
  public static Aggregation sum(String name, String field) {
    return new Aggregation(name, AggregationType.SUM, field, null);
  }

  /**
   * Creates a min metric aggregation.
   *
   * @param name aggregation name
   * @param field numeric field to find minimum
   * @return new Aggregation
   */
  public static Aggregation min(String name, String field) {
    return new Aggregation(name, AggregationType.MIN, field, null);
  }

  /**
   * Creates a max metric aggregation.
   *
   * @param name aggregation name
   * @param field numeric field to find maximum
   * @return new Aggregation
   */
  public static Aggregation max(String name, String field) {
    return new Aggregation(name, AggregationType.MAX, field, null);
  }

  /**
   * Creates a cardinality aggregation (count distinct values).
   *
   * @param name aggregation name
   * @param field field to count distinct values
   * @return new Aggregation
   */
  public static Aggregation cardinality(String name, String field) {
    return new Aggregation(name, AggregationType.CARDINALITY, field, null);
  }

  /**
   * Creates an aggregation builder.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Aggregation types. */
  public enum AggregationType {
    /** Group by field values (bucket aggregation) */
    TERMS,
    /** Date histogram (bucket aggregation) */
    DATE_HISTOGRAM,
    /** Numeric ranges (bucket aggregation) */
    RANGE,
    /** Average metric */
    AVG,
    /** Sum metric */
    SUM,
    /** Minimum value */
    MIN,
    /** Maximum value */
    MAX,
    /** Count of documents */
    COUNT,
    /** Unique value count */
    CARDINALITY,
    /** Statistics (min, max, avg, sum) */
    STATS
  }

  /** Builder for Aggregation. */
  public static final class Builder {
    private String name;
    private AggregationType type;
    private String field;
    private Integer size;

    private Builder() {}

    /**
     * Sets the aggregation name.
     *
     * @param name aggregation name
     * @return this builder
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the aggregation type.
     *
     * @param type aggregation type
     * @return this builder
     */
    public Builder type(AggregationType type) {
      this.type = type;
      return this;
    }

    /**
     * Sets the field to aggregate on.
     *
     * @param field field name
     * @return this builder
     */
    public Builder field(String field) {
      this.field = field;
      return this;
    }

    /**
     * Sets the size (for bucket aggregations).
     *
     * @param size number of buckets
     * @return this builder
     */
    public Builder size(Integer size) {
      this.size = size;
      return this;
    }

    /**
     * Builds the Aggregation.
     *
     * @return new Aggregation instance
     */
    public Aggregation build() {
      return new Aggregation(name, type, field, size);
    }
  }
}
