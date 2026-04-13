package com.marcusprado02.commons.adapters.notification.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.TopicManagementResponse;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.notification.NotificationPriority;
import com.marcusprado02.commons.ports.notification.NotificationTarget;
import com.marcusprado02.commons.ports.notification.PushNotification;
import com.marcusprado02.commons.ports.notification.PushNotificationPort;
import com.marcusprado02.commons.ports.notification.SendNotificationResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Firebase Cloud Messaging implementation of {@link PushNotificationPort}.
 *
 * <p>Supports sending notifications to:
 *
 * <ul>
 *   <li>Single device tokens
 *   <li>Multiple device tokens (multicast)
 *   <li>Topics (broadcast to subscribers)
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * FcmConfiguration config = FcmConfiguration.fromPath("service-account.json");
 * PushNotificationPort fcm = new FcmPushNotificationAdapter(config);
 *
 * PushNotification notification = PushNotification.builder()
 *     .title("Hello")
 *     .body("World")
 *     .build();
 *
 * Result<SendNotificationResult> result =
 *     fcm.send(NotificationTarget.device("token"), notification);
 * }</pre>
 */
public class FcmPushNotificationAdapter implements PushNotificationPort {

  private static final Logger logger = LoggerFactory.getLogger(FcmPushNotificationAdapter.class);
  private static final int MAX_MULTICAST_SIZE = 500;

  private final FirebaseMessaging messaging;
  private final boolean validateTokens;

  /** Creates a new FcmPushNotificationAdapter instance. */
  public FcmPushNotificationAdapter(FcmConfiguration config) {
    FirebaseApp app = config.initializeApp();
    this.messaging = FirebaseMessaging.getInstance(app);
    this.validateTokens = config.isValidateTokens();
    logger.info("FCM adapter initialized");
  }

  /**
   * Constructor for testing with custom FirebaseMessaging instance.
   *
   * @param messaging Firebase messaging instance
   * @param validateTokens whether to validate tokens before sending
   */
  public FcmPushNotificationAdapter(FirebaseMessaging messaging, boolean validateTokens) {
    this.messaging = messaging;
    this.validateTokens = validateTokens;
  }

  @Override
  public Result<SendNotificationResult> send(
      NotificationTarget target, PushNotification notification) {
    try {
      if (target.isDevice()) {
        return sendToDevice(target.getDeviceToken(), notification);
      } else if (target.isTopic()) {
        return sendToTopic(target.getTopic(), notification);
      } else if (target.isMultiDevice()) {
        return sendToMultipleDevices(target.getDeviceTokens(), notification);
      } else {
        return Result.fail(
            Problem.of(
                ErrorCode.of("INVALID_TARGET"),
                ErrorCategory.VALIDATION,
                Severity.ERROR,
                "Unknown target type: " + target.getType()));
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
      Message message = buildMessage(notification).setToken(token).build();
      String messageId = messaging.send(message);
      logger.debug("Notification sent to device: messageId={}", messageId);
      return Result.ok(SendNotificationResult.success(messageId));
    } catch (FirebaseMessagingException e) {
      logger.error("Failed to send to device: token={}", token, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SEND_TO_DEVICE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "FCM send failed: " + e.getMessage() + " (" + e.getMessagingErrorCode() + ")"));
    }
  }

