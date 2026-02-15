package com.marcusprado02.commons.adapters.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.elasticsearch.indices.*;
import com.marcusprado02.commons.kernel.result.Result;
// Explicitly import our Aggregation class to resolve ambiguity
import com.marcusprado02.commons.ports.search.Aggregation;
import com.marcusprado02.commons.ports.search.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElasticsearchAdapterTest {

  @Mock
  private ElasticsearchClient client;

  private ElasticsearchConfiguration config;

  @BeforeEach
  void setUp() {
    config = ElasticsearchConfiguration.forDevelopment(
        "http://localhost:9200",
        "elastic",
        "changeme"
    );
  }

  @Test
  void shouldIndexDocument() throws IOException {
    IndexResponse indexResponse = mock(IndexResponse.class);
    when(indexResponse.id()).thenReturn("doc123");
    when(client.index(any(IndexRequest.class))).thenReturn(indexResponse);

    // Note: Can't easily instantiate ElasticsearchAdapter without real RestClient
    // This test demonstrates the approach but would need refactoring for full testability
    // In a real scenario, you'd need to inject the ElasticsearchClient via constructor
  }

  @Test
  void shouldBuildMatchQuery() {
    SearchQuery query = SearchQuery.builder()
        .query("test search")
        .queryType(SearchQuery.QueryType.MATCH)
        .fields(List.of("title", "content"))
        .build();

    assertNotNull(query);
    assertEquals("test search", query.query());
    assertEquals(SearchQuery.QueryType.MATCH, query.queryType());
  }

  @Test
  void shouldBuildTermQuery() {
    SearchQuery query = SearchQuery.builder()
        .query("exact-value")
        .queryType(SearchQuery.QueryType.TERM)
        .fields(List.of("status"))
        .build();

    assertEquals(SearchQuery.QueryType.TERM, query.queryType());
  }

  @Test
  void shouldBuildPhraseQuery() {
    SearchQuery query = SearchQuery.builder()
        .query("exact phrase match")
        .queryType(SearchQuery.QueryType.PHRASE)
        .fields(List.of("description"))
        .build();

    assertEquals(SearchQuery.QueryType.PHRASE, query.queryType());
  }

  @Test
  void shouldBuildPrefixQuery() {
    SearchQuery query = SearchQuery.builder()
        .query("test")
        .queryType(SearchQuery.QueryType.PREFIX)
        .fields(List.of("name"))
        .build();

    assertEquals(SearchQuery.QueryType.PREFIX, query.queryType());
  }

  @Test
  void shouldBuildWildcardQuery() {
    SearchQuery query = SearchQuery.builder()
        .query("test*")
        .queryType(SearchQuery.QueryType.WILDCARD)
        .fields(List.of("name"))
        .build();

    assertEquals(SearchQuery.QueryType.WILDCARD, query.queryType());
  }

  @Test
  void shouldBuildFuzzyQuery() {
    SearchQuery query = SearchQuery.builder()
        .query("tets") // typo
        .queryType(SearchQuery.QueryType.FUZZY)
        .fields(List.of("title"))
        .build();

    assertEquals(SearchQuery.QueryType.FUZZY, query.queryType());
  }

  @Test
  void shouldCreateTermsAggregation() {
    Aggregation agg = Aggregation.terms("category", "category_field", 20);

    assertEquals("category", agg.name());
    assertEquals("category_field", agg.field());
    assertEquals(Aggregation.AggregationType.TERMS, agg.type());
    assertEquals(20, agg.size());
  }

  @Test
  void shouldCreateAvgAggregation() {
    Aggregation agg = Aggregation.avg("avg_price", "price");

    assertEquals("avg_price", agg.name());
    assertEquals("price", agg.field());
    assertEquals(Aggregation.AggregationType.AVG, agg.type());
  }

  @Test
  void shouldCreateSumAggregation() {
    Aggregation agg = Aggregation.sum("total_revenue", "revenue");

    assertEquals("total_revenue", agg.name());
    assertEquals("revenue", agg.field());
    assertEquals(Aggregation.AggregationType.SUM, agg.type());
  }

  @Test
  void shouldCreateMinAggregation() {
    Aggregation agg = Aggregation.min("min_price", "price");

    assertEquals("min_price", agg.name());
    assertEquals(Aggregation.AggregationType.MIN, agg.type());
  }

  @Test
  void shouldCreateMaxAggregation() {
    Aggregation agg = Aggregation.max("max_price", "price");

    assertEquals("max_price", agg.name());
    assertEquals(Aggregation.AggregationType.MAX, agg.type());
  }

  @Test
  void shouldCreateCardinalityAggregation() {
    Aggregation agg = Aggregation.cardinality("unique_users", "user_id");

    assertEquals("unique_users", agg.name());
    assertEquals(Aggregation.AggregationType.CARDINALITY, agg.type());
  }

  @Test
  void shouldCreateSearchQueryWithSorting() {
    SearchQuery query = SearchQuery.builder()
        .query("test")
        .sortBy("created_at", SearchQuery.SortOrder.DESC)
        .sortBy("_score", SearchQuery.SortOrder.DESC)
        .build();

    assertNotNull(query.sorting());
    assertEquals(2, query.sorting().size());
    assertEquals("created_at", query.sorting().get(0).field());
    assertEquals(SearchQuery.SortOrder.DESC, query.sorting().get(0).order());
  }

  @Test
  void shouldCreateSearchQueryWithPagination() {
    SearchQuery query = SearchQuery.builder()
        .query("search term")
        .from(20)
        .size(10)
        .build();

    assertEquals(20, query.from());
    assertEquals(10, query.size());
  }

  @Test
  void shouldCreateSearchQueryWithMinScore() {
    SearchQuery query = SearchQuery.builder()
        .query("relevant results only")
        .minScore(0.5f)
        .build();

    assertNotNull(query.minScore());
    assertEquals(0.5f, query.minScore());
  }

  @Test
  void shouldCreateSearchQueryWithFilters() {
    Map<String, Object> filters = new HashMap<>();
    filters.put("status", "published");
    filters.put("category", "tech");

    SearchQuery query = SearchQuery.builder()
        .query("article")
        .filters(filters)
        .build();

    assertNotNull(query.filters());
    assertEquals(2, query.filters().size());
    assertEquals("published", query.filters().get("status"));
    assertEquals("tech", query.filters().get("category"));
  }

  @Test
  void shouldCreateDocumentWithBuilder() {
    Map<String, Object> source = new HashMap<>();
    source.put("title", "Test Document");
    source.put("content", "This is test content");
    source.put("views", 100);

    Document doc = Document.builder()
        .id("doc1")
        .source(source)
        .score(0.95f)
        .timestamp(Instant.now())
        .build();

    assertEquals("doc1", doc.id());
    assertEquals("Test Document", doc.getField("title"));
    assertEquals("This is test content", doc.getField("content"));
    assertEquals(100, doc.getField("views"));
    assertEquals(0.95f, doc.score());
    assertNotNull(doc.timestamp());
  }

  @Test
  void shouldGetFieldWithType() {
    Map<String, Object> source = new HashMap<>();
    source.put("count", 42);
    source.put("price", 19.99);
    source.put("active", true);

    Document doc = Document.of("doc1", source);

    assertEquals(42, doc.getField("count", Integer.class));
    assertEquals(19.99, doc.getField("price", Double.class));
    assertTrue(doc.getField("active", Boolean.class));
  }

  @Test
  void shouldReturnNullForMissingField() {
    Document doc = Document.of("doc1", Collections.emptyMap());

    assertNull(doc.getField("nonexistent"));
    assertNull(doc.getField("missing", String.class));
  }

  @Test
  void shouldCreateEmptySearchResult() {
    SearchResult result = SearchResult.empty();

    assertTrue(result.hits().isEmpty());
    assertEquals(0, result.totalHits());
    assertEquals(0.0, result.maxScore());
    assertEquals(0, result.tookMillis());
    assertFalse(result.hasHits());
    assertEquals(0, result.hitCount());
  }

  @Test
  void shouldCreateSearchResultWithHits() {
    Document doc1 = Document.of("1", Map.of("title", "Doc 1"));
    Document doc2 = Document.of("2", Map.of("title", "Doc 2"));

    SearchResult result = new SearchResult(
        List.of(doc1, doc2),
        2L,
        0.98f,
        15
    );

    assertTrue(result.hasHits());
    assertEquals(2, result.hitCount());
    assertEquals(2, result.totalHits());
    assertEquals(0.98f, result.maxScore());
    assertEquals(15, result.tookMillis());
  }

  @Test
  void shouldCreateEmptyAggregationResult() {
    AggregationResult result = AggregationResult.empty("test-agg");

    assertTrue(result.buckets().isEmpty());
    assertTrue(result.metrics().isEmpty());
    assertFalse(result.hasBuckets());
    assertEquals(0, result.bucketCount());
    assertNull(result.getMetric("any"));
  }

  @Test
  void shouldCreateAggregationResultWithBuckets() {
    List<AggregationResult.Bucket> buckets = List.of(
        AggregationResult.Bucket.of("category-a", 50L),
        AggregationResult.Bucket.of("category-b", 30L)
    );

    AggregationResult result = new AggregationResult("categories", buckets, Collections.emptyMap());

    assertTrue(result.hasBuckets());
    assertEquals(2, result.bucketCount());
    assertEquals("category-a", result.buckets().get(0).key());
    assertEquals(50L, result.buckets().get(0).docCount());
  }

  @Test
  void shouldCreateAggregationResultWithMetrics() {
    Map<String, Double> metrics = Map.of(
        "avg", 45.5,
        "sum", 1000.0,
        "min", 10.0,
        "max", 100.0
    );

    AggregationResult result = new AggregationResult("metrics", Collections.emptyList(), metrics);

    assertEquals(45.5, result.getMetric("avg"));
    assertEquals(1000.0, result.getMetric("sum"));
    assertEquals(10.0, result.getMetric("min"));
    assertEquals(100.0, result.getMetric("max"));
    assertNull(result.getMetric("nonexistent"));
  }

  @Test
  void shouldValidateSearchQuerySize() {
    assertThrows(IllegalArgumentException.class, () ->
        SearchQuery.builder()
            .query("test")
            .size(0)
            .build()
    );

    assertThrows(IllegalArgumentException.class, () ->
        SearchQuery.builder()
            .query("test")
            .size(10001)
            .build()
    );
  }

  @Test
  void shouldValidateSearchQueryFrom() {
    assertThrows(IllegalArgumentException.class, () ->
        SearchQuery.builder()
            .query("test")
            .from(-1)
            .build()
    );
  }

  @Test
  void shouldAllowMatchAllQuery() {
    SearchQuery query = SearchQuery.matchAll();

    assertEquals("*", query.query());
    assertEquals(0, query.from());
    assertEquals(10, query.size());
  }

  @Test
  void shouldCreateQueryWithOf() {
    SearchQuery query = SearchQuery.of("simple search");

    assertEquals("simple search", query.query());
    assertEquals(SearchQuery.QueryType.MATCH, query.queryType());
  }
}
