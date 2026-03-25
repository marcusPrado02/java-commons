package com.example.orderservice.adapters.web;

import com.example.orderservice.application.ConfirmOrderUseCase;
import com.example.orderservice.application.PlaceOrderUseCase;
import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderItem;
import com.marcusprado02.commons.kernel.result.Result;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST API for order management. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final PlaceOrderUseCase placeOrder;
  private final ConfirmOrderUseCase confirmOrder;

  public OrderController(PlaceOrderUseCase placeOrder, ConfirmOrderUseCase confirmOrder) {
    this.placeOrder = placeOrder;
    this.confirmOrder = confirmOrder;
  }

  @PostMapping
  public ResponseEntity<?> place(@RequestBody PlaceOrderRequest request) {
    List<OrderItem> items = request.items().stream()
        .map(i -> new OrderItem(i.productId(), i.quantity(), i.unitPrice()))
        .toList();

    Result<Order> result = placeOrder.execute(request.customerId(), items);
    return result.isOk()
        ? ResponseEntity.ok(OrderResponse.from(result.getOrNull()))
        : ResponseEntity.badRequest().body(result.problemOrNull());
  }

  @PostMapping("/{orderId}/confirm")
  public ResponseEntity<?> confirm(@PathVariable String orderId) {
    Result<Order> result = confirmOrder.execute(orderId);
    return result.isOk()
        ? ResponseEntity.ok(OrderResponse.from(result.getOrNull()))
        : ResponseEntity.badRequest().body(result.problemOrNull());
  }

  // ---- DTOs ----

  public record ItemRequest(String productId, int quantity, BigDecimal unitPrice) {}

  public record PlaceOrderRequest(String customerId, List<ItemRequest> items) {}

  public record OrderResponse(
      String orderId,
      String customerId,
      String status,
      BigDecimal totalAmount) {

    static OrderResponse from(Order order) {
      return new OrderResponse(
          order.id().toString(),
          order.customerId(),
          order.status().name(),
          order.totalAmount());
    }
  }
}
