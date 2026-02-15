package com.marcusprado02.commons.ports.search;

import java.util.*;

/**
 * Represents a search query with filters, sorting, and pagination.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SearchQuery query = SearchQuery.builder()
 *     .query("laptop computer")
 *     .field("name")
 *     .filter("category", "electronics")
 *     .filter("price", ">", 500)
 *     .sortBy("price", SortOrder.ASC)
 *     .from(0)
 *     .size(20)
 *     .build();
 * }</pre>
 */
public record SearchQuery(
    String query,
    List<String> fields,
    Map<String, Object> filters,
    List<SortField> sorting,
    int from,
    int size,
    QueryType queryType,
    Float minScore
) {

  public SearchQuery {
    fields = fields == null ? List.of() : List.copyOf(fields);
    filters = filters == null ? Map.of() : Map.copyOf(filters);
    sorting = sorting == null ? List.of() : List.copyOf(sorting);

    if (from < 0) {
      throw new IllegalArgumentException("From must be >= 0");
    }
    if (size < 1) {
      throw new IllegalArgumentException("Size must be > 0");
    }
    if (size > 10_000) {
      throw new IllegalArgumentException("Size cannot exceed 10,000");
    }
  }

  /**
   * Creates a simple match-all query.
   *
   * @return match-all query
   */
  public static SearchQuery matchAll() {
    return builder().query("*").build();
  }

  /**
   * Creates a simple text query.
   *
   * @param query query text
   * @return search query
   */
  public static SearchQuery of(String query) {
    return builder().query(query).build();
  }

  /**
   * Creates a query builder.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Query execution type. */
  public enum QueryType {
    /** Match query (analyzed text matching) */
    MATCH,
    /** Term query (exact term matching) */
    TERM,
    /** Phrase query (exact phrase matching) */
    PHRASE,
    /** Prefix query */
    PREFIX,
    /** Wildcard query */
    WILDCARD,
    /** Fuzzy query (tolerates typos) */
    FUZZY,
    /** Boolean query (AND/OR combinations) */
    BOOL
  }

  /** Sort field configuration. */
  public record SortField(String field, SortOrder order) {
    public SortField {
      Objects.requireNonNull(field, "Sort field cannot be null");
      Objects.requireNonNull(order, "Sort order cannot be null");
    }
  }

  /** Sort order. */
  public enum SortOrder {
    ASC, DESC
  }

  /** Builder for SearchQuery. */
  public static final class Builder {
    private String query = "*";
    private final List<String> fields = new ArrayList<>();
    private final Map<String, Object> filters = new HashMap<>();
    private final List<SortField> sorting = new ArrayList<>();
    private int from = 0;
    private int size = 10;
    private QueryType queryType = QueryType.MATCH;
    private Float minScore;

    private Builder() {}

    /**
     * Sets the search query text.
     *
     * @param query query text
     * @return this builder
     */
    public Builder query(String query) {
      this.query = query;
      return this;
    }

    /**
     * Adds a field to search in.
     *
     * @param field field name
     * @return this builder
     */
    public Builder field(String field) {
      this.fields.add(field);
      return this;
    }

    /**
     * Sets multiple fields to search in.
     *
     * @param fields field names
     * @return this builder
     */
    public Builder fields(List<String> fields) {
      if (fields != null) {
        this.fields.addAll(fields);
      }
      return this;
    }

    /**
     * Adds an equality filter.
     *
     * @param field field name
     * @param value filter value
     * @return this builder
     */
    public Builder filter(String field, Object value) {
      this.filters.put(field, value);
      return this;
    }

    /**
     * Adds multiple filters.
     *
     * @param filters filter map
     * @return this builder
     */
    public Builder filters(Map<String, Object> filters) {
      if (filters != null) {
        this.filters.putAll(filters);
      }
      return this;
    }

    /**
     * Adds a sort field.
     *
     * @param field field to sort by
     * @param order sort order
     * @return this builder
     */
    public Builder sortBy(String field, SortOrder order) {
      this.sorting.add(new SortField(field, order));
      return this;
    }

    /**
     * Sets pagination offset.
     *
     * @param from offset (0-based)
     * @return this builder
     */
    public Builder from(int from) {
      this.from = from;
      return this;
    }

    /**
     * Sets page size.
     *
     * @param size number of results to return
     * @return this builder
     */
    public Builder size(int size) {
      this.size = size;
      return this;
    }

    /**
     * Sets query type.
     *
     * @param queryType query execution type
     * @return this builder
     */
    public Builder queryType(QueryType queryType) {
      this.queryType = queryType;
      return this;
    }

    /**
     * Sets minimum score threshold.
     *
     * @param minScore minimum relevance score
     * @return this builder
     */
    public Builder minScore(Float minScore) {
      this.minScore = minScore;
      return this;
    }

    /**
     * Builds the SearchQuery.
     *
     * @return new SearchQuery instance
     */
    public SearchQuery build() {
      return new SearchQuery(
          query,
          fields,
          filters,
          sorting,
          from,
          size,
          queryType,
          minScore
      );
    }
  }
}
