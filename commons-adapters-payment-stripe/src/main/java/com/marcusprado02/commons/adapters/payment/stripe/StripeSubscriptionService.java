package com.marcusprado02.commons.adapters.payment.stripe;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.payment.Subscription;
import com.marcusprado02.commons.ports.payment.SubscriptionService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.SubscriptionUpdateParams;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StripeSubscriptionService implements SubscriptionService {
  private static final Logger logger = LoggerFactory.getLogger(StripeSubscriptionService.class);

  private StripeSubscriptionService(String apiKey) {
    Stripe.apiKey = apiKey;
  }

  public static StripeSubscriptionService create(String apiKey) {
    return new StripeSubscriptionService(apiKey);
  }

  @Override
  public Result<Subscription> createSubscription(
      String customerId, String priceId, String paymentMethodId, Map<String, String> metadata) {
    try {
      var paramsBuilder =
          SubscriptionCreateParams.builder()
              .setCustomer(customerId)
              .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
              .setDefaultPaymentMethod(paymentMethodId);

      if (metadata != null && !metadata.isEmpty()) {
        paramsBuilder.putAllMetadata(metadata);
      }

      var subscription = com.stripe.model.Subscription.create(paramsBuilder.build());
      return Result.ok(mapToSubscription(subscription));

    } catch (StripeException e) {
      logger.error("Failed to create subscription: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SUBSCRIPTION.CREATE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to create subscription: " + e.getMessage()));
    }
  }

  @Override
  public Result<Subscription> getSubscription(String subscriptionId) {
    try {
      var subscription = com.stripe.model.Subscription.retrieve(subscriptionId);
      return Result.ok(mapToSubscription(subscription));
    } catch (StripeException e) {
      logger.error("Failed to retrieve subscription: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SUBSCRIPTION.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Subscription not found: " + subscriptionId));
    }
  }

  @Override
  public Result<List<Subscription>> listSubscriptions(String customerId) {
    try {
      var params = SubscriptionListParams.builder().setCustomer(customerId).build();
      var subscriptions = com.stripe.model.Subscription.list(params);
      var result =
          subscriptions.getData().stream()
              .map(this::mapToSubscription)
              .collect(Collectors.toList());
      return Result.ok(result);
    } catch (StripeException e) {
      logger.error("Failed to list subscriptions: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SUBSCRIPTION.LIST_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to list subscriptions: " + e.getMessage()));
    }
  }

  @Override
  public Result<Subscription> updateSubscription(
      String subscriptionId, String priceId, String paymentMethodId, Map<String, String> metadata) {
    try {
      var subscription = com.stripe.model.Subscription.retrieve(subscriptionId);
      var paramsBuilder = SubscriptionUpdateParams.builder();

      if (priceId != null && !priceId.isBlank()) {
        // Update the first subscription item's price
        var itemId = subscription.getItems().getData().get(0).getId();
        paramsBuilder.addItem(
            SubscriptionUpdateParams.Item.builder().setId(itemId).setPrice(priceId).build());
      }

      if (paymentMethodId != null && !paymentMethodId.isBlank()) {
        paramsBuilder.setDefaultPaymentMethod(paymentMethodId);
      }

      if (metadata != null && !metadata.isEmpty()) {
        paramsBuilder.putAllMetadata(metadata);
      }

      var updated = subscription.update(paramsBuilder.build());
      logger.info("Updated subscription: {}", subscriptionId);
      return Result.ok(mapToSubscription(updated));

    } catch (StripeException e) {
      logger.error("Failed to update subscription {}: {}", subscriptionId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SUBSCRIPTION.UPDATE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to update subscription: " + e.getMessage()));
    }
  }

  @Override
  public Result<Subscription> cancelSubscription(String subscriptionId, boolean immediately) {
    try {
      var subscription = com.stripe.model.Subscription.retrieve(subscriptionId);
      var params =
          SubscriptionCancelParams.builder()
              .setInvoiceNow(immediately)
              .setProrate(immediately)
              .build();
      var canceled = subscription.cancel(params);
      return Result.ok(mapToSubscription(canceled));
    } catch (StripeException e) {
      logger.error("Failed to cancel subscription: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SUBSCRIPTION.CANCEL_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to cancel subscription: " + e.getMessage()));
    }
  }

  @Override
  public Result<Subscription> resumeSubscription(String subscriptionId) {
    try {
      var subscription = com.stripe.model.Subscription.retrieve(subscriptionId);

      // Resume by removing the cancel_at_period_end flag
      var params =
          SubscriptionUpdateParams.builder()
              .setCancelAtPeriodEnd(false)
              .build();

      var resumed = subscription.update(params);
      logger.info("Resumed subscription: {}", subscriptionId);
      return Result.ok(mapToSubscription(resumed));

    } catch (StripeException e) {
      logger.error("Failed to resume subscription {}: {}", subscriptionId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SUBSCRIPTION.RESUME_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to resume subscription: " + e.getMessage()));
    }
  }

  private Subscription mapToSubscription(com.stripe.model.Subscription sub) {
    var item = sub.getItems().getData().get(0);
    return Subscription.builder()
        .id(sub.getId())
        .customerId(sub.getCustomer())
        .status(sub.getStatus())
        .priceId(item.getPrice().getId())
        .amount(BigDecimal.valueOf(item.getPrice().getUnitAmount()))
        .currency(item.getPrice().getCurrency())
        .interval(item.getPrice().getRecurring().getInterval())
        .intervalCount(item.getPrice().getRecurring().getIntervalCount().intValue())
        .currentPeriodStart(Instant.ofEpochSecond(sub.getCurrentPeriodStart()))
        .currentPeriodEnd(Instant.ofEpochSecond(sub.getCurrentPeriodEnd()))
        .canceledAt(sub.getCanceledAt() != null ? Instant.ofEpochSecond(sub.getCanceledAt()) : null)
        .createdAt(Instant.ofEpochSecond(sub.getCreated()))
        .metadata(sub.getMetadata() != null ? sub.getMetadata() : Map.of())
        .build();
  }
}
