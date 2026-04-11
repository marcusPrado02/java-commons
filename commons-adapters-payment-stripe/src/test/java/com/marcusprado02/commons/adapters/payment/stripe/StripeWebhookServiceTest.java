package com.marcusprado02.commons.adapters.payment.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StripeWebhookServiceTest {

  private static final String WEBHOOK_SECRET = "whsec_test_secret_1234567890abcdef";

  @Test
  void shouldRejectBlankWebhookSecret() {
    assertThatThrownBy(() -> StripeWebhookService.create(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("webhookSecret");

    assertThatThrownBy(() -> StripeWebhookService.create(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldCreateServiceWithValidSecret() {
    var service = StripeWebhookService.create(WEBHOOK_SECRET);
    assertThat(service).isNotNull();
  }

  @Test
  void shouldReturnFailureForEmptyPayload() {
    var service = StripeWebhookService.create(WEBHOOK_SECRET);

    var result = service.parseAndVerify(new byte[0], "t=123,v1=abc");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("WEBHOOK.EMPTY_PAYLOAD");
  }

  @Test
  void shouldReturnFailureForNullPayload() {
    var service = StripeWebhookService.create(WEBHOOK_SECRET);

    var result = service.parseAndVerify(null, "t=123,v1=abc");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("WEBHOOK.EMPTY_PAYLOAD");
  }

  @Test
  void shouldReturnFailureForMissingSignatureHeader() {
    var service = StripeWebhookService.create(WEBHOOK_SECRET);
    var payload = "{}".getBytes();

    var result = service.parseAndVerify(payload, null);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("WEBHOOK.MISSING_SIGNATURE");

    result = service.parseAndVerify(payload, "   ");
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("WEBHOOK.MISSING_SIGNATURE");
  }

  @Test
  void shouldReturnFailureForInvalidSignature() {
    var service = StripeWebhookService.create(WEBHOOK_SECRET);
    var payload = "{\"id\":\"evt_test\",\"type\":\"payment_intent.succeeded\"}".getBytes();

    var result = service.parseAndVerify(payload, "t=1234567890,v1=invalidsignature");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("WEBHOOK.INVALID_SIGNATURE");
  }

  @Test
  void shouldRecogniseSupportedEventTypes() {
    var service = StripeWebhookService.create(WEBHOOK_SECRET);

    assertThat(service.supports("payment_intent.succeeded")).isTrue();
    assertThat(service.supports("payment_intent.payment_failed")).isTrue();
    assertThat(service.supports("charge.refunded")).isTrue();
    assertThat(service.supports("invoice.payment_succeeded")).isTrue();
    assertThat(service.supports("customer.subscription.created")).isTrue();
  }

  @Test
  void shouldNotSupportUnknownEventTypes() {
    var service = StripeWebhookService.create(WEBHOOK_SECRET);

    assertThat(service.supports("unknown.event")).isFalse();
    assertThat(service.supports("")).isFalse();
    assertThat(service.supports("payment_intent.not_real")).isFalse();
  }
}
