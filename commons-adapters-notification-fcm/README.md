# Commons Adapters - Firebase Cloud Messaging (FCM)

Firebase Cloud Messaging adapter for push notifications to Android and iOS devices.

## Features

- ✅ Send to single device tokens
- ✅ Send to multiple devices (multicast up to 500 tokens)
- ✅ Send to topics (broadcast)
- ✅ Topic subscription management
- ✅ Token validation (dry-run)
- ✅ Visual notifications (title, body, image)
- ✅ Data-only notifications (silent)
- ✅ Priority levels (HIGH/NORMAL)
- ✅ Time-to-live (TTL)
- ✅ Platform-specific options (Android, iOS)

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-notification-fcm</artifactId>
</dependency>
```

## Setup

### 1. Get Firebase Service Account

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Project Settings** → **Service Accounts**
4. Click **Generate New Private Key**
5. Save the JSON file securely

### 2. Initialize Adapter

```java
// From file path
FcmConfiguration config = FcmConfiguration.fromPath(
    "/path/to/service-account.json"
);
PushNotificationPort fcm = new FcmPushNotificationAdapter(config);

// Or from InputStream
InputStream credentials = getClass().getResourceAsStream("service-account.json");
FcmConfiguration config = FcmConfiguration.fromStream(credentials);
PushNotificationPort fcm = new FcmPushNotificationAdapter(config);

// Or with builder
FcmConfiguration config = FcmConfiguration.builder()
    .credentialsPath("/path/to/service-account.json")
    .projectId("my-project-id")
    .validateTokens(true)
    .build();
```

## Usage

### Send to Single Device

```java
PushNotification notification = PushNotification.builder()
    .title("New Message")
    .body("You have a new message from John")
    .data(Map.of("messageId", "123", "senderId", "john"))
    .priority(NotificationPriority.HIGH)
    .sound("default")
    .badge(1)
    .build();

NotificationTarget target = NotificationTarget.device("device-token-here");
Result<SendNotificationResult> result = fcm.send(target, notification);

result.onSuccess(r ->
    System.out.println("Sent! Message ID: " + r.messageId())
);
result.onFailure(error ->
    System.err.println("Failed: " + error.getMessage())
);
```

### Send to Multiple Devices (Multicast)

```java
Set<String> tokens = Set.of("token1", "token2", "token3");
NotificationTarget target = NotificationTarget.devices(tokens);

Result<SendNotificationResult> result = fcm.send(target, notification);

result.onSuccess(r -> {
    System.out.println("Success: " + r.successCount());
    System.out.println("Failures: " + r.failureCount());

    if (r.hasFailures()) {
        r.getFailedTokens().forEach(token ->
            System.out.println("Failed token: " + token)
        );
    }
});
```

### Send to Topic (Broadcast)

```java
// Send to all devices subscribed to "news" topic
NotificationTarget target = NotificationTarget.topic("news");
Result<SendNotificationResult> result = fcm.send(target, notification);
```

### Data-Only Notifications (Silent)

```java
// No title/body = silent notification
PushNotification dataOnly = PushNotification.builder()
    .data(Map.of(
        "action", "sync",
        "userId", "123"
    ))
    .build();

Result<SendNotificationResult> result = fcm.send(target, dataOnly);
```

### Topic Management

```java
// Subscribe device to topic
Result<Void> result = fcm.subscribeToTopic("device-token", "news");

// Subscribe multiple devices
Set<String> tokens = Set.of("token1", "token2");
Result<Void> result = fcm.subscribeToTopic(tokens, "news");

// Unsubscribe
Result<Void> result = fcm.unsubscribeFromTopic("device-token", "news");
```

### Token Validation

```java
Result<Boolean> result = fcm.validateToken("device-token");

result.onSuccess(isValid -> {
    if (isValid) {
        System.out.println("Token is valid");
    } else {
        System.out.println("Token is invalid or unregistered");
    }
});
```

## Platform-Specific Features

### Android

```java
PushNotification notification = PushNotification.builder()
    .title("Update Available")
    .body("A new version is available")
    .priority(NotificationPriority.HIGH)  // Android priority
    .timeToLive(3600L)  // 1 hour TTL
    .sound("notification.wav")
    .clickAction("OPEN_ACTIVITY")
    .build();
```

### iOS (APNS)

```java
PushNotification notification = PushNotification.builder()
    .title("New Message")
    .body("You have a new message")
    .badge(5)  // iOS badge count
    .sound("default")
    .build();
```

## Error Handling

```java
Result<SendNotificationResult> result = fcm.send(target, notification);

result
    .onSuccess(r -> System.out.println("Message ID: " + r.messageId()))
    .onFailure(error -> {
        System.err.println("Error code: " + error.getCode());
        System.err.println("Error message: " + error.getMessage());

        // Access FCM-specific error details
        error.getContext().forEach((key, value) ->
            System.err.println(key + ": " + value)
        );
    });
```

## Multicast Batching

FCM limits multicast to 500 tokens per request. The adapter automatically batches larger sets:

```java
// Automatically batched into groups of 500
Set<String> tokens = getThousandTokens();  // 1000 tokens
NotificationTarget target = NotificationTarget.devices(tokens);

Result<SendNotificationResult> result = fcm.send(target, notification);

result.onSuccess(r -> {
    System.out.println("Total success: " + r.successCount());
    System.out.println("Total failures: " + r.failureCount());

    // Failed tokens with reasons
    r.failures().forEach((token, reason) ->
        System.out.println(token + " failed: " + reason)
    );
});
```

## Best Practices

1. **Reuse adapter instance**: Create once, use for all sends
2. **Handle token expiration**: Remove invalid tokens from database
3. **Use topics for broadcasts**: More efficient than sending to thousands of individual tokens
4. **Set appropriate TTL**: Avoid delivering stale notifications
5. **Use HIGH priority sparingly**: Battery impact on devices
6. **Validate tokens periodically**: Clean up invalid tokens

## Testing

The adapter supports dependency injection for testing:

```java
FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
PushNotificationPort adapter = new FcmPushNotificationAdapter(mockMessaging, false);

// Test sending
when(mockMessaging.send(any())).thenReturn("msg-id-123");
Result<SendNotificationResult> result = adapter.send(target, notification);

assertTrue(result.isSuccess());
assertEquals("msg-id-123", result.getValue().messageId());
```

## Configuration Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `credentialsPath` | String | Required | Path to service account JSON |
| `credentials` | InputStream | Required | Service account JSON stream |
| `projectId` | String | Optional | Firebase project ID |
| `validateTokens` | boolean | `true` | Enable token validation |

## FCM Limits

- **Multicast**: Max 500 tokens per request
- **Payload size**: Max 4KB
- **Topic name**: 1-900 characters, alphanumeric + `-_.~%`
- **Rate limits**: 600k messages/minute (free tier)

## See Also

- [PushNotificationPort Interface](../commons-ports-notification)
- [Firebase Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)
- [APNS Adapter](../commons-adapters-notification-apns)
