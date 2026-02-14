# Commons Adapters :: Email :: SendGrid

A robust and feature-rich SendGrid adapter for the Commons Email Port, providing seamless email delivery through SendGrid's powerful email service.

## 📦 Installation

Add this dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-email-sendgrid</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 🚀 Quick Start

### Basic Usage

```java
import com.marcusprado02.commons.adapters.email.sendgrid.*;
import com.marcusprado02.commons.ports.email.*;

// Configure SendGrid
SendGridConfiguration config = SendGridConfiguration.builder()
    .apiKey("SG.your-sendgrid-api-key-here")
    .defaultFromEmail("noreply@yourcompany.com")
    .defaultFromName("Your Company")
    .build();

// Create adapter
SendGridEmailAdapter emailAdapter = new SendGridEmailAdapter(config);

// Send email
Email email = Email.builder()
    .from("sender@yourcompany.com")
    .to("customer@example.com")
    .subject("Welcome to Our Service")
    .htmlContent("<h1>Welcome!</h1><p>Thank you for joining us.</p>")
    .build();

Result<EmailReceipt> result = emailAdapter.send(email);

if (result.isOk()) {
    System.out.println("Email sent successfully: " + result.getOrNull().messageId());
} else {
    System.err.println("Email failed: " + result.problemOrNull().message());
}
```

## 🔧 Configuration

### API Key Setup

