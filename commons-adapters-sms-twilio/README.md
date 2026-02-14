# Commons Adapters :: SMS :: Twilio

Twilio adapter for SMS operations using the Twilio Java SDK.

## Overview

This module provides a complete SMS and MMS adapter implementation using Twilio's communication platform. It supports sending text messages, multimedia messages, bulk SMS operations, and delivery status tracking.

## Features

- ✅ SMS sending with text content
- ✅ MMS support (with limitations)
- ✅ Bulk SMS operations
- ✅ Delivery status tracking
- ✅ Webhook callbacks for status updates
- ✅ International phone number support
- ✅ Connection verification
- ✅ Comprehensive error handling

## Installation

Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-sms-twilio</artifactId>
    <version>${commons.version}</version>
</dependency>
```

## Quick Start

### Basic Configuration

```java
TwilioConfiguration config = TwilioConfiguration.builder()
    .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")  // Your Twilio Account SID
    .authToken("your-auth-token")                   // Your Twilio Auth Token
    .fromPhoneNumber("+1234567890")                // Your verified Twilio phone number
    .build();

TwilioSMSAdapter smsAdapter = new TwilioSMSAdapter(config);
```

### Send Simple SMS

```java
SMS sms = SMS.builder()
    .from("+1234567890")
    .to("+0987654321")
    .message("Hello! Welcome to our service.")
    .build();

Result<SMSReceipt> result = smsAdapter.send(sms);
if (result.isOk()) {
    SMSReceipt receipt = result.getOrNull();
    System.out.println("SMS sent: " + receipt.messageId());
} else {
    System.err.println("Failed to send SMS: " + result.problemOrNull().message());
}
```

## Configuration

### Development Configuration

```java
TwilioConfiguration config = TwilioConfiguration.forDevelopment(
    "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "your-auth-token", 
    "+1234567890"
);
```

### Production Configuration

```java
TwilioConfiguration config = TwilioConfiguration.forProduction(
    "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "your-auth-token",
    "+1234567890",
    "https://your-app.com/sms-webhook"  // Webhook for delivery receipts
);
```

### Advanced Configuration

```java
TwilioConfiguration config = TwilioConfiguration.builder()
    .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
    .authToken("your-auth-token")
    .fromPhoneNumber("+1234567890")
    .requestTimeout(Duration.ofSeconds(30))
    .webhookUrl("https://your-app.com/webhook")
    .deliveryReceiptsEnabled(true)
    .build();
```

## Usage Examples

### SMS with Options

```java
SMSOptions options = SMSOptions.builder()
    .deliveryReceipt(true)
    .validityPeriodMinutes(1440)  // 24 hours
    .priority(SMSOptions.SMSPriority.HIGH)
    .build();

SMS sms = SMS.builder()
    .from("+1234567890")
    .to("+5511987654321")  // International number
    .message("Important notification: Your order has been shipped!")
    .options(options)
    .build();

Result<SMSReceipt> result = smsAdapter.send(sms);
```

### Bulk SMS

```java
List<String> recipients = List.of(
    "+1234567890",
    "+1234567891", 
    "+1234567892"
);

BulkSMS bulkSMS = BulkSMS.builder()
    .from("+1234567890")
    .toAll(recipients)
    .message("Flash sale! 50% off everything. Use code: FLASH50")
    .withDeliveryReceipt()
    .build();

Result<BulkSMSReceipt> result = smsAdapter.sendBulk(bulkSMS);
if (result.isOk()) {
    BulkSMSReceipt receipt = result.getOrNull();
    System.out.printf("Sent %d/%d messages successfully%n", 
        receipt.successCount(), receipt.totalMessages());
}
```

### MMS (Limitations)

> ⚠️ **Note**: This adapter currently doesn't support binary media content for MMS. 
> Twilio requires media to be accessible via public URLs. For binary content, 
> you'll need to first upload media to a public storage service.

```java
// This will fail with current implementation
MMS mms = MMS.builder()
    .from("+1234567890")
    .to("+0987654321")
    .message("Check out this image!")
    .addImage(imageBytes, "jpeg")  // ❌ Not supported
    .build();

