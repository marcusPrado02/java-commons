package com.marcusprado02.commons.adapters.web.rest;

import com.marcusprado02.commons.ports.persistence.model.Order;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.FilterOperator;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.SearchFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to parse query parameters into domain objects. Handles filter and sort parsing in a
 * null-safe, defensive manner.
 *
 * <p>Filter format: {@code ?filter=name:eq:Felipe,age:gt:18}
 *
 * <p>Sort format: {@code ?sort=name:asc,age:desc}
 */
public final class QueryParamsParser {

  private QueryParamsParser() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Parses filter query parameter into SearchCriteria.
   *
   * @param filterParam raw filter string (e.g., "name:eq:Felipe,age:gt:18")
   * @return SearchCriteria with parsed filters, empty if invalid or null
   */
  public static SearchCriteria parseFilters(String filterParam) {
    if (filterParam == null || filterParam.isBlank()) {
      return SearchCriteria.of();
    }

    List<SearchFilter> filters = new ArrayList<>();
    String[] parts = filterParam.split(",");

    for (String part : parts) {
      SearchFilter filter = parseFilter(part.trim());
      if (filter != null) {
        filters.add(filter);
      }
    }

    return SearchCriteria.of(filters.toArray(new SearchFilter[0]));
  }

  /**
   * Parses a single filter expression.
   *
   * @param filterExpr filter expression (e.g., "name:eq:Felipe")
   * @return SearchFilter or null if invalid
   */
  private static SearchFilter parseFilter(String filterExpr) {
    if (filterExpr == null || filterExpr.isBlank()) {
      return null;
    }

    String[] tokens = filterExpr.split(":", 3);
    if (tokens.length != 3) {
      return null;
    }

    String field = tokens[0].trim();
    String operatorStr = tokens[1].trim();
    String value = tokens[2].trim();

    if (field.isEmpty() || operatorStr.isEmpty()) {
      return null;
    }

    FilterOperator operator = parseOperator(operatorStr);
    if (operator == null) {
      return null;
    }

    return SearchFilter.of(field, operator, value);
  }

  /**
   * Maps operator string to FilterOperator enum.
   *
   * @param operatorStr operator as string (e.g., "eq", "gt", "like")
   * @return FilterOperator or null if invalid
   */
  private static FilterOperator parseOperator(String operatorStr) {
    if (operatorStr == null || operatorStr.isBlank()) {
      return null;
    }

    return switch (operatorStr.toLowerCase()) {
      case "eq" -> FilterOperator.EQ;
      case "neq" -> FilterOperator.NEQ;
      case "like" -> FilterOperator.LIKE;
      case "gt" -> FilterOperator.GT;
      case "lt" -> FilterOperator.LT;
      case "gte" -> FilterOperator.GTE;
      case "lte" -> FilterOperator.LTE;
      case "in" -> FilterOperator.IN;
      default -> null;
    };
  }

  /**
   * Parses sort query parameter into Sort object.
   *
   * @param sortParam raw sort string (e.g., "name:asc,age:desc")
   * @return Sort with parsed orders, empty if invalid or null
   */
  public static Sort parseSort(String sortParam) {
    if (sortParam == null || sortParam.isBlank()) {
      return Sort.of();
    }

    List<Order> orders = new ArrayList<>();
    String[] parts = sortParam.split(",");

    for (String part : parts) {
      Order order = parseOrder(part.trim());
      if (order != null) {
        orders.add(order);
      }
    }

    return Sort.of(orders.toArray(new Order[0]));
  }

  /**
   * Parses a single sort expression.
   *
   * @param orderExpr order expression (e.g., "name:asc")
   * @return Order or null if invalid
   */
  private static Order parseOrder(String orderExpr) {
    if (orderExpr == null || orderExpr.isBlank()) {
      return null;
    }

    String[] tokens = orderExpr.split(":", 2);
    if (tokens.length != 2) {
      return null;
    }

    String field = tokens[0].trim();
    String directionStr = tokens[1].trim();

    if (field.isEmpty() || directionStr.isEmpty()) {
      return null;
    }

    Order.Direction direction = parseDirection(directionStr);
    if (direction == null) {
      return null;
    }

    return new Order(field, direction);
  }

  /**
   * Maps direction string to Order.Direction enum.
   *
   * @param directionStr direction as string (e.g., "asc", "desc")
   * @return Order.Direction or null if invalid
   */
  private static Order.Direction parseDirection(String directionStr) {
    if (directionStr == null || directionStr.isBlank()) {
      return null;
    }

    return switch (directionStr.toLowerCase()) {
      case "asc" -> Order.Direction.ASC;
      case "desc" -> Order.Direction.DESC;
      default -> null;
    };
  }
}
