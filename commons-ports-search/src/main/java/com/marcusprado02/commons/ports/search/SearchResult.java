package com.marcusprado02.commons.ports.search;

import java.util.List;
import java.util.Objects;

/**
 * Search response containing hits and metadata.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SearchResult result = searchPort.search("products", query).getOrNull();
 *
 * System.out.println("Found " + result.totalHits() + " results");
 * for (Document doc : result.hits()) {
 *     System.out.println(doc.id() + ": " + doc.source());
 * }
 * }</pre>
 */
public record SearchResult(
    List<Document> hits,
    long totalHits,
    float maxScore,
    long tookMillis
) {

  public SearchResult {
    Objects.requireNonNull(hits, "Hits cannot be null");
    hits = List.copyOf(hits);

    if (totalHits < 0) {
      throw new IllegalArgumentException("Total hits must be >= 0");
    }
    if (tookMillis < 0) {
      throw new IllegalArgumentException("Took millis must be >= 0");
    }
  }

  /**
   * Creates an empty search result.
   *
   * @return empty result
   */
  public static SearchResult empty() {
    return new SearchResult(List.of(), 0, 0f, 0);
  }

  /**
   * Checks if there are any hits.
   *
   * @return true if hits exist
   */
  public boolean hasHits() {
    return !hits.isEmpty();
  }

  /**
   * Gets the number of returned hits.
   *
   * @return hit count
   */
  public int hitCount() {
    return hits.size();
  }
}