  private Result<SendNotificationResult> sendToTopic(String topic, PushNotification notification) {
    try {
      Message message = buildMessage(notification).setTopic(topic).build();
      String messageId = messaging.send(message);
      logger.debug("Notification sent to topic: topic={}, messageId={}", topic, messageId);
      return Result.ok(SendNotificationResult.success(messageId));
    } catch (FirebaseMessagingException e) {
      logger.error("Failed to send to topic: topic={}", topic, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SEND_TO_TOPIC_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "FCM topic send failed: " + e.getMessage()));
    }
  }

  private Result<SendNotificationResult> sendToMultipleDevices(
      Set<String> tokens, PushNotification notification) {
    try {
      List<String> tokenList = new ArrayList<>(tokens);

      // FCM limits multicast to 500 tokens
      if (tokenList.size() > MAX_MULTICAST_SIZE) {
        return sendInBatches(tokenList, notification);
      }

      MulticastMessage message = buildMulticastMessage(notification, tokenList);
      BatchResponse response = messaging.sendEachForMulticast(message);

      return buildMulticastResult(response, tokenList);
    } catch (FirebaseMessagingException e) {
      logger.error("Failed to send multicast", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("MULTICAST_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "FCM multicast failed: " + e.getMessage()));
    }
  }

  private Result<SendNotificationResult> sendInBatches(
      List<String> tokens, PushNotification notification) {
    int successCount = 0;
    int failureCount = 0;
    Map<String, String> failures = new HashMap<>();

    for (int i = 0; i < tokens.size(); i += MAX_MULTICAST_SIZE) {
      int end = Math.min(i + MAX_MULTICAST_SIZE, tokens.size());
      List<String> batch = tokens.subList(i, end);

      try {
        MulticastMessage message = buildMulticastMessage(notification, batch);
        BatchResponse response = messaging.sendEachForMulticast(message);

        successCount += response.getSuccessCount();
        failureCount += response.getFailureCount();

        collectFailures(response, batch, failures);
      } catch (FirebaseMessagingException e) {
        logger.error("Batch send failed: batch=[{}-{}]", i, end, e);
        failureCount += batch.size();
        for (String token : batch) {
          failures.put(token, e.getMessage());
        }
      }
    }

    return Result.ok(SendNotificationResult.multicast(successCount, failureCount, failures));
  }

  private Result<SendNotificationResult> buildMulticastResult(
      BatchResponse response, List<String> tokens) {
    int successCount = response.getSuccessCount();
    int failureCount = response.getFailureCount();
    Map<String, String> failures = new HashMap<>();

    if (failureCount > 0) {
      collectFailures(response, tokens, failures);
    }

    return Result.ok(SendNotificationResult.multicast(successCount, failureCount, failures));
  }

  private void collectFailures(
      BatchResponse response, List<String> tokens, Map<String, String> failures) {
    List<SendResponse> responses = response.getResponses();
    for (int i = 0; i < responses.size(); i++) {
      SendResponse sendResponse = responses.get(i);
      if (!sendResponse.isSuccessful()) {
        String token = tokens.get(i);
        String errorMessage =
            sendResponse.getException() != null
                ? sendResponse.getException().getMessage()
                : "Unknown error";
        failures.put(token, errorMessage);
      }
    }
  }

  @Override
  public Result<Void> subscribeToTopic(String deviceToken, String topic) {
    return subscribeToTopic(Set.of(deviceToken), topic);
  }

  @Override
  public Result<Void> subscribeToTopic(Set<String> deviceTokens, String topic) {
    try {
      TopicManagementResponse response =
          messaging.subscribeToTopic(new ArrayList<>(deviceTokens), topic);
      if (response.getFailureCount() > 0) {
        logger.warn(
            "Topic subscription partially failed: topic={}, failures={}",
            topic,
            response.getFailureCount());
        return Result.fail(
            Problem.of(
                ErrorCode.of("SUBSCRIPTION_PARTIAL_FAILURE"),
                ErrorCategory.TECHNICAL,
                Severity.WARNING,
                String.format(
                    "%d tokens failed to subscribe to topic %s",
                    response.getFailureCount(), topic)));
      }
      logger.debug("Subscribed {} tokens to topic: {}", deviceTokens.size(), topic);
      return Result.ok(null);
    } catch (FirebaseMessagingException e) {
      logger.error("Failed to subscribe to topic: topic={}", topic, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SUBSCRIPTION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Topic subscription failed: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> unsubscribeFromTopic(String deviceToken, String topic) {
    return unsubscribeFromTopic(Set.of(deviceToken), topic);
  }

  @Override
  public Result<Void> unsubscribeFromTopic(Set<String> deviceTokens, String topic) {
    try {
      TopicManagementResponse response =
          messaging.unsubscribeFromTopic(new ArrayList<>(deviceTokens), topic);
      if (response.getFailureCount() > 0) {
        logger.warn(
            "Topic unsubscription partially failed: topic={}, failures={}",
            topic,
            response.getFailureCount());
        return Result.fail(
            Problem.of(
                ErrorCode.of("UNSUBSCRIPTION_PARTIAL_FAILURE"),
                ErrorCategory.TECHNICAL,
                Severity.WARNING,
                String.format(
                    "%d tokens failed to unsubscribe from topic %s",
                    response.getFailureCount(), topic)));
      }
      logger.debug("Unsubscribed {} tokens from topic: {}", deviceTokens.size(), topic);
      return Result.ok(null);
    } catch (FirebaseMessagingException e) {
      logger.error("Failed to unsubscribe from topic: topic={}", topic, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("UNSUBSCRIPTION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Topic unsubscription failed: " + e.getMessage()));
    }
  }

  @Override
  public Result<Boolean> validateToken(String deviceToken) {
    if (!validateTokens) {
      return Result.ok(true);
    }

    try {
      // FCM doesn't have a direct token validation API, so we try to send a dry run message
      Message message =
          Message.builder().setToken(deviceToken).putData("validation", "true").build();

      // Use dry run to validate without actually sending
      messaging.send(message, true);
      return Result.ok(true);
    } catch (FirebaseMessagingException e) {
      if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT
          || e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
        return Result.ok(false);
      }
      logger.error("Token validation failed: token={}", deviceToken, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("VALIDATION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Token validation failed: " + e.getMessage()));
    }
  }

  /** Shared payload components for both single-message and multicast builders. */
  private record MessagePayload(
      Notification notification,
      Map<String, String> data,
      AndroidConfig androidConfig,
      ApnsConfig apnsConfig) {}

  private MessagePayload buildPayload(PushNotification notification) {
    Notification notif = null;
    if (notification.hasVisualContent()) {
      Notification.Builder nb =
          Notification.builder().setTitle(notification.title()).setBody(notification.body());
      if (notification.imageUrl() != null) {
        nb.setImage(notification.imageUrl());
      }
      notif = nb.build();
    }
    Map<String, String> data = notification.hasData() ? notification.data() : Map.of();
    return new MessagePayload(
        notif, data, buildAndroidConfig(notification), buildApnsConfig(notification));
  }

  private Message.Builder buildMessage(PushNotification notification) {
    MessagePayload p = buildPayload(notification);
    Message.Builder builder = Message.builder();
    if (p.notification() != null) {
      builder.setNotification(p.notification());
    }
    if (!p.data().isEmpty()) {
      builder.putAllData(p.data());
    }
    return builder.setAndroidConfig(p.androidConfig()).setApnsConfig(p.apnsConfig());
  }

  private MulticastMessage buildMulticastMessage(
      PushNotification notification, List<String> tokens) {
    MessagePayload p = buildPayload(notification);
    MulticastMessage.Builder builder = MulticastMessage.builder();
    if (p.notification() != null) {
      builder.setNotification(p.notification());
    }
    if (!p.data().isEmpty()) {
      builder.putAllData(p.data());
    }
    return builder
        .setAndroidConfig(p.androidConfig())
        .setApnsConfig(p.apnsConfig())
        .addAllTokens(tokens)
        .build();
  }

  private AndroidConfig buildAndroidConfig(PushNotification notification) {
    AndroidConfig.Builder androidBuilder = AndroidConfig.builder();

    // Set priority
    AndroidConfig.Priority priority =
        notification.priority() == NotificationPriority.HIGH
            ? AndroidConfig.Priority.HIGH
            : AndroidConfig.Priority.NORMAL;
    androidBuilder.setPriority(priority);

    // Set TTL
    if (notification.timeToLive() != null) {
      androidBuilder.setTtl(notification.timeToLive());
    }

    // Set notification options
    if (notification.hasVisualContent()) {
      AndroidNotification.Builder androidNotification = AndroidNotification.builder();

      if (notification.sound() != null) {
        androidNotification.setSound(notification.sound());
      }

      if (notification.clickAction() != null) {
        androidNotification.setClickAction(notification.clickAction());
      }

      androidBuilder.setNotification(androidNotification.build());
    }

    return androidBuilder.build();
  }

  private ApnsConfig buildApnsConfig(PushNotification notification) {
    ApnsConfig.Builder apnsBuilder = ApnsConfig.builder();

    // Build APS payload
    Aps.Builder apsBuilder = Aps.builder();

    if (notification.hasVisualContent()) {
      // Set alert
      ApsAlert alert =
          ApsAlert.builder().setTitle(notification.title()).setBody(notification.body()).build();
      apsBuilder.setAlert(alert);

      // Set badge
      if (notification.badge() != null) {
        apsBuilder.setBadge(notification.badge());
      }

      // Set sound
      if (notification.sound() != null) {
        apsBuilder.setSound(notification.sound());
      }
    }

    // Set content-available for data-only notifications
    if (!notification.hasVisualContent() && notification.hasData()) {
      apsBuilder.setContentAvailable(true);
    }

    apnsBuilder.setAps(apsBuilder.build());

    return apnsBuilder.build();
  }
}
