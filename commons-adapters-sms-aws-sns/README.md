# Commons Adapters SMS AWS SNS

AWS Simple Notification Service (SNS) adapter for the SMS Port, providing SMS sending capabilities through Amazon's managed messaging service.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [Examples](#examples)
- [Local Development](#local-development)
- [Security](#security)
- [Troubleshooting](#troubleshooting)
- [API Reference](#api-reference)

## Overview

This adapter implements the `SMSPort` interface using AWS SNS for SMS delivery. It supports both individual and bulk SMS operations with configurable pricing limits, delivery tracking, and comprehensive error handling.

### Key Benefits

- **Scalable**: Built on AWS infrastructure for high availability
- **Cost-effective**: Pay-per-use pricing model with safety limits
- **Global reach**: International SMS support via AWS global infrastructure
- **Integrated**: Works seamlessly with other AWS services
- **Secure**: IAM-based authentication and authorization

## Features

### SMS Operations
- ✅ Individual SMS sending
- ✅ Bulk SMS operations (iterative sending)
- ✅ Connection verification
- ✅ Configurable pricing limits
- ✅ Delivery status logging
- ❌ MMS support (use Amazon Pinpoint instead)
- ❌ Direct delivery status tracking (SNS limitation)

### Authentication Methods
- AWS Access Keys (programmatic access)
- Session tokens (temporary credentials)
- IAM roles (EC2, Lambda, ECS)
- Default credential chain

### Message Types
- **Transactional**: OTP codes, notifications, alerts
- **Promotional**: Marketing messages, campaigns

## Installation

Add the dependency to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-sms-aws-sns</artifactId>
    <version>${commons.version}</version>
</dependency>
```

For Gradle:

```gradle
implementation 'com.marcusprado02.commons:commons-adapters-sms-aws-sns:${commonsVersion}'
```

## Configuration

### Basic Configuration

```java
SnsConfiguration config = SnsConfiguration.builder()
    .region(Region.US_EAST_1)
    .accessKeyId("AKIA...")
    .secretAccessKey("secret...")
    .maxPriceUSD(0.50)
    .build();

SnsSMSAdapter smsAdapter = new SnsSMSAdapter(config);
```

### Production Configuration

```java
SnsConfiguration config = SnsConfiguration.forProduction(
    Region.US_EAST_1,
    "AKIA...",
    "secret...", 
    "MyCompany"  // Sender ID
);
```

### IAM Role Configuration

```java
SnsConfiguration config = SnsConfiguration.withIamRole(Region.US_EAST_1);
```

### Configuration Options

| Option | Description | Default | Required |
|--------|-------------|---------|----------|
| `region` | AWS region for SNS service | - | Yes |
| `accessKeyId` | AWS access key ID | - | If not using IAM |
| `secretAccessKey` | AWS secret access key | - | If using access key |
| `sessionToken` | Session token for temporary credentials | null | No |
| `requestTimeout` | HTTP request timeout | 15s | No |
| `defaultSenderId` | Default sender ID (11 chars max) | null | No |
| `maxPriceUSD` | Maximum price per SMS (safety limit) | $0.50 | No |
| `smsType` | Message type (TRANSACTIONAL/PROMOTIONAL) | TRANSACTIONAL | No |
| `deliveryStatusLogging` | Enable delivery status logs | false | No |

## Usage

### Basic SMS Sending

```java
// Create configuration
SnsConfiguration config = SnsConfiguration.forDevelopment(
    Region.US_EAST_1,
    "your-access-key",
    "your-secret-key"
);

// Create adapter  
SnsSMSAdapter smsAdapter = new SnsSMSAdapter(config);

// Send SMS
PhoneNumber recipient = PhoneNumber.of("+1234567890");
SMS sms = SMS.of(recipient, "Hello from AWS SNS!");

Result<SMSReceipt> result = smsAdapter.send(sms);

if (result.isSuccess()) {
    SMSReceipt receipt = result.getValue();
    System.out.println("Message sent: " + receipt.messageId());
} else {
    System.err.println("Failed: " + result.getError().detail());
}
```

### Bulk SMS Operations

```java
List<PhoneNumber> recipients = List.of(
    PhoneNumber.of("+1234567890"),
    PhoneNumber.of("+1987654321"),
    PhoneNumber.of("+44123456789")
);

BulkSMS bulkSMS = BulkSMS.of(recipients, "Bulk notification message");

Result<BulkSMSReceipt> result = smsAdapter.sendBulk(bulkSMS);

if (result.isSuccess()) {
    BulkSMSReceipt receipt = result.getValue();
    System.out.println("Successful: " + receipt.successfulReceipts().size());
    System.out.println("Failed: " + receipt.failedReceipts().size());
}
```

### Connection Verification

```java
Result<Boolean> verification = smsAdapter.verify();

if (verification.isSuccess()) {
    System.out.println("Connection verified successfully");
} else {
    System.err.println("Connection failed: " + verification.getError().detail());
}
```

## Examples

### OTP Verification System

```java
public class OTPService {
    private final SnsSMSAdapter smsAdapter;
    
    public OTPService(SnsSMSAdapter smsAdapter) {
        this.smsAdapter = smsAdapter;
    }
    
    public Result<String> sendOTP(PhoneNumber phoneNumber) {
        String otpCode = generateOTP();
        String message = String.format(
            "Your verification code is: %s. Valid for 5 minutes.", 
            otpCode
        );
        
        SMS sms = SMS.of(phoneNumber, message);
        SMSOptions options = SMSOptions.builder()
            .priority(SMSOptions.SMSPriority.HIGH)
            .deliveryReceipt(true)
            .build();
            
        return smsAdapter.send(sms, options)
            .map(receipt -> otpCode);
    }
    
    private String generateOTP() {
        return String.format("%06d", new Random().nextInt(1000000));
    }
}
```

### Marketing Campaign

```java
public class MarketingService {
    private final SnsSMSAdapter smsAdapter;
    
    public MarketingService() {
        SnsConfiguration config = SnsConfiguration.builder()
            .region(Region.US_EAST_1)
            .accessKeyId(System.getenv("AWS_ACCESS_KEY_ID"))
            .secretAccessKey(System.getenv("AWS_SECRET_ACCESS_KEY"))
            .smsType(SnsConfiguration.SmsType.PROMOTIONAL)
            .maxPriceUSD(0.10) // Lower limit for promotions
            .defaultSenderId("DEALS")
            .build();
            
        this.smsAdapter = new SnsSMSAdapter(config);
    }
    
    public void sendDailyDeals(List<PhoneNumber> subscribers) {
        String message = "Daily Deal: 50% off all items! Visit our store today.";
        BulkSMS campaign = BulkSMS.of(subscribers, message);
        
        Result<BulkSMSReceipt> result = smsAdapter.sendBulk(campaign);
        
        result.ifSuccess(receipt -> {
            logCampaignMetrics(receipt);
        }).ifFailure(error -> {
            logError("Campaign failed", error);
        });
    }
}
```

### Spring Boot Integration

```java
@Configuration
@EnableConfigurationProperties(SnsProperties.class)
public class SnsConfig {
    
    @Bean
    @ConditionalOnProperty("aws.sns.enabled")
    public SnsSMSAdapter snsSMSAdapter(SnsProperties properties) {
        SnsConfiguration config = SnsConfiguration.builder()
            .region(Region.of(properties.getRegion()))
            .accessKeyId(properties.getAccessKeyId())
            .secretAccessKey(properties.getSecretAccessKey())
            .requestTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .maxPriceUSD(properties.getMaxPriceUSD())
            .smsType(properties.getSmsType())
            .deliveryStatusLogging(properties.isDeliveryStatusLogging())
            .build();
            
        return new SnsSMSAdapter(config);
    }
}

@ConfigurationProperties("aws.sns")
@Data
public class SnsProperties {
    private boolean enabled = false;
    private String region = "us-east-1";
    private String accessKeyId;
    private String secretAccessKey;
    private int timeoutSeconds = 15;
    private double maxPriceUSD = 0.50;
    private SnsConfiguration.SmsType smsType = SnsConfiguration.SmsType.TRANSACTIONAL;
    private boolean deliveryStatusLogging = false;
}
```

## Local Development

### LocalStack Integration

For local development and testing, use LocalStack to emulate AWS SNS:

```yaml
# docker-compose.yml
version: '3.8'
services:
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"
    environment:
      - SERVICES=sns
      - DEBUG=1
    volumes:
      - "./localstack:/tmp/localstack"
```

Create local configuration:

```java
SnsConfiguration localConfig = SnsConfiguration.builder()
    .region(Region.US_EAST_1)
    .accessKeyId("test")
    .secretAccessKey("test")
    .build();

// Override endpoint for LocalStack
// Note: This requires custom client configuration
```

### Testing Best Practices

```java
@TestMethodOrder(OrderAnnotation.class)
class SmsIntegrationTest {
    
    private SnsSMSAdapter adapter;
    
    @BeforeEach
    void setUp() {
        SnsConfiguration config = SnsConfiguration.forDevelopment(
            Region.US_EAST_1,
            "test-key",
            "test-secret"
        );
        adapter = new SnsSMSAdapter(config);
    }
    
    @Test
    @Order(1)
    void shouldVerifyConnection() {
        Result<Boolean> result = adapter.verify();
        assertTrue(result.isSuccess());
    }
    
    @Test
    @Order(2) 
    void shouldSendTestSMS() {
        SMS sms = SMS.of(PhoneNumber.of("+1234567890"), "Test message");
        Result<SMSReceipt> result = adapter.send(sms);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getValue().messageId());
    }
}
```

## Security

### AWS Credentials Management

**Never hardcode credentials in your application:**

```java
// ❌ DON'T DO THIS
SnsConfiguration badConfig = SnsConfiguration.builder()
    .accessKeyId("AKIA123456789")  // Hardcoded!
    .secretAccessKey("secret123")  // Hardcoded!
    .build();

// ✅ DO THIS INSTEAD
SnsConfiguration goodConfig = SnsConfiguration.builder()
    .accessKeyId(System.getenv("AWS_ACCESS_KEY_ID"))
    .secretAccessKey(System.getenv("AWS_SECRET_ACCESS_KEY"))
    .build();

// ✅ OR USE IAM ROLES (BEST)
SnsConfiguration bestConfig = SnsConfiguration.withIamRole(Region.US_EAST_1);
```

### IAM Policy Example

Minimal IAM policy for SMS operations:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "sns:Publish",
                "sns:GetSMSAttributes"
            ],
            "Resource": "*"
        }
    ]
}
```

For production environments with specific topics:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow", 
            "Action": [
                "sns:Publish"
            ],
            "Resource": [
                "arn:aws:sns:us-east-1:123456789012:sms-notifications",
                "arn:aws:sns:us-east-1:123456789012:sms-alerts"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "sns:GetSMSAttributes"
            ],
            "Resource": "*"
        }
    ]
}
```

### Cost Control

Always set appropriate pricing limits:

```java
SnsConfiguration config = SnsConfiguration.builder()
    .maxPriceUSD(0.10)  // 10 cents maximum per SMS
    .smsType(SnsConfiguration.SmsType.TRANSACTIONAL)
    .build();
