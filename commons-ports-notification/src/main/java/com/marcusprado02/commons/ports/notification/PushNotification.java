package com.marcusprado02.commons.ports.notification;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a push notification to be sent.
 *
 * <p>Supports different notification types:
 *
 * <ul>
 *   <li>Data-only: Silent notification with custom data
 *   <li>Notification-only: Visual notification displayed to user
 *   <li>Combined: Visual notification with additional data payload
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * PushNotification notification = PushNotification.builder()
 *     .title("New Message")
 *     .body("You have a new message from John")
 *     .data(Map.of("messageId", "123", "senderId", "456"))
 *     .build();
 * }</pre>
 */
public record PushNotification(
    String title,
    String body,
    String imageUrl,
    Map<String, String> data,
    NotificationPriority priority,
    Long timeToLive,
    String sound,
    Integer badge,
    String clickAction) {

  public PushNotification {
    data = data != null ? Map.copyOf(data) : Map.of();
  }

  public static Builder builder() {
    return new Builder();
  }

  public boolean hasVisualContent() {
    return title != null || body != null;
  }

  public boolean hasData() {
    return !data.isEmpty();
  }

  public static class Builder {
    private String title;
    private String body;
    private String imageUrl;
    private Map<String, String> data = Map.of();
    private NotificationPriority priority = NotificationPriority.NORMAL;
    private Long timeToLive;
    private String sound;
    private Integer badge;
    private String clickAction;

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder body(String body) {
      this.body = body;
      return this;
    }

    public Builder imageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    public Builder data(Map<String, String> data) {
      this.data = Objects.requireNonNull(data, "data cannot be null");
      return this;
    }

    public Builder priority(NotificationPriority priority) {
      this.priority = Objects.requireNonNull(priority, "priority cannot be null");
      return this;
    }

    public Builder timeToLive(Long timeToLive) {
      this.timeToLive = timeToLive;
      return this;
    }

    public Builder sound(String sound) {
      this.sound = sound;
      return this;
    }

    public Builder badge(Integer badge) {
      this.badge = badge;
      return this;
    }

    public Builder clickAction(String clickAction) {
      this.clickAction = clickAction;
      return this;
    }

    public PushNotification build() {
      return new PushNotification(
          title, body, imageUrl, data, priority, timeToLive, sound, badge, clickAction);
    }
  }
}
