package com.marcusprado02.commons.ports.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.ports.persistence.exception.EntityNotFoundException;
import com.marcusprado02.commons.ports.persistence.exception.OptimisticLockException;
import com.marcusprado02.commons.ports.persistence.exception.PersistenceException;
import com.marcusprado02.commons.ports.persistence.model.Filter;
import com.marcusprado02.commons.ports.persistence.model.Order;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import java.util.List;
import org.junit.jupiter.api.Test;

class PersistenceModelTest {

  @Test
  void pageRequest_valid_construction() {
    PageRequest r = new PageRequest(0, 10);
    assertEquals(0, r.page());
    assertEquals(10, r.size());
    assertTrue(r.sort().isEmpty());
    assertTrue(r.filters().isEmpty());
  }

  @Test
  void pageRequest_rejects_negative_page() {
    assertThrows(IllegalArgumentException.class, () -> new PageRequest(-1, 10));
  }

  @Test
  void pageRequest_rejects_zero_size() {
    assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, 0));
  }

  @Test
  void pageRequest_rejects_negative_size() {
    assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, -5));
  }

  @Test
  void pageRequest_rejects_null_sort() {
    assertThrows(NullPointerException.class,
        () -> new PageRequest(0, 10, null, List.of()));
  }

  @Test
  void pageRequest_rejects_null_filters() {
    assertThrows(NullPointerException.class,
        () -> new PageRequest(0, 10, List.of(), null));
  }

  @Test
  void sort_of_and_and_combines_orders() {
    Order asc = new Order("name", Order.Direction.ASC);
    Order desc = new Order("age", Order.Direction.DESC);
    Sort s1 = Sort.of(asc);
    Sort s2 = Sort.of(desc);
    Sort combined = s1.and(s2);
    assertEquals(2, combined.orders().size());
    assertEquals("name", combined.orders().get(0).field());
    assertEquals("age", combined.orders().get(1).field());
  }

  @Test
  void filter_record_stores_fields() {
    Filter f = new Filter("status", Filter.Operator.EQ, "ACTIVE");
    assertEquals("status", f.field());
    assertEquals(Filter.Operator.EQ, f.operator());
    assertEquals("ACTIVE", f.value());
  }

  @Test
  void filter_all_operators_are_accessible() {
    for (Filter.Operator op : Filter.Operator.values()) {
      assertNotNull(op.name());
    }
  }

  @Test
  void order_direction_values() {
    assertEquals("ASC", Order.Direction.ASC.name());
    assertEquals("DESC", Order.Direction.DESC.name());
  }

  @Test
  void pageResult_record_stores_fields() {
    PageResult<String> r = new PageResult<>(List.of("a", "b"), 100L, 0, 10);
    assertEquals(2, r.content().size());
    assertEquals(100L, r.totalElements());
    assertEquals(0, r.page());
    assertEquals(10, r.size());
  }

  @Test
  void entityNotFoundException_message() {
    EntityNotFoundException ex = new EntityNotFoundException("id-99");
    assertTrue(ex.getMessage().contains("id-99"));
    assertInstanceOf(PersistenceException.class, ex);
  }

  @Test
  void optimisticLockException_is_persistence_exception() {
    OptimisticLockException ex = new OptimisticLockException("conflict", null);
    assertInstanceOf(PersistenceException.class, ex);
    assertEquals("conflict", ex.getMessage());
  }
}
