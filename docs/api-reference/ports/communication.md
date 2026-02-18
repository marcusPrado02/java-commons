# Port: Communication

## Visão Geral

`commons-ports-communication` define contratos para comunicação multicanal (email, SMS, push notifications), abstraindo implementações como SendGrid, Twilio, APNS, FCM.

**Quando usar:**
- Envio de emails transacionais
- SMS para verificação e notificações
- Push notifications mobile
- Comunicação multicanal
- Notification preferences

**Implementações disponíveis:**
- `commons-adapters-email-sendgrid` - SendGrid
- `commons-adapters-email-smtp` - SMTP
- `commons-adapters-sms-twilio` - Twilio
- `commons-adapters-sms-aws-sns` - AWS SNS
- `commons-adapters-notification-fcm` - Firebase Cloud Messaging
- `commons-adapters-notification-apns` - Apple Push Notification Service

---

## 📦 Instalação

```xml
<!-- Port (interface) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-communication</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapter (email) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-email-sendgrid</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapter (SMS) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-sms-twilio</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 📧 Email Interface

### EmailSender

Interface para envio de emails.

```java
public interface EmailSender {
    
    /**
     * Envia email simples.
     */
    Result<MessageId> send(Email email);
    
    /**
     * Envia email com template.
     */
    Result<MessageId> send(TemplateEmail email);
    
    /**
     * Envia emails em batch.
     */
    Result<List<MessageId>> sendBatch(List<Email> emails);
}
```

### Email Model

```java
public record Email(
    EmailAddress from,
    List<EmailAddress> to,
    List<EmailAddress> cc,
    List<EmailAddress> bcc,
    String subject,
    String textBody,
    String htmlBody,
    List<Attachment> attachments,
    Map<String, String> headers
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        public Builder from(String email);
        public Builder from(String email, String name);
        public Builder to(String email);
        public Builder to(String email, String name);
        public Builder cc(String email);
        public Builder bcc(String email);
        public Builder subject(String subject);
        public Builder text(String textBody);
        public Builder html(String htmlBody);
        public Builder attachment(String filename, byte[] content);
        public Builder header(String name, String value);
        public Email build();
    }
}

public record EmailAddress(String email, Optional<String> name) {
    public static EmailAddress of(String email) {
        return new EmailAddress(email, Optional.empty());
    }
    
    public static EmailAddress of(String email, String name) {
        return new EmailAddress(email, Optional.of(name));
    }
}
```

---

## 💡 Email Usage Examples

### Transactional Email

```java
@Service
public class OrderNotificationService {
    
    private final EmailSender emailSender;
    
    public Result<Void> sendOrderConfirmation(Order order, User user) {
        String htmlBody = buildOrderConfirmationHtml(order);
        
        Email email = Email.builder()
            .from("orders@mycompany.com", "My Company Orders")
            .to(user.email(), user.name())
            .subject("Order Confirmation #" + order.id().value())
            .html(htmlBody)
            .text(buildOrderConfirmationText(order))
            .header("X-Order-Id", order.id().value())
            .build();
        
        return emailSender.send(email)
            .andThen(messageId -> {
                log.info("Order confirmation sent")
                    .field("orderId", order.id().value())
                    .field("messageId", messageId.value())
                    .field("email", user.email())
                    .log();
            })
            .mapToVoid();
    }
    
