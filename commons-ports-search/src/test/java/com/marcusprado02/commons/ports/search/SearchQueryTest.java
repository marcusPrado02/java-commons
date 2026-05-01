package com.marcusprado02.commons.ports.search;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SearchQueryTest {

  @Test
  void searchQuery_matchAll_creates_default_query() {
    SearchQuery q = SearchQuery.matchAll();
    assertEquals("*", q.query());
    assertEquals(0, q.from());
    assertEquals(10, q.size());
    assertTrue(q.fields().isEmpty());
    assertTrue(q.filters().isEmpty());
    assertTrue(q.sorting().isEmpty());
  }

  @Test
  void searchQuery_of_text_sets_query() {
    SearchQuery q = SearchQuery.of("laptop");
    assertEquals("laptop", q.query());
  }

  @Test
  void searchQuery_builder_with_all_options() {
    SearchQuery q =
        SearchQuery.builder()
            .query("phone")
            .field("name")
            .fields(List.of("description"))
            .filter("category", "electronics")
            .filters(Map.of("brand", "apple"))
            .sortBy("price", SearchQuery.SortOrder.ASC)
            .from(10)
            .size(20)
            .queryType(SearchQuery.QueryType.MATCH)
            .minScore(0.5f)
            .build();

    assertEquals("phone", q.query());
    assertEquals(2, q.fields().size());
    assertEquals(2, q.filters().size());
    assertEquals(1, q.sorting().size());
    assertEquals(10, q.from());
    assertEquals(20, q.size());
    assertEquals(SearchQuery.QueryType.MATCH, q.queryType());
    assertEquals(0.5f, q.minScore());
  }

  @Test
  void searchQuery_null_fields_defaults_to_empty() {
    SearchQuery q =
        new SearchQuery("*", null, null, null, 0, 10, SearchQuery.QueryType.MATCH, null);
    assertTrue(q.fields().isEmpty());
    assertTrue(q.filters().isEmpty());
    assertTrue(q.sorting().isEmpty());
  }

  @Test
  void searchQuery_rejects_negative_from() {
    assertThrows(IllegalArgumentException.class, () -> SearchQuery.builder().from(-1).build());
  }

  @Test
  void searchQuery_rejects_zero_size() {
    assertThrows(IllegalArgumentException.class, () -> SearchQuery.builder().size(0).build());
  }

  @Test
  void searchQuery_rejects_size_exceeding_limit() {
    assertThrows(IllegalArgumentException.class, () -> SearchQuery.builder().size(10_001).build());
  }

  @Test
  void sortField_rejects_null_field() {
    assertThrows(
        NullPointerException.class,
        () -> new SearchQuery.SortField(null, SearchQuery.SortOrder.ASC));
  }

  @Test
  void sortField_rejects_null_order() {
    assertThrows(NullPointerException.class, () -> new SearchQuery.SortField("price", null));
  }

  @Test
  void query_type_enum_values() {
    assertNotNull(SearchQuery.QueryType.BOOL);
    assertNotNull(SearchQuery.QueryType.FUZZY);
    assertNotNull(SearchQuery.QueryType.WILDCARD);
  }
}