// For MMS, upload media to public URL first, then use Twilio's media_url parameter
// (requires custom implementation or different approach)
```

### Status Tracking

```java
// Get status of a sent message
Result<SMSStatus> statusResult = smsAdapter.getStatus("SMxxxxxxxxxxxxxxxxxxxx");
if (statusResult.isOk()) {
    SMSStatus status = statusResult.getOrNull();
    System.out.println("Message status: " + status);  // DELIVERED, FAILED, etc.
}
```

### Connection Verification

```java
Result<Void> verificationResult = smsAdapter.verify();
if (verificationResult.isOk()) {
    System.out.println("Twilio connection verified successfully");
} else {
    System.err.println("Connection verification failed: " + 
        verificationResult.problemOrNull().message());
}
```

## Use Cases

### 1. User Authentication (OTP)

```java
public class OTPService {
    private final SMSPort smsPort;
    
    public Result<String> sendOTP(PhoneNumber phoneNumber) {
        String code = generateOTP();
        
        SMS sms = SMS.builder()
            .from(config.getFromNumber())
            .to(phoneNumber)
            .message("Your verification code: " + code + ". Valid for 5 minutes.")
            .options(SMSOptions.builder()
                .validityPeriodMinutes(5)
                .priority(SMSOptions.SMSPriority.HIGH)
                .build())
            .build();
            
        return smsPort.send(sms)
            .map(receipt -> code);  // Return OTP code if SMS sent successfully
    }
}
```

### 2. Order Notifications

```java
public class OrderNotificationService {
    private final SMSPort smsPort;
    
    public void notifyOrderShipped(Order order) {
        String message = String.format(
            "Hi %s! Your order #%s has been shipped. Track: %s",
            order.getCustomerName(),
            order.getId(),
            order.getTrackingUrl()
        );
        
        SMS sms = SMS.of(
            config.getFromNumber().toE164(),
            order.getCustomerPhone().toE164(),
            message
        );
        
        smsPort.send(sms)
            .onFailure(problem -> 
                log.error("Failed to send order notification: {}", problem.message()));
    }
}
```

### 3. Marketing Campaigns

```java
public class MarketingService {
    private final SMSPort smsPort;
    
    public void sendCampaign(Campaign campaign) {
        BulkSMS bulkSMS = BulkSMS.builder()
            .from(campaign.getFromNumber())
            .toAllPhones(campaign.getRecipients())
            .message(campaign.getMessage())
            .withDeliveryReceipt()
            .validityPeriod(1440)  // 24 hours
            .build();
            
        Result<BulkSMSReceipt> result = smsPort.sendBulk(bulkSMS);
        
        result.onSuccess(receipt -> {
            campaign.markSent(receipt.successCount(), receipt.failureCount());
            log.info("Campaign sent: {}/{} messages delivered", 
                receipt.successCount(), receipt.totalMessages());
        });
    }
}
```

## Phone Number Handling

### International Support

```java
// Brazil
SMS sms = SMS.of("+5511987654321", "+5511123456789", "Olá from Brazil!");

// US/Canada  
SMS sms = SMS.of("+15551234567", "+15559876543", "Hello from North America!");

// UK
SMS sms = SMS.of("+447700900123", "+447700900456", "Hello from the UK!");

// Germany
SMS sms = SMS.of("+4915123456789", "+4915987654321", "Hallo from Germany!");
```

### Phone Number Validation

The `PhoneNumber` value object provides automatic validation and normalization:

```java
// These all work
PhoneNumber phone1 = PhoneNumber.of("+1 (555) 123-4567");
PhoneNumber phone2 = PhoneNumber.of("+15551234567");
PhoneNumber phone3 = PhoneNumber.of("1-555-123-4567");  // Gets normalized to +15551234567

// Display formats
System.out.println(phone1.toE164());          // +15551234567
System.out.println(phone1.toDisplayFormat()); // +1 (555) 123-4567
```

## Error Handling

### Common Error Scenarios

```java
Result<SMSReceipt> result = smsAdapter.send(sms);

