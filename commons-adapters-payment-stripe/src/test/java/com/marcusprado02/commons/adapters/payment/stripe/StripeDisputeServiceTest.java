package com.marcusprado02.commons.adapters.payment.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.stripe.exception.StripeException;
import com.stripe.model.Dispute;
import com.stripe.model.DisputeCollection;
import com.stripe.param.DisputeListParams;
import com.stripe.param.DisputeUpdateParams;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StripeDisputeServiceTest {

  private StripeDisputeService service;

  @BeforeEach
  void setUp() {
    service = StripeDisputeService.create("sk_test_fake_key");
  }

  @Test
  void shouldCreateService() {
    assertThat(service).isNotNull();
  }

  @Test
  void shouldReturnFailureOnGetDisputeException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(Dispute.class)) {
      mocked.when(() -> Dispute.retrieve("dp_test")).thenThrow(ex);
      var result = service.getDispute("dp_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("DISPUTE.NOT_FOUND");
    }
  }

  @Test
  void shouldReturnOkOnGetDisputeSuccess() throws Exception {
    try (var mocked = mockStatic(Dispute.class)) {
      var mockDispute = mock(Dispute.class);
      when(mockDispute.getId()).thenReturn("dp_test");
      when(mockDispute.getCharge()).thenReturn("ch_test");
      when(mockDispute.getAmount()).thenReturn(1000L);
      when(mockDispute.getCurrency()).thenReturn("usd");
      when(mockDispute.getStatus()).thenReturn("needs_response");
      when(mockDispute.getReason()).thenReturn("fraudulent");
      when(mockDispute.getCreated()).thenReturn(1_234_567_890L);
      when(mockDispute.getMetadata()).thenReturn(Map.of());
      mocked.when(() -> Dispute.retrieve("dp_test")).thenReturn(mockDispute);

      var result = service.getDispute("dp_test");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull().id()).isEqualTo("dp_test");
    }
  }

  @Test
  void shouldReturnFailureOnListDisputesException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(Dispute.class)) {
      mocked.when(() -> Dispute.list(any(DisputeListParams.class))).thenThrow(ex);
      var result = service.listDisputes(null, null);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("DISPUTE.LIST_FAILED");
    }
  }

  @Test
  void shouldReturnOkOnListDisputesSuccess() throws Exception {
    try (var mocked = mockStatic(Dispute.class)) {
      var collection = mock(DisputeCollection.class);
      when(collection.getData()).thenReturn(List.of());
      mocked.when(() -> Dispute.list(any(DisputeListParams.class))).thenReturn(collection);

      // null chargeId covers the no-charge-filter path
      var result = service.listDisputes(null, null);
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEmpty();
    }
  }

  @Test
  void shouldReturnOkOnListDisputesWithChargeId() throws Exception {
    try (var mocked = mockStatic(Dispute.class)) {
      var collection = mock(DisputeCollection.class);
      when(collection.getData()).thenReturn(List.of());
      mocked.when(() -> Dispute.list(any(DisputeListParams.class))).thenReturn(collection);

      // non-null chargeId covers the setCharge branch
      var result = service.listDisputes("ch_test", null);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void shouldReturnFailureOnSubmitEvidenceException() throws Exception {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(Dispute.class)) {
      var mockDispute = mock(Dispute.class);
      mocked.when(() -> Dispute.retrieve("dp_test")).thenReturn(mockDispute);
      when(mockDispute.update(any(DisputeUpdateParams.class))).thenThrow(ex);
      var result = service.submitEvidence("dp_test", Map.of(), false);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("DISPUTE.EVIDENCE_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnSubmitEvidenceWithFieldsException() throws Exception {
    // Covers the evidence-field switch-case branches
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(Dispute.class)) {
      var mockDispute = mock(Dispute.class);
      mocked.when(() -> Dispute.retrieve("dp_test")).thenReturn(mockDispute);
      when(mockDispute.update(any(DisputeUpdateParams.class))).thenThrow(ex);
      var evidence =
          Map.of(
              "product_description", "Widget",
              "customer_email_address", "cust@example.com",
              "customer_name", "Alice",
              "shipping_tracking_number", "TRACK123",
              "uncategorized_text", "Additional info",
              "billing_address", "123 Main St");
      var result = service.submitEvidence("dp_test", evidence, true);
      assertThat(result.isFail()).isTrue();
    }
  }

  @Test
  void shouldReturnFailureOnAcceptDisputeException() throws Exception {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(Dispute.class)) {
      var mockDispute = mock(Dispute.class);
      mocked.when(() -> Dispute.retrieve("dp_test")).thenReturn(mockDispute);
      when(mockDispute.close()).thenThrow(ex);
      var result = service.acceptDispute("dp_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("DISPUTE.ACCEPT_FAILED");
    }
  }
}
