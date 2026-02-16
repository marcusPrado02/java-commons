package com.marcusprado02.commons.ports.notification;

/**
 * Priority levels for push notifications.
 *
 * <p>Priority affects delivery speed and device behavior:
 *
 * <ul>
 *   <li>HIGH: Immediate delivery, wakes device from doze mode
 *   <li>NORMAL: Standard delivery, batched for battery efficiency
 * </ul>
 */
public enum NotificationPriority {
  /** High priority - immediate delivery, wakes device. */
  HIGH,

  /** Normal priority - standard delivery, battery efficient. */
  NORMAL
}
