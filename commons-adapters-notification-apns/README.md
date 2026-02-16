# Commons Adapters - Apple Push Notification Service (APNS)

Apple Push Notification Service adapter for iOS, iPadOS, macOS, watchOS, and tvOS push notifications.

## Features

- ✅ Send to single device tokens
- ✅ Send to multiple devices (sequential)
- ✅ Visual notifications (title, body, badge)
- ✅ Data-only notifications (silent/content-available)
- ✅ Priority levels (HIGH/NORMAL)
- ✅ Token-based authentication (p8) - recommended
- ✅ Certificate-based authentication (p12)
- ✅ Production and sandbox environments
- ❌ Topic subscriptions (not supported by APNS)

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-notification-apns</artifactId>
</dependency>
```

## Setup

### Token-Based Authentication (Recommended)

**Advantages:**
- Single key works for all apps
- Never expires
- More secure than certificates

**Setup:**
1. Go to [Apple Developer Portal](https://developer.apple.com/account/resources/authkeys)
2. Create a new **APNs Auth Key** (.p8 file)
3. Note your **Team ID** and **Key ID**
4. Download the `.p8` file (can only be downloaded once)

```java
ApnsConfiguration config = ApnsConfiguration.builder()
    .p8KeyPath("/path/to/AuthKey_KEYID.p8")
    .teamId("TEAM123456")  // Your 10-character Team ID
    .keyId("KEY123456")    // Your 10-character Key ID
    .topic("com.example.app")  // Your app bundle ID
    .production(true)  // false for sandbox
    .build();

PushNotificationPort apns = new ApnsPushNotificationAdapter(config);
```

### Certificate-Based Authentication

**Setup:**
1. Go to Apple Developer Portal → Certificates
2. Create an **Apple Push Notification SSL Certificate**
3. Export as `.p12` file from Keychain Access (macOS)

```java
ApnsConfiguration config = ApnsConfiguration.builder()
    .p12Path("/path/to/certificate.p12")
    .p12Password("your-password")
    .topic("com.example.app")
    .production(false)  // sandbox for development
    .build();

PushNotificationPort apns = new ApnsPushNotificationAdapter(config);
```

## Usage

### Send to Single Device

```java
PushNotification notification = PushNotification.builder()
    .title("New Message")
    .body("You have a new message from John")
    .badge(3)  // Update badge count
    .sound("default")
    .data(Map.of("messageId", "123"))
    .priority(NotificationPriority.HIGH)
    .build();

NotificationTarget target = NotificationTarget.device("device-token-here");
Result<SendNotificationResult> result = apns.send(target, notification);

result.onSuccess(r ->
    System.out.println("Sent! APNS ID: " + r.messageId())
);
result.onFailure(error ->
    System.err.println("Failed: " + error.getMessage())
);
```

### Send to Multiple Devices

```java
Set<String> tokens = Set.of("token1", "token2", "token3");
NotificationTarget target = NotificationTarget.devices(tokens);

Result<SendNotificationResult> result = apns.send(target, notification);

result.onSuccess(r -> {
    System.out.println("Success: " + r.successCount());
    System.out.println("Failures: " + r.failureCount());

    if (r.hasFailures()) {
        r.failures().forEach((token, reason) ->
            System.out.println(token + " failed: " + reason)
        );
    }
});
```

### Silent Notifications (Background Data)

```java
// No title/body = silent notification with content-available
PushNotification dataOnly = PushNotification.builder()
    .data(Map.of(
        "action", "sync",
        "userId", "123"
    ))
    .build();

Result<SendNotificationResult> result = apns.send(target, dataOnly);
```

### Badge Updates

```java
// Update badge count without showing notification
PushNotification badgeUpdate = PushNotification.builder()
    .badge(5)  // Set badge to 5
    .build();
```

### Custom Sound

```java
PushNotification notification = PushNotification.builder()
    .title("Alert")
    .body("Custom sound notification")
    .sound("notification.caf")  // Must be in app bundle
    .build();
```

### Category Actions

```java
PushNotification notification = PushNotification.builder()
    .title("Meeting Reminder")
    .body("Meeting starts in 5 minutes")
    .clickAction("MEETING_CATEGORY")  // Registered category
    .build();
```

## Limitations

### Topics Not Supported

APNS does not support topic-based broadcasting. Use FCM for topics:

```java
// This will fail with APNS
NotificationTarget target = NotificationTarget.topic("news");
Result<SendNotificationResult> result = apns.send(target, notification);
// Returns error: "TOPIC_NOT_SUPPORTED"
```

**Alternative:** Use FCM for cross-platform topic support.

### Token Validation

APNS does not provide a token validation API. Tokens are validated by attempting to send:

```java
// APNS returns success by default (no actual validation)
Result<Boolean> result = apns.validateToken("device-token");
// Always returns true (validation happens during send)
```

**Alternative:** Track failed sends and remove invalid tokens.

## Error Handling

```java
Result<SendNotificationResult> result = apns.send(target, notification);

