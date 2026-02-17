package com.marcusprado02.commons.adapters.payment.stripe;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.payment.Refund;
import com.marcusprado02.commons.ports.payment.RefundService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.RefundListParams;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StripeRefundService implements RefundService {
  private static final Logger logger = LoggerFactory.getLogger(StripeRefundService.class);

  private StripeRefundService(String apiKey) {
    Stripe.apiKey = apiKey;
  }

  public static StripeRefundService create(String apiKey) {
    return new StripeRefundService(apiKey);
  }

  @Override
  public Result<Refund> createRefund(
      String paymentId, BigDecimal amount, String reason, Map<String, String> metadata) {
    try {
      var paramsBuilder = RefundCreateParams.builder().setPaymentIntent(paymentId);

      if (amount != null) {
        paramsBuilder.setAmount(amount.longValue());
      }

      if (reason != null) {
        paramsBuilder.setReason(RefundCreateParams.Reason.valueOf(reason.toUpperCase()));
      }

      if (metadata != null && !metadata.isEmpty()) {
        paramsBuilder.putAllMetadata(metadata);
      }

      var refund = com.stripe.model.Refund.create(paramsBuilder.build());
      logger.info("Created refund: {}", refund.getId());
      return Result.ok(mapToRefund(refund));

    } catch (StripeException e) {
      logger.error("Failed to create refund: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("REFUND.CREATE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to create refund: " + e.getMessage()));
    }
  }

  @Override
  public Result<Refund> getRefund(String refundId) {
    try {
      var refund = com.stripe.model.Refund.retrieve(refundId);
      return Result.ok(mapToRefund(refund));
    } catch (StripeException e) {
      logger.error("Failed to retrieve refund: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("REFUND.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Refund not found: " + refundId));
    }
  }

  @Override
  public Result<List<Refund>> listRefunds(String paymentId) {
    try {
      var params = RefundListParams.builder().setPaymentIntent(paymentId).build();
      var refunds = com.stripe.model.Refund.list(params);
      var result = refunds.getData().stream().map(this::mapToRefund).collect(Collectors.toList());
      return Result.ok(result);
    } catch (StripeException e) {
      logger.error("Failed to list refunds: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("REFUND.LIST_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to list refunds: " + e.getMessage()));
    }
  }

  @Override
  public Result<Refund> cancelRefund(String refundId) {
    try {
      var refund = com.stripe.model.Refund.retrieve(refundId);
      var canceled = refund.cancel();
      return Result.ok(mapToRefund(canceled));
    } catch (StripeException e) {
      logger.error("Failed to cancel refund: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("REFUND.CANCEL_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to cancel refund: " + e.getMessage()));
    }
  }

  private Refund mapToRefund(com.stripe.model.Refund refund) {
    return Refund.builder()
        .id(refund.getId())
        .paymentId(refund.getPaymentIntent())
        .amount(BigDecimal.valueOf(refund.getAmount()))
        .currency(refund.getCurrency())
        .status(refund.getStatus())
        .reason(refund.getReason())
        .createdAt(Instant.ofEpochSecond(refund.getCreated()))
        .metadata(refund.getMetadata() != null ? refund.getMetadata() : Map.of())
        .build();
  }
}
