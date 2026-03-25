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
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentListParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.PaymentMethodCreateParams;
import com.stripe.param.PaymentMethodListParams;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    try {
      var typeEnum =
          switch (type.toLowerCase()) {
            case "card" -> PaymentMethodCreateParams.Type.CARD;
            case "us_bank_account" -> PaymentMethodCreateParams.Type.US_BANK_ACCOUNT;
            default ->
                throw new IllegalArgumentException("Unsupported payment method type: " + type);
          };

      var paramsBuilder = PaymentMethodCreateParams.builder().setType(typeEnum);

      if (typeEnum == PaymentMethodCreateParams.Type.CARD && details != null) {
        var cardBuilder = PaymentMethodCreateParams.CardDetails.builder();
        if (details.containsKey("number")) cardBuilder.setNumber(details.get("number"));
        if (details.containsKey("exp_month"))
          cardBuilder.setExpMonth(Long.parseLong(details.get("exp_month")));
        if (details.containsKey("exp_year"))
          cardBuilder.setExpYear(Long.parseLong(details.get("exp_year")));
        if (details.containsKey("cvc")) cardBuilder.setCvc(details.get("cvc"));
        paramsBuilder.setCard(cardBuilder.build());
      }

      var pm = com.stripe.model.PaymentMethod.create(paramsBuilder.build());

      // Attach to customer if provided
      if (customerId != null && !customerId.isBlank()) {
        pm.attach(PaymentMethodAttachParams.builder().setCustomer(customerId).build());
      }

      logger.info("Created payment method: {}", pm.getId());
      return Result.ok(mapToPaymentMethod(pm, customerId));

    } catch (StripeException e) {
      logger.error("Failed to create payment method: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("PAYMENT_METHOD.CREATE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to create payment method: " + e.getMessage()));
    } catch (IllegalArgumentException e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("PAYMENT_METHOD.UNSUPPORTED_TYPE"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              e.getMessage()));
    }
  }

  @Override
  public Result<PaymentMethod> getPaymentMethod(String paymentMethodId) {
    try {
      var pm = com.stripe.model.PaymentMethod.retrieve(paymentMethodId);
      return Result.ok(mapToPaymentMethod(pm, pm.getCustomer()));
    } catch (StripeException e) {
      logger.error("Failed to retrieve payment method {}: {}", paymentMethodId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("PAYMENT_METHOD.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Payment method not found: " + paymentMethodId));
    }
  }

  @Override
  public Result<List<PaymentMethod>> listPaymentMethods(String customerId) {
    try {
      var params =
          PaymentMethodListParams.builder()
              .setCustomer(customerId)
              .setType(PaymentMethodListParams.Type.CARD)
              .build();

      PaymentMethodCollection collection = com.stripe.model.PaymentMethod.list(params);
      var methods =
          collection.getData().stream()
              .map(pm -> mapToPaymentMethod(pm, customerId))
              .collect(Collectors.toList());

      return Result.ok(methods);

    } catch (StripeException e) {
      logger.error(
          "Failed to list payment methods for customer {}: {}", customerId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("PAYMENT_METHOD.LIST_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to list payment methods: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> deletePaymentMethod(String paymentMethodId) {
    try {
      var pm = com.stripe.model.PaymentMethod.retrieve(paymentMethodId);
      pm.detach();
      logger.info("Detached payment method: {}", paymentMethodId);
      return Result.ok(null);
    } catch (StripeException e) {
      logger.error("Failed to delete payment method {}: {}", paymentMethodId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("PAYMENT_METHOD.DELETE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to delete payment method: " + e.getMessage()));
    }
  }

  /**
   * Creates a Stripe Customer and returns the customer ID.
   *
   * @param email customer email address
   * @param name customer display name
   * @param metadata additional key-value metadata
   * @return Stripe customer ID
   */
  public Result<String> createCustomer(
      String email, String name, Map<String, String> metadata) {
    try {
      var paramsBuilder = CustomerCreateParams.builder();
      if (email != null) paramsBuilder.setEmail(email);
      if (name != null) paramsBuilder.setName(name);
      if (metadata != null && !metadata.isEmpty()) paramsBuilder.putAllMetadata(metadata);

      var customer = Customer.create(paramsBuilder.build());
      logger.info("Created Stripe customer: {}", customer.getId());
      return Result.ok(customer.getId());

    } catch (StripeException e) {
      logger.error("Failed to create customer: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("CUSTOMER.CREATE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to create customer: " + e.getMessage()));
    }
  }

  /**
   * Retrieves a Stripe Customer by ID.
   *
   * @param customerId Stripe customer ID
   * @return customer data as key-value map
   */
  public Result<Map<String, String>> getCustomer(String customerId) {
    try {
      var customer = Customer.retrieve(customerId);
      var data =
          Map.of(
              "id", customer.getId(),
              "email", customer.getEmail() != null ? customer.getEmail() : "",
              "name", customer.getName() != null ? customer.getName() : "",
              "created", String.valueOf(customer.getCreated()));
      return Result.ok(data);

    } catch (StripeException e) {
      logger.error("Failed to retrieve customer {}: {}", customerId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("CUSTOMER.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Customer not found: " + customerId));
    }
  }

  /**
   * Deletes a Stripe Customer.
   *
   * @param customerId Stripe customer ID
   * @return void result
   */
  public Result<Void> deleteCustomer(String customerId) {
    try {
      var customer = Customer.retrieve(customerId);
      customer.delete();
      logger.info("Deleted Stripe customer: {}", customerId);
      return Result.ok(null);
    } catch (StripeException e) {
      logger.error("Failed to delete customer {}: {}", customerId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("CUSTOMER.DELETE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to delete customer: " + e.getMessage()));
    }
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

  private PaymentMethod mapToPaymentMethod(
      com.stripe.model.PaymentMethod pm, String customerId) {
    var card = pm.getCard();
    return PaymentMethod.builder()
        .id(pm.getId())
        .type(pm.getType())
        .customerId(customerId)
        .last4(card != null ? card.getLast4() : "")
        .brand(card != null ? card.getBrand() : null)
        .expiryMonth(card != null ? card.getExpMonth().intValue() : null)
        .expiryYear(card != null ? card.getExpYear().intValue() : null)
        .createdAt(Instant.ofEpochSecond(pm.getCreated()))
        .metadata(pm.getMetadata() != null ? pm.getMetadata() : Map.of())
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
