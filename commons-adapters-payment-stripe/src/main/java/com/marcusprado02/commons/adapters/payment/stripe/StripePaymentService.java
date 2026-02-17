package com.marcusprado02.commons.adapters.payment.stripe;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.payment.Payment;
import com.marcusprado02.commons.ports.payment.PaymentMethod;
import com.marcusprado02.commons.ports.payment.PaymentService;
import com.marcusprado02.commons.ports.payment.PaymentStatus;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentListParams;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stripe implementation of PaymentService.
 *
 * <p>This adapter converts between our domain Payment model and Stripe's PaymentIntent API. It
 * handles payment creation, retrieval, confirmation, and cancellation.
 *
 * <p>Usage:
 *
 * <pre>
 * var service = StripePaymentService.create("sk_test_...");
 * var result = service.createPayment(
 *     BigDecimal.valueOf(1000), // $10.00
 *     "usd",
 *     "cus_123",
 *     "pm_123",
 *     "Order payment",
 *     Map.of("order_id", "ord_456")
 * );
 * </pre>
 */
public class StripePaymentService implements PaymentService {
  private static final Logger logger = LoggerFactory.getLogger(StripePaymentService.class);

  private final String apiKey;

  private StripePaymentService(String apiKey) {
    this.apiKey = apiKey;
    Stripe.apiKey = apiKey;
  }

  /**
   * Create a StripePaymentService instance.
   *
   * @param apiKey Stripe API key (starts with sk_test_ or sk_live_)
   * @return new StripePaymentService instance
   */
  public static StripePaymentService create(String apiKey) {
    return new StripePaymentService(apiKey);
  }

  @Override
  public Result<Payment> createPayment(
      BigDecimal amount,
      String currency,
      String customerId,
      String paymentMethodId,
      String description,
      Map<String, String> metadata) {
    try {
      var paramsBuilder =
          PaymentIntentCreateParams.builder()
              .setAmount(amount.longValue())
              .setCurrency(currency)
              .setCustomer(customerId)
              .setAutomaticPaymentMethods(
                  PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                      .setEnabled(true)
                      .build());

      if (paymentMethodId != null) {
        paramsBuilder.setPaymentMethod(paymentMethodId);
      }

      if (description != null) {
        paramsBuilder.setDescription(description);
      }

      if (metadata != null && !metadata.isEmpty()) {
        paramsBuilder.putAllMetadata(metadata);
      }

      var paymentIntent = PaymentIntent.create(paramsBuilder.build());
      var payment = mapToPayment(paymentIntent);

      logger.info("Created payment intent: {}", paymentIntent.getId());
      return Result.ok(payment);

    } catch (StripeException e) {
      logger.error("Failed to create payment: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("PAYMENT.CREATE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to create payment: " + e.getMessage()));
    }
  }

  @Override
  public Result<Payment> getPayment(String paymentId) {
    try {
      var paymentIntent = PaymentIntent.retrieve(paymentId);
      var payment = mapToPayment(paymentIntent);
      return Result.ok(payment);

    } catch (StripeException e) {
      logger.error("Failed to retrieve payment {}: {}", paymentId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("PAYMENT.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Payment not found: " + paymentId));
    }
  }

  @Override
  public Result<List<Payment>> listPayments(String customerId, int limit) {
    try {
      var params =
          PaymentIntentListParams.builder().setCustomer(customerId).setLimit((long) limit).build();

      var paymentIntents = PaymentIntent.list(params);
      var payments =
          paymentIntents.getData().stream().map(this::mapToPayment).collect(Collectors.toList());

      return Result.ok(payments);

    } catch (StripeException e) {
      logger.error("Failed to list payments for customer {}: {}", customerId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("PAYMENT.LIST_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to list payments: " + e.getMessage()));
    }
  }

  @Override
  public Result<Payment> cancelPayment(String paymentId) {
    try {
      var paymentIntent = PaymentIntent.retrieve(paymentId);
      var canceled = paymentIntent.cancel(PaymentIntentCancelParams.builder().build());
      var payment = mapToPayment(canceled);

      logger.info("Canceled payment: {}", paymentId);
      return Result.ok(payment);

    } catch (StripeException e) {
      logger.error("Failed to cancel payment {}: {}", paymentId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("PAYMENT.CANCEL_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to cancel payment: " + e.getMessage()));
    }
  }

  @Override
  public Result<Payment> confirmPayment(String paymentId) {
    try {
      var paymentIntent = PaymentIntent.retrieve(paymentId);
      var confirmed = paymentIntent.confirm(PaymentIntentConfirmParams.builder().build());
      var payment = mapToPayment(confirmed);

      logger.info("Confirmed payment: {}", paymentId);
      return Result.ok(payment);

    } catch (StripeException e) {
      logger.error("Failed to confirm payment {}: {}", paymentId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("PAYMENT.CONFIRM_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to confirm payment: " + e.getMessage()));
    }
  }

  @Override
  public Result<PaymentMethod> createPaymentMethod(
      String customerId, String type, Map<String, String> details) {
    // To be implemented - requires more complex Stripe PaymentMethod API
    return Result.fail(
        Problem.of(
            ErrorCode.of("PAYMENT.NOT_IMPLEMENTED"),
            ErrorCategory.BUSINESS,
            Severity.INFO,
            "createPaymentMethod not yet implemented"));
  }

  @Override
  public Result<PaymentMethod> getPaymentMethod(String paymentMethodId) {
    // To be implemented
    return Result.fail(
        Problem.of(
            ErrorCode.of("PAYMENT.NOT_IMPLEMENTED"),
            ErrorCategory.BUSINESS,
            Severity.INFO,
            "getPaymentMethod not yet implemented"));
  }

  @Override
  public Result<List<PaymentMethod>> listPaymentMethods(String customerId) {
    // To be implemented
    return Result.fail(
        Problem.of(
            ErrorCode.of("PAYMENT.NOT_IMPLEMENTED"),
            ErrorCategory.BUSINESS,
            Severity.INFO,
            "listPaymentMethods not yet implemented"));
  }

  @Override
  public Result<Void> deletePaymentMethod(String paymentMethodId) {
    // To be implemented
    return Result.fail(
        Problem.of(
            ErrorCode.of("PAYMENT.NOT_IMPLEMENTED"),
            ErrorCategory.BUSINESS,
            Severity.INFO,
            "deletePaymentMethod not yet implemented"));
  }

  private Payment mapToPayment(PaymentIntent intent) {
    return Payment.builder()
        .id(intent.getId())
        .amount(BigDecimal.valueOf(intent.getAmount()))
        .currency(intent.getCurrency())
        .status(mapStatus(intent.getStatus()))
        .customerId(intent.getCustomer())
        .paymentMethodId(intent.getPaymentMethod())
        .description(intent.getDescription())
        .createdAt(Instant.ofEpochSecond(intent.getCreated()))
        .updatedAt(Instant.now())
        .metadata(intent.getMetadata() != null ? intent.getMetadata() : Map.of())
        .error(
            intent.getLastPaymentError() != null ? intent.getLastPaymentError().getMessage() : null)
        .build();
  }

  private PaymentStatus mapStatus(String stripeStatus) {
    return switch (stripeStatus) {
      case "requires_payment_method", "requires_confirmation" -> PaymentStatus.PENDING;
      case "requires_action" -> PaymentStatus.REQUIRES_ACTION;
      case "processing" -> PaymentStatus.PROCESSING;
      case "succeeded" -> PaymentStatus.SUCCEEDED;
      case "canceled" -> PaymentStatus.CANCELED;
      default -> PaymentStatus.FAILED;
    };
  }
}
