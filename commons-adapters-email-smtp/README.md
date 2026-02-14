# commons-adapters-email-smtp

SMTP adapter for EmailPort using Jakarta Mail with Thymeleaf template support.

## 📦 Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-email-smtp</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Optional: Template Support

For Thymeleaf template rendering:

```xml
<dependency>
  <groupId>org.thymeleaf</groupId>
  <artifactId>thymeleaf</artifactId>
  <version>3.1.2.RELEASE</version>
</dependency>
```

## ✨ Features

- ✅ **Jakarta Mail Integration** - Industry-standard email sending
- ✅ **HTML & Plain Text** - Support for both content types
- ✅ **Attachments** - Send files with your emails
- ✅ **Template Rendering** - Thymeleaf templates for dynamic content
- ✅ **CC/BCC Support** - Send copies to multiple recipients
- ✅ **Reply-To** - Configure reply-to addresses
- ✅ **TLS/StartTLS** - Secure connections
- ✅ **Authentication** - SMTP authentication support
- ✅ **Connection Verification** - Test your SMTP server connectivity
- ✅ **AutoCloseable** - Resource management with try-with-resources

## 🚀 Quick Start

### Basic Email

```java
SmtpConfiguration config = SmtpConfiguration.builder()
    .host("smtp.gmail.com")
    .port(587)
    .username("your-email@gmail.com")
    .password("your-app-password")
    .useStartTls(true)
    .build();

EmailPort emailPort = new SmtpEmailAdapter(config);

Email email = Email.builder()
    .from("noreply@example.com")
    .to("user@example.com")
    .subject("Welcome!")
    .htmlContent("<h1>Hello, World!</h1><p>Welcome to our service.</p>")
    .build();

Result<EmailReceipt, EmailError> result = emailPort.send(email);

if (result.isSuccess()) {
    System.out.println("Email sent: " + result.get().messageId());
} else {
    System.err.println("Failed to send email: " + result.getError());
}
```

### Email with Attachments

```java
byte[] pdfData = Files.readAllBytes(Path.of("report.pdf"));
EmailAttachment attachment = EmailAttachment.of(
    "report.pdf",
    "application/pdf",
    pdfData
);

Email email = Email.builder()
    .from("reports@example.com")
    .to("user@example.com")
    .subject("Monthly Report")
    .textContent("Please find attached your monthly report.")
    .attachment(attachment)
    .build();

emailPort.send(email);
```

### Template Email with Thymeleaf

**1. Create template** (`resources/email-templates/welcome.html`):

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Welcome</title>
</head>
<body>
    <h1>Welcome, [[${userName}]]!</h1>
    <p>Your account has been created successfully.</p>
    <p>Your activation link: <a th:href="${activationLink}">Click here</a></p>
    <p>Best regards,<br>The Team</p>
</body>
</html>
```

**2. Send with template**:

```java
SmtpConfiguration config = SmtpConfiguration.builder()
    .host("smtp.example.com")
    .port(587)
    .username("user")
    .password("pass")
    .useStartTls(true)
    .build();

TemplateRenderer renderer = ThymeleafTemplateRenderer.withDefaults();
EmailPort emailPort = new SmtpEmailAdapter(config, renderer);

TemplateEmailRequest request = TemplateEmailRequest.builder()
    .from("noreply@example.com")
    .to("newuser@example.com")
    .subject("Welcome to our service!")
    .templateName("welcome")
    .variable("userName", "John Doe")
    .variable("activationLink", "https://example.com/activate?token=abc123")
    .build();

