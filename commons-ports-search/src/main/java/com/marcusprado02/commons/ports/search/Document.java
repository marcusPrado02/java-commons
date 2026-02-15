package com.marcusprado02.commons.ports.search;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a search document with ID and source fields.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Document doc = Document.builder()
 *     .id("product-123")
 *     .field("name", "Laptop")
 *     .field("price", 999.99)
 *     .field("category", "Electronics")
 *     .timestamp(Instant.now())
 *     .build();
 *
 * // Or using factory method
 * Document simple = Document.of("id-1", Map.of("field", "value"));
 * }</pre>
 */
public record Document(
    String id,
    Map<String, Object> source,
    Float score,
    Instant timestamp
) {

  public Document {
    Objects.requireNonNull(id, "Document ID cannot be null");
    source = source == null ? Map.of() : Map.copyOf(source);
  }

  /**
   * Creates a document with ID and source fields.
   *
   * @param id document ID
   * @param source source fields
   * @return new Document
   */
  public static Document of(String id, Map<String, Object> source) {
    return new Document(id, source, null, null);
  }

  /**
   * Creates a document builder.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets a field value from the source.
   *
   * @param field field name
   * @return field value or null
   */
  public Object getField(String field) {
    return source.get(field);
  }

  /**
   * Gets a field value with type casting.
   *
   * @param field field name
   * @param type expected type
   * @param <T> value type
   * @return typed field value or null
   */
  @SuppressWarnings("unchecked")
  public <T> T getField(String field, Class<T> type) {
    Object value = source.get(field);
    if (value == null || !type.isInstance(value)) {
      return null;
    }
    return (T) value;
  }

  /** Builder for Document. */
  public static final class Builder {
    private String id;
    private final Map<String, Object> source = new HashMap<>();
    private Float score;
    private Instant timestamp;

    private Builder() {}

    /**
     * Sets the document ID.
     *
     * @param id document ID
     * @return this builder
     */
    public Builder id(String id) {
      this.id = id;
      return this;
    }

    /**
     * Sets all source fields.
     *
     * @param source source fields
     * @return this builder
     */
    public Builder source(Map<String, Object> source) {
      if (source != null) {
        this.source.putAll(source);
      }
      return this;
    }

    /**
     * Sets a single field value.
     *
     * @param name field name
     * @param value field value
     * @return this builder
     */
    public Builder field(String name, Object value) {
      this.source.put(name, value);
      return this;
    }

    /**
     * Sets the search score.
     *
     * @param score relevance score
     * @return this builder
     */
    public Builder score(Float score) {
      this.score = score;
      return this;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp document timestamp
     * @return this builder
     */
    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    /**
     * Builds the Document.
     *
     * @return new Document instance
     */
    public Document build() {
      return new Document(id, source, score, timestamp);
    }
  }
}
