package com.marcusprado02.commons.ports.search;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SearchModelTest {

  // --- SearchResult ---

  @Test
  void searchResult_empty_has_no_hits() {
    SearchResult r = SearchResult.empty();
    assertFalse(r.hasHits());
    assertEquals(0, r.hitCount());
    assertEquals(0L, r.totalHits());
  }

  @Test
  void searchResult_with_hit() {
    Document doc = Document.of("id-1", Map.of("field", "value"));
    SearchResult r = new SearchResult(List.of(doc), 1L, 1.0f, 100L);
    assertTrue(r.hasHits());
    assertEquals(1, r.hitCount());
  }

  @Test
  void searchResult_null_hits_throws() {
    assertThrows(NullPointerException.class, () -> new SearchResult(null, 0L, 0f, 0L));
  }

  @Test
  void searchResult_negative_total_hits_throws() {
    assertThrows(IllegalArgumentException.class, () -> new SearchResult(List.of(), -1L, 0f, 0L));
  }

  @Test
  void searchResult_negative_took_millis_throws() {
    assertThrows(IllegalArgumentException.class, () -> new SearchResult(List.of(), 0L, 0f, -1L));
  }

  // --- Document ---

  @Test
  void document_of_with_source() {
    Document doc = Document.of("doc-1", Map.of("name", "test"));
    assertEquals("doc-1", doc.id());
    assertEquals("test", doc.getField("name"));
    assertNull(doc.score());
    assertNull(doc.timestamp());
  }

  @Test
  void document_null_source_defaults_to_empty() {
    Document doc = Document.of("doc-1", null);
    assertNotNull(doc.source());
    assertTrue(doc.source().isEmpty());
  }

  @Test
  void document_null_id_throws() {
    assertThrows(NullPointerException.class, () -> new Document(null, Map.of(), null, null));
  }

  @Test
  void document_builder_with_all_fields() {
    Instant now = Instant.now();
    Document doc =
        Document.builder()
            .id("doc-2")
            .field("name", "Alice")
            .field("age", 30)
            .score(0.95f)
            .timestamp(now)
            .build();
    assertEquals("doc-2", doc.id());
    assertEquals("Alice", doc.getField("name"));
    assertEquals(0.95f, doc.score());
    assertEquals(now, doc.timestamp());
  }

  @Test
  void document_builder_source_map_null_is_ignored() {
    Document doc = Document.builder().id("doc-3").source(null).build();
    assertNotNull(doc.source());
  }

  @Test
  void document_builder_source_map_merges() {
    Document doc = Document.builder().id("d").source(Map.of("k", "v")).field("k2", "v2").build();
    assertEquals("v", doc.getField("k"));
    assertEquals("v2", doc.getField("k2"));
  }

  @Test
  void document_getField_typed_found() {
    Document doc = Document.of("d", Map.of("name", "Alice"));
    String name = doc.getField("name", String.class);
    assertEquals("Alice", name);
  }

  @Test
  void document_getField_typed_null_returns_null() {
    Document doc = Document.of("d", Map.of());
    assertNull(doc.getField("missing", String.class));
  }

  @Test
  void document_getField_typed_wrong_type_returns_null() {
    Document doc = Document.of("d", Map.of("count", 42));
    assertNull(doc.getField("count", String.class));
  }

  // --- AggregationResult ---

  @Test
  void aggregationResult_empty_factory() {
    AggregationResult r = AggregationResult.empty("cats");
    assertEquals("cats", r.name());
    assertFalse(r.hasBuckets());
    assertEquals(0, r.bucketCount());
  }

  @Test
  void aggregationResult_null_name_throws() {
    assertThrows(
        NullPointerException.class, () -> new AggregationResult(null, List.of(), Map.of()));
  }

  @Test
  void aggregationResult_null_buckets_defaults_to_empty() {
    AggregationResult r = new AggregationResult("agg", null, null);
    assertNotNull(r.buckets());
    assertNotNull(r.metrics());
  }

  @Test
  void aggregationResult_with_buckets_and_metrics() {
    AggregationResult.Bucket bucket = AggregationResult.Bucket.of("cat-1", 10L);
    AggregationResult r = new AggregationResult("cats", List.of(bucket), Map.of("total", 100.0));
    assertTrue(r.hasBuckets());
    assertEquals(1, r.bucketCount());
    assertEquals(100.0, r.getMetric("total"));
    assertNull(r.getMetric("missing"));
  }

  @Test
  void bucket_of_creates_valid() {
    AggregationResult.Bucket b = AggregationResult.Bucket.of("key-1", 5L);
    assertEquals("key-1", b.key());
    assertEquals(5L, b.docCount());
    assertTrue(b.subAggregations().isEmpty());
  }

  @Test
  void bucket_null_key_throws() {
    assertThrows(NullPointerException.class, () -> new AggregationResult.Bucket(null, 5L, null));
  }

  @Test
  void bucket_negative_doc_count_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> new AggregationResult.Bucket("key", -1L, null));
  }

  @Test
  void bucket_null_sub_aggregations_defaults_to_empty() {
    AggregationResult.Bucket b = new AggregationResult.Bucket("key", 5L, null);
    assertNotNull(b.subAggregations());
    assertTrue(b.subAggregations().isEmpty());
  }

  // --- Aggregation ---

  @Test
  void aggregation_terms_factory() {
    Aggregation a = Aggregation.terms("cats", "category");
    assertEquals("cats", a.name());
    assertEquals(Aggregation.AggregationType.TERMS, a.type());
    assertEquals("category", a.field());
    assertEquals(10, a.size());
  }

  @Test
  void aggregation_terms_with_size() {
    Aggregation a = Aggregation.terms("cats", "category", 20);
    assertEquals(20, a.size());
  }

  @Test
  void aggregation_avg_factory() {
    Aggregation a = Aggregation.avg("avg_price", "price");
    assertEquals(Aggregation.AggregationType.AVG, a.type());
    assertNull(a.size());
  }

  @Test
  void aggregation_sum_factory() {
    assertEquals(Aggregation.AggregationType.SUM, Aggregation.sum("s", "f").type());
  }

  @Test
  void aggregation_min_factory() {
    assertEquals(Aggregation.AggregationType.MIN, Aggregation.min("m", "f").type());
  }

  @Test
  void aggregation_max_factory() {
    assertEquals(Aggregation.AggregationType.MAX, Aggregation.max("m", "f").type());
  }

  @Test
  void aggregation_cardinality_factory() {
    assertEquals(Aggregation.AggregationType.CARDINALITY, Aggregation.cardinality("c", "f").type());
  }

  @Test
  void aggregation_null_name_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new Aggregation(null, Aggregation.AggregationType.TERMS, "field", null));
  }

  @Test
  void aggregation_null_type_throws() {
    assertThrows(NullPointerException.class, () -> new Aggregation("name", null, "field", null));
  }

  @Test
  void aggregation_builder_with_all_fields() {
    Aggregation a =
        Aggregation.builder()
            .name("prices")
            .type(Aggregation.AggregationType.RANGE)
            .field("price")
            .size(5)
            .build();
    assertEquals("prices", a.name());
    assertEquals(Aggregation.AggregationType.RANGE, a.type());
    assertEquals(5, a.size());
  }
}