Result<EmailReceipt, EmailError> result = emailPort.sendWithTemplate(request);
```

## 📖 Configuration

### SmtpConfiguration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `host` | String | localhost | SMTP server hostname |
| `port` | int | 25 | SMTP server port |
| `username` | String | null | SMTP username (required if `requireAuth=true`) |
| `password` | String | null | SMTP password (required if `requireAuth=true`) |
| `requireAuth` | boolean | true | Enable SMTP authentication |
| `useTls` | boolean | false | Use TLS/SSL encryption |
| `useStartTls` | boolean | false | Use STARTTLS upgrade |
| `connectionTimeout` | int | 10000 | Connection timeout (ms) |
| `writeTimeout` | int | 10000 | Write timeout (ms) |

### Common SMTP Configurations

#### Gmail

```java
SmtpConfiguration gmail = SmtpConfiguration.builder()
    .host("smtp.gmail.com")
    .port(587)
    .username("your-email@gmail.com")
    .password("your-app-password") // Use App Password, not regular password
    .useStartTls(true)
    .build();
```

**Note**: Enable 2-factor authentication and create an App Password at https://myaccount.google.com/apppasswords

#### Microsoft 365 / Outlook

```java
SmtpConfiguration outlook = SmtpConfiguration.builder()
    .host("smtp.office365.com")
    .port(587)
    .username("your-email@outlook.com")
    .password("your-password")
    .useStartTls(true)
    .build();
```

#### AWS SES

```java
SmtpConfiguration ses = SmtpConfiguration.builder()
    .host("email-smtp.us-east-1.amazonaws.com")
    .port(587)
    .username("your-smtp-username") // From SES Console
    .password("your-smtp-password")
    .useStartTls(true)
    .build();
```

#### Local Development (GreenMail)

```java
SmtpConfiguration local = SmtpConfiguration.builder()
    .host("localhost")
    .port(3025)
    .requireAuth(false)
    .build();
```

## 🎨 Template Rendering

### Thymeleaf Configuration

```java
TemplateRenderer renderer = ThymeleafTemplateRenderer.builder()
    .templatePrefix("/email-templates/")  // Classpath location
    .templateSuffix(".html")
    .cacheable(true)                      // Enable caching in production
    .cacheableTTLMs(3600000L)            // Cache for 1 hour
    .build();
```

### Template Variables

All template variables are passed as a `Map<String, Object>`:

```java
Map<String, Object> variables = Map.of(
    "userName", "John",
    "orderNumber", 12345,
    "orderDate", LocalDate.now(),
    "items", List.of(item1, item2),
    "total", new BigDecimal("99.99")
);

TemplateEmailRequest request = TemplateEmailRequest.builder()
    .from("orders@example.com")
    .to("customer@example.com")
    .subject("Order Confirmation #12345")
    .templateName("order-confirmation")
    .variables(variables)
    .build();
```

### Template Examples

**Simple Text Replacement**:

```html
<p>Hello, [[${userName}]]!</p>
```

**Conditional Content**:

```html
<div th:if="${isPremium}">
    <p>Thank you for being a premium member!</p>
</div>
```

**Loops**:

```html
<ul>
    <li th:each="item : ${items}">
        [[${item.name}]] - $[[${item.price}]]
    </li>
