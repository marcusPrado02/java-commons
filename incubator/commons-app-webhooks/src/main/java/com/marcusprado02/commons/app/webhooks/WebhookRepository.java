package com.marcusprado02.commons.app.webhooks;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing webhook registrations.
 *
 * <p>Provides CRUD operations for webhook endpoints.
 */
public interface WebhookRepository {

  /**
   * Saves a webhook registration.
   *
   * @param webhook the webhook to save
   * @return result containing the saved webhook
   */
  Result<Webhook> save(Webhook webhook);

  /**
   * Finds a webhook by ID.
   *
   * @param id the webhook ID
   * @return result containing optional webhook
   */
  Result<Optional<Webhook>> findById(String id);

  /**
   * Finds all active webhooks subscribed to the specified event.
   *
   * @param eventType the event type
   * @return result containing list of webhooks
   */
  Result<List<Webhook>> findByEventType(String eventType);

  /**
   * Finds all webhooks.
   *
   * @return result containing list of all webhooks
   */
  Result<List<Webhook>> findAll();

  /**
   * Deletes a webhook by ID.
   *
   * @param id the webhook ID
   * @return result indicating success
   */
  Result<Void> deleteById(String id);
}
