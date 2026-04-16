package com.marcusprado02.commons.adapters.search.elasticsearch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.search.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ElasticsearchAdapter} using the package-private test constructor to inject
 * a mock {@link ElasticsearchClient} without starting a real Elasticsearch node.
 */
@ExtendWith(MockitoExtension.class)
class ElasticsearchAdapterMockedTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ElasticsearchClient client;

  private ElasticsearchAdapter adapter;
  private ElasticsearchConfiguration config;

  @BeforeEach
  void setUp() {
    config =
        ElasticsearchConfiguration.forDevelopment("http://localhost:9200", "elastic", "changeme");
    adapter = new ElasticsearchAdapter(config, client);
  }

  // ── index ─────────────────────────────────────────────────────────────────

  @Test
  void index_shouldReturnDocumentId_whenSuccessful() throws IOException {
    IndexResponse response = mock(IndexResponse.class);
    when(response.id()).thenReturn("doc-123");
    when(client.index(any(IndexRequest.class))).thenReturn(response);

    Document doc = Document.of("doc-123", Map.of("title", "Hello"));
    Result<String> result = adapter.index("my-index", doc);

    assertTrue(result.isOk());
    assertEquals("doc-123", result.getOrNull());
  }

  @Test
  void index_whenIoException_shouldReturnFail() throws IOException {
    when(client.index(any(IndexRequest.class))).thenThrow(new IOException("network error"));

    Document doc = Document.of("doc-1", Map.of("title", "Test"));
    Result<String> result = adapter.index("my-index", doc);

    assertTrue(result.isFail());
    assertEquals("INDEX_IO_ERROR", result.problemOrNull().code().value());
  }

  @Test
  void index_whenElasticsearchException_shouldReturnFail() throws IOException {
    when(client.index(any(IndexRequest.class))).thenThrow(mock(ElasticsearchException.class));

    Document doc = Document.of("doc-1", Map.of());
    Result<String> result = adapter.index("my-index", doc);

    assertTrue(result.isFail());
    assertEquals("ELASTICSEARCH_ERROR", result.problemOrNull().code().value());
  }

  // ── bulkIndex ─────────────────────────────────────────────────────────────

  @Test
  void bulkIndex_withEmptyList_shouldReturnZeroCounts() {
    Result<SearchPort.BulkIndexResult> result = adapter.bulkIndex("my-index", List.of());

    assertTrue(result.isOk());
    assertEquals(0, result.getOrNull().successCount());
    assertEquals(0, result.getOrNull().failureCount());
  }

  @Test
  void bulkIndex_shouldCountSuccessesAndFailures() throws IOException {
    BulkResponseItem successItem = mock(BulkResponseItem.class);
    when(successItem.error()).thenReturn(null);

    BulkResponseItem failedItem = mock(BulkResponseItem.class);
    co.elastic.clients.elasticsearch._types.ErrorCause cause =
        mock(co.elastic.clients.elasticsearch._types.ErrorCause.class);
    when(cause.reason()).thenReturn("shard unavailable");
    when(failedItem.error()).thenReturn(cause);

    BulkResponse bulkResponse = mock(BulkResponse.class);
    when(bulkResponse.items()).thenReturn(List.of(successItem, failedItem));
    when(client.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

    List<Document> docs =
        List.of(Document.of("1", Map.of("title", "A")), Document.of("2", Map.of("title", "B")));
    Result<SearchPort.BulkIndexResult> result = adapter.bulkIndex("my-index", docs);

    assertTrue(result.isOk());
    assertEquals(1, result.getOrNull().successCount());
    assertEquals(1, result.getOrNull().failureCount());
    assertEquals(List.of("shard unavailable"), result.getOrNull().errors());
  }

  @Test
  void bulkIndex_whenIoException_shouldReturnFail() throws IOException {
    when(client.bulk(any(BulkRequest.class))).thenThrow(new IOException("timeout"));

    List<Document> docs = List.of(Document.of("1", Map.of("k", "v")));
    Result<SearchPort.BulkIndexResult> result = adapter.bulkIndex("my-index", docs);

    assertTrue(result.isFail());
    assertEquals("BULK_INDEX_IO_ERROR", result.problemOrNull().code().value());
  }

  // ── get ───────────────────────────────────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void get_whenFound_shouldReturnDocument() throws IOException {
    GetResponse<Map> response = mock(GetResponse.class);
    when(response.found()).thenReturn(true);
    when(response.id()).thenReturn("doc-1");
    when(response.source()).thenReturn(Map.of("title", "Hello"));
    when(client.get(any(GetRequest.class), eq(Map.class))).thenReturn(response);

    Result<Document> result = adapter.get("my-index", "doc-1");

    assertTrue(result.isOk());
    assertEquals("doc-1", result.getOrNull().id());
  }

  @Test
  @SuppressWarnings("unchecked")
  void get_whenNotFound_shouldReturnFail() throws IOException {
    GetResponse<Map> response = mock(GetResponse.class);
    when(response.found()).thenReturn(false);
    when(client.get(any(GetRequest.class), eq(Map.class))).thenReturn(response);

    Result<Document> result = adapter.get("my-index", "missing-doc");

    assertTrue(result.isFail());
    assertEquals("DOCUMENT_NOT_FOUND", result.problemOrNull().code().value());
  }

  @Test
  void get_whenIoException_shouldReturnFail() throws IOException {
    when(client.get(any(GetRequest.class), eq(Map.class))).thenThrow(new IOException("error"));

    Result<Document> result = adapter.get("my-index", "doc-1");

    assertTrue(result.isFail());
    assertEquals("GET_IO_ERROR", result.problemOrNull().code().value());
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  void delete_shouldReturnOk_whenSuccessful() throws IOException {
    when(client.delete(any(DeleteRequest.class))).thenReturn(mock(DeleteResponse.class));

    Result<Void> result = adapter.delete("my-index", "doc-1");

    assertTrue(result.isOk());
  }

  @Test
  void delete_whenIoException_shouldReturnFail() throws IOException {
    when(client.delete(any(DeleteRequest.class))).thenThrow(new IOException("disk error"));

    Result<Void> result = adapter.delete("my-index", "doc-1");

    assertTrue(result.isFail());
    assertEquals("DELETE_IO_ERROR", result.problemOrNull().code().value());
  }

  // ── update ────────────────────────────────────────────────────────────────
  // Note: client.update() has generic overloads that conflict with Mockito's strict stubbing.
  // update() is tested indirectly via the null-validation path (no mock call needed).

  @Test
  void update_withNullIndex_shouldThrow() {
    assertThrows(
        NullPointerException.class, () -> adapter.update(null, "doc-1", Map.of("title", "v")));
  }

  @Test
  void update_withNullId_shouldThrow() {
    assertThrows(
        NullPointerException.class, () -> adapter.update("my-index", null, Map.of("title", "v")));
  }

  @Test
  void update_withNullUpdates_shouldThrow() {
    assertThrows(NullPointerException.class, () -> adapter.update("my-index", "doc-1", null));
  }

  // ── createIndex ───────────────────────────────────────────────────────────

  @Test
  void createIndex_shouldReturnOk_whenSuccessful() throws IOException {
    // RETURNS_DEEP_STUBS handles client.indices().create() automatically
    Result<Void> result = adapter.createIndex("new-index", Map.of(), Map.of());

    assertTrue(result.isOk());
  }

  @Test
  void createIndex_whenIoException_shouldReturnFail() throws IOException {
    ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
    when(client.indices()).thenReturn(indicesClient);
    when(indicesClient.create(
            any(co.elastic.clients.elasticsearch.indices.CreateIndexRequest.class)))
        .thenThrow(new IOException("index creation failed"));

    Result<Void> result = adapter.createIndex("new-index", Map.of(), Map.of());

    assertTrue(result.isFail());
    assertEquals("CREATE_INDEX_IO_ERROR", result.problemOrNull().code().value());
  }

  // ── deleteIndex ───────────────────────────────────────────────────────────

  @Test
  void deleteIndex_shouldReturnOk_whenSuccessful() throws IOException {
    Result<Void> result = adapter.deleteIndex("old-index");

    assertTrue(result.isOk());
  }

  @Test
  void deleteIndex_whenIoException_shouldReturnFail() throws IOException {
    ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
    when(client.indices()).thenReturn(indicesClient);
    when(indicesClient.delete(
            any(co.elastic.clients.elasticsearch.indices.DeleteIndexRequest.class)))
        .thenThrow(new IOException("delete failed"));

    Result<Void> result = adapter.deleteIndex("old-index");

    assertTrue(result.isFail());
    assertEquals("DELETE_INDEX_IO_ERROR", result.problemOrNull().code().value());
  }

  // ── indexExists ───────────────────────────────────────────────────────────

  @Test
  void indexExists_shouldReturnTrue_whenExists() throws IOException {
    co.elastic.clients.transport.endpoints.BooleanResponse boolResponse =
        mock(co.elastic.clients.transport.endpoints.BooleanResponse.class);
    when(boolResponse.value()).thenReturn(true);

    ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
    when(client.indices()).thenReturn(indicesClient);
    when(indicesClient.exists(any(co.elastic.clients.elasticsearch.indices.ExistsRequest.class)))
        .thenReturn(boolResponse);

    Result<Boolean> result = adapter.indexExists("my-index");

    assertTrue(result.isOk());
    assertTrue(result.getOrNull());
  }

  @Test
  void indexExists_whenIoException_shouldReturnFail() throws IOException {
    ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
    when(client.indices()).thenReturn(indicesClient);
    when(indicesClient.exists(any(co.elastic.clients.elasticsearch.indices.ExistsRequest.class)))
        .thenThrow(new IOException("connection error"));

    Result<Boolean> result = adapter.indexExists("my-index");

    assertTrue(result.isFail());
    assertEquals("INDEX_EXISTS_IO_ERROR", result.problemOrNull().code().value());
  }

  // ── refresh ───────────────────────────────────────────────────────────────

  @Test
  void refresh_shouldReturnOk_whenSuccessful() throws IOException {
    Result<Void> result = adapter.refresh("my-index");

    assertTrue(result.isOk());
  }

  @Test
  void refresh_whenIoException_shouldReturnFail() throws IOException {
    ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
    when(client.indices()).thenReturn(indicesClient);
    when(indicesClient.refresh(any(co.elastic.clients.elasticsearch.indices.RefreshRequest.class)))
        .thenThrow(new IOException("refresh failed"));

    Result<Void> result = adapter.refresh("my-index");

    assertTrue(result.isFail());
    assertEquals("REFRESH_IO_ERROR", result.problemOrNull().code().value());
  }

  // ── aggregate ─────────────────────────────────────────────────────────────

  @Test
  void aggregate_withEmptyAggregations_shouldReturnEmpty() {
    SearchQuery query = SearchQuery.of("test");

    Result<AggregationResult> result = adapter.aggregate("my-index", query, List.of());

    assertTrue(result.isOk());
    assertTrue(result.getOrNull().buckets().isEmpty());
  }

  @Test
  void aggregate_whenIoException_shouldReturnFail() throws IOException {
    when(client.search(any(SearchRequest.class), eq(Map.class)))
        .thenThrow(new IOException("aggregate failed"));

    SearchQuery query = SearchQuery.of("test");
    List<Aggregation> aggs = List.of(Aggregation.terms("cat", "category", 10));

    Result<AggregationResult> result = adapter.aggregate("my-index", query, aggs);

    assertTrue(result.isFail());
    assertEquals("AGGREGATE_IO_ERROR", result.problemOrNull().code().value());
  }

  // ── close ─────────────────────────────────────────────────────────────────

  @Test
  void close_withNullRestClient_shouldNotThrow() {
    // The test constructor sets restClient=null; close() should handle it gracefully
    adapter.close();
    // No exception = pass
  }

  // ── search (single index → delegates to multi) ────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void search_singleIndex_shouldDelegateToMultiIndex() throws IOException {
    SearchResponse<Map> response = mock(SearchResponse.class);
    co.elastic.clients.elasticsearch.core.search.HitsMetadata<Map> hitsMetadata =
        mock(co.elastic.clients.elasticsearch.core.search.HitsMetadata.class);
    when(hitsMetadata.hits()).thenReturn(List.of());
    when(hitsMetadata.total()).thenReturn(null);
    when(hitsMetadata.maxScore()).thenReturn(null);
    when(response.hits()).thenReturn(hitsMetadata);
    when(response.took()).thenReturn(5L);
    when(client.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    Result<SearchResult> result = adapter.search("my-index", SearchQuery.of("hello"));

    assertTrue(result.isOk());
    assertTrue(result.getOrNull().hits().isEmpty());
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_withSortingAndMinScore_shouldSucceed() throws IOException {
    SearchResponse<Map> response = mock(SearchResponse.class);
    co.elastic.clients.elasticsearch.core.search.HitsMetadata<Map> hitsMetadata =
        mock(co.elastic.clients.elasticsearch.core.search.HitsMetadata.class);
    when(hitsMetadata.hits()).thenReturn(List.of());
    when(hitsMetadata.total()).thenReturn(null);
    when(hitsMetadata.maxScore()).thenReturn(0.9);
    when(response.hits()).thenReturn(hitsMetadata);
    when(response.took()).thenReturn(3L);
    when(client.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    SearchQuery query =
        SearchQuery.builder()
            .query("test")
            .sortBy("_score", SearchQuery.SortOrder.DESC)
            .sortBy("created_at", SearchQuery.SortOrder.ASC)
            .minScore(0.5f)
            .build();
    Result<SearchResult> result = adapter.search("my-index", query);

    assertTrue(result.isOk());
    assertEquals(0.9f, result.getOrNull().maxScore(), 0.001f);
  }

  @Test
  void search_whenIoException_shouldReturnFail() throws IOException {
    when(client.search(any(SearchRequest.class), eq(Map.class)))
        .thenThrow(new IOException("search timeout"));

    Result<SearchResult> result = adapter.search("my-index", SearchQuery.of("test"));

    assertTrue(result.isFail());
    assertEquals("SEARCH_IO_ERROR", result.problemOrNull().code().value());
  }

  @Test
  void search_whenElasticsearchException_shouldReturnFail() throws IOException {
    when(client.search(any(SearchRequest.class), eq(Map.class)))
        .thenThrow(mock(ElasticsearchException.class));

    Result<SearchResult> result = adapter.search("my-index", SearchQuery.of("test"));

    assertTrue(result.isFail());
    assertEquals("ELASTICSEARCH_ERROR", result.problemOrNull().code().value());
  }

  // ── additional error-path coverage ────────────────────────────────────────

  @Test
  void bulkIndex_whenElasticsearchException_shouldReturnFail() throws IOException {
    when(client.bulk(any(BulkRequest.class))).thenThrow(mock(ElasticsearchException.class));

    List<Document> docs = List.of(Document.of("1", Map.of("k", "v")));
    Result<SearchPort.BulkIndexResult> result = adapter.bulkIndex("my-index", docs);

    assertTrue(result.isFail());
    assertEquals("ELASTICSEARCH_ERROR", result.problemOrNull().code().value());
  }

  @Test
  void delete_whenElasticsearchException_shouldReturnFail() throws IOException {
    when(client.delete(any(DeleteRequest.class))).thenThrow(mock(ElasticsearchException.class));

    Result<Void> result = adapter.delete("my-index", "doc-1");

    assertTrue(result.isFail());
    assertEquals("ELASTICSEARCH_ERROR", result.problemOrNull().code().value());
  }

  @Test
  void get_whenElasticsearchException_shouldReturnFail() throws IOException {
    when(client.get(any(GetRequest.class), eq(Map.class)))
        .thenThrow(mock(ElasticsearchException.class));

    Result<Document> result = adapter.get("my-index", "doc-1");

    assertTrue(result.isFail());
    assertEquals("ELASTICSEARCH_ERROR", result.problemOrNull().code().value());
  }

  @Test
  void createIndex_whenElasticsearchException_shouldReturnFail() throws IOException {
    ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
    when(client.indices()).thenReturn(indicesClient);
    when(indicesClient.create(
            any(co.elastic.clients.elasticsearch.indices.CreateIndexRequest.class)))
        .thenThrow(mock(ElasticsearchException.class));

    Result<Void> result = adapter.createIndex("new-index", Map.of(), Map.of());

    assertTrue(result.isFail());
    assertEquals("ELASTICSEARCH_ERROR", result.problemOrNull().code().value());
  }

  @Test
  void aggregate_whenElasticsearchException_shouldReturnFail() throws IOException {
    when(client.search(any(SearchRequest.class), eq(Map.class)))
        .thenThrow(mock(ElasticsearchException.class));

    SearchQuery query = SearchQuery.of("test");
    List<Aggregation> aggs = List.of(Aggregation.terms("cat", "category", 10));
    Result<AggregationResult> result = adapter.aggregate("my-index", query, aggs);

    assertTrue(result.isFail());
    assertEquals("ELASTICSEARCH_ERROR", result.problemOrNull().code().value());
  }

  // ── buildQuery branch coverage ─────────────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void search_withMatchAllQuery_shouldSucceed() throws IOException {
    stubSearchResponse();
    Result<SearchResult> result = adapter.search("my-index", SearchQuery.matchAll());
    assertTrue(result.isOk());
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_withTermQuery_withFields_shouldSucceed() throws IOException {
    stubSearchResponse();
    SearchQuery q =
        SearchQuery.builder()
            .query("published")
            .queryType(SearchQuery.QueryType.TERM)
            .fields(List.of("status"))
            .build();
    assertTrue(adapter.search("my-index", q).isOk());
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_withTermQuery_noFields_shouldSucceed() throws IOException {
    stubSearchResponse();
    SearchQuery q =
        SearchQuery.builder().query("published").queryType(SearchQuery.QueryType.TERM).build();
    assertTrue(adapter.search("my-index", q).isOk());
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_withPhraseQuery_shouldSucceed() throws IOException {
    stubSearchResponse();
    SearchQuery q =
        SearchQuery.builder()
            .query("exact phrase")
            .queryType(SearchQuery.QueryType.PHRASE)
            .fields(List.of("description"))
            .build();
    assertTrue(adapter.search("my-index", q).isOk());
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_withPrefixQuery_shouldSucceed() throws IOException {
    stubSearchResponse();
    SearchQuery q =
        SearchQuery.builder()
            .query("pre")
            .queryType(SearchQuery.QueryType.PREFIX)
            .fields(List.of("name"))
            .build();
    assertTrue(adapter.search("my-index", q).isOk());
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_withWildcardQuery_shouldSucceed() throws IOException {
    stubSearchResponse();
    SearchQuery q =
        SearchQuery.builder()
            .query("test*")
            .queryType(SearchQuery.QueryType.WILDCARD)
            .fields(List.of("name"))
            .build();
    assertTrue(adapter.search("my-index", q).isOk());
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_withFuzzyQuery_shouldSucceed() throws IOException {
    stubSearchResponse();
    SearchQuery q =
        SearchQuery.builder()
            .query("tets")
            .queryType(SearchQuery.QueryType.FUZZY)
            .fields(List.of("title"))
            .build();
    assertTrue(adapter.search("my-index", q).isOk());
  }

  @SuppressWarnings("unchecked")
  private void stubSearchResponse() throws IOException {
    SearchResponse<Map> response = mock(SearchResponse.class);
    co.elastic.clients.elasticsearch.core.search.HitsMetadata<Map> hits =
        mock(co.elastic.clients.elasticsearch.core.search.HitsMetadata.class);
    when(hits.hits()).thenReturn(List.of());
    when(hits.total()).thenReturn(null);
    when(hits.maxScore()).thenReturn(null);
    when(response.hits()).thenReturn(hits);
    when(response.took()).thenReturn(1L);
    when(client.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);
  }
}
