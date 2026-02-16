# Commons Ports - Notification

Platform-agnostic API for sending push notifications to mobile devices.

## Features

- ✅ Unified interface for FCM (Android/iOS) and APNS (iOS)
- ✅ Single device targeting
- ✅ Multiple device targeting (multicast)
- ✅ Topic broadcasting (FCM only)
- ✅ Visual notifications (title, body, image)
- ✅ Data-only notifications (silent)
- ✅ Priority levels (HIGH/NORMAL)
- ✅ Platform-specific options (badge, sound, TTL)
- ✅ Result pattern error handling
- ✅ Topic subscription management

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-ports-notification</artifactId>
</dependency>
```

## Core Concepts

### PushNotificationPort

Main interface for sending notifications. Implementations:
- **FCM** (Firebase Cloud Messaging): Android, iOS, Web
- **APNS** (Apple Push Notification Service): iOS, macOS, watchOS, tvOS

### NotificationTarget

Three targeting modes:

```java
// Single device
NotificationTarget target = NotificationTarget.device("device-token");

// Multiple devices (multicast)
Set<String> tokens = Set.of("token1", "token2");
NotificationTarget target = NotificationTarget.devices(tokens);

// Topic broadcast (FCM only)
NotificationTarget target = NotificationTarget.topic("news");
```

### PushNotification

Notification content with builder pattern:

```java
PushNotification notification = PushNotification.builder()
    .title("Title")
    .body("Body text")
    .imageUrl("https://example.com/image.png")
    .data(Map.of("key", "value"))
    .priority(NotificationPriority.HIGH)
    .timeToLive(3600L)
    .sound("default")
    .badge(1)
    .clickAction("OPEN_ACTIVITY")
    .build();
```

### SendNotificationResult

Send operation result:

```java
SendNotificationResult result = SendNotificationResult.success("msg-123");

SendNotificationResult multicast = SendNotificationResult.multicast(
    10,  // success count
    2,   // failure count
    Map.of("token1", "BadDeviceToken", "token2", "Unregistered")
);

if (result.hasFailures()) {
    result.getFailedTokens().forEach(token ->
        System.out.println("Failed: " + token)
    );
}
```

## Usage Examples

### Basic Notification

```java
PushNotificationPort port = // ... get implementation

PushNotification notification = PushNotification.builder()
    .title("New Message")
    .body("You have a new message")
    .build();

NotificationTarget target = NotificationTarget.device("device-token");

Result<SendNotificationResult> result = port.send(target, notification);

result
    .onSuccess(r -> System.out.println("Sent: " + r.messageId()))
    .onFailure(e -> System.err.println("Failed: " + e.getMessage()));
```

### Silent Notification (Data-Only)

```java
// No title/body = silent notification
PushNotification dataOnly = PushNotification.builder()
    .data(Map.of(
        "action", "sync",
        "userId", "123"
    ))
    .build();

Result<SendNotificationResult> result = port.send(target, dataOnly);
```

### Multicast Notification

```java
Set<String> tokens = Set.of("token1", "token2", "token3");
NotificationTarget target = NotificationTarget.devices(tokens);

Result<SendNotificationResult> result = port.send(target, notification);

result.onSuccess(r -> {
    System.out.println("Success: " + r.successCount());
    System.out.println("Failures: " + r.failureCount());

    // Handle failures
    r.failures().forEach((token, reason) -> {
        if (reason.contains("Unregistered") || reason.contains("BadDeviceToken")) {
            removeTokenFromDatabase(token);
        }
    });
});
```

### Topic Broadcast (FCM Only)

```java
NotificationTarget target = NotificationTarget.topic("news");

Result<SendNotificationResult> result = port.send(target, notification);
```

### Topic Management (FCM Only)

```java
// Subscribe to topic
Result<Void> result = port.subscribeToTopic("device-token", "news");

// Subscribe multiple devices
Set<String> tokens = Set.of("token1", "token2");
Result<Void> result = port.subscribeToTopic(tokens, "news");

// Unsubscribe
Result<Void> result = port.unsubscribeFromTopic("device-token", "news");
```

### Token Validation

```java
Result<Boolean> result = port.validateToken("device-token");

result.onSuccess(isValid -> {
    if (!isValid) {
        removeTokenFromDatabase();
    }
});
```

## Notification Types

### Visual Notification
Title and/or body present. Displayed to user.

```java
PushNotification visual = PushNotification.builder()
    .title("Alert")
    .body("This is a visible notification")
    .imageUrl("https://example.com/image.png")
    .build();

System.out.println(visual.hasVisualContent());  // true
```

### Data-Only Notification
No title/body. Silent background notification.

```java
PushNotification dataOnly = PushNotification.builder()
    .data(Map.of("action", "sync"))
    .build();

System.out.println(dataOnly.hasVisualContent());  // false
System.out.println(dataOnly.hasData());  // true
```

### Combined Notification
Both visual and data.

```java
PushNotification combined = PushNotification.builder()
    .title("New Order")
    .body("Order #123 received")
    .data(Map.of("orderId", "123"))
    .build();
```

## Priority Levels

### HIGH Priority
- Immediate delivery
- Wakes device from doze mode
- Battery impact
- Use for time-sensitive notifications (messages, calls)

### NORMAL Priority
- Standard delivery
- Batched with other notifications
- Battery efficient
- Use for non-urgent notifications (news, promotions)

```java
PushNotification highPriority = PushNotification.builder()
    .title("Incoming Call")
    .body("John is calling")
    .priority(NotificationPriority.HIGH)
    .build();
