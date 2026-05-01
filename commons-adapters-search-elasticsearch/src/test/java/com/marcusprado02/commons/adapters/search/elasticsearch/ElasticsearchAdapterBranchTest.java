package com.marcusprado02.commons.adapters.search.elasticsearch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.search.Aggregation;
import com.marcusprado02.commons.ports.search.AggregationResult;
import com.marcusprado02.commons.ports.search.SearchQuery;
import com.marcusprado02.commons.ports.search.SearchResult;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Branch-coverage tests for ElasticsearchAdapter and ElasticsearchConfiguration that complement
 * ElasticsearchAdapterMockedTest and ElasticsearchConfigurationTest.
 */
@ExtendWith(MockitoExtension.class)
class ElasticsearchAdapterBranchTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ElasticsearchClient client;

  private ElasticsearchAdapter adapter;

  @BeforeEach
  void setUp() {
    ElasticsearchConfiguration config =
        ElasticsearchConfiguration.forDevelopment("http://localhost:9200", "elastic", "changeme");
    adapter = new ElasticsearchAdapter(config, client);
  }

  // ── mapHitToDocument: non-null source and non-null score ──────────────────

  @Test
  @SuppressWarnings("unchecked")
  void search_withHitHavingSourceAndScore_mapsCorrectly() throws IOException {
    TotalHits totalHits = TotalHits.of(t -> t.value(1L).relation(TotalHitsRelation.Eq));

    Hit<Map> hit = mock(Hit.class);
    when(hit.id()).thenReturn("doc-1");
    when(hit.source()).thenReturn(Map.of("title", "Hello"));
    when(hit.score()).thenReturn(0.95);

    HitsMetadata<Map> hitsMetadata = mock(HitsMetadata.class);
    when(hitsMetadata.hits()).thenReturn(List.of(hit));
    when(hitsMetadata.total()).thenReturn(totalHits);
    when(hitsMetadata.maxScore()).thenReturn(0.95);

    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.hits()).thenReturn(hitsMetadata);
    when(response.took()).thenReturn(5L);
    when(client.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    Result<SearchResult> result = adapter.search("my-index", SearchQuery.of("hello"));

    assertTrue(result.isOk());
    assertEquals(1, result.getOrNull().hits().size());
    assertEquals("doc-1", result.getOrNull().hits().get(0).id());
    assertEquals(1L, result.getOrNull().totalHits());
    assertEquals(0.95f, result.getOrNull().maxScore(), 0.01f);
  }

  // ── buildQuery: MATCH with explicit fields ─────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void search_withMatchQueryAndFields_buildsMultiMatchWithFields() throws IOException {
    stubEmptySearchResponse();

    SearchQuery query =
        SearchQuery.builder()
            .query("hello")
            .queryType(SearchQuery.QueryType.MATCH)
            .fields(List.of("title", "body"))
            .build();
    Result<SearchResult> result = adapter.search("my-index", query);

    assertTrue(result.isOk());
  }

  // ── buildQuery: PHRASE/PREFIX/WILDCARD/FUZZY without fields ───────────────

  @Test
  @SuppressWarnings("unchecked")
  void search_withPhraseQueryNoFields_usesDefaultField() throws IOException {
    stubEmptySearchResponse();
    SearchQuery q =
        SearchQuery.builder().query("exact phrase").queryType(SearchQuery.QueryType.PHRASE).build();
    assertTrue(adapter.search("my-index", q).isOk());
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_withPrefixQueryNoFields_usesDefaultField() throws IOException {
    stubEmptySearchResponse();
    SearchQuery q =
        SearchQuery.builder().query("pre").queryType(SearchQuery.QueryType.PREFIX).build();
    assertTrue(adapter.search("my-index", q).isOk());
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_withWildcardQueryNoFields_usesDefaultField() throws IOException {
    stubEmptySearchResponse();
    SearchQuery q =
        SearchQuery.builder().query("test*").queryType(SearchQuery.QueryType.WILDCARD).build();
    assertTrue(adapter.search("my-index", q).isOk());
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_withFuzzyQueryNoFields_usesDefaultField() throws IOException {
    stubEmptySearchResponse();
    SearchQuery q =
        SearchQuery.builder().query("tets").queryType(SearchQuery.QueryType.FUZZY).build();
    assertTrue(adapter.search("my-index", q).isOk());
  }

  // ── aggregate: metric types (AVG, SUM, MIN, MAX, CARDINALITY) ─────────────

  @Test
  @SuppressWarnings("unchecked")
  void aggregate_withAvgType_returnsMetricValue() throws IOException {
    Aggregate avgAgg = Aggregate.of(b -> b.avg(a -> a.value(5.5)));
    stubAggregateResponse("avg_price", avgAgg);

    Result<AggregationResult> result =
        adapter.aggregate(
            "idx", SearchQuery.of("*"), List.of(Aggregation.avg("avg_price", "price")));

    assertTrue(result.isOk());
    assertEquals(5.5, result.getOrNull().getMetric("value"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void aggregate_withSumType_returnsMetricValue() throws IOException {
    Aggregate sumAgg = Aggregate.of(b -> b.sum(a -> a.value(100.0)));
    stubAggregateResponse("total_sales", sumAgg);

    Result<AggregationResult> result =
        adapter.aggregate(
            "idx", SearchQuery.of("*"), List.of(Aggregation.sum("total_sales", "amount")));

    assertTrue(result.isOk());
    assertEquals(100.0, result.getOrNull().getMetric("value"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void aggregate_withMinType_returnsMetricValue() throws IOException {
    Aggregate minAgg = Aggregate.of(b -> b.min(a -> a.value(1.0)));
    stubAggregateResponse("min_price", minAgg);

    Result<AggregationResult> result =
        adapter.aggregate(
            "idx", SearchQuery.of("*"), List.of(Aggregation.min("min_price", "price")));

    assertTrue(result.isOk());
    assertEquals(1.0, result.getOrNull().getMetric("value"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void aggregate_withMaxType_returnsMetricValue() throws IOException {
    Aggregate maxAgg = Aggregate.of(b -> b.max(a -> a.value(999.0)));
    stubAggregateResponse("max_price", maxAgg);

    Result<AggregationResult> result =
        adapter.aggregate(
            "idx", SearchQuery.of("*"), List.of(Aggregation.max("max_price", "price")));

    assertTrue(result.isOk());
    assertEquals(999.0, result.getOrNull().getMetric("value"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void aggregate_withCardinalityType_returnsMetricValue() throws IOException {
    Aggregate cardAgg = Aggregate.of(b -> b.cardinality(c -> c.value(42L)));
    stubAggregateResponse("unique_users", cardAgg);

    Result<AggregationResult> result =
        adapter.aggregate(
            "idx",
            SearchQuery.of("*"),
            List.of(Aggregation.cardinality("unique_users", "user_id")));

    assertTrue(result.isOk());
    assertEquals(42.0, result.getOrNull().getMetric("value"));
  }

  // ── aggregate: TERMS/sterms — bucket mapping ──────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void aggregate_withStermsType_returnsBuckets() throws IOException {
    StringTermsBucket bucket = StringTermsBucket.of(b -> b.key("electronics").docCount(10));
    Aggregate stermsAgg =
        Aggregate.of(a -> a.sterms(t -> t.buckets(bk -> bk.array(List.of(bucket)))));
    stubAggregateResponse("by_category", stermsAgg);

    Result<AggregationResult> result =
        adapter.aggregate(
            "idx", SearchQuery.of("*"), List.of(Aggregation.terms("by_category", "category")));

    assertTrue(result.isOk());
    assertEquals(1, result.getOrNull().buckets().size());
    assertEquals("electronics", result.getOrNull().buckets().get(0).key());
    assertEquals(10L, result.getOrNull().buckets().get(0).docCount());
  }

  // ── aggregate: aggregate key missing from response map ────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void aggregate_whenAggregateNotInResponse_returnsEmpty() throws IOException {
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(Map.of()); // empty map — key not found
    when(client.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    Result<AggregationResult> result =
        adapter.aggregate(
            "idx", SearchQuery.of("*"), List.of(Aggregation.avg("missing_agg", "price")));

    assertTrue(result.isOk());
    assertTrue(result.getOrNull().buckets().isEmpty());
    assertTrue(result.getOrNull().metrics().isEmpty());
  }

  // ── aggregate: response.aggregations() is null ────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void aggregate_whenAggregationsNull_returnsEmpty() throws IOException {
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(null);
    when(client.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    Result<AggregationResult> result =
        adapter.aggregate("idx", SearchQuery.of("*"), List.of(Aggregation.avg("my_avg", "price")));

    assertTrue(result.isOk());
    assertTrue(result.getOrNull().buckets().isEmpty());
  }

  // ── ElasticsearchConfiguration: missed branch coverage ────────────────────

  @Test
  void config_withNullUrlInList_throwsIllegalArgument() {
    List<String> urlsWithNull = new java.util.ArrayList<>();
    urlsWithNull.add(null);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ElasticsearchConfiguration.builder()
                    .serverUrls(urlsWithNull)
                    .username("elastic")
                    .password("changeme")
                    .build());
    assertEquals("Server URL cannot be blank", ex.getMessage());
  }

  @Test
  void config_withNegativeSocketTimeout_throwsIllegalArgument() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ElasticsearchConfiguration.builder()
                    .serverUrl("http://localhost:9200")
                    .username("elastic")
                    .password("changeme")
                    .socketTimeout(Duration.ofSeconds(-1))
                    .build());
    assertEquals("Socket timeout must be positive", ex.getMessage());
  }

  @Test
  void config_withBlankUsername_noApiKey_throwsAuth() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ElasticsearchConfiguration.builder()
                    .serverUrl("http://localhost:9200")
                    .username("   ")
                    .password("changeme")
                    .build());
    assertTrue(ex.getMessage().contains("authentication"));
  }

  @Test
  void config_withBlankPassword_noApiKey_throwsAuth() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ElasticsearchConfiguration.builder()
                    .serverUrl("http://localhost:9200")
                    .username("elastic")
                    .password("   ")
                    .build());
    assertTrue(ex.getMessage().contains("authentication"));
  }

  @Test
  void config_withBlankApiKey_noBasicAuth_throwsAuth() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ElasticsearchConfiguration.builder()
                    .serverUrl("http://localhost:9200")
                    .apiKey("   ")
                    .build());
    assertTrue(ex.getMessage().contains("authentication"));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private void stubEmptySearchResponse() throws IOException {
    HitsMetadata<Map> hitsMetadata = mock(HitsMetadata.class);
    when(hitsMetadata.hits()).thenReturn(List.of());
    when(hitsMetadata.total()).thenReturn(null);
    when(hitsMetadata.maxScore()).thenReturn(null);

    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.hits()).thenReturn(hitsMetadata);
    when(response.took()).thenReturn(1L);
    when(client.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);
  }

  @SuppressWarnings("unchecked")
  private void stubAggregateResponse(String aggName, Aggregate aggregate) throws IOException {
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(Map.of(aggName, aggregate));
    when(client.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);
  }
}
