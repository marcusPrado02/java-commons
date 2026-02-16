package com.marcusprado02.commons.adapters.notification.apns;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorDetails;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.notification.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apple Push Notification Service (APNS) implementation of {@link PushNotificationPort}.
 *
 * <p>Supports sending notifications to iOS, iPadOS, macOS, watchOS, and tvOS devices.
 *
 * <p><b>Limitations:</b>
 *
 * <ul>
 *   <li>APNS does not support topic subscriptions (use FCM for topics)
 *   <li>Token validation requires actual send attempt (dry run not available)
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * ApnsConfiguration config = ApnsConfiguration.builder()
 *     .p8KeyPath("/path/to/AuthKey.p8")
 *     .teamId("TEAM123")
 *     .keyId("KEY123")
 *     .topic("com.example.app")
 *     .production(true)
 *     .build();
 *
 * PushNotificationPort apns = new ApnsPushNotificationAdapter(config);
 *
 * PushNotification notification = PushNotification.builder()
 *     .title("Hello")
 *     .body("World")
 *     .badge(1)
 *     .sound("default")
 *     .build();
 *
 * Result<SendNotificationResult> result =
 *     apns.send(NotificationTarget.device("device-token"), notification);
 * }</pre>
 */
public class ApnsPushNotificationAdapter implements PushNotificationPort {

  private static final Logger logger = LoggerFactory.getLogger(ApnsPushNotificationAdapter.class);

  private final ApnsClient apnsClient;
  private final String defaultTopic;

  public ApnsPushNotificationAdapter(ApnsConfiguration config) throws IOException {
    ApnsClientBuilder builder = new ApnsClientBuilder();
    this.apnsClient = config.configureClientBuilder(builder).build();
    this.defaultTopic = config.getTopic();
    logger.info(
        "APNS adapter initialized: production={}, topic={}", config.isProduction(), defaultTopic);
  }

  /**
   * Constructor for testing with custom ApnsClient.
   *
   * @param apnsClient APNS client instance
   * @param defaultTopic default app bundle ID
   */
  public ApnsPushNotificationAdapter(ApnsClient apnsClient, String defaultTopic) {
    this.apnsClient = apnsClient;
    this.defaultTopic = defaultTopic;
  }

  @Override
  public Result<SendNotificationResult> send(
      NotificationTarget target, PushNotification notification) {
    try {
      if (target.isDevice()) {
        return sendToDevice(target.getDeviceToken(), notification);
      } else if (target.isTopic()) {
        return Result.failure(
            ErrorDetails.of(
                "TOPIC_NOT_SUPPORTED",
                "APNS does not support topic-based broadcasting. Use FCM for topic subscriptions.",
                ErrorCategory.VALIDATION,
                Map.of("topic", target.getTopic())));
      } else if (target.isMultiDevice()) {
        return sendToMultipleDevices(target.getDeviceTokens(), notification);
      } else {
        return Result.failure(
            ErrorDetails.of(
                "INVALID_TARGET",
                "Unknown target type",
                ErrorCategory.VALIDATION,
                Map.of("targetType", target.getType().toString())));
      }
    } catch (Exception e) {
      logger.error("Failed to send notification", e);
      return Result.failure(
          ErrorDetails.of(
              "SEND_FAILED",
              "Failed to send notification: " + e.getMessage(),
              ErrorCategory.TECHNICAL,
              Map.of("exception", e.getClass().getSimpleName())));
    }
  }

