/**
 * Push notification abstractions for mobile devices.
 *
 * <p>This package provides a platform-agnostic API for sending push notifications to mobile devices
 * through services like Firebase Cloud Messaging (FCM) and Apple Push Notification Service (APNS).
 *
 * <p><b>Core Components:</b>
 *
 * <ul>
 *   <li>{@link PushNotificationPort} - Main interface for sending notifications
 *   <li>{@link PushNotification} - Notification content (title, body, data)
 *   <li>{@link NotificationTarget} - Targeting (device, topic, multi-device)
 *   <li>{@link SendNotificationResult} - Send operation result
 *   <li>{@link NotificationPriority} - Delivery priority (HIGH/NORMAL)
 * </ul>
 *
 * <p><b>Quick Start:</b>
 *
 * <pre>{@code
 * // Create notification
 * PushNotification notification = PushNotification.builder()
 *     .title("New Message")
 *     .body("You have a new message")
 *     .data(Map.of("messageId", "123"))
 *     .priority(NotificationPriority.HIGH)
 *     .build();
 *
 * // Send to device
 * NotificationTarget target = NotificationTarget.device("device-token");
 * Result<SendNotificationResult> result = port.send(target, notification);
 *
 * // Send to topic (broadcast)
 * NotificationTarget topicTarget = NotificationTarget.topic("news");
 * Result<SendNotificationResult> result = port.send(topicTarget, notification);
 *
 * // Topic subscriptions
 * port.subscribeToTopic("device-token", "news");
 * port.unsubscribeFromTopic("device-token", "news");
 * }</pre>
 *
 * <p><b>Implementations:</b>
 *
 * <ul>
 *   <li>commons-adapters-notification-fcm - Firebase Cloud Messaging
 *   <li>commons-adapters-notification-apns - Apple Push Notification Service
 * </ul>
 *
 * @see PushNotificationPort
 * @see PushNotification
 */
package com.marcusprado02.commons.ports.notification;
