package com.marcusprado02.commons.ports.notification;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NotificationModelTest {

  @Test
  void notificationTarget_device_creates_device_target() {
    NotificationTarget t = NotificationTarget.device("token-abc");
    assertTrue(t.isDevice());
    assertFalse(t.isTopic());
    assertFalse(t.isMultiDevice());
    assertEquals("token-abc", t.getDeviceToken());
    assertNull(t.getTopic());
    assertEquals(NotificationTarget.TargetType.DEVICE, t.getType());
  }

  @Test
  void notificationTarget_topic_creates_topic_target() {
    NotificationTarget t = NotificationTarget.topic("news");
    assertTrue(t.isTopic());
    assertFalse(t.isDevice());
    assertEquals("news", t.getTopic());
  }

  @Test
  void notificationTarget_devices_creates_multi_target() {
    NotificationTarget t = NotificationTarget.devices(Set.of("t1", "t2"));
    assertTrue(t.isMultiDevice());
    assertEquals(2, t.getDeviceTokens().size());
  }

  @Test
  void notificationTarget_devices_rejects_empty_set() {
    assertThrows(IllegalArgumentException.class, () -> NotificationTarget.devices(Set.of()));
  }

  @Test
  void notificationTarget_device_rejects_null() {
    assertThrows(NullPointerException.class, () -> NotificationTarget.device(null));
  }

  @Test
  void notificationTarget_topic_rejects_null() {
    assertThrows(NullPointerException.class, () -> NotificationTarget.topic(null));
  }

  @Test
  void notificationTarget_devices_rejects_null_set() {
    assertThrows(NullPointerException.class, () -> NotificationTarget.devices(null));
  }

  @Test
  void pushNotification_builder_with_visual_content() {
    PushNotification n =
        PushNotification.builder()
            .title("Title")
            .body("Body")
            .data(Map.of("k", "v"))
            .priority(NotificationPriority.HIGH)
            .build();

    assertTrue(n.hasVisualContent());
    assertTrue(n.hasData());
    assertEquals("Title", n.title());
    assertEquals("Body", n.body());
    assertEquals(NotificationPriority.HIGH, n.priority());
  }

  @Test
  void pushNotification_no_visual_content_when_no_title_or_body() {
    PushNotification n = PushNotification.builder().data(Map.of("silent", "true")).build();

    assertFalse(n.hasVisualContent());
    assertTrue(n.hasData());
  }

  @Test
  void pushNotification_null_data_defaults_to_empty_map() {
    PushNotification n = new PushNotification(null, null, null, null, null, null, null, null, null);
    assertNotNull(n.data());
    assertTrue(n.data().isEmpty());
    assertFalse(n.hasData());
  }

  @Test
  void pushNotification_builder_with_optional_fields() {
    PushNotification n =
        PushNotification.builder()
            .title("T")
            .imageUrl("http://img.url")
            .timeToLive(3600L)
            .sound("default")
            .badge(1)
            .clickAction("OPEN_APP")
            .build();

    assertEquals("http://img.url", n.imageUrl());
    assertEquals(3600L, n.timeToLive());
    assertEquals("default", n.sound());
    assertEquals(1, n.badge());
    assertEquals("OPEN_APP", n.clickAction());
  }
}