```

## Platform-Specific Options

### Time to Live (TTL)
How long FCM/APNS should store notification if device is offline.

```java
PushNotification notification = PushNotification.builder()
    .title("Flash Sale")
    .body("Ends in 1 hour")
    .timeToLive(3600L)  // 1 hour in seconds
    .build();
```

### Sound
Custom notification sound.

```java
PushNotification notification = PushNotification.builder()
    .title("Alert")
    .body("Important notification")
    .sound("custom_sound.wav")  // Android
    .sound("custom_sound.caf")  // iOS
    .build();
```

### Badge (iOS)
App icon badge count.

```java
PushNotification notification = PushNotification.builder()
    .title("New Message")
    .body("You have messages")
    .badge(5)  // Show "5" on app icon
    .build();
```

### Click Action
Action when notification is tapped.

```java
PushNotification notification = PushNotification.builder()
    .title("New Order")
    .body("Order received")
    .clickAction("OPEN_ORDER")  // Android activity
    .clickAction("ORDER_CATEGORY")  // iOS category
    .build();
```

## Error Handling

```java
Result<SendNotificationResult> result = port.send(target, notification);

result
    .onSuccess(r -> {
        if (r.hasFailures()) {
            // Partial success (multicast)
            handleMulticastFailures(r.failures());
        } else {
            // Complete success
            System.out.println("Sent: " + r.messageId());
        }
    })
    .onFailure(error -> {
        ErrorDetails details = error;
        System.err.println("Code: " + details.getCode());
        System.err.println("Message: " + details.getMessage());
        System.err.println("Category: " + details.getCategory());

        // Access context
        details.getContext().forEach((key, value) ->
            System.err.println(key + ": " + value)
        );
    });
```

## Best Practices

1. **Handle token expiration**: Remove invalid tokens from database
2. **Use HIGH priority sparingly**: Battery impact on devices
3. **Set appropriate TTL**: Avoid delivering stale notifications
4. **Use topics for broadcasts**: More efficient than thousands of individual sends
5. **Validate tokens periodically**: Clean up invalid tokens
6. **Track send failures**: Monitor rejection rates
7. **Keep payloads small**: Max 4KB (FCM and APNS limit)

## Testing

Use mocks for unit testing:

```java
PushNotificationPort mockPort = mock(PushNotificationPort.class);

SendNotificationResult success = SendNotificationResult.success("msg-123");
when(mockPort.send(any(), any())).thenReturn(Result.success(success));

// Test
Result<SendNotificationResult> result = mockPort.send(target, notification);

assertTrue(result.isSuccess());
assertEquals("msg-123", result.getValue().messageId());
```

## Implementations

### FCM (Firebase Cloud Messaging)

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-notification-fcm</artifactId>
</dependency>
```

```java
FcmConfiguration config = FcmConfiguration.fromPath("service-account.json");
PushNotificationPort fcm = new FcmPushNotificationAdapter(config);
```

**Supports:**
- Android, iOS, Web
- Topics
- Token validation
- Multicast (up to 500 tokens)

### APNS (Apple Push Notification Service)

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-notification-apns</artifactId>
</dependency>
```

```java
ApnsConfiguration config = ApnsConfiguration.builder()
    .p8KeyPath("/path/to/AuthKey.p8")
    .teamId("TEAM123")
    .keyId("KEY123")
    .topic("com.example.app")
    .production(true)
    .build();

PushNotificationPort apns = new ApnsPushNotificationAdapter(config);
```

**Supports:**
- iOS, macOS, watchOS, tvOS
- Certificate and token-based auth
- Production and sandbox

**Limitations:**
- No topic subscriptions
- No direct token validation

## API Reference

### PushNotificationPort

| Method | Description |
|--------|-------------|
| `send(NotificationTarget, PushNotification)` | Send notification |
| `subscribeToTopic(String, String)` | Subscribe device to topic |
| `subscribeToTopic(Set<String>, String)` | Subscribe devices to topic |
| `unsubscribeFromTopic(String, String)` | Unsubscribe device from topic |
| `unsubscribeFromTopic(Set<String>, String)` | Unsubscribe devices from topic |
| `validateToken(String)` | Validate device token |

### NotificationTarget

| Method | Description |
|--------|-------------|
| `device(String)` | Target single device |
| `devices(Set<String>)` | Target multiple devices |
| `topic(String)` | Target topic subscribers |
| `isDevice()` | Check if device target |
| `isTopic()` | Check if topic target |
| `isMultiDevice()` | Check if multi-device target |

### PushNotification

| Property | Type | Description |
|----------|------|-------------|
| `title` | String | Notification title |
| `body` | String | Notification body |
| `imageUrl` | String | Image URL |
| `data` | Map | Custom key-value data |
| `priority` | NotificationPriority | HIGH or NORMAL |
| `timeToLive` | Long | TTL in seconds |
| `sound` | String | Sound filename |
| `badge` | Integer | Badge count (iOS) |
| `clickAction` | String | Action on click |

### SendNotificationResult

| Property | Type | Description |
|----------|------|-------------|
| `success` | boolean | Overall success |
| `messageId` | String | Message ID (single send) |
| `failureReason` | String | Failure reason (single send) |
| `successCount` | int | Success count (multicast) |
| `failureCount` | int | Failure count (multicast) |
| `failures` | Map | Failed tokens with reasons |

## See Also

- [FCM Adapter](../commons-adapters-notification-fcm)
- [APNS Adapter](../commons-adapters-notification-apns)
- [Result Pattern](../commons-kernel-result)
