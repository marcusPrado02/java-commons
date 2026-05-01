package com.marcusprado02.commons.adapters.persistence.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.FilterOperator;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.SearchFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class MongoBranchTest {

  // ── MongoQueryBuilder direct tests ──────────────────────────────────────────

  @Test
  void applyFilters_nullCriteria_isNoop() {
    MongoQueryBuilder<Object> builder = new MongoQueryBuilder<>();
    Query query = new Query();
    builder.applyFilters(query, null);
    assertThat(query.getQueryObject().isEmpty()).isTrue();
  }

  @Test
  void applyFilters_emptyCriteria_isNoop() {
    MongoQueryBuilder<Object> builder = new MongoQueryBuilder<>();
    Query query = new Query();
    builder.applyFilters(query, SearchCriteria.of());
    assertThat(query.getQueryObject().isEmpty()).isTrue();
  }

  @Test
  void applySort_nullSort_isNoop() {
    MongoQueryBuilder<Object> builder = new MongoQueryBuilder<>();
    Query query = new Query();
    builder.applySort(query, null);
  }

  @Test
  void applySort_emptySort_isNoop() {
    MongoQueryBuilder<Object> builder = new MongoQueryBuilder<>();
    Query query = new Query();
    builder.applySort(query, Sort.of());
  }

  @Test
  void buildCriteria_gteOperator_appliesGte() {
    MongoQueryBuilder<Object> builder = new MongoQueryBuilder<>();
    Query query = new Query();
    SearchFilter filter = SearchFilter.of("age", FilterOperator.GTE, "25");
    builder.applyFilters(query, SearchCriteria.of(filter));
    assertThat(query.getQueryObject().toJson()).contains("$gte");
  }

  @Test
  void buildCriteria_lteOperator_appliesLte() {
    MongoQueryBuilder<Object> builder = new MongoQueryBuilder<>();
    Query query = new Query();
    SearchFilter filter = SearchFilter.of("age", FilterOperator.LTE, "30");
    builder.applyFilters(query, SearchCriteria.of(filter));
    assertThat(query.getQueryObject().toJson()).contains("$lte");
  }

  @Test
  void parseValue_integerString_returnsInteger() {
    MongoQueryBuilder<Object> builder = new MongoQueryBuilder<>();
    Query query = new Query();
    SearchFilter filter = SearchFilter.of("count", FilterOperator.EQ, "42");
    builder.applyFilters(query, SearchCriteria.of(filter));
    assertThat(query.getQueryObject().toJson()).contains("42");
  }

  @Test
  void parseValue_longString_returnsLong() {
    MongoQueryBuilder<Object> builder = new MongoQueryBuilder<>();
    Query query = new Query();
    SearchFilter filter = SearchFilter.of("count", FilterOperator.EQ, "9999999999");
    builder.applyFilters(query, SearchCriteria.of(filter));
    assertThat(query.getQueryObject().toJson()).contains("9999999999");
  }

  @Test
  void parseValue_doubleString_returnsDouble() {
    MongoQueryBuilder<Object> builder = new MongoQueryBuilder<>();
    Query query = new Query();
    SearchFilter filter = SearchFilter.of("score", FilterOperator.EQ, "3.14");
    builder.applyFilters(query, SearchCriteria.of(filter));
    assertThat(query.getQueryObject().toJson()).contains("3.14");
  }

  @Test
  void parseNumericValue_nonNumericString_throwsIllegalArgument() {
    MongoQueryBuilder<Object> builder = new MongoQueryBuilder<>();
    Query query = new Query();
    SearchFilter filter = SearchFilter.of("field", FilterOperator.GT, "not-a-number");
    assertThatThrownBy(() -> builder.applyFilters(query, SearchCriteria.of(filter)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not-a-number");
  }

  // ── MongoPageableRepository branch tests (mocked MongoTemplate) ─────────────

  @Test
  @SuppressWarnings("unchecked")
  void findAllWithCriteria_nullCriteria_skipsFilter() {
    MongoTemplate template = mock(MongoTemplate.class);
    when(template.find(any(Query.class), eq(String.class))).thenReturn(List.of("a", "b"));
    when(template.count(any(Query.class), eq(String.class))).thenReturn(2L);

    MongoPageableRepository<String, Object> repo =
        new MongoPageableRepository<>(template, String.class);
    PageResult<String> result = repo.findAll(new PageRequest(0, 10), (SearchCriteria) null);

    assertThat(result.content()).hasSize(2);
  }

  @Test
  @SuppressWarnings("unchecked")
  void findAllWithCriteria_emptyCriteria_skipsFilter() {
    MongoTemplate template = mock(MongoTemplate.class);
    when(template.find(any(Query.class), eq(String.class))).thenReturn(List.of("x"));
    when(template.count(any(Query.class), eq(String.class))).thenReturn(1L);

    MongoPageableRepository<String, Object> repo =
        new MongoPageableRepository<>(template, String.class);
    PageResult<String> result = repo.findAll(new PageRequest(0, 10), SearchCriteria.of());

    assertThat(result.content()).hasSize(1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_nullSort_skipsSort() {
    MongoTemplate template = mock(MongoTemplate.class);
    when(template.find(any(Query.class), eq(String.class))).thenReturn(List.of("z"));
    when(template.count(any(Query.class), eq(String.class))).thenReturn(1L);

    MongoPageableRepository<String, Object> repo =
        new MongoPageableRepository<>(template, String.class);
    PageResult<String> result = repo.search(new PageRequest(0, 10), null, null);

    assertThat(result.content()).hasSize(1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void search_emptySort_skipsSort() {
    MongoTemplate template = mock(MongoTemplate.class);
    when(template.find(any(Query.class), eq(String.class))).thenReturn(List.of());
    when(template.count(any(Query.class), eq(String.class))).thenReturn(0L);

    MongoPageableRepository<String, Object> repo =
        new MongoPageableRepository<>(template, String.class);
    PageResult<String> result = repo.search(new PageRequest(0, 10), null, Sort.of());

    assertThat(result.content()).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void deleteById_notFound_throwsIllegalArgument() {
    MongoTemplate template = mock(MongoTemplate.class);
    when(template.findById(eq("missing"), eq(String.class))).thenReturn(null);

    MongoPageableRepository<String, Object> repo =
        new MongoPageableRepository<>(template, String.class);

    assertThatThrownBy(() -> repo.deleteById("missing"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("missing");
  }
}
