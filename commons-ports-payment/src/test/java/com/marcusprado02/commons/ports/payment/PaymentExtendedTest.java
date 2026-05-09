package com.marcusprado02.commons.ports.payment;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaymentExtendedTest {

  // --- Refund ---

  @Test
  void refund_builder_succeeded() {
    Refund r =
        Refund.builder()
            .id("ref-1")
            .paymentId("pay-1")
            .amount(BigDecimal.TEN)
            .currency("USD")
            .status("succeeded")
            .reason("duplicate")
            .build();
    assertEquals("ref-1", r.id());
    assertEquals("USD", r.currency());
    assertTrue(r.isSucceeded());
    assertFalse(r.isFailed());
    assertTrue(r.reason().isPresent());
    assertEquals("duplicate", r.reason().get());
  }

  @Test
  void refund_builder_failed() {
    Refund r =
        Refund.builder()
            .id("ref-2")
            .paymentId("pay-2")
            .amount(BigDecimal.ONE)
            .currency("EUR")
            .status("failed")
            .error("insufficient_funds")
            .createdAt(Instant.now())
            .metadata(Map.of("k", "v"))
            .build();
    assertFalse(r.isSucceeded());
    assertTrue(r.isFailed());
    assertTrue(r.error().isPresent());
  }

  @Test
  void refund_builder_pending_defaults() {
    Refund r = Refund.builder().id("ref-3").paymentId("pay-3").amount(BigDecimal.TEN).build();
    assertFalse(r.isSucceeded());
    assertFalse(r.isFailed());
    assertFalse(r.reason().isPresent());
    assertFalse(r.error().isPresent());
    assertNotNull(r.createdAt());
    assertTrue(r.metadata().isEmpty());
  }

  // --- Subscription ---

  @Test
  void subscription_builder_active() {
    Subscription s =
        Subscription.builder()
            .id("sub-1")
            .customerId("cust-1")
            .status("active")
            .priceId("price-monthly")
            .amount(new BigDecimal("9.99"))
            .currency("USD")
            .interval("month")
            .intervalCount(1)
            .build();
    assertEquals("sub-1", s.id());
    assertTrue(s.isActive());
    assertFalse(s.isCanceled());
    assertFalse(s.canceledAt().isPresent());
    assertFalse(s.endedAt().isPresent());
  }

  @Test
  void subscription_trialing_is_active() {
    Subscription s = Subscription.builder().id("s").customerId("c").status("trialing").build();
    assertTrue(s.isActive());
  }

  @Test
  void subscription_canceled_status_is_not_active() {
    Subscription s = Subscription.builder().id("s").customerId("c").status("canceled").build();
    assertFalse(s.isActive());
  }

  @Test
  void subscription_with_canceled_at_is_canceled() {
    Instant now = Instant.now();
    Subscription s =
        Subscription.builder().id("s").customerId("c").canceledAt(now).endedAt(now).build();
    assertTrue(s.isCanceled());
    assertTrue(s.canceledAt().isPresent());
    assertTrue(s.endedAt().isPresent());
  }

  @Test
  void subscription_builder_defaults() {
    Subscription s = Subscription.builder().id("s").customerId("c").build();
    assertEquals("active", s.status());
    assertEquals("month", s.interval());
    assertEquals(1, s.intervalCount());
    assertNotNull(s.currentPeriodStart());
    assertNotNull(s.createdAt());
    assertTrue(s.metadata().isEmpty());
  }

  @Test
  void subscription_builder_all_fields() {
    Instant now = Instant.now();
    Subscription s =
        Subscription.builder()
            .id("sub")
            .customerId("cust")
            .priceId("price")
            .amount(new BigDecimal("19.99"))
            .currency("BRL")
            .interval("year")
            .intervalCount(1)
            .currentPeriodStart(now)
            .currentPeriodEnd(now.plusSeconds(365 * 24 * 3600))
            .createdAt(now)
            .metadata(Map.of("plan", "enterprise"))
            .build();
    assertEquals("BRL", s.currency());
    assertEquals("year", s.interval());
  }

  // --- PaymentMethod ---

  @Test
  void paymentMethod_builder_card() {
    PaymentMethod pm =
        PaymentMethod.builder()
            .id("pm-1")
            .type("card")
            .customerId("cust-1")
            .last4("4242")
            .brand("visa")
            .expiryMonth(12)
            .expiryYear(2027)
            .isDefault(true)
            .createdAt(Instant.now())
            .metadata(Map.of("source", "stripe"))
            .build();
    assertEquals("pm-1", pm.id());
    assertEquals("card", pm.type());
    assertEquals("4242", pm.last4());
    assertTrue(pm.brand().isPresent());
    assertEquals("visa", pm.brand().get());
    assertTrue(pm.expiryMonth().isPresent());
    assertTrue(pm.expiryYear().isPresent());
    assertTrue(pm.isDefault());
  }

  @Test
  void paymentMethod_builder_bank_account_no_expiry() {
    PaymentMethod pm =
        PaymentMethod.builder()
            .id("pm-2")
            .type("bank_account")
            .customerId("cust-2")
            .last4("6789")
            .build();
    assertFalse(pm.brand().isPresent());
    assertFalse(pm.expiryMonth().isPresent());
    assertFalse(pm.expiryYear().isPresent());
    assertFalse(pm.isDefault());
    assertTrue(pm.metadata().isEmpty());
    assertNotNull(pm.createdAt());
  }
}