```

Monitor AWS costs and set up billing alerts for SNS usage.

## Troubleshooting

### Common Issues

#### Authentication Errors

```
AUTHORIZATION_ERROR: AWS credentials or permissions invalid
```

**Solutions:**
1. Verify AWS credentials are correct
2. Check IAM policies have SNS permissions
3. Ensure credentials are not expired
4. Verify region matches your AWS setup

#### Invalid Phone Numbers

```
INVALID_PARAMETER: Invalid phone number format
```

**Solutions:**
1. Use E.164 format: `+1234567890`
2. Verify country code is correct
3. Check phone number is mobile (not landline)

#### Rate Limiting

```
RATE_LIMIT_EXCEEDED: SNS rate limit exceeded
```

**Solutions:**
1. Implement exponential backoff
2. Reduce sending rate
3. Consider using SQS for queuing messages
4. Contact AWS support for limit increases

#### Opted Out Numbers

```
PHONE_OPTED_OUT: The phone number has opted out of receiving SMS
```

**Solutions:**
1. Remove number from sending lists
2. Implement opt-in confirmation flow
3. Respect user preferences

### Debug Configuration

Enable detailed logging:

```java
SnsConfiguration config = SnsConfiguration.builder()
    .region(Region.US_EAST_1)
    .accessKeyId("...")
    .secretAccessKey("...")
    .deliveryStatusLogging(true)  // Enable SNS delivery logs
    .build();
