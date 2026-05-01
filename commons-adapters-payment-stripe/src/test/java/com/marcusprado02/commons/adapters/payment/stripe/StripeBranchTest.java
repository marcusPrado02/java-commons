package com.marcusprado02.commons.adapters.payment.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.stripe.model.Customer;
import com.stripe.model.Dispute;
import com.stripe.model.DisputeCollection;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeError;
import com.stripe.net.Webhook;
import com.stripe.param.DisputeListParams;
import com.stripe.param.DisputeUpdateParams;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for StripeWebhookService, StripePaymentService, and StripeDisputeService.
 * Exercises success paths and remaining switch cases not covered by the primary test files.
 */
class StripeBranchTest {

  // ── StripeWebhookService: success path (mockStatic Webhook.constructEvent) ─

  @Test
  void webhook_successPath_withNullApiVersion_andNoPaymentIntent() throws Exception {
    StripeWebhookService service = StripeWebhookService.create("whsec_test1234567890abcdef");

    EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
    when(deserializer.getObject()).thenReturn(Optional.empty());

    Event event = mock(Event.class);
    when(event.getId()).thenReturn("evt_test");
    when(event.getType()).thenReturn("charge.succeeded");
    when(event.getLivemode()).thenReturn(false);
    when(event.getApiVersion()).thenReturn(null);
    when(event.getCreated()).thenReturn(1_234_567_890L);
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);

