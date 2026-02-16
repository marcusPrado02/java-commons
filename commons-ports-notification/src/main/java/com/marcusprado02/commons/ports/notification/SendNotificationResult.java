package com.marcusprado02.commons.ports.notification;

import java.util.List;
import java.util.Map;

/**
 * Result of sending a push notification.
 *
 * <p>For single-device notifications, check {@link #success()} For multi-device notifications,
 * check {@link #successCount()} and {@link #failures()}
 *
 * <p>Example:
 *
 * <pre>{@code
 * Result<SendNotificationResult> result = notificationPort.send(target, notification);
 *
 * if (result.isOk()) {
 *     SendNotificationResult sendResult = result.getOrNull();
 *     if (sendResult.ok()) {
 *         logger.info("Notification sent successfully: {}", sendResult.messageId());
 *     } else {
 *         logger.error("Notification failed: {}", sendResult.failureReason());
 *     }
 * }
 * }</pre>
 */
public record SendNotificationResult(
    boolean success,
    String messageId,
    String failureReason,
    int successCount,
    int failureCount,
    Map<String, String> failures) {

  public SendNotificationResult {
    failures = failures != null ? Map.copyOf(failures) : Map.of();
  }

  public static SendNotificationResult success(String messageId) {
    return new SendNotificationResult(true, messageId, null, 1, 0, Map.of());
  }

  public static SendNotificationResult failure(String reason) {
    return new SendNotificationResult(false, null, reason, 0, 1, Map.of());
  }

  public static SendNotificationResult multicast(
      int successCount, int failureCount, Map<String, String> failures) {
    boolean success = failureCount == 0;
    return new SendNotificationResult(success, null, null, successCount, failureCount, failures);
  }

  public boolean hasFailures() {
    return failureCount > 0;
  }

  public List<String> getFailedTokens() {
    return List.copyOf(failures.keySet());
  }
}