1. **Get your SendGrid API Key**:
   - Sign up at [SendGrid](https://sendgrid.com/)
   - Go to Settings → API Keys
   - Create a new API key with "Mail Send" permissions

2. **Set up authentication**:
   ```java
   // Production configuration
   SendGridConfiguration config = SendGridConfiguration.forProduction(
       "SG.your-production-api-key",
       "noreply@yourcompany.com", 
       "Your Company Name"
   );
   ```

### Configuration Options

```java
SendGridConfiguration config = SendGridConfiguration.builder()
    .apiKey("SG.your-api-key")                    // Required: SendGrid API Key
    .defaultFromEmail("noreply@company.com")      // Optional: Default sender email
    .defaultFromName("Company Name")              // Optional: Default sender name
    .requestTimeout(Duration.ofSeconds(30))       // Optional: HTTP timeout (default: 10s)
    .trackClicks(true)                           // Optional: Enable click tracking (default: true)
    .trackOpens(true)                            // Optional: Enable open tracking (default: true)
    .sandboxMode(false)                          // Optional: Sandbox mode for testing (default: false)
    .build();
```

### Environment-Specific Configurations

#### Development/Testing
```java
// Testing configuration with sandbox mode
SendGridConfiguration config = SendGridConfiguration.forTesting("SG.test-key");
```

#### Production
```java
// Production configuration with full tracking
SendGridConfiguration config = SendGridConfiguration.forProduction(
    System.getenv("SENDGRID_API_KEY"),
    "noreply@mycompany.com",
    "My Company"
);
```

## 📧 Sending Emails

### Simple Text Email

```java
Email email = Email.builder()
    .from("sender@company.com")
    .to("recipient@example.com")
    .subject("Simple Text Message")
    .textContent("This is a plain text email message.")
    .build();

Result<EmailReceipt> result = emailAdapter.send(email);
```

### HTML Email

```java
Email email = Email.builder()
    .from("marketing@company.com") 
    .to("customer@example.com")
    .subject("Welcome to Our Service!")
    .htmlContent("""
        <html>
        <body>
            <h1>Welcome!</h1>
            <p>Thank you for joining <strong>Our Service</strong>.</p>
            <p>Get started by <a href="https://app.company.com">logging in</a>.</p>
        </body>
        </html>
        """)
    .build();

Result<EmailReceipt> result = emailAdapter.send(email);
```

### Multi-Part Email (HTML + Text)

```java
Email email = Email.builder()
    .from("newsletter@company.com")
    .to("subscriber@example.com") 
    .subject("Monthly Newsletter")
    .htmlContent("<h1>Newsletter</h1><p>HTML version...</p>")
    .textContent("Newsletter\n\nText version...")
    .build();
```

### Email with Multiple Recipients

```java
Email email = Email.builder()
    .from("admin@company.com")
    .to("user1@example.com")
    .to("user2@example.com")              // Multiple TO recipients
    .cc("manager@company.com")            // CC recipient
    .bcc("archive@company.com")           // BCC recipient (hidden)
    .replyTo("support@company.com")       // Reply-to address
    .subject("Team Update")
    .htmlContent("<p>Important team announcement...</p>")
    .build();
```

### Email with Attachments

```java
// Prepare attachments
byte[] pdfData = Files.readAllBytes(Paths.get("document.pdf"));
byte[] imageData = Files.readAllBytes(Paths.get("logo.png"));

Email email = Email.builder()
    .from("documents@company.com")
    .to("client@example.com")
    .subject("Contract Documents")
    .htmlContent("<p>Please find the attached contract documents.</p>")
    .attachment(EmailAttachment.of("contract.pdf", "application/pdf", pdfData))
    .attachment(EmailAttachment.of("logo.png", "image/png", imageData))
    .build();

Result<EmailReceipt> result = emailAdapter.send(email);
```

## 🏗️ Advanced Features

### Click and Open Tracking

SendGrid automatically tracks email opens and clicks when enabled:

```java
SendGridConfiguration config = SendGridConfiguration.builder()
    .apiKey("SG.your-key")
    .trackClicks(true)     // Track link clicks
    .trackOpens(true)      // Track email opens
    .build();
```

### Sandbox Mode for Testing

Use sandbox mode to test email sending without actually delivering emails:

```java
SendGridConfiguration config = SendGridConfiguration.builder()
    .apiKey("SG.test-key")
    .sandboxMode(true)     // Emails validated but not sent
    .build();

// Emails will be processed but not delivered
Result<EmailReceipt> result = emailAdapter.send(email);
// result.isOk() will be true, but no actual email is sent
```

### Connection Verification

Test your SendGrid configuration:

```java
Result<Void> connectionResult = emailAdapter.verify();

if (connectionResult.isOk()) {
    System.out.println("SendGrid connection verified successfully");
} else {
    System.err.println("Connection failed: " + connectionResult.problemOrNull().message());
}
```

## 🧪 Testing

### Unit Testing with Mocks

```java
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailPort emailPort;

    @Test
    void shouldSendWelcomeEmail() {
        // Given
        EmailReceipt mockReceipt = EmailReceipt.of("sendgrid-message-123");
        when(emailPort.send(any(Email.class))).thenReturn(Result.ok(mockReceipt));

        // When
        Result<EmailReceipt> result = emailPort.send(welcomeEmail);

        // Then
        assertThat(result.isOk()).isTrue();
        assertThat(result.getOrNull().messageId()).isEqualTo("sendgrid-message-123");
    }
}
```

### Integration Testing

```java
@TestConfiguration
public class EmailTestConfig {
    
    @Bean
    @Primary
    public SendGridConfiguration testSendGridConfig() {
        return SendGridConfiguration.forTesting("SG.test-key-for-integration-tests");
    }
}
```

## 🌟 Use Cases

### 1. Welcome Email

```java
public void sendWelcomeEmail(String userEmail, String userName) {
    Email email = Email.builder()
        .from("welcome@company.com")
        .to(userEmail)
        .subject("Welcome to Our Platform, " + userName + "!")
        .htmlContent(String.format("""
            <h1>Welcome %s!</h1>
            <p>Thank you for joining our platform.</p>
            <p><a href="https://app.company.com/onboarding">Complete your setup</a></p>
            """, userName))
        .build();

    Result<EmailReceipt> result = emailAdapter.send(email);
    
    if (result.isFail()) {
        logger.error("Failed to send welcome email to {}: {}", 
                    userEmail, result.problemOrNull().message());
    }
}
```

### 2. Password Reset

```java
public void sendPasswordResetEmail(String userEmail, String resetToken) {
    String resetLink = "https://app.company.com/reset-password?token=" + resetToken;
    
    Email email = Email.builder()
        .from("security@company.com")
        .to(userEmail)
        .subject("Password Reset Request")
        .htmlContent(String.format("""
            <h2>Password Reset</h2>
            <p>Click the link below to reset your password:</p>
            <p><a href="%s">Reset Password</a></p>
            <p>This link expires in 1 hour.</p>
            <p>If you didn't request this, please ignore this email.</p>
            """, resetLink))
        .build();

    emailAdapter.send(email);
}
```

### 3. Order Confirmation

```java
public void sendOrderConfirmation(String customerEmail, Order order) {
    Email email = Email.builder()
        .from("orders@company.com") 
        .to(customerEmail)
        .subject("Order Confirmation - #" + order.getId())
        .htmlContent(generateOrderHtml(order))
        .attachment(generateInvoicePdf(order))
        .build();

    emailAdapter.send(email);
}
```

### 4. Newsletter

```java
public void sendNewsletter(List<String> subscribers, Newsletter newsletter) {
    for (String subscriber : subscribers) {
        Email email = Email.builder()
            .from("newsletter@company.com")
            .to(subscriber)
            .subject(newsletter.getSubject())
            .htmlContent(newsletter.getHtmlContent())
            .textContent(newsletter.getTextContent())
            .build();

        emailAdapter.send(email);
        
        // Add delay to respect rate limits
        Thread.sleep(100);
    }
}
```

## 🔒 Security Best Practices

### 1. API Key Management

```java
// ✅ Good: Use environment variables
String apiKey = System.getenv("SENDGRID_API_KEY");
if (apiKey == null) {
    throw new IllegalStateException("SENDGRID_API_KEY environment variable is required");
}

// ❌ Bad: Hardcode API keys
String apiKey = "SG.hardcoded-key-123"; // Never do this!
```

### 2. Input Validation

```java
public Result<EmailReceipt> sendUserEmail(String userEmail, String subject, String content) {
    // Validate email format
    if (!isValidEmail(userEmail)) {
        return Result.fail(Problem.of(
            ErrorCode.of("INVALID_EMAIL_FORMAT"),
            ErrorCategory.BUSINESS,
            Severity.ERROR,
            "Invalid email format: " + userEmail));
    }

    // Sanitize content
    String sanitizedSubject = sanitizeSubject(subject);
    String sanitizedContent = sanitizeHtmlContent(content);

    // Send email...
}
```

### 3. Rate Limiting

SendGrid has rate limits. Consider implementing your own:

```java
@Component
public class EmailService {
    
    private final RateLimiter rateLimiter = RateLimiter.create(100.0); // 100 emails/second
    
    public Result<EmailReceipt> sendEmail(Email email) {
        if (!rateLimiter.tryAcquire(Duration.ofSeconds(1))) {
            return Result.fail(Problem.of(
                ErrorCode.of("EMAIL_RATE_LIMITED"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "Email sending rate limit exceeded"));
        }
        
        return emailAdapter.send(email);
    }
}
```

## 📊 Monitoring and Observability

### Error Handling

```java
Result<EmailReceipt> result = emailAdapter.send(email);

if (result.isFail()) {
    Problem problem = result.problemOrNull();
    
    switch (problem.code().value()) {
        case "SENDGRID_UNAUTHORIZED":
            logger.error("SendGrid API key is invalid or expired");
            // Alert operations team
            break;
            
        case "SENDGRID_RATE_LIMITED":
            logger.warn("SendGrid rate limit exceeded, retrying later");
            // Implement exponential backoff
            break;
            
        case "SENDGRID_TIMEOUT":
            logger.warn("SendGrid API timeout, email may be retried");
            // Queue for retry
            break;
            
        default:
            logger.error("SendGrid email failed: {}", problem.message());
    }
}
```

### Metrics Collection

```java
@Component
public class EmailMetrics {
    
    private final Counter emailsSent = Counter.build()
        .name("emails_sent_total")
        .help("Total emails sent")
        .labelNames("adapter", "status")
        .register();
    
    public void recordEmailSent(boolean success) {
        emailsSent.labels("sendgrid", success ? "success" : "failure").inc();
    }
}
```

## ⚡ Performance Considerations

### 1. Batch Operations

For sending multiple emails, consider SendGrid's batch API (future enhancement):

```java
// Current: Individual sends
for (String recipient : recipients) {
    Email email = Email.builder()
        .from("bulk@company.com")
        .to(recipient)
        .subject("Newsletter")
        .htmlContent(content)
        .build();
    
    emailAdapter.send(email);
    Thread.sleep(10); // Rate limiting
}
```

### 2. Async Processing

```java
@Service
public class AsyncEmailService {
    
    @Async("emailExecutor")
    public CompletableFuture<Result<EmailReceipt>> sendEmailAsync(Email email) {
        return CompletableFuture.completedFuture(emailAdapter.send(email));
    }
}
```

### 3. Connection Pooling

The SendGrid SDK handles HTTP connection pooling internally, but you can configure timeouts:

```java
SendGridConfiguration config = SendGridConfiguration.builder()
    .apiKey("SG.your-key")
    .requestTimeout(Duration.ofSeconds(30))  // Longer timeout for large attachments
    .build();
```

## 🐛 Troubleshooting

### Common Issues

1. **401 Unauthorized**
   - Check API key validity
   - Ensure API key has "Mail Send" permissions
   - Verify key is not expired

2. **403 Forbidden** 
   - Check sender email domain verification
   - Ensure sender reputation is good
   - Verify account is not suspended

3. **429 Rate Limited**
   - Implement exponential backoff
   - Consider upgrading SendGrid plan
   - Spread sends over time

4. **Timeout Errors**
   - Increase request timeout
   - Check network connectivity
   - Reduce attachment sizes

### Debug Configuration

```java
SendGridConfiguration debugConfig = SendGridConfiguration.builder()
    .apiKey("SG.your-key")
    .sandboxMode(true)           // Test without sending
    .requestTimeout(Duration.ofMinutes(1))  // Longer timeout
    .build();
```

### Logging

Enable verbose logging in your application:

```properties
# application.properties
logging.level.com.marcusprado02.commons.adapters.email.sendgrid=DEBUG
logging.level.com.sendgrid=DEBUG
```

## 📚 API Reference

### SendGridConfiguration

| Method | Description | Default |
|---------|-------------|---------|
| `apiKey(String)` | SendGrid API key (required) | - |
| `requestTimeout(Duration)` | HTTP request timeout | 10 seconds |
| `defaultFromEmail(String)` | Default sender email | null |
| `defaultFromName(String)` | Default sender name | null |
| `trackClicks(boolean)` | Enable click tracking | true |
| `trackOpens(boolean)` | Enable open tracking | true |
| `sandboxMode(boolean)` | Enable sandbox mode | false |

### SendGridEmailAdapter

| Method | Return Type | Description |
|---------|-------------|-------------|
| `send(Email)` | `Result<EmailReceipt>` | Send email via SendGrid |
| `sendWithTemplate(TemplateEmailRequest)` | `Result<EmailReceipt>` | Send template email (not implemented) |
| `verify()` | `Result<Void>` | Verify SendGrid connection |
| `close()` | `void` | Close adapter resources |

### Error Codes

| Code | Category | Description |
|------|----------|-------------|
| `SENDGRID_UNAUTHORIZED` | UNAUTHORIZED | Invalid API key |
| `SENDGRID_FORBIDDEN` | UNAUTHORIZED | Insufficient permissions |
| `SENDGRID_RATE_LIMITED` | BUSINESS | Rate limit exceeded |
| `SENDGRID_TIMEOUT` | TECHNICAL | Request timeout |
| `SENDGRID_CONNECTION_FAILED` | TECHNICAL | Network connection failed |
| `SENDGRID_EMAIL_SEND_ERROR` | TECHNICAL | General send error |
| `SENDGRID_TEMPLATE_NOT_SUPPORTED` | TECHNICAL | Template feature not implemented |

## 🔗 Related

- [Commons Ports :: Email](../commons-ports-email/) - Email port interface
- [Commons Adapters :: Email :: SMTP](../commons-adapters-email-smtp/) - SMTP adapter
- [SendGrid Documentation](https://docs.sendgrid.com/) - Official SendGrid docs
- [SendGrid Java SDK](https://github.com/sendgrid/sendgrid-java) - Official Java SDK

## 🏆 Examples

Complete working examples are available in the [examples](../../examples/) directory:

- `sendgrid-basic-email/` - Basic email sending
- `sendgrid-html-email/` - HTML emails with styling
- `sendgrid-batch-emails/` - Bulk email sending
- `sendgrid-spring-integration/` - Spring Boot integration
