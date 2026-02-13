package com.marcusprado02.commons.adapters.persistence.mongodb;

import com.marcusprado02.commons.ports.persistence.model.Order;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.FilterOperator;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.SearchFilter;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Converts SearchCriteria and Sort to MongoDB queries.
 *
 * @param <E> Entity type
 */
public class MongoQueryBuilder<E> {

  public void applyFilters(Query query, SearchCriteria searchCriteria) {
    if (searchCriteria == null || searchCriteria.filters().isEmpty()) {
      return;
    }

    Criteria criteria = new Criteria();
    Criteria[] criteriaArray =
        searchCriteria.filters().stream().map(this::buildCriteria).toArray(Criteria[]::new);

    if (criteriaArray.length == 1) {
      query.addCriteria(criteriaArray[0]);
    } else {
      query.addCriteria(criteria.andOperator(criteriaArray));
    }
  }

  public void applySort(Query query, Sort sort) {
    if (sort == null || sort.orders().isEmpty()) {
      return;
    }

    org.springframework.data.domain.Sort mongoSort =
        org.springframework.data.domain.Sort.by(
            sort.orders().stream().map(this::convertOrder).toList());

    query.with(mongoSort);
  }

  private Criteria buildCriteria(SearchFilter filter) {
    String field = filter.field();
    String value = filter.value();
    FilterOperator operator = filter.operator();

    return switch (operator) {
      case EQ -> Criteria.where(field).is(parseValue(value));
      case NEQ -> Criteria.where(field).ne(parseValue(value));
      case LIKE -> {
        // Convert SQL-like wildcards to regex
        String regex = value.replace("%", ".*");
        yield Criteria.where(field).regex(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
      }
      case GT -> Criteria.where(field).gt(parseNumericValue(value));
      case LT -> Criteria.where(field).lt(parseNumericValue(value));
      case GTE -> Criteria.where(field).gte(parseNumericValue(value));
      case LTE -> Criteria.where(field).lte(parseNumericValue(value));
      case IN -> {
        String[] values = value.split(",");
        Object[] parsedValues =
            Arrays.stream(values).map(String::trim).map(this::parseValue).toArray();
        yield Criteria.where(field).in(parsedValues);
      }
    };
  }

  private org.springframework.data.domain.Sort.Order convertOrder(Order order) {
    Direction direction = order.direction() == Order.Direction.ASC ? Direction.ASC : Direction.DESC;
    return new org.springframework.data.domain.Sort.Order(direction, order.field());
  }

  private Object parseValue(String value) {
    if (value == null) {
      return null;
    }

    // Try boolean
    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
      return Boolean.parseBoolean(value);
    }

    // Try integer
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      // Not an integer
    }

    // Try long
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      // Not a long
    }

    // Try double
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      // Not a double
    }

    // Return as string
    return value;
  }

  private Object parseNumericValue(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      // Not an integer
    }

    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      // Not a long
    }

    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Value is not numeric: " + value);
    }
  }
}