  private Result<SendNotificationResult> sendToDevice(String token, PushNotification notification) {
    try {
      String payload = buildApnsPayload(notification);
      SimpleApnsPushNotification pushNotification =
          new SimpleApnsPushNotification(token, defaultTopic, payload);

      PushNotificationResponse<SimpleApnsPushNotification> response =
          apnsClient.sendNotification(pushNotification).get();

      if (response.isAccepted()) {
        String messageId = response.getApnsId() != null ? response.getApnsId().toString() : null;
        logger.debug("Notification sent to device: apnsId={}", messageId);
        return Result.ok(SendNotificationResult.success(messageId));
      } else {
        String reason =
            response.getRejectionReason() != null ? response.getRejectionReason() : "Unknown";
        logger.error("Notification rejected by APNS: token={}, reason={}", token, reason);
        return Result.failure(
            ErrorDetails.of(
                "APNS_REJECTED",
                "APNS rejected notification: " + reason,
                ErrorCategory.TECHNICAL,
                Map.of("token", token, "reason", reason)));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Send interrupted: token={}", token, e);
      return Result.failure(
          ErrorDetails.of(
              "SEND_INTERRUPTED",
              "Send operation was interrupted",
              ErrorCategory.TECHNICAL,
              Map.of("token", token)));
    } catch (ExecutionException e) {
      logger.error("Send failed: token={}", token, e);
      return Result.failure(
          ErrorDetails.of(
              "SEND_TO_DEVICE_FAILED",
              e.getCause() != null ? e.getCause().getMessage() : e.getMessage(),
              ErrorCategory.TECHNICAL,
              Map.of("token", token)));
    }
  }

  private Result<SendNotificationResult> sendToMultipleDevices(
      Set<String> tokens, PushNotification notification) {
    int successCount = 0;
    int failureCount = 0;
    Map<String, String> failures = new HashMap<>();

    String payload = buildApnsPayload(notification);

    for (String token : tokens) {
      try {
        SimpleApnsPushNotification pushNotification =
            new SimpleApnsPushNotification(token, defaultTopic, payload);

        PushNotificationResponse<SimpleApnsPushNotification> response =
            apnsClient.sendNotification(pushNotification).get();

        if (response.isAccepted()) {
          successCount++;
          logger.debug("Notification sent: token={}, apnsId={}", token, response.getApnsId());
        } else {
          failureCount++;
          String reason =
              response.getRejectionReason() != null ? response.getRejectionReason() : "Unknown";
          failures.put(token, reason);
          logger.warn("Notification rejected: token={}, reason={}", token, reason);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        failureCount++;
        failures.put(token, "Interrupted");
        logger.error("Send interrupted: token={}", token, e);
      } catch (ExecutionException e) {
        failureCount++;
        String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        failures.put(token, errorMessage);
        logger.error("Send failed: token={}", token, e);
      }
    }

    return Result.ok(SendNotificationResult.multicast(successCount, failureCount, failures));
  }

  @Override
  public Result<Void> subscribeToTopic(String deviceToken, String topic) {
    return unsupportedTopicOperation("subscribeToTopic");
  }

  @Override
  public Result<Void> subscribeToTopic(Set<String> deviceTokens, String topic) {
    return unsupportedTopicOperation("subscribeToTopic");
  }

  @Override
  public Result<Void> unsubscribeFromTopic(String deviceToken, String topic) {
    return unsupportedTopicOperation("unsubscribeFromTopic");
  }

  @Override
  public Result<Void> unsubscribeFromTopic(Set<String> deviceTokens, String topic) {
    return unsupportedTopicOperation("unsubscribeFromTopic");
  }

  private Result<Void> unsupportedTopicOperation(String operation) {
    return Result.failure(
        ErrorDetails.of(
            "TOPIC_NOT_SUPPORTED",
            "APNS does not support topic subscriptions. Use FCM for topic management.",
            ErrorCategory.VALIDATION,
            Map.of("operation", operation)));
  }

  @Override
  public Result<Boolean> validateToken(String deviceToken) {
    // APNS doesn't have a direct validation API
    // We'd need to attempt a send to validate
    logger.warn(
        "APNS does not support token validation without sending. Returning true by default.");
    return Result.ok(true);
  }

  private String buildApnsPayload(PushNotification notification) {
    Map<String, Object> payload = new HashMap<>();

    // Build APS dictionary
    Map<String, Object> aps = new HashMap<>();

    if (notification.hasVisualContent()) {
      // Build alert
      Map<String, String> alert = new HashMap<>();
      alert.put("title", notification.title());
      alert.put("body", notification.body());
      aps.put("alert", alert);

      // Badge
      if (notification.badge() != null) {
        aps.put("badge", notification.badge());
      }

      // Sound
      if (notification.sound() != null) {
        aps.put("sound", notification.sound());
      }

      // Click action (category)
      if (notification.clickAction() != null) {
        aps.put("category", notification.clickAction());
      }
    } else if (notification.hasData()) {
      // Silent notification (data-only)
      aps.put("content-available", 1);
    }

    // Priority (implicitly handled by APNS based on content)
    if (notification.priority() == NotificationPriority.HIGH) {
      // High priority is implicit when alert is present
      // For silent notifications, use apns-priority header (handled by Pushy)
    }

    payload.put("aps", aps);

    // Add custom data
    if (notification.hasData()) {
      payload.putAll(notification.data());
    }

    return toJson(payload);
  }

  private String toJson(Map<String, Object> map) {
    // Simple JSON serialization (for production, use Jackson or Gson)
    StringBuilder json = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (!first) {
        json.append(",");
      }
      json.append("\"").append(entry.getKey()).append("\":");
      json.append(serializeValue(entry.getValue()));
      first = false;
    }
    json.append("}");
    return json.toString();
  }

  private String serializeValue(Object value) {
    if (value == null) {
      return "null";
    } else if (value instanceof String) {
      return "\"" + escapeJson((String) value) + "\"";
    } else if (value instanceof Number) {
      return value.toString();
    } else if (value instanceof Boolean) {
      return value.toString();
    } else if (value instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) value;
      return toJson(map);
    } else if (value instanceof Collection) {
      Collection<?> collection = (Collection<?>) value;
      StringBuilder json = new StringBuilder("[");
      boolean first = true;
      for (Object item : collection) {
        if (!first) {
          json.append(",");
        }
        json.append(serializeValue(item));
        first = false;
      }
      json.append("]");
      return json.toString();
    } else {
      return "\"" + escapeJson(value.toString()) + "\"";
    }
  }

  private String escapeJson(String str) {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  /** Closes the APNS client and releases resources. */
  public void close() {
    try {
      apnsClient.close().get();
      logger.info("APNS client closed");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Failed to close APNS client", e);
    } catch (ExecutionException e) {
      logger.error("Failed to close APNS client", e);
    }
  }
}
