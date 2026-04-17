package com.marcusprado02.commons.adapters.payment.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.stripe.exception.StripeException;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.RefundListParams;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StripeRefundServiceTest {

  private StripeRefundService service;

  @BeforeEach
  void setUp() {
    service = StripeRefundService.create("sk_test_fake_key");
  }

  @Test
  void shouldCreateService() {
    assertThat(service).isNotNull();
  }

  @Test
  void shouldReturnFailureOnCreateRefundException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Refund.class)) {
      mocked
          .when(() -> com.stripe.model.Refund.create(any(RefundCreateParams.class)))
          .thenThrow(ex);
      var result = service.createRefund("pi_test", null, null, null);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("REFUND.CREATE_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnCreateRefundWithAllParamsException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Refund.class)) {
      mocked
          .when(() -> com.stripe.model.Refund.create(any(RefundCreateParams.class)))
          .thenThrow(ex);
      // amount + reason + metadata covers all conditional branches in createRefund
      var result =
          service.createRefund("pi_test", BigDecimal.valueOf(100), "DUPLICATE", Map.of("k", "v"));
      assertThat(result.isFail()).isTrue();
    }
  }

  @Test
  void shouldReturnFailureOnGetRefundException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Refund.class)) {
      mocked.when(() -> com.stripe.model.Refund.retrieve("re_test")).thenThrow(ex);
      var result = service.getRefund("re_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("REFUND.NOT_FOUND");
    }
  }

  @Test
  void shouldReturnFailureOnListRefundsException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Refund.class)) {
      mocked.when(() -> com.stripe.model.Refund.list(any(RefundListParams.class))).thenThrow(ex);
      var result = service.listRefunds("pi_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("REFUND.LIST_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnCancelRefundException() throws Exception {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Refund.class)) {
      var mockRefund = mock(com.stripe.model.Refund.class);
      mocked.when(() -> com.stripe.model.Refund.retrieve("re_test")).thenReturn(mockRefund);
      when(mockRefund.cancel()).thenThrow(ex);
      var result = service.cancelRefund("re_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("REFUND.CANCEL_FAILED");
    }
  }

  @Test
  void shouldReturnOkOnCreateRefundSuccess() throws Exception {
    try (var mocked = mockStatic(com.stripe.model.Refund.class)) {
      var mockRefund = mock(com.stripe.model.Refund.class);
      when(mockRefund.getId()).thenReturn("re_test");
      when(mockRefund.getPaymentIntent()).thenReturn("pi_test");
      when(mockRefund.getAmount()).thenReturn(500L);
      when(mockRefund.getCurrency()).thenReturn("usd");
      when(mockRefund.getStatus()).thenReturn("succeeded");
      when(mockRefund.getReason()).thenReturn(null);
      when(mockRefund.getCreated()).thenReturn(1_234_567_890L);
      when(mockRefund.getMetadata()).thenReturn(Map.of());
      mocked
          .when(() -> com.stripe.model.Refund.create(any(RefundCreateParams.class)))
          .thenReturn(mockRefund);

      var result = service.createRefund("pi_test", null, null, null);
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isNotNull();
    }
  }

  @Test
  void shouldReturnOkOnGetRefundSuccess() throws Exception {
    try (var mocked = mockStatic(com.stripe.model.Refund.class)) {
      var mockRefund = mock(com.stripe.model.Refund.class);
      when(mockRefund.getId()).thenReturn("re_test");
      when(mockRefund.getPaymentIntent()).thenReturn("pi_test");
      when(mockRefund.getAmount()).thenReturn(500L);
      when(mockRefund.getCurrency()).thenReturn("usd");
      when(mockRefund.getStatus()).thenReturn("succeeded");
      when(mockRefund.getReason()).thenReturn(null);
      when(mockRefund.getCreated()).thenReturn(1_234_567_890L);
      when(mockRefund.getMetadata()).thenReturn(null);
      mocked.when(() -> com.stripe.model.Refund.retrieve("re_test")).thenReturn(mockRefund);

      var result = service.getRefund("re_test");
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void shouldReturnOkOnListRefundsSuccess() throws Exception {
    try (var mocked = mockStatic(com.stripe.model.Refund.class)) {
      var refundCollection = mock(com.stripe.model.RefundCollection.class);
      when(refundCollection.getData()).thenReturn(List.of());
      mocked
          .when(() -> com.stripe.model.Refund.list(any(RefundListParams.class)))
          .thenReturn(refundCollection);

      var result = service.listRefunds("pi_test");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEmpty();
    }
  }
}
