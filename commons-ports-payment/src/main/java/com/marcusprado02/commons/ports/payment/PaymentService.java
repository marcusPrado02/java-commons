package com.marcusprado02.commons.ports.payment;

import com.marcusprado02.commons.kernel.result.Result;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Payment service port interface for payment processing operations.
 *
 * <p>This interface defines the contract for payment providers like Stripe, PayPal, etc.
 * Implementations should handle: - Payment intent creation and processing - Payment method
 * management - Payment retrieval and listing - Payment cancellation
 *
 * <p>All methods return Result<T> for consistent error handling.
 */
public interface PaymentService {

  /**
   * Create a new payment intent.
   *
   * @param amount payment amount in minor units (e.g., cents)
   * @param currency ISO 4217 currency code (USD, EUR, etc.)
   * @param customerId customer identifier
   * @param paymentMethodId payment method identifier (optional)
   * @param description payment description (optional)
   * @param metadata additional key-value metadata
   * @return Result containing the created Payment
   */
  Result<Payment> createPayment(
      BigDecimal amount,
      String currency,
      String customerId,
      String paymentMethodId,
      String description,
      Map<String, String> metadata);

  /**
   * Retrieve a payment by ID.
   *
   * @param paymentId payment identifier
   * @return Result containing the Payment
   */
  Result<Payment> getPayment(String paymentId);

  /**
   * List all payments for a customer.
   *
   * @param customerId customer identifier
   * @param limit maximum number of payments to return
   * @return Result containing list of Payments
   */
  Result<List<Payment>> listPayments(String customerId, int limit);

  /**
   * Cancel a payment before it's completed.
   *
   * @param paymentId payment identifier
   * @return Result containing the canceled Payment
   */
  Result<Payment> cancelPayment(String paymentId);

  /**
   * Confirm a payment (for payment methods requiring confirmation).
   *
   * @param paymentId payment identifier
   * @return Result containing the confirmed Payment
   */
  Result<Payment> confirmPayment(String paymentId);

  /**
   * Create a payment method for a customer.
   *
   * @param customerId customer identifier
   * @param type payment method type (card, bank_account, etc.)
   * @param details payment method details (card number, routing number, etc.)
   * @return Result containing the created PaymentMethod
   */
  Result<PaymentMethod> createPaymentMethod(
      String customerId, String type, Map<String, String> details);

  /**
   * Retrieve a payment method by ID.
   *
   * @param paymentMethodId payment method identifier
   * @return Result containing the PaymentMethod
   */
  Result<PaymentMethod> getPaymentMethod(String paymentMethodId);

  /**
   * List all payment methods for a customer.
   *
   * @param customerId customer identifier
   * @return Result containing list of PaymentMethods
   */
  Result<List<PaymentMethod>> listPaymentMethods(String customerId);

  /**
   * Delete a payment method.
   *
   * @param paymentMethodId payment method identifier
   * @return Result indicating success or failure
   */
  Result<Void> deletePaymentMethod(String paymentMethodId);
}