```

Check CloudWatch logs for detailed error information.

### Performance Optimization

For high-volume applications:

```java
// Use async operations
CompletableFuture<Result<SMSReceipt>> future = CompletableFuture
    .supplyAsync(() -> smsAdapter.send(sms))
    .whenComplete((result, throwable) -> {
        if (throwable != null) {
            logger.error("SMS send failed", throwable);
        } else {
            logger.info("SMS sent: {}", result.getValue().messageId());
        }
    });

// Batch operations efficiently  
List<CompletableFuture<Result<SMSReceipt>>> futures = messages.stream()
    .map(sms -> CompletableFuture.supplyAsync(() -> smsAdapter.send(sms)))
    .collect(Collectors.toList());

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .thenRun(() -> System.out.println("All messages processed"));
```

## API Reference

### SnsConfiguration

Configuration record for AWS SNS SMS adapter.

#### Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `builder()` | Creates configuration builder | `Builder` |
| `forDevelopment(region, keyId, secret)` | Development configuration | `SnsConfiguration` |
| `forProduction(region, keyId, secret, senderId)` | Production configuration | `SnsConfiguration` |
| `withIamRole(region)` | IAM role configuration | `SnsConfiguration` |

### SnsSMSAdapter

AWS SNS implementation of SMSPort interface.

#### Constructor

```java
public SnsSMSAdapter(SnsConfiguration configuration)
```

#### Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `send(SMS sms)` | Send individual SMS | `Result<SMSReceipt>` |
| `send(SMS sms, SMSOptions options)` | Send SMS with options | `Result<SMSReceipt>` |
| `sendBulk(BulkSMS bulkSMS)` | Send bulk SMS | `Result<BulkSMSReceipt>` |
| `sendBulk(BulkSMS bulkSMS, SMSOptions options)` | Send bulk SMS with options | `Result<BulkSMSReceipt>` |
| `sendMMS(...)` | MMS not supported | `Result<SMSReceipt>` (failure) |
| `verify()` | Test connection | `Result<Boolean>` |
| `getStatus(String messageId)` | Get message status | `Result<SMSStatus>` (UNKNOWN) |
| `close()` | Close adapter and client | `void` |

### Error Types

| Type | Status | Description |
|------|--------|-------------|
| `INVALID_PARAMETER` | 400 | Invalid request parameter |
| `AUTHORIZATION_ERROR` | 401 | Authentication/authorization failed |
| `PHONE_OPTED_OUT` | 400 | Phone number opted out |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_SERVER_ERROR` | 500 | AWS internal error |
| `MMS_NOT_SUPPORTED` | 501 | MMS not supported by SNS |

### SMS Status Values

| Status | Description |
|--------|-------------|
| `SENT` | Message accepted by SNS |
| `UNKNOWN` | Status tracking not available |

**Note**: AWS SNS doesn't provide direct delivery status tracking for SMS. Use `deliveryStatusLogging` to send logs to CloudWatch instead.

---

For more information, see the [AWS SNS Documentation](https://docs.aws.amazon.com/sns/) and [SMS Best Practices](https://docs.aws.amazon.com/sns/latest/dg/sms_preferences.html).
