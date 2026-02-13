package com.marcusprado02.commons.adapters.persistence.inmemory;

import com.marcusprado02.commons.ports.persistence.contract.PageableRepository;
import com.marcusprado02.commons.ports.persistence.model.Order;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.SearchFilter;
import com.marcusprado02.commons.ports.persistence.specification.Specification;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Implementação em memória de PageableRepository<E, ID>. */
public class InMemoryPageableRepository<E, ID> extends BaseInMemoryRepository<E, ID>
    implements PageableRepository<E, ID> {

  public InMemoryPageableRepository(IdExtractor<E, ID> idExtractor) {
    super(idExtractor);
  }

  @Override
  public PageResult<E> findAll(PageRequest pageRequest, Specification<E> specification) {
    // Specification não é adequado para in-memory sem JPA
    // Retorna todos os dados sem filtro (pode ser melhorado com um SpecificationAdapter)
    return findAll(pageRequest);
  }

  @Override
  public PageResult<E> findAll(PageRequest pageRequest, SearchCriteria criteria) {
    List<E> all = new ArrayList<>(storage.values());

    // Aplicar filtros
    List<E> filtered =
        all.stream()
            .filter(entity -> matchesCriteria(entity, criteria))
            .collect(Collectors.toList());

    return paginate(filtered, pageRequest);
  }

  @Override
  public PageResult<E> search(PageRequest pageRequest, Specification<E> spec, Sort sort) {
    // Specification não é adequado para in-memory sem JPA
    // Implementação simplificada: apenas ordenação
    List<E> all = new ArrayList<>(storage.values());
    List<E> sorted = applySorting(all, sort);
    return paginate(sorted, pageRequest);
  }

  public PageResult<E> findAll(PageRequest pageRequest) {
    List<E> all = new ArrayList<>(storage.values());
    return paginate(all, pageRequest);
  }

  private boolean matchesCriteria(E entity, SearchCriteria criteria) {
    if (criteria == null || criteria.filters().isEmpty()) {
      return true;
    }

    return criteria.filters().stream().allMatch(filter -> matchesFilter(entity, filter));
  }

  private boolean matchesFilter(E entity, SearchFilter filter) {
    try {
      Object fieldValue = getFieldValue(entity, filter.field());
      if (fieldValue == null) {
        return filter.value() == null;
      }

      return switch (filter.operator()) {
        case EQ -> compareEqual(fieldValue, filter.value());
        case NEQ -> !compareEqual(fieldValue, filter.value());
        case LIKE -> compareLike(fieldValue, filter.value());
        case GT -> compareGreaterThan(fieldValue, filter.value());
        case LT -> compareLessThan(fieldValue, filter.value());
        case GTE -> compareGreaterThanOrEqual(fieldValue, filter.value());
        case LTE -> compareLessThanOrEqual(fieldValue, filter.value());
        case IN -> compareIn(fieldValue, filter.value());
      };
    } catch (Exception e) {
      throw new RuntimeException("Failed to apply filter on field: " + filter.field(), e);
    }
  }

  private Object getFieldValue(E entity, String fieldName) throws IllegalAccessException {
    try {
      Field field = findField(entity.getClass(), fieldName);
      field.setAccessible(true);
      return field.get(entity);
    } catch (NoSuchFieldException e) {
      throw new IllegalArgumentException("Field not found: " + fieldName, e);
    }
  }

  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      if (clazz.getSuperclass() != null) {
        return findField(clazz.getSuperclass(), fieldName);
      }
      throw e;
    }
  }

  private boolean compareEqual(Object fieldValue, String filterValue) {
    return fieldValue.toString().equals(filterValue);
  }

  private boolean compareLike(Object fieldValue, String filterValue) {
    String value = fieldValue.toString().toLowerCase();
    String pattern = filterValue.toLowerCase().replace("%", ".*");
    return value.matches(pattern);
  }

  @SuppressWarnings("unchecked")
  private boolean compareGreaterThan(Object fieldValue, String filterValue) {
    if (fieldValue instanceof Comparable comparable) {
      return comparable.compareTo(convertToType(filterValue, fieldValue.getClass())) > 0;
    }
    throw new IllegalArgumentException("Field is not comparable: " + fieldValue.getClass());
  }

  @SuppressWarnings("unchecked")
  private boolean compareLessThan(Object fieldValue, String filterValue) {
    if (fieldValue instanceof Comparable comparable) {
      return comparable.compareTo(convertToType(filterValue, fieldValue.getClass())) < 0;
    }
    throw new IllegalArgumentException("Field is not comparable: " + fieldValue.getClass());
  }

  @SuppressWarnings("unchecked")
  private boolean compareGreaterThanOrEqual(Object fieldValue, String filterValue) {
    if (fieldValue instanceof Comparable comparable) {
      return comparable.compareTo(convertToType(filterValue, fieldValue.getClass())) >= 0;
    }
    throw new IllegalArgumentException("Field is not comparable: " + fieldValue.getClass());
  }

  @SuppressWarnings("unchecked")
  private boolean compareLessThanOrEqual(Object fieldValue, String filterValue) {
    if (fieldValue instanceof Comparable comparable) {
      return comparable.compareTo(convertToType(filterValue, fieldValue.getClass())) <= 0;
    }
    throw new IllegalArgumentException("Field is not comparable: " + fieldValue.getClass());
  }

  private boolean compareIn(Object fieldValue, String filterValue) {
    String[] values = filterValue.split(",");
    return Arrays.stream(values).map(String::trim).anyMatch(v -> fieldValue.toString().equals(v));
  }

  private Object convertToType(String value, Class<?> targetType) {
    if (targetType == String.class) {
      return value;
    } else if (targetType == Integer.class || targetType == int.class) {
      return Integer.parseInt(value);
    } else if (targetType == Long.class || targetType == long.class) {
      return Long.parseLong(value);
    } else if (targetType == Double.class || targetType == double.class) {
      return Double.parseDouble(value);
    } else if (targetType == Float.class || targetType == float.class) {
      return Float.parseFloat(value);
    } else if (targetType == Boolean.class || targetType == boolean.class) {
      return Boolean.parseBoolean(value);
    }
    return value;
  }

  private List<E> applySorting(List<E> entities, Sort sort) {
    if (sort == null || sort.orders().isEmpty()) {
      return entities;
    }

    Comparator<E> comparator = null;

    for (Order order : sort.orders()) {
      Comparator<E> orderComparator = createComparator(order);
      if (comparator == null) {
        comparator = orderComparator;
      } else {
        comparator = comparator.thenComparing(orderComparator);
      }
    }

    return entities.stream().sorted(comparator).collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private Comparator<E> createComparator(Order order) {
    return (e1, e2) -> {
      try {
        Object v1 = getFieldValue(e1, order.field());
        Object v2 = getFieldValue(e2, order.field());

        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return order.direction() == Order.Direction.ASC ? -1 : 1;
        if (v2 == null) return order.direction() == Order.Direction.ASC ? 1 : -1;

        int comparison = 0;
        if (v1 instanceof Comparable comparable1 && v2 instanceof Comparable comparable2) {
          comparison = comparable1.compareTo(comparable2);
        } else {
          comparison = v1.toString().compareTo(v2.toString());
        }

        return order.direction() == Order.Direction.ASC ? comparison : -comparison;
      } catch (Exception e) {
        throw new RuntimeException("Failed to compare entities by field: " + order.field(), e);
      }
    };
  }

  private PageResult<E> paginate(List<E> entities, PageRequest pageRequest) {
    int from = pageRequest.page() * pageRequest.size();
    int to = Math.min(from + pageRequest.size(), entities.size());

    if (from > entities.size()) {
      return new PageResult<>(List.of(), entities.size(), pageRequest.page(), pageRequest.size());
    }

    List<E> page = entities.subList(from, to);
    return new PageResult<>(page, entities.size(), pageRequest.page(), pageRequest.size());
  }
}
