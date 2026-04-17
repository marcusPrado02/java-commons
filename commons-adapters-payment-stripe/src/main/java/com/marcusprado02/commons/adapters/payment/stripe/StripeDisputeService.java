package com.marcusprado02.commons.adapters.payment.stripe;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Dispute;
import com.stripe.param.DisputeListParams;
import com.stripe.param.DisputeUpdateParams;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stripe implementation for dispute management.
 *
 * <p>Supports retrieving, listing, responding to, and closing Stripe disputes (chargebacks).
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var disputeService = StripeDisputeService.create("sk_test_...");
 *
 * // List open disputes
 * disputeService.listDisputes("ch_123", null).peek(disputes -> ...);
 *
 * // Respond with evidence
 * disputeService.submitEvidence("dp_123", Map.of(
 *     "product_description", "Digital software license",
 *     "customer_email_address", "customer@example.com"
 * ));
 * }</pre>
 */
public final class StripeDisputeService {

  private static final Logger logger = LoggerFactory.getLogger(StripeDisputeService.class);

  private StripeDisputeService(String apiKey) {
    Stripe.apiKey = apiKey;
  }

  /**
   * Creates a new StripeDisputeService instance.
   *
   * @param apiKey Stripe API key
   * @return new StripeDisputeService instance
   */
  public static StripeDisputeService create(String apiKey) {
    return new StripeDisputeService(apiKey);
  }

  /**
   * Retrieves a dispute by its ID.
   *
   * @param disputeId Stripe dispute ID (starts with {@code dp_})
   * @return dispute details
   */
  public Result<DisputeRecord> getDispute(String disputeId) {
    try {
      var dispute = Dispute.retrieve(disputeId);
      return Result.ok(mapToRecord(dispute));
    } catch (StripeException e) {
      logger.error("Failed to retrieve dispute {}: {}", disputeId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("DISPUTE.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Dispute not found: " + disputeId));
    }
  }

  /**
   * Lists disputes, optionally filtered by charge ID and status.
   *
   * @param chargeId filter by charge (optional, pass null to omit)
   * @param status filter by status e.g. {@code "needs_response"} (optional, pass null to omit)
   * @return list of matching disputes
   */
  public Result<List<DisputeRecord>> listDisputes(String chargeId, String status) {
    try {
      var paramsBuilder = DisputeListParams.builder().setLimit(100L);
      if (chargeId != null && !chargeId.isBlank()) {
        paramsBuilder.setCharge(chargeId);
      }

      var disputes = Dispute.list(paramsBuilder.build());
      var records =
          disputes.getData().stream()
              .filter(d -> status == null || status.equals(d.getStatus()))
              .map(this::mapToRecord)
              .collect(Collectors.toList());

      return Result.ok(records);

    } catch (StripeException e) {
      logger.error("Failed to list disputes: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("DISPUTE.LIST_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to list disputes: " + e.getMessage()));
    }
  }

  /**
   * Submits evidence for a dispute.
   *
   * <p>The {@code evidence} map accepts any of the Stripe evidence fields documented at
   * https://stripe.com/docs/api/disputes/update. Common fields:
   *
   * <ul>
   *   <li>{@code product_description}
   *   <li>{@code customer_email_address}
   *   <li>{@code customer_name}
   *   <li>{@code shipping_tracking_number}
   *   <li>{@code uncategorized_text}
   * </ul>
   *
   * @param disputeId dispute to respond to
   * @param evidence key-value evidence fields
   * @param submit if {@code true}, submits the evidence to Stripe immediately
   * @return updated dispute
   */
  public Result<DisputeRecord> submitEvidence(
      String disputeId, Map<String, String> evidence, boolean submit) {
    try {
      var evidenceBuilder = DisputeUpdateParams.Evidence.builder();

      for (var entry : evidence.entrySet()) {
        switch (entry.getKey()) {
          case "product_description" -> evidenceBuilder.setProductDescription(entry.getValue());
          case "customer_email_address" ->
              evidenceBuilder.setCustomerEmailAddress(entry.getValue());
          case "customer_name" -> evidenceBuilder.setCustomerName(entry.getValue());
          case "shipping_tracking_number" ->
              evidenceBuilder.setShippingTrackingNumber(entry.getValue());
          case "uncategorized_text" -> evidenceBuilder.setUncategorizedText(entry.getValue());
          case "billing_address" -> evidenceBuilder.setBillingAddress(entry.getValue());
          case "customer_purchase_ip" -> evidenceBuilder.setCustomerPurchaseIp(entry.getValue());
          case "receipt" -> evidenceBuilder.setReceipt(entry.getValue());
          case "refund_policy" -> evidenceBuilder.setRefundPolicy(entry.getValue());
          case "refund_policy_disclosure" ->
              evidenceBuilder.setRefundPolicyDisclosure(entry.getValue());
          case "service_date" -> evidenceBuilder.setServiceDate(entry.getValue());
          case "service_documentation" -> evidenceBuilder.setServiceDocumentation(entry.getValue());
          default -> logger.debug("Ignoring unknown evidence field: {}", entry.getKey());
        }
      }

      var params =
          DisputeUpdateParams.builder()
              .setEvidence(evidenceBuilder.build())
              .setSubmit(submit)
              .build();

      var dispute = Dispute.retrieve(disputeId);
      var updated = dispute.update(params);

      logger.info("Submitted evidence for dispute: {} (submitted={})", disputeId, submit);
      return Result.ok(mapToRecord(updated));

    } catch (StripeException e) {
      logger.error("Failed to submit evidence for dispute {}: {}", disputeId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("DISPUTE.EVIDENCE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to submit dispute evidence: " + e.getMessage()));
    }
  }

  /**
   * Accepts a dispute (concedes without submitting evidence).
   *
   * @param disputeId dispute to close
   * @return updated dispute with {@code status=lost}
   */
  public Result<DisputeRecord> acceptDispute(String disputeId) {
    try {
      var dispute = Dispute.retrieve(disputeId);
      var closed = dispute.close();

      logger.info("Accepted (closed) dispute: {}", disputeId);
      return Result.ok(mapToRecord(closed));

    } catch (StripeException e) {
      logger.error("Failed to accept dispute {}: {}", disputeId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("DISPUTE.ACCEPT_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.WARNING,
              "Failed to accept dispute: " + e.getMessage()));
    }
  }

  // -------------------------------------------------------------------------
  // Domain model
  // -------------------------------------------------------------------------

  /**
   * Represents a Stripe dispute (chargeback).
   *
   * @param id Stripe dispute ID
   * @param chargeId related charge ID
   * @param amount amount disputed in minor units
   * @param currency currency code
   * @param status dispute status
   * @param reason reason for the dispute
   * @param createdAt when the dispute was created
   * @param metadata additional metadata
   */
  public record DisputeRecord(
      String id,
      String chargeId,
      long amount,
      String currency,
      String status,
      String reason,
      Instant createdAt,
      Map<String, String> metadata) {}

  // -------------------------------------------------------------------------
  // Mapping
  // -------------------------------------------------------------------------

  private DisputeRecord mapToRecord(Dispute d) {
    return new DisputeRecord(
        d.getId(),
        d.getCharge(),
        d.getAmount(),
        d.getCurrency(),
        d.getStatus(),
        d.getReason(),
        Instant.ofEpochSecond(d.getCreated()),
        d.getMetadata() != null ? d.getMetadata() : Map.of());
  }
}
