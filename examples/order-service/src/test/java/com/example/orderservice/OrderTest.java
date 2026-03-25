package com.example.orderservice;

import static org.junit.jupiter.api.Assertions.*;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderItem;
import com.example.orderservice.domain.event.OrderCancelledEvent;
import com.example.orderservice.domain.event.OrderConfirmedEvent;
import com.example.orderservice.domain.event.OrderCreatedEvent;
import com.example.orderservice.domain.event.OrderShippedEvent;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

  private static final List<OrderItem> ONE_ITEM =
      List.of(new OrderItem("prod-1", 2, new BigDecimal("49.99")));

  @Test
  void create_should_produce_pending_order_with_created_event() {
    var result = Order.create("cust-1", ONE_ITEM);

    assertTrue(result.isOk());
    Order order = result.getOrNull();
    assertEquals(Order.Status.PENDING, order.status());
    assertEquals("cust-1", order.customerId());

    var events = order.flushEvents();
    assertEquals(1, events.size());
    assertInstanceOf(OrderCreatedEvent.class, events.get(0));
  }

  @Test
  void create_should_fail_without_customer() {
    var result = Order.create(null, ONE_ITEM);
    assertTrue(result.isFail());
    assertEquals("ORDER.INVALID_CUSTOMER", result.problemOrNull().code().value());
  }

  @Test
  void create_should_fail_without_items() {
    var result = Order.create("cust-1", List.of());
    assertTrue(result.isFail());
    assertEquals("ORDER.NO_ITEMS", result.problemOrNull().code().value());
  }

  @Test
  void confirm_should_transition_to_confirmed() {
    Order order = Order.create("cust-1", ONE_ITEM).getOrNull();
    order.flushEvents(); // clear created event

    var confirmResult = order.confirm();

    assertTrue(confirmResult.isOk());
    assertEquals(Order.Status.CONFIRMED, order.status());

    var events = order.flushEvents();
    assertEquals(1, events.size());
    assertInstanceOf(OrderConfirmedEvent.class, events.get(0));
  }

  @Test
  void confirm_should_fail_if_not_pending() {
    Order order = Order.create("cust-1", ONE_ITEM).getOrNull();
    order.confirm(); // first confirm

    var secondConfirm = order.confirm();
    assertTrue(secondConfirm.isFail());
    assertEquals("ORDER.INVALID_STATE", secondConfirm.problemOrNull().code().value());
  }

  @Test
  void ship_should_transition_from_confirmed_to_shipped() {
    Order order = Order.create("cust-1", ONE_ITEM).getOrNull();
    order.confirm();
    order.flushEvents();

    var shipResult = order.ship("TRACK-123");

    assertTrue(shipResult.isOk());
    assertEquals(Order.Status.SHIPPED, order.status());

    var events = order.flushEvents();
    assertInstanceOf(OrderShippedEvent.class, events.get(0));
    assertEquals("TRACK-123", ((OrderShippedEvent) events.get(0)).trackingNumber());
  }

  @Test
  void cancel_should_work_on_pending_order() {
    Order order = Order.create("cust-1", ONE_ITEM).getOrNull();
    order.flushEvents();

    var cancelResult = order.cancel("Customer request");

    assertTrue(cancelResult.isOk());
    assertEquals(Order.Status.CANCELLED, order.status());

    var events = order.flushEvents();
    assertInstanceOf(OrderCancelledEvent.class, events.get(0));
  }

  @Test
  void cancel_should_fail_on_shipped_order() {
    Order order = Order.create("cust-1", ONE_ITEM).getOrNull();
    order.confirm();
    order.ship("TRACK-X");

    var cancelResult = order.cancel("Too late");
    assertTrue(cancelResult.isFail());
    assertEquals("ORDER.CANNOT_CANCEL", cancelResult.problemOrNull().code().value());
  }

  @Test
  void totalAmount_should_sum_all_items() {
    List<OrderItem> items = List.of(
        new OrderItem("A", 2, new BigDecimal("10.00")),
        new OrderItem("B", 1, new BigDecimal("5.50")));

    Order order = Order.create("cust-1", items).getOrNull();

    assertEquals(new BigDecimal("25.50"), order.totalAmount());
  }

  @Test
  void flushEvents_should_clear_events_after_call() {
    Order order = Order.create("cust-1", ONE_ITEM).getOrNull();
    List<Object> first = order.flushEvents();
    List<Object> second = order.flushEvents();

    assertFalse(first.isEmpty());
    assertTrue(second.isEmpty());
  }
}