result.onFailure(problem -> {
    switch (problem.code().value()) {
        case "TWILIO_AUTHENTICATION_ERROR":
            log.error("Invalid Twilio credentials");
            break;
        case "TWILIO_INVALID_PHONE_NUMBER":
            log.error("Invalid phone number format");
            break;
        case "TWILIO_UNVERIFIED_PHONE_NUMBER":
            log.error("From number not verified with Twilio");
            break;
        case "TWILIO_MESSAGE_QUEUE_FULL":
            log.warn("Message queue full, retry later");
            break;
        default:
            log.error("SMS send failed: {}", problem.message());
    }
});
```

### Error Categories

- **UNAUTHORIZED**: Invalid credentials (error code 20003)
- **BUSINESS**: Invalid phone numbers, unverified numbers, message format issues
- **TECHNICAL**: Network issues, API errors, configuration problems

## Webhook Integration

### Setup Webhook Endpoint

```java
@RestController
@RequestMapping("/webhooks/sms")
public class SMSWebhookController {
    
    @PostMapping("/status")
    public ResponseEntity<Void> handleStatusCallback(
            @RequestParam("MessageSid") String messageSid,
            @RequestParam("MessageStatus") String status,
            @RequestParam("To") String to) {
        
        log.info("SMS {} to {} status: {}", messageSid, to, status);
        
        // Update your database with delivery status
        smsStatusService.updateStatus(messageSid, status);
        
        return ResponseEntity.ok().build();
    }
}
```

### Configure Webhook in Twilio

```java
TwilioConfiguration config = TwilioConfiguration.builder()
    .accountSid("ACxxx...")
    .authToken("token")
    .fromPhoneNumber("+1234567890")
    .webhookUrl("https://your-app.com/webhooks/sms/status")
    .deliveryReceiptsEnabled(true)
    .build();
```

## Security Best Practices

### 1. Secure Credential Storage

```java
// ❌ Don't hardcode credentials
TwilioConfiguration config = TwilioConfiguration.builder()
    .accountSid("ACxxxxx")  // ❌ Never hardcode
    .authToken("token")     // ❌ Never hardcode
    .build();

// ✅ Use environment variables or secret store
TwilioConfiguration config = TwilioConfiguration.builder()
    .accountSid(System.getenv("TWILIO_ACCOUNT_SID"))
    .authToken(System.getenv("TWILIO_AUTH_TOKEN"))
    .fromPhoneNumber(System.getenv("TWILIO_FROM_NUMBER"))
    .build();
```

### 2. Rate Limiting

```java
@Service
public class RateLimitedSMSService {
    private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 SMS/second
    private final SMSPort smsPort;
    