result
    .onSuccess(r -> System.out.println("APNS ID: " + r.messageId()))
    .onFailure(error -> {
        System.err.println("Error code: " + error.getCode());
        System.err.println("Error message: " + error.getMessage());

        // Common APNS rejection reasons
        String reason = (String) error.getContext().get("reason");
        switch (reason) {
            case "BadDeviceToken":
                // Remove token from database
                break;
            case "Unregistered":
                // App uninstalled, remove token
                break;
            case "DeviceTokenNotForTopic":
                // Wrong bundle ID/topic
                break;
            case "PayloadTooLarge":
                // Reduce payload size (max 4KB)
                break;
        }
    });
```

### Common APNS Error Codes

| Code | Meaning | Action |
|------|---------|--------|
| `BadDeviceToken` | Invalid token format | Remove token |
| `Unregistered` | Device no longer registered | Remove token |
| `DeviceTokenNotForTopic` | Wrong bundle ID | Check topic configuration |
| `PayloadTooLarge` | Payload exceeds 4KB | Reduce payload size |
| `TooManyRequests` | Rate limit exceeded | Implement backoff |
| `Forbidden` | Authentication failed | Check credentials |

## Device Token Format

APNS device tokens are 64-character hexadecimal strings:

```
a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd
```

**Important:** Remove spaces and brackets if present in iOS code.

## Production vs Sandbox

```java
// Development/testing (sandbox)
ApnsConfiguration config = ApnsConfiguration.builder()
    .p8KeyPath("/path/to/AuthKey.p8")
    .teamId("TEAM123")
    .keyId("KEY123")
    .topic("com.example.app")
    .production(false)  // Use sandbox
    .build();

// Production
ApnsConfiguration config = ApnsConfiguration.builder()
    .p8KeyPath("/path/to/AuthKey.p8")
    .teamId("TEAM123")
    .keyId("KEY123")
    .topic("com.example.app")
    .production(true)  // Use production servers
    .build();
```

**Note:** Device tokens from sandbox builds only work with sandbox servers, and vice versa.

## Best Practices

1. **Use token-based auth**: Easier to manage, never expires
2. **Reuse adapter instance**: Create once, use for all sends
3. **Handle rejected tokens**: Remove `BadDeviceToken` and `Unregistered` from database
4. **Track failures**: Monitor rejection rates to detect issues
5. **Keep payloads small**: Max 4KB, aim for under 2KB
6. **Test with sandbox first**: Always test with development builds
7. **Close adapter on shutdown**: Releases HTTP/2 connections

```java
// Graceful shutdown
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    if (apns instanceof ApnsPushNotificationAdapter) {
        ((ApnsPushNotificationAdapter) apns).close();
    }
}));
```

## Testing

The adapter supports dependency injection for testing:

```java
ApnsClient mockClient = mock(ApnsClient.class);
PushNotificationPort adapter = new ApnsPushNotificationAdapter(
    mockClient,
    "com.example.app"
);

// Mock successful response
PushNotificationResponse<?> response = mock(PushNotificationResponse.class);
when(response.isAccepted()).thenReturn(true);
when(response.getApnsId()).thenReturn(UUID.randomUUID());
when(mockClient.sendNotification(any())).thenReturn(
    CompletableFuture.completedFuture(response)
);

// Test sending
Result<SendNotificationResult> result = adapter.send(target, notification);

assertTrue(result.isSuccess());
assertNotNull(result.getValue().messageId());
```

## Configuration Reference

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `p8KeyPath` | String | Either p8 or p12 | Path to .p8 auth key |
| `p8KeyStream` | InputStream | Either p8 or p12 | .p8 auth key stream |
| `teamId` | String | For p8 auth | Apple Team ID (10 chars) |
| `keyId` | String | For p8 auth | Key ID (10 chars) |
| `p12Path` | String | Either p8 or p12 | Path to .p12 certificate |
| `p12Certificate` | File | Either p8 or p12 | .p12 certificate file |
| `p12Password` | String | For p12 auth | Certificate password |
| `topic` | String | **Required** | App bundle ID |
| `production` | boolean | Optional | Use production servers (default: false) |

## APNS Limits

- **Payload size**: Max 4KB (4096 bytes)
- **Rate limits**: No official limit, but throttling occurs with bursts
- **Token size**: 32 bytes (64 hex characters)
- **Topic name**: Must match app bundle ID

## Troubleshooting

### "BadDeviceToken" errors
- Token format is incorrect (must be 64 hex chars)
- Token contains spaces or brackets
- Using production token with sandbox (or vice versa)

### "Forbidden" errors
- Invalid credentials (p8/p12)
- Wrong Team ID or Key ID
- Certificate expired (p12 only)

### "DeviceTokenNotForTopic" errors
- Bundle ID doesn't match token's app
- Check `topic` configuration

### No notifications received
- Check device has network connection
- Verify app has notification permissions
- Ensure using correct environment (sandbox/production)
- Check device token is current

## See Also

- [PushNotificationPort Interface](../commons-ports-notification)
- [APNS Documentation](https://developer.apple.com/documentation/usernotifications)
- [Pushy Library](https://github.com/jchambers/pushy)
- [FCM Adapter](../commons-adapters-notification-fcm)
