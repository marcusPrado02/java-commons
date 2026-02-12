package com.marcusprado02.commons.adapters.web.rest;

import com.marcusprado02.commons.ports.persistence.model.Order;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.FilterOperator;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.SearchFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryParamsParserTest {

  @Test
  void parseFilters_validSingleFilter_shouldReturnSearchCriteria() {
    // Given
    String filterParam = "name:eq:Felipe";

    // When
    SearchCriteria result = QueryParamsParser.parseFilters(filterParam);

    // Then
    assertNotNull(result);
    assertEquals(1, result.filters().size());

    SearchFilter filter = result.filters().get(0);
    assertEquals("name", filter.field());
    assertEquals(FilterOperator.EQ, filter.operator());
    assertEquals("Felipe", filter.value());
  }

  @Test
  void parseFilters_validMultipleFilters_shouldReturnSearchCriteria() {
    // Given
    String filterParam = "name:eq:Felipe,age:gt:18,city:like:São Paulo";

    // When
    SearchCriteria result = QueryParamsParser.parseFilters(filterParam);

    // Then
    assertNotNull(result);
    assertEquals(3, result.filters().size());

    SearchFilter filter1 = result.filters().get(0);
    assertEquals("name", filter1.field());
    assertEquals(FilterOperator.EQ, filter1.operator());
    assertEquals("Felipe", filter1.value());

    SearchFilter filter2 = result.filters().get(1);
    assertEquals("age", filter2.field());
    assertEquals(FilterOperator.GT, filter2.operator());
    assertEquals("18", filter2.value());

    SearchFilter filter3 = result.filters().get(2);
    assertEquals("city", filter3.field());
    assertEquals(FilterOperator.LIKE, filter3.operator());
    assertEquals("São Paulo", filter3.value());
  }

  @Test
  void parseFilters_allOperators_shouldParseCorrectly() {
    // Given
    String filterParam = "f1:eq:v1,f2:neq:v2,f3:like:v3,f4:gt:v4,f5:lt:v5,f6:gte:v6,f7:lte:v7,f8:in:v8";

    // When
    SearchCriteria result = QueryParamsParser.parseFilters(filterParam);

    // Then
    assertNotNull(result);
    assertEquals(8, result.filters().size());

    assertEquals(FilterOperator.EQ, result.filters().get(0).operator());
    assertEquals(FilterOperator.NEQ, result.filters().get(1).operator());
    assertEquals(FilterOperator.LIKE, result.filters().get(2).operator());
    assertEquals(FilterOperator.GT, result.filters().get(3).operator());
    assertEquals(FilterOperator.LT, result.filters().get(4).operator());
    assertEquals(FilterOperator.GTE, result.filters().get(5).operator());
    assertEquals(FilterOperator.LTE, result.filters().get(6).operator());
    assertEquals(FilterOperator.IN, result.filters().get(7).operator());
  }

  @Test
  void parseFilters_invalidFilter_shouldSkipInvalid() {
    // Given
    String filterParam = "name:eq:Felipe,invalid,age:gt:18";

    // When
    SearchCriteria result = QueryParamsParser.parseFilters(filterParam);

    // Then
    assertNotNull(result);
    assertEquals(2, result.filters().size());
    assertEquals("name", result.filters().get(0).field());
    assertEquals("age", result.filters().get(1).field());
  }

  @Test
  void parseFilters_invalidOperator_shouldSkipFilter() {
    // Given
    String filterParam = "name:eq:Felipe,age:invalid:18,city:like:SP";

    // When
    SearchCriteria result = QueryParamsParser.parseFilters(filterParam);

    // Then
    assertNotNull(result);
    assertEquals(2, result.filters().size());
    assertEquals("name", result.filters().get(0).field());
    assertEquals("city", result.filters().get(1).field());
  }

  @Test
  void parseFilters_nullInput_shouldReturnEmptyCriteria() {
    // When
    SearchCriteria result = QueryParamsParser.parseFilters(null);

    // Then
    assertNotNull(result);
    assertTrue(result.filters().isEmpty());
  }

  @Test
  void parseFilters_emptyInput_shouldReturnEmptyCriteria() {
    // When
    SearchCriteria result = QueryParamsParser.parseFilters("");

    // Then
    assertNotNull(result);
    assertTrue(result.filters().isEmpty());
  }

  @Test
  void parseFilters_blankInput_shouldReturnEmptyCriteria() {
    // When
    SearchCriteria result = QueryParamsParser.parseFilters("   ");

    // Then
    assertNotNull(result);
    assertTrue(result.filters().isEmpty());
  }

  @Test
  void parseFilters_missingField_shouldSkipFilter() {
    // Given
    String filterParam = ":eq:value";

    // When
    SearchCriteria result = QueryParamsParser.parseFilters(filterParam);

    // Then
    assertNotNull(result);
    assertTrue(result.filters().isEmpty());
  }

  @Test
  void parseFilters_missingOperator_shouldSkipFilter() {
    // Given
    String filterParam = "field::value";

    // When
    SearchCriteria result = QueryParamsParser.parseFilters(filterParam);

    // Then
    assertNotNull(result);
    assertTrue(result.filters().isEmpty());
  }

  @Test
  void parseFilters_emptyValue_shouldAcceptFilter() {
    // Given
    String filterParam = "field:eq:";

    // When
    SearchCriteria result = QueryParamsParser.parseFilters(filterParam);

    // Then
    assertNotNull(result);
    assertEquals(1, result.filters().size());
    assertEquals("", result.filters().get(0).value());
  }

  @Test
  void parseFilters_valueWithColon_shouldParseCorrectly() {
    // Given
    String filterParam = "url:eq:http://example.com";

    // When
    SearchCriteria result = QueryParamsParser.parseFilters(filterParam);

    // Then
    assertNotNull(result);
    assertEquals(1, result.filters().size());
    assertEquals("http://example.com", result.filters().get(0).value());
  }

  @Test
  void parseFilters_caseInsensitiveOperator_shouldParse() {
    // Given
    String filterParam = "name:EQ:value,age:Gt:18,city:LiKe:SP";

    // When
    SearchCriteria result = QueryParamsParser.parseFilters(filterParam);

    // Then
    assertNotNull(result);
    assertEquals(3, result.filters().size());
    assertEquals(FilterOperator.EQ, result.filters().get(0).operator());
    assertEquals(FilterOperator.GT, result.filters().get(1).operator());
    assertEquals(FilterOperator.LIKE, result.filters().get(2).operator());
  }

  // Sort tests

  @Test
  void parseSort_validSingleSort_shouldReturnSort() {
    // Given
    String sortParam = "name:asc";

    // When
    Sort result = QueryParamsParser.parseSort(sortParam);

    // Then
    assertNotNull(result);
    assertEquals(1, result.orders().size());

    Order order = result.orders().get(0);
    assertEquals("name", order.field());
    assertEquals(Order.Direction.ASC, order.direction());
  }

  @Test
  void parseSort_validMultipleSorts_shouldReturnSort() {
    // Given
    String sortParam = "name:asc,age:desc,createdAt:asc";

    // When
    Sort result = QueryParamsParser.parseSort(sortParam);

    // Then
    assertNotNull(result);
    assertEquals(3, result.orders().size());

    Order order1 = result.orders().get(0);
    assertEquals("name", order1.field());
    assertEquals(Order.Direction.ASC, order1.direction());

    Order order2 = result.orders().get(1);
    assertEquals("age", order2.field());
    assertEquals(Order.Direction.DESC, order2.direction());

    Order order3 = result.orders().get(2);
    assertEquals("createdAt", order3.field());
    assertEquals(Order.Direction.ASC, order3.direction());
  }

  @Test
  void parseSort_invalidSort_shouldSkipInvalid() {
    // Given
    String sortParam = "name:asc,invalid,age:desc";

    // When
    Sort result = QueryParamsParser.parseSort(sortParam);

    // Then
    assertNotNull(result);
    assertEquals(2, result.orders().size());
    assertEquals("name", result.orders().get(0).field());
    assertEquals("age", result.orders().get(1).field());
  }

  @Test
  void parseSort_invalidDirection_shouldSkipSort() {
    // Given
    String sortParam = "name:asc,age:invalid,city:desc";

    // When
    Sort result = QueryParamsParser.parseSort(sortParam);

    // Then
    assertNotNull(result);
    assertEquals(2, result.orders().size());
    assertEquals("name", result.orders().get(0).field());
    assertEquals("city", result.orders().get(1).field());
  }

  @Test
  void parseSort_nullInput_shouldReturnEmptySort() {
    // When
    Sort result = QueryParamsParser.parseSort(null);

    // Then
    assertNotNull(result);
    assertTrue(result.orders().isEmpty());
  }

  @Test
  void parseSort_emptyInput_shouldReturnEmptySort() {
    // When
    Sort result = QueryParamsParser.parseSort("");

    // Then
    assertNotNull(result);
    assertTrue(result.orders().isEmpty());
  }

  @Test
  void parseSort_blankInput_shouldReturnEmptySort() {
    // When
    Sort result = QueryParamsParser.parseSort("   ");

    // Then
    assertNotNull(result);
    assertTrue(result.orders().isEmpty());
  }

  @Test
  void parseSort_missingField_shouldSkipSort() {
    // Given
    String sortParam = ":asc";

    // When
    Sort result = QueryParamsParser.parseSort(sortParam);

    // Then
    assertNotNull(result);
    assertTrue(result.orders().isEmpty());
  }

  @Test
  void parseSort_missingDirection_shouldSkipSort() {
    // Given
    String sortParam = "field:";

    // When
    Sort result = QueryParamsParser.parseSort(sortParam);

    // Then
    assertNotNull(result);
    assertTrue(result.orders().isEmpty());
  }

  @Test
  void parseSort_caseInsensitiveDirection_shouldParse() {
    // Given
    String sortParam = "name:ASC,age:Desc,city:aSc";

    // When
    Sort result = QueryParamsParser.parseSort(sortParam);

    // Then
    assertNotNull(result);
    assertEquals(3, result.orders().size());
    assertEquals(Order.Direction.ASC, result.orders().get(0).direction());
    assertEquals(Order.Direction.DESC, result.orders().get(1).direction());
    assertEquals(Order.Direction.ASC, result.orders().get(2).direction());
  }

  @Test
  void parseSort_onlyOneColon_shouldReturnEmptySort() {
    // Given
    String sortParam = "nameascage";

    // When
    Sort result = QueryParamsParser.parseSort(sortParam);

    // Then
    assertNotNull(result);
    assertTrue(result.orders().isEmpty());
  }
}
