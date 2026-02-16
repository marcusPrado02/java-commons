package com.marcusprado02.commons.ports.notification;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Set;

/**
 * Port for sending push notifications to mobile devices.
 *
 * <p>Supports multiple notification platforms (FCM, APNS) behind a unified interface.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * PushNotificationPort notificationPort = // injected
 *
 * PushNotification notification = PushNotification.builder()
 *     .title("New Message")
 *     .body("You have a new message")
 *     .data(Map.of("messageId", "123"))
 *     .build();
 *
 * NotificationTarget target = NotificationTarget.device("device-token-123");
 *
 * Result<SendNotificationResult> result = notificationPort.send(target, notification);
 * }</pre>
 */
public interface PushNotificationPort {

  /**
   * Sends a push notification to the specified target.
   *
   * @param target the notification target (device, topic, or multiple devices)
   * @param notification the notification content
   * @return result containing send outcome
   */
  Result<SendNotificationResult> send(NotificationTarget target, PushNotification notification);

  /**
   * Subscribes a device token to a topic.
   *
   * @param deviceToken the device token
   * @param topic the topic name
   * @return result indicating success or failure
   */
  Result<Void> subscribeToTopic(String deviceToken, String topic);

  /**
   * Subscribes multiple device tokens to a topic.
   *
   * @param deviceTokens the device tokens
   * @param topic the topic name
   * @return result indicating success or failure
   */
  Result<Void> subscribeToTopic(Set<String> deviceTokens, String topic);

  /**
   * Unsubscribes a device token from a topic.
   *
   * @param deviceToken the device token
   * @param topic the topic name
   * @return result indicating success or failure
   */
  Result<Void> unsubscribeFromTopic(String deviceToken, String topic);

  /**
   * Unsubscribes multiple device tokens from a topic.
   *
   * @param deviceTokens the device tokens
   * @param topic the topic name
   * @return result indicating success or failure
   */
  Result<Void> unsubscribeFromTopic(Set<String> deviceTokens, String topic);

  /**
   * Validates a device token.
   *
   * @param deviceToken the device token to validate
   * @return result with true if valid, false otherwise
   */
  Result<Boolean> validateToken(String deviceToken);
}
