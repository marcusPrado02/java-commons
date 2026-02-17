package com.marcusprado02.commons.adapters.payment.stripe;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.payment.PaymentStatus;
import java.math.BigDecimal;
import java.util.Map;
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

  @Test
  void shouldCreateStripePaymentService() {
    var service = StripePaymentService.create("sk_test_fake_key");
    assertThat(service).isNotNull();
  }

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
