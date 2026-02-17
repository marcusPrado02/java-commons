package com.marcusprado02.commons.ports.payment;

import com.marcusprado02.commons.kernel.result.Result;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Refund service port interface for payment reversals.
 *
 * <p>This interface defines the contract for refund operations with payment providers.
 * Implementations should handle: - Full and partial refunds - Refund retrieval and listing - Refund
 * status tracking
 *
 * <p>All methods return Result<T> for consistent error handling.
 */
public interface RefundService {

  /**
   * Create a refund for a payment.
   *
   * @param paymentId payment identifier to refund
   * @param amount refund amount (null for full refund)
   * @param reason refund reason (duplicate, fraudulent, requested_by_customer, etc.)
   * @param metadata additional key-value metadata
   * @return Result containing the created Refund
   */
  Result<Refund> createRefund(
      String paymentId, BigDecimal amount, String reason, Map<String, String> metadata);

  /**
   * Retrieve a refund by ID.
   *
   * @param refundId refund identifier
   * @return Result containing the Refund
   */
  Result<Refund> getRefund(String refundId);

  /**
   * List all refunds for a payment.
   *
   * @param paymentId payment identifier
   * @return Result containing list of Refunds
   */
  Result<List<Refund>> listRefunds(String paymentId);

  /**
   * Cancel a refund before it's completed (if supported by provider).
   *
   * @param refundId refund identifier
   * @return Result containing the canceled Refund
   */
  Result<Refund> cancelRefund(String refundId);
}