    public Result<SMSReceipt> send(SMS sms) {
        if (!rateLimiter.tryAcquire()) {
            return Result.fail(Problem.of(
                ErrorCode.of("RATE_LIMIT_EXCEEDED"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "SMS rate limit exceeded"));
        }
        
        return smsPort.send(sms);
    }
}
```

### 3. Input Validation

```java
public class SMSValidator {
    public static Result<SMS> validateSMS(SMS sms) {
        // Check message length
        if (sms.message().length() > 1600) {
            return Result.fail(Problem.of(
                ErrorCode.of("MESSAGE_TOO_LONG"),
                ErrorCategory.BUSINESS,
                Severity.ERROR,
                "Message exceeds maximum length"));
        }
        
        // Validate phone numbers
        if (!isValidPhoneNumber(sms.to())) {
            return Result.fail(Problem.of(
                ErrorCode.of("INVALID_PHONE_NUMBER"),
                ErrorCategory.BUSINESS,
                Severity.ERROR,
                "Invalid recipient phone number"));
        }
        
        return Result.ok(sms);
    }
}
```

## Performance Considerations

### 1. Connection Pooling

The Twilio SDK handles connection pooling internally. Each `TwilioSMSAdapter` instance reuses connections efficiently.

### 2. Bulk Operations

For sending to multiple recipients, use `sendBulk()` instead of multiple `send()` calls:

```java
// ❌ Inefficient
recipients.forEach(phone -> 
    smsPort.send(SMS.of(from, phone, message)));

// ✅ Efficient
BulkSMS bulkSMS = BulkSMS.of(from, recipients, message);
smsPort.sendBulk(bulkSMS);
```

### 3. Async Processing

For high-volume scenarios, consider async processing:

```java
@Async
public CompletableFuture<SMSReceipt> sendAsync(SMS sms) {
    return CompletableFuture.supplyAsync(() -> 
        smsPort.send(sms).getOrThrow());
}
```

## Testing

### Unit Testing with Mocks

```java
@Test
void shouldSendSMSSuccessfully() {
    // Given
    TwilioConfiguration config = TwilioConfiguration.forDevelopment(
        "ACtest", "token", "+1234567890");
    TwilioSMSAdapter adapter = new TwilioSMSAdapter(config);
    
    SMS sms = SMS.of("+1234567890", "+0987654321", "Test message");
    
    // Mock Twilio response
    // (See TwilioSMSAdapterTest for full examples)
}
```

### Integration Testing

For integration tests, use Twilio's test credentials:

```java
@TestConfiguration
public class TestTwilioConfiguration {
    
    @Bean
    @Primary
    public TwilioConfiguration testTwilioConfiguration() {
        return TwilioConfiguration.forDevelopment(
            "ACtest...test",     // Twilio test Account SID
            "test_token",        // Twilio test Auth Token  
            "+15005550006"       // Twilio test number
        );
    }
}
```

## Troubleshooting

### Common Issues

1. **Authentication Failed (20003)**
   - Verify Account SID starts with "AC"
   - Check Auth Token is correct
   - Ensure credentials are not expired

2. **Invalid Phone Number (21211)**
   - Use E.164 format (+1234567890)
   - Verify country code is correct
   - Check for typos in phone number

3. **Unverified Phone Number (21608)**
   - Verify sender number in Twilio Console
   - For trial accounts, verify recipient numbers too

4. **Message Body Invalid (21614)**
   - Check message length (max 1600 characters)
   - Avoid special characters that might cause encoding issues
   - Ensure message is not empty

### Debug Mode

Enable debug logging to troubleshoot issues:

```properties
# application.properties
logging.level.com.twilio=DEBUG
logging.level.com.marcusprado02.commons.adapters.sms.twilio=DEBUG
```

### Health Checks

```java
@Component
public class TwilioHealthCheck implements HealthIndicator {
    private final TwilioSMSAdapter smsAdapter;
    
    @Override
    public Health health() {
        Result<Void> verifyResult = smsAdapter.verify();
        
        if (verifyResult.isOk()) {
            return Health.up()
                .withDetail("provider", "Twilio")
                .withDetail("status", "Connected")
                .build();
        } else {
            return Health.down()
                .withDetail("provider", "Twilio")
                .withDetail("error", verifyResult.problemOrNull().message())
                .build();
        }
    }
}
```

## Dependencies

This adapter requires:

- **Twilio Java SDK 10.0.0+**: Official Twilio client library
- **Jackson**: For JSON processing
- **Commons Kernel**: Result pattern and error handling
- **Commons Ports SMS**: SMS port interfaces

## Limitations

1. **MMS Binary Content**: Currently doesn't support binary media content. Media must be accessible via public URLs.

2. **Message Length**: SMS messages are limited to 1600 characters (10 concatenated SMS).

3. **Rate Limits**: Subject to Twilio's rate limits based on your account type.

4. **Trial Account Restrictions**: Trial accounts can only send to verified phone numbers.

## Version History

- **0.1.0**: Initial implementation with SMS, MMS, and bulk SMS support

## License

This project is licensed under the same license as the Commons Platform.

---

For more information about Twilio APIs, visit [Twilio Documentation](https://www.twilio.com/docs/sms).
