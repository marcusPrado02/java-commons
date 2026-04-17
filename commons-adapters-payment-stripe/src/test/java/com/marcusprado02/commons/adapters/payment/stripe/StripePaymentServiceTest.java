package com.marcusprado02.commons.adapters.payment.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.payment.PaymentStatus;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentIntentCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentListParams;
import com.stripe.param.PaymentMethodCreateParams;
import com.stripe.param.PaymentMethodListParams;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for StripePaymentService.
 *
 * <p>These tests are disabled by default as they require a real Stripe API key and will make actual
 * API calls. To run them: 1. Set STRIPE_API_KEY environment variable to your test key (starts with
 * sk_test_) 2. Remove @Disabled annotations 3. Ensure you have a valid customer ID and payment
 * method ID
 *
 * <p>For automated testing, consider using Stripe's test mode or mock server.
 */
class StripePaymentServiceTest {

  private StripePaymentService service;

  @BeforeEach
  void setUp() {
    service = StripePaymentService.create("sk_test_fake_key");
  }

  @Test
  void shouldCreateStripePaymentService() {
    assertThat(service).isNotNull();
  }

  @Test
  void shouldReturnFailureOnCreatePaymentException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(PaymentIntent.class)) {
      mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class))).thenThrow(ex);
      var result =
          service.createPayment(BigDecimal.valueOf(1000), "usd", "cus_test", null, null, null);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("PAYMENT.CREATE_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnCreatePaymentWithAllParamsException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(PaymentIntent.class)) {
      mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class))).thenThrow(ex);
      // paymentMethodId + description + metadata covers all if-branches
      var result =
          service.createPayment(
              BigDecimal.valueOf(1000), "usd", "cus_test", "pm_test", "desc", Map.of("k", "v"));
      assertThat(result.isFail()).isTrue();
    }
  }

  @Test
  void shouldReturnOkOnCreatePaymentSuccess() throws Exception {
    try (var mocked = mockStatic(PaymentIntent.class)) {
      var mockIntent = buildMockPaymentIntent("requires_payment_method");
      mocked
          .when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
          .thenReturn(mockIntent);
      var result =
          service.createPayment(BigDecimal.valueOf(1000), "usd", "cus_test", null, null, null);
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull().status()).isEqualTo(PaymentStatus.PENDING);
    }
  }

  @Test
  void shouldReturnFailureOnGetPaymentException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(PaymentIntent.class)) {
      mocked.when(() -> PaymentIntent.retrieve("pi_test")).thenThrow(ex);
      var result = service.getPayment("pi_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("PAYMENT.NOT_FOUND");
    }
  }

  @Test
  void shouldReturnOkOnGetPaymentSuccess() throws Exception {
    try (var mocked = mockStatic(PaymentIntent.class)) {
      var mockIntent = buildMockPaymentIntent("succeeded");
      mocked.when(() -> PaymentIntent.retrieve("pi_test")).thenReturn(mockIntent);
      var result = service.getPayment("pi_test");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull().status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }
  }

  @Test
  void shouldReturnFailureOnListPaymentsException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(PaymentIntent.class)) {
      mocked.when(() -> PaymentIntent.list(any(PaymentIntentListParams.class))).thenThrow(ex);
      var result = service.listPayments("cus_test", 10);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("PAYMENT.LIST_FAILED");
    }
  }

  @Test
  void shouldReturnOkOnListPaymentsSuccess() throws Exception {
    try (var mocked = mockStatic(PaymentIntent.class)) {
      var collection = mock(PaymentIntentCollection.class);
      when(collection.getData()).thenReturn(List.of());
      mocked
          .when(() -> PaymentIntent.list(any(PaymentIntentListParams.class)))
          .thenReturn(collection);
      var result = service.listPayments("cus_test", 10);
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEmpty();
    }
  }

  @Test
  void shouldReturnFailureOnCancelPaymentException() throws Exception {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(PaymentIntent.class)) {
      var mockIntent = mock(PaymentIntent.class);
      mocked.when(() -> PaymentIntent.retrieve("pi_test")).thenReturn(mockIntent);
      when(mockIntent.cancel(any(PaymentIntentCancelParams.class))).thenThrow(ex);
      var result = service.cancelPayment("pi_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("PAYMENT.CANCEL_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnConfirmPaymentException() throws Exception {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(PaymentIntent.class)) {
      var mockIntent = mock(PaymentIntent.class);
      mocked.when(() -> PaymentIntent.retrieve("pi_test")).thenReturn(mockIntent);
      when(mockIntent.confirm(any(PaymentIntentConfirmParams.class))).thenThrow(ex);
      var result = service.confirmPayment("pi_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("PAYMENT.CONFIRM_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnUnsupportedPaymentMethodType() {
    // No static mocking needed — the switch default throws IllegalArgumentException before any API
    // call
    var result = service.createPaymentMethod("cus_test", "unsupported_type", Map.of());
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("PAYMENT_METHOD.UNSUPPORTED_TYPE");
  }

  @Test
  void shouldReturnFailureOnCreatePaymentMethodException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.PaymentMethod.class)) {
      mocked
          .when(() -> com.stripe.model.PaymentMethod.create(any(PaymentMethodCreateParams.class)))
          .thenThrow(ex);
      var result =
          service.createPaymentMethod(
              "cus_test",
              "card",
              Map.of(
                  "number", "4242424242424242",
                  "exp_month", "12",
                  "exp_year", "2030",
                  "cvc", "123"));
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("PAYMENT_METHOD.CREATE_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnGetPaymentMethodException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.PaymentMethod.class)) {
      mocked.when(() -> com.stripe.model.PaymentMethod.retrieve("pm_test")).thenThrow(ex);
      var result = service.getPaymentMethod("pm_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("PAYMENT_METHOD.NOT_FOUND");
    }
  }

  @Test
  void shouldReturnFailureOnListPaymentMethodsException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.PaymentMethod.class)) {
      mocked
          .when(() -> com.stripe.model.PaymentMethod.list(any(PaymentMethodListParams.class)))
          .thenThrow(ex);
      var result = service.listPaymentMethods("cus_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("PAYMENT_METHOD.LIST_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnDeletePaymentMethodException() throws Exception {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.PaymentMethod.class)) {
      var mockPm = mock(com.stripe.model.PaymentMethod.class);
      mocked.when(() -> com.stripe.model.PaymentMethod.retrieve("pm_test")).thenReturn(mockPm);
      when(mockPm.detach()).thenThrow(ex);
      var result = service.deletePaymentMethod("pm_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("PAYMENT_METHOD.DELETE_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnCreateCustomerException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(Customer.class)) {
      mocked.when(() -> Customer.create(any(CustomerCreateParams.class))).thenThrow(ex);
      var result = service.createCustomer("test@example.com", "Alice", null);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("CUSTOMER.CREATE_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnCreateCustomerWithAllParamsException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(Customer.class)) {
      mocked.when(() -> Customer.create(any(CustomerCreateParams.class))).thenThrow(ex);
      // email + name + metadata covers all if-branches
      var result = service.createCustomer("test@example.com", "Alice", Map.of("ref", "123"));
      assertThat(result.isFail()).isTrue();
    }
  }

  @Test
  void shouldReturnFailureOnGetCustomerException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(Customer.class)) {
      mocked.when(() -> Customer.retrieve("cus_test")).thenThrow(ex);
      var result = service.getCustomer("cus_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("CUSTOMER.NOT_FOUND");
    }
  }

  @Test
  void shouldReturnFailureOnDeleteCustomerException() throws Exception {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(Customer.class)) {
      var mockCustomer = mock(Customer.class);
      mocked.when(() -> Customer.retrieve("cus_test")).thenReturn(mockCustomer);
      when(mockCustomer.delete()).thenThrow(ex);
      var result = service.deleteCustomer("cus_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("CUSTOMER.DELETE_FAILED");
    }
  }

  @Test
  void shouldMapAllPaymentStatuses() throws Exception {
    // Verifies all branches of mapStatus via createPayment success path
    String[] statuses = {
      "requires_confirmation", "requires_action", "processing", "canceled", "unknown_status"
    };
    PaymentStatus[] expected = {
      PaymentStatus.PENDING,
      PaymentStatus.REQUIRES_ACTION,
      PaymentStatus.PROCESSING,
      PaymentStatus.CANCELED,
      PaymentStatus.FAILED
    };
    for (int i = 0; i < statuses.length; i++) {
      try (var mocked = mockStatic(PaymentIntent.class)) {
        var mockIntent = buildMockPaymentIntent(statuses[i]);
        mocked
            .when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
            .thenReturn(mockIntent);
        var result =
            service.createPayment(BigDecimal.valueOf(100), "usd", "cus_test", null, null, null);
        assertThat(result.isOk()).isTrue();
        assertThat(result.getOrNull().status()).isEqualTo(expected[i]);
      }
    }
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  private PaymentIntent buildMockPaymentIntent(String status) {
    var mockIntent = mock(PaymentIntent.class);
    when(mockIntent.getId()).thenReturn("pi_test");
    when(mockIntent.getAmount()).thenReturn(1000L);
    when(mockIntent.getCurrency()).thenReturn("usd");
    when(mockIntent.getCustomer()).thenReturn("cus_test");
    when(mockIntent.getPaymentMethod()).thenReturn(null);
    when(mockIntent.getDescription()).thenReturn(null);
    when(mockIntent.getCreated()).thenReturn(1_234_567_890L);
    when(mockIntent.getMetadata()).thenReturn(Map.of());
    when(mockIntent.getLastPaymentError()).thenReturn(null);
    when(mockIntent.getStatus()).thenReturn(status);
    return mockIntent;
  }

  // ─── Disabled integration tests (require real Stripe API key) ──────────────

  @Test
  @Disabled("Requires valid Stripe API key")
  void shouldCreatePayment() {
    var apiKey = System.getenv("STRIPE_API_KEY");
    var service = StripePaymentService.create(apiKey);

    var result =
        service.createPayment(
            BigDecimal.valueOf(1000), // $10.00
            "usd",
            "cus_test",
            null,
            "Test payment",
            Map.of("test", "true"));

    assertThat(result.isOk()).isTrue();
    var payment = result.getOrNull();
    assertThat(payment.id()).isNotNull();
    assertThat(payment.amount()).isEqualTo(BigDecimal.valueOf(1000));
    assertThat(payment.currency()).isEqualTo("usd");
    assertThat(payment.status()).isIn(PaymentStatus.PENDING, PaymentStatus.PROCESSING);
  }

  @Test
  @Disabled("Requires valid Stripe API key and payment ID")
  void shouldGetPayment() {
    var apiKey = System.getenv("STRIPE_API_KEY");
    var service = StripePaymentService.create(apiKey);

    var result = service.getPayment("pi_test");

    assertThat(result.isOk()).isTrue();
    var payment = result.getOrNull();
    assertThat(payment.id()).isEqualTo("pi_test");
  }

  @Test
  @Disabled("Requires valid Stripe API key")
  void shouldListPayments() {
    var apiKey = System.getenv("STRIPE_API_KEY");
    var service = StripePaymentService.create(apiKey);

    var result = service.listPayments("cus_test", 10);

    assertThat(result.isOk()).isTrue();
    var payments = result.getOrNull();
    assertThat(payments).isNotNull();
    assertThat(payments.size()).isLessThanOrEqualTo(10);
  }

  @Test
  @Disabled("Requires valid Stripe API key and payment ID")
  void shouldCancelPayment() {
    var apiKey = System.getenv("STRIPE_API_KEY");
    var service = StripePaymentService.create(apiKey);

    var result = service.cancelPayment("pi_test");

    assertThat(result.isOk()).isTrue();
    var payment = result.getOrNull();
    assertThat(payment.status()).isEqualTo(PaymentStatus.CANCELED);
  }
}
