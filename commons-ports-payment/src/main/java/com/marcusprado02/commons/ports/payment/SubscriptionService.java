package com.marcusprado02.commons.ports.payment;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import java.util.Map;

/**
 * Subscription service port interface for recurring payment management.
 *
 * <p>This interface defines the contract for subscription management with payment providers.
 * Implementations should handle: - Subscription creation and management - Subscription updates and
 * cancellation - Subscription retrieval and listing
 *
 * <p>All methods return Result<T> for consistent error handling.
 */
public interface SubscriptionService {

  /**
   * Create a new subscription for a customer.
   *
   * @param customerId customer identifier
   * @param priceId price/plan identifier
   * @param paymentMethodId payment method to use for billing
   * @param metadata additional key-value metadata
   * @return Result containing the created Subscription
   */
  Result<Subscription> createSubscription(
      String customerId, String priceId, String paymentMethodId, Map<String, String> metadata);

  /**
   * Retrieve a subscription by ID.
   *
   * @param subscriptionId subscription identifier
   * @return Result containing the Subscription
   */
  Result<Subscription> getSubscription(String subscriptionId);

  /**
   * List all subscriptions for a customer.
   *
   * @param customerId customer identifier
   * @return Result containing list of Subscriptions
   */
  Result<List<Subscription>> listSubscriptions(String customerId);

  /**
   * Update a subscription (change plan, payment method, metadata, etc.).
   *
   * @param subscriptionId subscription identifier
   * @param priceId new price/plan identifier (optional)
   * @param paymentMethodId new payment method (optional)
   * @param metadata metadata to update
   * @return Result containing the updated Subscription
   */
  Result<Subscription> updateSubscription(
      String subscriptionId, String priceId, String paymentMethodId, Map<String, String> metadata);

  /**
   * Cancel a subscription.
   *
   * @param subscriptionId subscription identifier
   * @param immediately whether to cancel immediately or at period end
   * @return Result containing the canceled Subscription
   */
  Result<Subscription> cancelSubscription(String subscriptionId, boolean immediately);

  /**
   * Resume a canceled subscription (before it ends).
   *
   * @param subscriptionId subscription identifier
   * @return Result containing the resumed Subscription
   */
  Result<Subscription> resumeSubscription(String subscriptionId);
}