    try (var mocked = mockStatic(Webhook.class)) {
      mocked.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(event);
      var result = service.parseAndVerify("{}".getBytes(), "t=1,v1=sig");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull().type()).isEqualTo("charge.succeeded");
      assertThat(result.getOrNull().paymentId()).isNull();
    }
  }

  @Test
  void webhook_successPath_withApiVersion_andPaymentIntentObject() throws Exception {
    StripeWebhookService service = StripeWebhookService.create("whsec_test1234567890abcdef");

    PaymentIntent pi = mock(PaymentIntent.class);
    when(pi.getId()).thenReturn("pi_extracted");

    EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
    when(deserializer.getObject()).thenReturn(Optional.of(pi));

    Event event = mock(Event.class);
    when(event.getId()).thenReturn("evt_pi");
    when(event.getType()).thenReturn("payment_intent.succeeded");
    when(event.getLivemode()).thenReturn(true);
    when(event.getApiVersion()).thenReturn("2023-10-16");
    when(event.getCreated()).thenReturn(1_234_567_890L);
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);

    try (var mocked = mockStatic(Webhook.class)) {
      mocked.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(event);
      var result = service.parseAndVerify("{}".getBytes(), "t=1,v1=sig");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull().paymentId()).isEqualTo("pi_extracted");
    }
  }

  @Test
  void webhook_parseException_returnsFail() {
    StripeWebhookService service = StripeWebhookService.create("whsec_test1234567890abcdef");

    try (var mocked = mockStatic(Webhook.class)) {
      mocked
          .when(() -> Webhook.constructEvent(any(), any(), any()))
          .thenThrow(new RuntimeException("parse error"));
      var result = service.parseAndVerify("{}".getBytes(), "t=1,v1=sig");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("WEBHOOK.PARSE_FAILED");
    }
  }

  // ── StripePaymentService: success paths for mapping methods ──────────────

  @Test
  void payment_getCustomer_withNonNullEmailAndName() throws Exception {
    StripePaymentService service = StripePaymentService.create("sk_test_fake");
    Customer customer = mock(Customer.class);
    when(customer.getId()).thenReturn("cus_123");
    when(customer.getEmail()).thenReturn("user@example.com");
    when(customer.getName()).thenReturn("Alice");
    when(customer.getCreated()).thenReturn(1_234_567_890L);

    try (var mocked = mockStatic(Customer.class)) {
      mocked.when(() -> Customer.retrieve("cus_123")).thenReturn(customer);
      var result = service.getCustomer("cus_123");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull().get("email")).isEqualTo("user@example.com");
      assertThat(result.getOrNull().get("name")).isEqualTo("Alice");
    }
  }

  @Test
  void payment_getCustomer_withNullEmailAndName() throws Exception {
    StripePaymentService service = StripePaymentService.create("sk_test_fake");
    Customer customer = mock(Customer.class);
    when(customer.getId()).thenReturn("cus_456");
    when(customer.getEmail()).thenReturn(null);
    when(customer.getName()).thenReturn(null);
    when(customer.getCreated()).thenReturn(1_234_567_890L);

    try (var mocked = mockStatic(Customer.class)) {
      mocked.when(() -> Customer.retrieve("cus_456")).thenReturn(customer);
      var result = service.getCustomer("cus_456");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull().get("email")).isEqualTo("");
      assertThat(result.getOrNull().get("name")).isEqualTo("");
    }
  }

  @Test
  void payment_getPaymentMethod_withNonNullCard() throws Exception {
    StripePaymentService service = StripePaymentService.create("sk_test_fake");

    com.stripe.model.PaymentMethod.Card card = mock(com.stripe.model.PaymentMethod.Card.class);
    when(card.getLast4()).thenReturn("4242");
    when(card.getBrand()).thenReturn("visa");
    when(card.getExpMonth()).thenReturn(12L);
    when(card.getExpYear()).thenReturn(2030L);

    com.stripe.model.PaymentMethod pm = mock(com.stripe.model.PaymentMethod.class);
    when(pm.getId()).thenReturn("pm_test");
    when(pm.getType()).thenReturn("card");
    when(pm.getCustomer()).thenReturn("cus_123");
    when(pm.getCreated()).thenReturn(1_234_567_890L);
    when(pm.getMetadata()).thenReturn(Map.of("key", "val"));
    when(pm.getCard()).thenReturn(card);

    try (var mocked = mockStatic(com.stripe.model.PaymentMethod.class)) {
      mocked.when(() -> com.stripe.model.PaymentMethod.retrieve("pm_test")).thenReturn(pm);
      var result = service.getPaymentMethod("pm_test");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull().last4()).isEqualTo("4242");
      assertThat(result.getOrNull().brand()).contains("visa");
    }
  }

  @Test
  void payment_getPaymentMethod_withNullCard() throws Exception {
    StripePaymentService service = StripePaymentService.create("sk_test_fake");

    com.stripe.model.PaymentMethod pm = mock(com.stripe.model.PaymentMethod.class);
    when(pm.getId()).thenReturn("pm_test2");
    when(pm.getType()).thenReturn("us_bank_account");
    when(pm.getCustomer()).thenReturn("cus_123");
    when(pm.getCreated()).thenReturn(1_234_567_890L);
    when(pm.getMetadata()).thenReturn(null);
    when(pm.getCard()).thenReturn(null);

    try (var mocked = mockStatic(com.stripe.model.PaymentMethod.class)) {
      mocked.when(() -> com.stripe.model.PaymentMethod.retrieve("pm_test2")).thenReturn(pm);
      var result = service.getPaymentMethod("pm_test2");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull().last4()).isEqualTo("");
      assertThat(result.getOrNull().brand()).isEmpty();
    }
  }

  @Test
  void payment_mapToPayment_withNonNullLastPaymentError() throws Exception {
    StripePaymentService service = StripePaymentService.create("sk_test_fake");

    StripeError error = mock(StripeError.class);
    when(error.getMessage()).thenReturn("card declined");

    PaymentIntent intent = mock(PaymentIntent.class);
    when(intent.getId()).thenReturn("pi_fail");
    when(intent.getAmount()).thenReturn(500L);
    when(intent.getCurrency()).thenReturn("usd");
    when(intent.getCustomer()).thenReturn("cus_test");
    when(intent.getPaymentMethod()).thenReturn(null);
    when(intent.getDescription()).thenReturn(null);
    when(intent.getCreated()).thenReturn(1_234_567_890L);
    when(intent.getMetadata()).thenReturn(null);
    when(intent.getLastPaymentError()).thenReturn(error);
    when(intent.getStatus()).thenReturn("requires_payment_method");

    try (var mocked = mockStatic(PaymentIntent.class)) {
      mocked.when(() -> PaymentIntent.retrieve("pi_fail")).thenReturn(intent);
      var result = service.getPayment("pi_fail");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull().error()).contains("card declined");
    }
  }

  // ── StripeDisputeService: submitEvidence remaining switch cases ───────────

  @Test
  void dispute_submitEvidence_remainingFields() throws Exception {
    StripeDisputeService service = StripeDisputeService.create("sk_test_fake");

    var ex = mock(com.stripe.exception.StripeException.class);
    try (var mocked = mockStatic(Dispute.class)) {
      var mockDispute = mock(Dispute.class);
      mocked.when(() -> Dispute.retrieve("dp_test")).thenReturn(mockDispute);
      when(mockDispute.update(any(DisputeUpdateParams.class))).thenThrow(ex);

      // Covers: customer_purchase_ip, receipt, refund_policy, refund_policy_disclosure,
      // service_date, service_documentation, and an unknown key (default branch)
      var evidence =
          Map.of(
              "customer_purchase_ip", "1.2.3.4",
              "receipt", "receipt_data",
              "refund_policy", "no_refunds",
              "refund_policy_disclosure", "disclosed",
              "service_date", "2024-01-01",
              "service_documentation", "docs",
              "unknown_field", "ignored");
      var result = service.submitEvidence("dp_test", evidence, false);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("DISPUTE.EVIDENCE_FAILED");
    }
  }

  @Test
  void dispute_listDisputes_withStatusFilter() throws Exception {
    StripeDisputeService service = StripeDisputeService.create("sk_test_fake");

    Dispute d1 = mock(Dispute.class);
    when(d1.getId()).thenReturn("dp_1");
    when(d1.getCharge()).thenReturn("ch_1");
    when(d1.getAmount()).thenReturn(1000L);
    when(d1.getCurrency()).thenReturn("usd");
    when(d1.getStatus()).thenReturn("needs_response");
    when(d1.getReason()).thenReturn("fraudulent");
    when(d1.getCreated()).thenReturn(1_234_567_890L);
    when(d1.getMetadata()).thenReturn(null);

    Dispute d2 = mock(Dispute.class);
    when(d2.getStatus()).thenReturn("won");

    DisputeCollection collection = mock(DisputeCollection.class);
    when(collection.getData()).thenReturn(java.util.List.of(d1, d2));

    try (var mocked = mockStatic(Dispute.class)) {
      mocked.when(() -> Dispute.list(any(DisputeListParams.class))).thenReturn(collection);
      // status filter covers the status != null && status.equals() branch
      var result = service.listDisputes("ch_1", "needs_response");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).hasSize(1);
      assertThat(result.getOrNull().get(0).id()).isEqualTo("dp_1");
    }
  }
}
