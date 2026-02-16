package com.marcusprado02.commons.adapters.notification.apns;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
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
        return Result.fail(
            Problem.of(
                ErrorCode.of("TOPIC_NOT_SUPPORTED"),
                ErrorCategory.VALIDATION,
                Severity.ERROR,
                "APNS does not support topic-based broadcasting. Use FCM for topic subscriptions."));
      } else if (target.isMultiDevice()) {
        return sendToMultipleDevices(target.getDeviceTokens(), notification);
      } else {
        return Result.fail(
            Problem.of(
                ErrorCode.of("INVALID_TARGET"),
                ErrorCategory.VALIDATION,
                Severity.ERROR,
                "Unknown target type"));
      }
    } catch (Exception e) {
      logger.error("Failed to send notification", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SEND_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send notification: " + e.getMessage()));
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
        String reason = response.getRejectionReason().orElse("Unknown");
        logger.error("Notification rejected by APNS: token={}, reason={}", token, reason);
        return Result.fail(
            Problem.of(
                ErrorCode.of("APNS_REJECTED"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "APNS rejected notification: " + reason));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Send interrupted: token={}", token, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SEND_INTERRUPTED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Send operation was interrupted"));
    } catch (ExecutionException e) {
      logger.error("Send failed: token={}", token, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SEND_TO_DEVICE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
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
          String reason = response.getRejectionReason().orElse("Unknown");
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
    return Result.fail(
        Problem.of(
            ErrorCode.of("TOPIC_NOT_SUPPORTED"),
            ErrorCategory.VALIDATION,
            Severity.ERROR,
            "APNS does not support topic subscriptions. Use FCM for topic management."));
  }

  @Override
  public Result<Boolean> validateToken(String deviceToken) {
    // APNS doesn't have a direct validation API
    // Would need to perform actual send attempt to validate
    return Result.ok(true);
  }

  /**
   * Closes the APNS client and releases resources.
   *
   * @throws RuntimeException if close operation fails
   */
  public void close() {
    try {
      apnsClient.close().get();
      logger.info("APNS client closed");
    } catch (Exception e) {
      logger.error("Failed to close APNS client", e);
      throw new RuntimeException("Failed to close APNS client", e);
    }
  }

  /**
   * Builds APNS JSON payload from PushNotification.
   *
   * @param notification notification to convert
   * @return JSON payload string
   */
  private String buildApnsPayload(PushNotification notification) {
    Map<String, Object> aps = new HashMap<>();
    Map<String, Object> alert = new HashMap<>();

    if (notification.title() != null) {
      alert.put("title", notification.title());
    }
    if (notification.body() != null) {
      alert.put("body", notification.body());
    }

    if (!alert.isEmpty()) {
      aps.put("alert", alert);
    }

    if (notification.badge() != null) {
      aps.put("badge", notification.badge());
    }

    if (notification.sound() != null) {
      aps.put("sound", notification.sound());
    } else {
      aps.put("sound", "default");
    }

    // Content-available for background updates
    if (notification.priority() == NotificationPriority.HIGH) {
      aps.put("content-available", 1);
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("aps", aps);

    // Add custom data
    if (notification.data() != null && !notification.data().isEmpty()) {
      payload.putAll(notification.data());
    }

    // Simple JSON serialization (production code should use Jackson/Gson)
    return toJson(payload);
  }

  private String toJson(Map<String, Object> map) {
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (!first) {
        sb.append(",");
      }
      first = false;
      sb.append("\"").append(entry.getKey()).append("\":");
      Object value = entry.getValue();
      if (value instanceof String) {
        sb.append("\"").append(escape((String) value)).append("\"");
      } else if (value instanceof Number) {
        sb.append(value);
      } else if (value instanceof Map) {
        sb.append(toJson((Map<String, Object>) value));
      } else {
        sb.append("null");
      }
    }
    sb.append("}");
    return sb.toString();
  }

  private String escape(String str) {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
