package com.marcusprado02.commons.ports.search;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import java.util.Map;

/**
 * Port for full-text search and indexing operations.
 *
 * <p>Provides abstraction for search engines like Elasticsearch, OpenSearch, Solr, etc.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SearchPort searchPort = ...;
 *
 * // Index a document
 * Document doc = Document.of("product-1", Map.of("name", "Laptop", "price", 999));
 * searchPort.index("products", doc);
 *
 * // Search documents
 * SearchQuery query = SearchQuery.builder()
 *     .query("laptop")
 *     .field("name")
 *     .size(10)
 *     .build();
 * Result<SearchResult> result = searchPort.search("products", query);
 * }</pre>
 */
public interface SearchPort extends AutoCloseable {

  /**
   * Indexes a single document.
   *
   * @param index index name
   * @param document document to index
   * @return Result containing the indexed document ID
   */
  Result<String> index(String index, Document document);

  /**
   * Indexes multiple documents in a single bulk operation.
   *
   * @param index index name
   * @param documents documents to index
   * @return Result containing bulk indexing response
   */
  Result<BulkIndexResult> bulkIndex(String index, List<Document> documents);

  /**
   * Searches documents in an index.
   *
   * @param index index name
   * @param query search query
   * @return Result containing search results
   */
  Result<SearchResult> search(String index, SearchQuery query);

  /**
   * Searches documents across multiple indices.
   *
   * @param indices index names
   * @param query search query
   * @return Result containing search results
   */
  Result<SearchResult> search(List<String> indices, SearchQuery query);

  /**
   * Gets a document by ID.
   *
   * @param index index name
   * @param id document ID
   * @return Result containing the document if found
   */
  Result<Document> get(String index, String id);

  /**
   * Deletes a document by ID.
   *
   * @param index index name
   * @param id document ID
   * @return Result indicating success or failure
   */
  Result<Void> delete(String index, String id);

  /**
   * Updates a document by ID.
   *
   * @param index index name
   * @param id document ID
   * @param updates partial document updates
   * @return Result indicating success or failure
   */
  Result<Void> update(String index, String id, Map<String, Object> updates);

  /**
   * Creates an index with optional settings and mappings.
   *
   * @param index index name
   * @param settings index settings
   * @param mappings field mappings
   * @return Result indicating success or failure
   */
  Result<Void> createIndex(String index, Map<String, Object> settings, Map<String, Object> mappings);

  /**
   * Deletes an index.
   *
   * @param index index name
   * @return Result indicating success or failure
   */
  Result<Void> deleteIndex(String index);

  /**
   * Checks if an index exists.
   *
   * @param index index name
   * @return Result containing true if index exists
   */
  Result<Boolean> indexExists(String index);

  /**
   * Refreshes an index to make recent changes searchable.
   *
   * @param index index name
   * @return Result indicating success or failure
   */
  Result<Void> refresh(String index);

  /**
   * Executes an aggregation query.
   *
   * @param index index name
   * @param query search query (can be match-all)
   * @param aggregations aggregations to execute
   * @return Result containing aggregation results
   */
  Result<AggregationResult> aggregate(String index, SearchQuery query, List<Aggregation> aggregations);

  @Override
  void close();

  /**
   * Bulk indexing result with success/failure counts.
   *
   * @param totalDocs total documents processed
   * @param successCount successfully indexed documents
   * @param failureCount failed documents
   * @param errors list of errors for failed documents
   */
  record BulkIndexResult(
      int totalDocs,
      int successCount,
      int failureCount,
      List<String> errors
  ) {}
}
