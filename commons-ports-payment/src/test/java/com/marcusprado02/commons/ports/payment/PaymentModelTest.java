package com.marcusprado02.commons.ports.payment;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaymentModelTest {

  @Test
  void payment_builder_with_succeeded_status_is_terminal() {
    Payment p =
        Payment.builder()
            .id("pay-1")
            .amount(BigDecimal.valueOf(100))
            .currency("USD")
            .status(PaymentStatus.SUCCEEDED)
            .customerId("cust-1")
            .build();

    assertTrue(p.isTerminal());
    assertTrue(p.isSucceeded());
    assertFalse(p.isFailed());
    assertEquals("pay-1", p.id());
    assertEquals("USD", p.currency());
  }

  @Test
  void payment_failed_status_is_terminal_and_failed() {
    Payment p =
        Payment.builder()
            .id("pay-2")
            .amount(BigDecimal.ONE)
            .currency("EUR")
            .status(PaymentStatus.FAILED)
            .customerId("c")
            .build();
    assertTrue(p.isTerminal());
    assertTrue(p.isFailed());
    assertFalse(p.isSucceeded());
  }

  @Test
  void payment_canceled_is_terminal() {
    Payment p =
        Payment.builder()
            .id("p")
            .amount(BigDecimal.ONE)
            .currency("USD")
            .status(PaymentStatus.CANCELED)
            .customerId("c")
            .build();
    assertTrue(p.isTerminal());
  }

  @Test
  void payment_refunded_is_terminal() {
    Payment p =
        Payment.builder()
            .id("p")
            .amount(BigDecimal.ONE)
            .currency("USD")
            .status(PaymentStatus.REFUNDED)
            .customerId("c")
            .build();
    assertTrue(p.isTerminal());
  }

  @Test
  void payment_pending_is_not_terminal() {
    Payment p =
        Payment.builder()
            .id("p")
            .amount(BigDecimal.ONE)
            .currency("USD")
            .status(PaymentStatus.PENDING)
            .customerId("c")
            .build();
    assertFalse(p.isTerminal());
    assertFalse(p.isSucceeded());
    assertFalse(p.isFailed());
  }

  @Test
  void payment_builder_with_optional_fields() {
    Instant now = Instant.now();
    Payment p =
        Payment.builder()
            .id("p")
            .amount(BigDecimal.TEN)
            .currency("BRL")
            .customerId("c")
            .paymentMethodId("pm-1")
            .description("desc")
            .statementDescriptor("stmt")
            .receiptEmail("r@r.com")
            .createdAt(now)
            .updatedAt(now)
            .metadata(Map.of("k", "v"))
            .error("err")
            .build();

    assertTrue(p.paymentMethodId().isPresent());
    assertTrue(p.description().isPresent());
    assertTrue(p.error().isPresent());
    assertEquals("v", p.metadata().get("k"));
  }

  @Test
  void webhookEvent_stores_fields_with_null_data_defaults_to_empty() {
    Instant now = Instant.now();
    WebhookEvent e = new WebhookEvent("id-1", "payment.succeeded", "pay-1", null, now);
    assertEquals("id-1", e.id());
    assertEquals("payment.succeeded", e.type());
    assertEquals("pay-1", e.paymentId());
    assertNotNull(e.data());
    assertTrue(e.data().isEmpty());
    assertEquals(now, e.occurredAt());
  }

  @Test
  void webhookEvent_rejects_null_id() {
    assertThrows(
        NullPointerException.class,
        () -> new WebhookEvent(null, "type", null, null, Instant.now()));
  }

  @Test
  void webhookEvent_rejects_null_type() {
    assertThrows(
        NullPointerException.class, () -> new WebhookEvent("id", null, null, null, Instant.now()));
  }

  @Test
  void webhookEvent_rejects_null_occurredAt() {
    assertThrows(
        NullPointerException.class, () -> new WebhookEvent("id", "type", null, null, null));
  }
}