</ul>
```

**Formatting**:

```html
<p>Order Date: [[${#temporals.format(orderDate, 'dd/MM/yyyy')}]]</p>
<p>Total: $[[${#numbers.formatDecimal(total, 1, 2)}]]</p>
```

## 📝 Email Builder API

### Basic Email Construction

```java
Email email = Email.builder()
    .from("sender@example.com")
    .to("recipient@example.com")
    .subject("Subject Line")
    .textContent("Plain text content")
    .build();
```

### HTML Email

```java
Email email = Email.builder()
    .from("sender@example.com")
    .to("recipient@example.com")
    .subject("HTML Email")
    .htmlContent("<h1>Title</h1><p>Content</p>")
    .build();
```

### Multipart (HTML + Text)

Best practice for maximum compatibility:

```java
Email email = Email.builder()
    .from("sender@example.com")
    .to("recipient@example.com")
    .subject("Multipart Email")
    .bothContent(
        "<h1>HTML Version</h1><p>This is HTML</p>",
        "Plain text version\nThis is text"
    )
    .build();
```

### Multiple Recipients

```java
Email email = Email.builder()
    .from("sender@example.com")
    .to("user1@example.com")
    .to("user2@example.com")
    .cc("manager@example.com")
    .bcc("archive@example.com")
    .subject("Team Update")
    .textContent("Important team update")
    .build();
```

### Bulk Recipients

```java
List<EmailAddress> recipients = users.stream()
    .map(User::getEmail)
    .map(EmailAddress::of)
    .toList();

Email email = Email.builder()
    .from("newsletter@example.com")
    .to(recipients)
    .subject("Monthly Newsletter")
    .htmlContent(newsletterHtml)
    .build();
```

### Reply-To Address

```java
Email email = Email.builder()
    .from("noreply@example.com")
    .to("customer@example.com")
    .replyTo("support@example.com")
    .subject("Support Ticket Response")
    .textContent("Your ticket has been updated")
    .build();
```

## 🔒 Security Best Practices

### 1. Use Environment Variables for Credentials

```java
SmtpConfiguration config = SmtpConfiguration.builder()
    .host(System.getenv("SMTP_HOST"))
    .port(Integer.parseInt(System.getenv("SMTP_PORT")))
    .username(System.getenv("SMTP_USERNAME"))
    .password(System.getenv("SMTP_PASSWORD"))
    .useStartTls(true)
    .build();
```

### 2. Use App Passwords (Gmail)

Never use your main Gmail password. Create an App Password:
1. Enable 2-factor authentication
2. Go to https://myaccount.google.com/apppasswords
3. Generate a new app password
4. Use the generated password in your configuration

### 3. Enable TLS/StartTLS

Always use encrypted connections in production:

```java
.useStartTls(true)  // For port 587
// OR
.useTls(true)       // For port 465
```

### 4. Validate Email Addresses

Email addresses are validated automatically by `EmailAddress`:

```java
try {
    EmailAddress address = EmailAddress.of("invalid-email");
} catch (IllegalArgumentException e) {
    // Handle invalid email
}
```

### 5. Sanitize Template Variables

Thymeleaf automatically escapes HTML by default, but be cautious with user-provided content:

```html
<!-- Safe: Automatically escaped -->
<p>[[${userInput}]]</p>

<!-- Unsafe: No escaping -->
<p th:utext="${userInput}"></p>
```

## 🧪 Testing

### With GreenMail

```xml
<dependency>
    <groupId>com.icegreen</groupId>
    <artifactId>greenmail-junit5</artifactId>
    <version>2.0.1</version>
    <scope>test</scope>
</dependency>
```

```java
@ExtendWith({GreenMailExtension.class})
class EmailTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    void shouldSendEmail() {
        SmtpConfiguration config = SmtpConfiguration.builder()
            .host("localhost")
            .port(greenMail.getSmtp().getPort())
            .requireAuth(false)
            .build();

        EmailPort emailPort = new SmtpEmailAdapter(config);

        Email email = Email.builder()
            .from("test@example.com")
            .to("recipient@example.com")
            .subject("Test")
            .textContent("Test message")
            .build();

        emailPort.send(email);

        greenMail.waitForIncomingEmail(1);
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
    }
}
```

## 🎯 Use Cases

### 1. Welcome Email

```java
TemplateEmailRequest welcome = TemplateEmailRequest.builder()
    .from("welcome@example.com")
    .to(newUser.getEmail())
    .subject("Welcome to Example.com!")
    .templateName("welcome")
    .variable("userName", newUser.getName())
    .variable("activationLink", generateActivationLink(newUser))
    .build();

emailPort.sendWithTemplate(welcome);
```

### 2. Password Reset

```java
TemplateEmailRequest reset = TemplateEmailRequest.builder()
    .from("security@example.com")
    .to(user.getEmail())
    .subject("Password Reset Request")
    .templateName("password-reset")
    .variable("userName", user.getName())
    .variable("resetLink", generateResetLink(user))
    .variable("expiresIn", "1 hour")
    .build();

emailPort.sendWithTemplate(reset);
```

### 3. Order Confirmation

```java
Email orderEmail = Email.builder()
    .from("orders@example.com")
    .to(customer.getEmail())
    .subject("Order Confirmation #" + order.getId())
    .htmlContent(renderOrderConfirmation(order))
    .attachment(createInvoicePdf(order))
    .build();

emailPort.send(orderEmail);
```

### 4. Newsletter

```java
List<User> subscribers = userRepository.findAllSubscribers();

for (User subscriber : subscribers) {
    TemplateEmailRequest newsletter = TemplateEmailRequest.builder()
        .from("newsletter@example.com")
        .to(subscriber.getEmail())
        .subject("Monthly Newsletter - " + YearMonth.now())
        .templateName("newsletter")
        .variable("userName", subscriber.getName())
        .variable("articles", getLatestArticles())
        .variable("unsubscribeLink", generateUnsubscribeLink(subscriber))
        .build();

    emailPort.sendWithTemplate(newsletter);
}
```

## ⚠️ Error Handling

```java
Result<EmailReceipt, EmailError> result = emailPort.send(email);

result.match(
    receipt -> {
        logger.info("Email sent successfully: {}", receipt.messageId());
        return null;
    },
    error -> {
        switch (error) {
            case AUTHENTICATION_FAILED:
                logger.error("SMTP authentication failed - check credentials");
                break;
            case CONNECTION_FAILED:
                logger.error("Failed to connect to SMTP server");
                break;
            case SEND_FAILED:
                logger.error("Email rejected by SMTP server");
                break;
            case TEMPLATE_ERROR:
                logger.error("Failed to render email template");
                break;
            default:
                logger.error("Unknown error sending email");
        }
        return null;
    }
);
```

## 🔍 Connection Verification

Verify SMTP connectivity before sending:

```java
Result<Void, EmailError> verifyResult = emailPort.verify();

if (verifyResult.isSuccess()) {
    System.out.println("SMTP connection successful");
} else {
    System.err.println("SMTP connection failed: " + verifyResult.getError());
}
```

## 📊 Performance Considerations

### Connection Pooling

Jakarta Mail doesn't maintain persistent connections. Each `send()` call opens a new connection. For high-volume scenarios, consider:

1. **Batch emails** to reduce connection overhead
2. **Use dedicated email services** (SendGrid, AWS SES API) for bulk operations
3. **Implement retry logic** for transient failures

### Template Caching

Enable template caching in production:

```java
TemplateRenderer renderer = ThymeleafTemplateRenderer.builder()
    .cacheable(true)
    .cacheableTTLMs(3600000L) // 1 hour
    .build();
```

### Attachment Size

Be mindful of attachment sizes. Most SMTP servers have size limits (typically 10-25MB). For large files, consider:

1. Uploading to cloud storage
2. Including download links in emails instead of attachments

## 🔧 Troubleshooting

### "Authentication Failed"

- **Gmail**: Use App Password, not your main password
- **Office365**: Ensure SMTP authentication is enabled
- **AWS SES**: Use SMTP credentials from SES console, not IAM credentials

### "Connection Refused"

- Check firewall/security groups allow outbound connections on SMTP port
- Verify SMTP server hostname and port
- Try using `telnet smtp.server.com 587` to test connectivity

### "SSL/TLS Errors"

```java
// For StartTLS (port 587)
.useStartTls(true)
.useTls(false)

// For implicit SSL (port 465)
.useTls(true)
.useStartTls(false)
```

### "Template Not Found"

- Verify template location in classpath
- Check template prefix and suffix configuration
- Ensure template file extension matches suffix

## 📚 Related Modules

- **commons-ports-email** - Email port interface definitions
- **commons-adapters-email-sendgrid** - SendGrid adapter (planned)
- **commons-adapters-email-ses** - AWS SES adapter (planned)

## 📄 License

This module is part of the Commons Platform project.
