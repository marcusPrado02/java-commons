package com.marcusprado02.commons.app.webhooks;

import com.marcusprado02.commons.kernel.result.Result;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing webhook delivery records.
 *
 * <p>Tracks delivery attempts, retries, and status of webhook events.
 */
public interface WebhookDeliveryRepository {

  /**
   * Saves a webhook delivery record.
   *
   * @param delivery the delivery to save
   * @return result containing the saved delivery
   */
  Result<WebhookDelivery> save(WebhookDelivery delivery);

  /**
   * Finds a delivery by ID.
   *
   * @param id the delivery ID
   * @return result containing optional delivery
   */
  Result<Optional<WebhookDelivery>> findById(String id);

  /**
   * Finds all deliveries for a specific event.
   *
   * @param eventId the event ID
   * @return result containing list of deliveries
   */
  Result<List<WebhookDelivery>> findByEventId(String eventId);

  /**
   * Finds all deliveries for a specific webhook.
   *
   * @param webhookId the webhook ID
   * @return result containing list of deliveries
   */
  Result<List<WebhookDelivery>> findByWebhookId(String webhookId);

  /**
   * Finds all deliveries scheduled before the specified time with the given status.
   *
   * @param before the time threshold
   * @param status the delivery status
   * @return result containing list of deliveries
   */
  Result<List<WebhookDelivery>> findScheduledBefore(Instant before, WebhookDeliveryStatus status);

  /**
   * Deletes deliveries older than the specified time.
   *
   * @param before the time threshold
   * @return result indicating number of deleted records
   */
  Result<Integer> deleteOlderThan(Instant before);
}
