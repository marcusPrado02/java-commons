package com.marcusprado02.commons.ports.notification;

import java.util.Objects;
import java.util.Set;

/**
 * Represents the target(s) for a push notification.
 *
 * <p>Supports three targeting modes:
 *
 * <ul>
 *   <li>Device token: Send to specific device
 *   <li>Topic: Send to all devices subscribed to a topic
 *   <li>Multiple tokens: Send to multiple specific devices
 * </ul>
 *
 * <p>Examples:
 *
 * <pre>{@code
 * // Single device
 * NotificationTarget target = NotificationTarget.device("device-token-123");
 *
 * // Topic (broadcast)
 * NotificationTarget target = NotificationTarget.topic("news-updates");
 *
 * // Multiple devices
 * NotificationTarget target = NotificationTarget.devices(Set.of("token1", "token2"));
 * }</pre>
 */
public final class NotificationTarget {

  private final String deviceToken;
  private final String topic;
  private final Set<String> deviceTokens;
  private final TargetType type;

  private NotificationTarget(
      String deviceToken, String topic, Set<String> deviceTokens, TargetType type) {
    this.deviceToken = deviceToken;
    this.topic = topic;
    this.deviceTokens = deviceTokens;
    this.type = type;
  }

  public static NotificationTarget device(String deviceToken) {
    Objects.requireNonNull(deviceToken, "deviceToken cannot be null");
    return new NotificationTarget(deviceToken, null, null, TargetType.DEVICE);
  }

  public static NotificationTarget topic(String topic) {
    Objects.requireNonNull(topic, "topic cannot be null");
    return new NotificationTarget(null, topic, null, TargetType.TOPIC);
  }

  public static NotificationTarget devices(Set<String> deviceTokens) {
    Objects.requireNonNull(deviceTokens, "deviceTokens cannot be null");
    if (deviceTokens.isEmpty()) {
      throw new IllegalArgumentException("deviceTokens cannot be empty");
    }
    return new NotificationTarget(null, null, Set.copyOf(deviceTokens), TargetType.MULTI_DEVICE);
  }

  public String getDeviceToken() {
    return deviceToken;
  }

  public String getTopic() {
    return topic;
  }

  public Set<String> getDeviceTokens() {
    return deviceTokens;
  }

  public TargetType getType() {
    return type;
  }

  public boolean isDevice() {
    return type == TargetType.DEVICE;
  }

  public boolean isTopic() {
    return type == TargetType.TOPIC;
  }

  public boolean isMultiDevice() {
    return type == TargetType.MULTI_DEVICE;
  }

  public enum TargetType {
    DEVICE,
    TOPIC,
    MULTI_DEVICE
  }
}