    private String buildOrderConfirmationHtml(Order order) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Order Confirmation</title>
            </head>
            <body>
                <h1>Thank you for your order!</h1>
                <p>Order ID: %s</p>
                <p>Total: %s</p>
                <h2>Items:</h2>
                <ul>
                    %s
                </ul>
            </body>
            </html>
            """.formatted(
                order.id().value(),
                order.total(),
                order.items().stream()
                    .map(item -> "<li>" + item.name() + " - " + item.price() + "</li>")
                    .collect(Collectors.joining())
            );
    }
}
```

### Template Email

```java
public record TemplateEmail(
    String templateId,
    EmailAddress from,
    EmailAddress to,
    Map<String, Object> variables
) {}

@Service
public class WelcomeEmailService {
    
    private final EmailSender emailSender;
    
    public Result<Void> sendWelcomeEmail(User user) {
        TemplateEmail email = new TemplateEmail(
            "welcome-email-v1",
            EmailAddress.of("welcome@mycompany.com", "My Company"),
            EmailAddress.of(user.email(), user.name()),
            Map.of(
                "userName", user.name(),
                "activationLink", buildActivationLink(user),
                "supportEmail", "support@mycompany.com"
            )
        );
        
        return emailSender.send(email).mapToVoid();
    }
}
```

---

## 📱 SMS Interface

### SmsSender

Interface para envio de SMS.

```java
public interface SmsSender {
    
    /**
     * Envia SMS.
     */
    Result<MessageId> send(Sms sms);
    
    /**
     * Envia SMS em batch.
     */
    Result<List<MessageId>> sendBatch(List<Sms> smsList);
}
```

### SMS Model

```java
public record Sms(
    PhoneNumber from,
    PhoneNumber to,
    String message,
    Map<String, String> metadata
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        public Builder from(String phoneNumber);
        public Builder to(String phoneNumber);
        public Builder message(String message);
        public Builder metadata(String key, String value);
        public Sms build();
    }
}

public record PhoneNumber(String countryCode, String number) {
    public String toE164() {
        return "+" + countryCode + number;
    }
    
    public static PhoneNumber parse(String e164) {
        // +55 11 98765-4321 -> PhoneNumber("55", "11987654321")
        String digits = e164.replaceAll("[^0-9]", "");
        return new PhoneNumber(
            digits.substring(0, 2),
            digits.substring(2)
        );
    }
}
```

---

## 💬 SMS Usage Examples

### Verification Code

```java
@Service
public class PhoneVerificationService {
    
    private final SmsSender smsSender;
    private final CacheProvider cache;
    
    public Result<Void> sendVerificationCode(PhoneNumber phoneNumber) {
        // Generate 6-digit code
        String code = String.format("%06d", new Random().nextInt(1000000));
        
        // Store in cache (expires in 10 minutes)
        String cacheKey = "verification:" + phoneNumber.toE164();
        cache.set(cacheKey, code, Duration.ofMinutes(10));
        
        // Send SMS
        Sms sms = Sms.builder()
            .from(PhoneNumber.parse("+15551234567"))
            .to(phoneNumber)
            .message("Your verification code is: " + code)
            .metadata("type", "verification")
            .build();
        
        return smsSender.send(sms)
            .andThen(messageId -> {
                log.info("Verification SMS sent")
                    .field("phoneNumber", phoneNumber.toE164())
                    .field("messageId", messageId.value())
                    .log();
            })
            .mapToVoid();
    }
    
    public Result<Boolean> verifyCode(PhoneNumber phoneNumber, String code) {
        String cacheKey = "verification:" + phoneNumber.toE164();
        
        return cache.get(cacheKey, String.class)
            .map(storedCode -> {
                boolean valid = storedCode.equals(code);
                
                if (valid) {
                    cache.delete(cacheKey);
                }
                
                return Result.ok(valid);
            })
            .orElse(Result.ok(false));
    }
}
```

### Order Status SMS

```java
@Service
public class OrderSmsNotificationService {
    
    private final SmsSender smsSender;
    
    public Result<Void> sendOrderShipped(Order order, User user) {
        if (user.phoneNumber().isEmpty()) {
            return Result.ok(); // User has no phone
        }
        
        PhoneNumber phoneNumber = user.phoneNumber().get();
        
        String message = String.format(
            "Your order #%s has been shipped! Track: %s",
            order.id().value().substring(0, 8),
            order.trackingUrl()
        );
        
        Sms sms = Sms.builder()
            .from(PhoneNumber.parse("+15551234567"))
            .to(phoneNumber)
            .message(message)
            .metadata("orderId", order.id().value())
            .metadata("type", "order-shipped")
            .build();
        
        return smsSender.send(sms).mapToVoid();
    }
}
```

---

## 🔔 Push Notification Interface

### PushNotificationSender

Interface para push notifications.

```java
public interface PushNotificationSender {
    
    /**
     * Envia push notification.
     */
    Result<MessageId> send(PushNotification notification);
    
    /**
     * Envia para múltiplos devices.
     */
    Result<List<MessageId>> sendToDevices(
        List<DeviceToken> devices,
        PushNotification notification
    );
    
    /**
     * Envia para topic.
     */
    Result<MessageId> sendToTopic(String topic, PushNotification notification);
}
```

### Push Notification Model

```java
public record PushNotification(
    String title,
    String body,
    Optional<String> imageUrl,
    Map<String, String> data,
    PushNotificationPriority priority,
    Optional<Duration> ttl
) {
    public static Builder builder() {
        return new Builder();
    }
}

public enum PushNotificationPriority {
    HIGH,
    NORMAL,
    LOW
}

public record DeviceToken(
    String token,
    DevicePlatform platform
) {}

public enum DevicePlatform {
    IOS,
    ANDROID,
    WEB
}
```

---

## 📲 Push Notification Usage

### Order Status Push

```java
@Service
public class PushNotificationService {
    
    private final PushNotificationSender pushSender;
    private final DeviceRepository deviceRepository;
    
    public Result<Void> sendOrderStatusNotification(Order order, User user) {
        // Get user's devices
        List<DeviceToken> devices = deviceRepository.findByUserId(user.id());
        
        if (devices.isEmpty()) {
            return Result.ok(); // No devices registered
        }
        
        PushNotification notification = PushNotification.builder()
            .title("Order Update")
            .body("Your order #" + order.id().value() + " has been shipped!")
            .imageUrl("https://cdn.mycompany.com/shipping-icon.png")
            .data(Map.of(
                "orderId", order.id().value(),
                "type", "order-shipped",
                "deepLink", "myapp://orders/" + order.id().value()
            ))
            .priority(PushNotificationPriority.HIGH)
            .ttl(Duration.ofHours(24))
            .build();
        
        return pushSender.sendToDevices(devices, notification)
            .andThen(messageIds -> {
                log.info("Push notifications sent")
                    .field("userId", user.id().value())
                    .field("deviceCount", devices.size())
                    .field("messageIds", messageIds.size())
                    .log();
            })
            .mapToVoid();
    }
}
```

### Topic-Based Notifications

```java
@Service
public class AnnouncementService {
    
    private final PushNotificationSender pushSender;
    
    public Result<Void> sendMaintenanceAnnouncement() {
        PushNotification notification = PushNotification.builder()
            .title("Scheduled Maintenance")
            .body("Our app will be under maintenance tomorrow from 2-4 AM")
            .priority(PushNotificationPriority.NORMAL)
            .data(Map.of(
                "type", "announcement",
                "category", "maintenance"
            ))
            .build();
        
        // Send to all users subscribed to announcements topic
        return pushSender.sendToTopic("announcements", notification)
            .mapToVoid();
    }
}
```

---

## 🎯 Multi-Channel Notification

### NotificationService

Serviço unificado para notificações multicanal.

```java
@Service
public class NotificationService {
    
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final PushNotificationSender pushSender;
    private final UserPreferencesRepository preferencesRepository;
    
    public Result<Void> sendOrderNotification(Order order, User user) {
        // Get user preferences
        UserPreferences preferences = preferencesRepository
            .findByUserId(user.id())
            .orElse(UserPreferences.defaults());
        
        List<CompletableFuture<Result<Void>>> notifications = new ArrayList<>();
        
        // Send email (if enabled)
        if (preferences.emailEnabled()) {
            CompletableFuture<Result<Void>> emailFuture = CompletableFuture
                .supplyAsync(() -> sendOrderEmail(order, user));
            notifications.add(emailFuture);
        }
        
        // Send SMS (if enabled and phone exists)
        if (preferences.smsEnabled() && user.phoneNumber().isPresent()) {
            CompletableFuture<Result<Void>> smsFuture = CompletableFuture
                .supplyAsync(() -> sendOrderSms(order, user));
            notifications.add(smsFuture);
        }
        
        // Send push (if enabled)
        if (preferences.pushEnabled()) {
            CompletableFuture<Result<Void>> pushFuture = CompletableFuture
                .supplyAsync(() -> sendOrderPush(order, user));
            notifications.add(pushFuture);
        }
        
        // Wait for all
        CompletableFuture.allOf(
            notifications.toArray(new CompletableFuture[0])
        ).join();
        
        return Result.ok();
    }
}

public record UserPreferences(
    UserId userId,
    boolean emailEnabled,
    boolean smsEnabled,
    boolean pushEnabled
) {
    public static UserPreferences defaults() {
        return new UserPreferences(
            null,
            true,  // Email on by default
            false, // SMS off by default
            true   // Push on by default
        );
    }
}
```

---

## 📊 Notification Tracking

### Delivery Status

```java
public interface NotificationTrackingService {
    
    /**
     * Registra envio de notificação.
     */
    void trackSent(
        UserId userId,
        NotificationChannel channel,
        MessageId messageId
    );
    
    /**
     * Registra delivery.
     */
    void trackDelivered(MessageId messageId);
    
    /**
     * Registra abertura.
     */
    void trackOpened(MessageId messageId);
    
    /**
     * Registra clique.
     */
    void trackClicked(MessageId messageId);
    
    /**
     * Busca estatísticas.
     */
    NotificationStats getStats(UserId userId);
}

public enum NotificationChannel {
    EMAIL,
    SMS,
    PUSH
}

public record NotificationStats(
    long sent,
    long delivered,
    long opened,
    long clicked
) {
    public double deliveryRate() {
        return sent == 0 ? 0.0 : (double) delivered / sent;
    }
    
    public double openRate() {
        return delivered == 0 ? 0.0 : (double) opened / delivered;
    }
    
    public double clickRate() {
        return opened == 0 ? 0.0 : (double) clicked / opened;
    }
}
```

---

## 🧪 Testing

### Mock Email Sender

```java
public class MockEmailSender implements EmailSender {
    
    private final List<Email> sentEmails = new ArrayList<>();
    
    @Override
    public Result<MessageId> send(Email email) {
        sentEmails.add(email);
        return Result.ok(MessageId.generate());
    }
    
    public List<Email> getSentEmails() {
        return Collections.unmodifiableList(sentEmails);
    }
    
    public Optional<Email> findEmailTo(String emailAddress) {
        return sentEmails.stream()
            .filter(email -> email.to().stream()
                .anyMatch(to -> to.email().equals(emailAddress))
            )
            .findFirst();
    }
    
    public void reset() {
        sentEmails.clear();
    }
}
```

### Test Example

```java
class OrderNotificationServiceTest {
    
    private MockEmailSender emailSender;
    private OrderNotificationService notificationService;
    
    @BeforeEach
    void setUp() {
        emailSender = new MockEmailSender();
        notificationService = new OrderNotificationService(emailSender);
    }
    
    @Test
    void shouldSendOrderConfirmationEmail() {
        // Given
        Order order = Order.create(customer, items);
        User user = User.create("john@example.com", "John Doe");
        
        // When
        Result<Void> result = notificationService.sendOrderConfirmation(order, user);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        List<Email> sent = emailSender.getSentEmails();
        assertThat(sent).hasSize(1);
        
        Email email = sent.get(0);
        assertThat(email.to().get(0).email()).isEqualTo("john@example.com");
        assertThat(email.subject()).contains("Order Confirmation");
        assertThat(email.htmlBody()).contains(order.id().value());
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use templates para emails complexos
TemplateEmail email = new TemplateEmail("welcome-v1", from, to, variables);

// ✅ Respeite preferências do usuário
if (preferences.emailEnabled()) {
    sendEmail();
}

// ✅ Track delivery status
trackingService.trackSent(userId, CHANNEL.EMAIL, messageId);

// ✅ Use async para não bloquear
CompletableFuture.supplyAsync(() -> emailSender.send(email));

// ✅ Adicione unsubscribe link
email.header("List-Unsubscribe", unsubscribeUrl);
```

### ❌ DON'T

```java
// ❌ NÃO envie sem verificar preferências
emailSender.send(email);  // ❌ User opted out!

// ❌ NÃO bloqueie thread principal
emailSender.send(email);  // ❌ Síncrono pode demorar!

// ❌ NÃO envie SMS sem phone validation
smsSender.send(new Sms(from, "12345", message));  // ❌ Invalid!

// ❌ NÃO ignore falhas de envio
emailSender.send(email);  // ❌ Sem verificar resultado!

// ❌ NÃO exponha emails em logs
log.info("Sent to: " + email.to());  // ❌ GDPR!
```

---

## Ver Também

- [SendGrid Adapter](../../../commons-adapters-email-sendgrid/) - Email implementation
- [Twilio Adapter](../../../commons-adapters-sms-twilio/) - SMS implementation
- [FCM Adapter](../../../commons-adapters-notification-fcm/) - Push notifications
- [I18n](../app-i18n.md) - Localized notifications
