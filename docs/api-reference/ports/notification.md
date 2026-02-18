# Port: Notification

## Visão Geral

`commons-ports-notification` define contratos para sistema de notificações unificado, abstraindo canais (email, SMS, push, in-app).

**Quando usar:**
- Notificações multicanal
- Preferências por usuário
- Notification center (in-app)
- Delivery tracking
- Notification templates

**Integrações:**
- `commons-ports-communication` - Email/SMS/Push
- `commons-ports-messaging` - Event-driven notifications
- `commons-app-i18n` - Localized messages

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-notification</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔔 NotificationService Interface

### Core Methods

```java
public interface NotificationService {
    
    /**
     * Envia notificação.
     */
    Result<NotificationId> send(Notification notification);
    
    /**
     * Envia para usuário (respeitando preferências).
     */
    Result<List<NotificationId>> sendToUser(
        UserId userId,
        NotificationRequest request
    );
    
    /**
     * Marca como lida.
     */
    Result<Void> markAsRead(UserId userId, NotificationId notificationId);
    
    /**
     * Busca notificações do usuário.
     */
    Result<List<Notification>> findByUser(UserId userId, int page, int size);
    
    /**
     * Conta não lidas.
     */
    Result<Long> countUnread(UserId userId);
}
```

### Notification Model

```java
public record Notification(
    NotificationId id,
    UserId userId,
    NotificationType type,
    String title,
    String message,
    Map<String, String> data,
    List<NotificationChannel> channels,
    Priority priority,
    boolean read,
    LocalDateTime createdAt,
    Optional<LocalDateTime> readAt
) {
    public static Builder builder() {
        return new Builder();
    }
}

public enum NotificationType {
    ORDER_CREATED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    ACCOUNT_VERIFIED,
    PASSWORD_RESET,
    PROMO_CAMPAIGN,
    SYSTEM_ALERT
}

public enum NotificationChannel {
    EMAIL,
    SMS,
    PUSH,
    IN_APP
}

public enum Priority {
    HIGH,      // Immediate delivery
    NORMAL,    // Standard delivery
    LOW        // Can be batched
}
```

---

## 💡 Basic Usage

### Order Notification

```java
@Service
public class OrderNotificationService {
    
    private final NotificationService notificationService;
    
    public Result<Void> notifyOrderCreated(Order order, User user) {
        NotificationRequest request = NotificationRequest.builder()
            .type(NotificationType.ORDER_CREATED)
            .title("Order Confirmed")
            .message("Your order #" + order.id().value() + " has been confirmed!")
            .data(Map.of(
                "orderId", order.id().value(),
                "orderTotal", String.valueOf(order.total().amount()),
                "deepLink", "app://orders/" + order.id().value()
            ))
            .priority(Priority.HIGH)
            .build();
        
        return notificationService.sendToUser(user.id(), request)
            .andThen(notificationIds -> {
                log.info("Order notification sent")
                    .field("orderId", order.id().value())
                    .field("userId", user.id().value())
                    .field("channels", notificationIds.size())
                    .log();
            })
            .mapToVoid();
    }
    
    public Result<Void> notifyOrderShipped(Order order, User user) {
        NotificationRequest request = NotificationRequest.builder()
            .type(NotificationType.ORDER_SHIPPED)
            .title("Order Shipped!")
            .message("Your order is on the way. Track: " + order.trackingNumber())
            .data(Map.of(
                "orderId", order.id().value(),
                "trackingNumber", order.trackingNumber(),
                "estimatedDelivery", order.estimatedDelivery().toString()
            ))
            .priority(Priority.NORMAL)
            .build();
        
        return notificationService.sendToUser(user.id(), request).mapToVoid();
    }
}
```

---

## ⚙️ User Preferences

### NotificationPreferences

```java
public record NotificationPreferences(
    UserId userId,
    Map<NotificationType, ChannelPreferences> preferences
) {
    public ChannelPreferences getPreferencesFor(NotificationType type) {
        return preferences.getOrDefault(type, ChannelPreferences.defaults());
    }
    
    public static NotificationPreferences defaults(UserId userId) {
        return new NotificationPreferences(
            userId,
            Map.of(
                NotificationType.ORDER_CREATED, new ChannelPreferences(true, false, true, true),
                NotificationType.ORDER_SHIPPED, new ChannelPreferences(true, true, true, true),
                NotificationType.PAYMENT_FAILED, new ChannelPreferences(true, true, true, true),
                NotificationType.PROMO_CAMPAIGN, new ChannelPreferences(true, false, false, true)
            )
        );
    }
}

public record ChannelPreferences(
    boolean emailEnabled,
    boolean smsEnabled,
    boolean pushEnabled,
    boolean inAppEnabled
) {
    public static ChannelPreferences defaults() {
        return new ChannelPreferences(true, false, true, true);
    }
    
    public List<NotificationChannel> enabledChannels() {
        List<NotificationChannel> channels = new ArrayList<>();
        if (emailEnabled) channels.add(NotificationChannel.EMAIL);
        if (smsEnabled) channels.add(NotificationChannel.SMS);
        if (pushEnabled) channels.add(NotificationChannel.PUSH);
        if (inAppEnabled) channels.add(NotificationChannel.IN_APP);
        return channels;
    }
}
```

### Notification Service Implementation

```java
@Service
public class UserNotificationService implements NotificationService {
    
    private final NotificationPreferencesRepository preferencesRepository;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final PushNotificationSender pushSender;
    private final NotificationRepository notificationRepository;
    
    @Override
    public Result<List<NotificationId>> sendToUser(
        UserId userId,
        NotificationRequest request
    ) {
        // Get user preferences
        NotificationPreferences preferences = preferencesRepository
            .findByUserId(userId)
            .orElse(NotificationPreferences.defaults(userId));
        
        ChannelPreferences channelPrefs = preferences.getPreferencesFor(request.type());
        
        List<NotificationChannel> enabledChannels = channelPrefs.enabledChannels();
        
        if (enabledChannels.isEmpty()) {
            log.warn("All channels disabled for user")
                .field("userId", userId.value())
                .field("type", request.type())
                .log();
            return Result.ok(List.of());
        }
        
        // Send to each enabled channel
        List<CompletableFuture<Result<NotificationId>>> futures = enabledChannels.stream()
            .map(channel -> CompletableFuture.supplyAsync(() -> 
                sendToChannel(userId, request, channel)
            ))
            .toList();
        
        // Wait for all
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Collect successful notifications
        List<NotificationId> notificationIds = futures.stream()
            .map(CompletableFuture::join)
            .filter(Result::isOk)
            .map(Result::get)
            .toList();
        
        return Result.ok(notificationIds);
    }
    
    private Result<NotificationId> sendToChannel(
        UserId userId,
        NotificationRequest request,
        NotificationChannel channel
    ) {
        Notification notification = Notification.builder()
            .id(NotificationId.generate())
            .userId(userId)
            .type(request.type())
            .title(request.title())
            .message(request.message())
            .data(request.data())
            .channels(List.of(channel))
            .priority(request.priority())
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();
        
        return switch (channel) {
            case EMAIL -> sendEmail(notification);
            case SMS -> sendSms(notification);
            case PUSH -> sendPush(notification);
            case IN_APP -> saveInApp(notification);
        };
    }
    
    private Result<NotificationId> saveInApp(Notification notification) {
        return notificationRepository.save(notification)
            .map(saved -> saved.id());
    }
}
```

---

## 📱 In-App Notifications

### Notification Center API

```java
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getNotifications(
        @AuthenticatedUser UserId userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Result<List<Notification>> result = notificationService.findByUser(
            userId,
            page,
            size
        );
        
        if (result.isError()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
        List<NotificationDto> dtos = result.get().stream()
            .map(NotificationDto::from)
            .toList();
        
        return ResponseEntity.ok(new Page<>(dtos, page, size));
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountDto> getUnreadCount(
        @AuthenticatedUser UserId userId
    ) {
        Result<Long> result = notificationService.countUnread(userId);
        
        if (result.isError()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
        return ResponseEntity.ok(new UnreadCountDto(result.get()));
    }
    
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
        @AuthenticatedUser UserId userId,
        @PathVariable String notificationId
    ) {
        Result<Void> result = notificationService.markAsRead(
            userId,
            NotificationId.from(notificationId)
        );
        
        if (result.isError()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(
        @AuthenticatedUser UserId userId
    ) {
        Result<Void> result = notificationService.markAllAsRead(userId);
        
        if (result.isError()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
        return ResponseEntity.noContent().build();
    }
}

public record NotificationDto(
    String id,
    String type,
    String title,
    String message,
    Map<String, String> data,
    boolean read,
    String createdAt
) {
    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
            notification.id().value(),
            notification.type().name(),
            notification.title(),
            notification.message(),
            notification.data(),
            notification.read(),
            notification.createdAt().toString()
        );
    }
}
```

---

## 🔄 Event-Based Notifications

### Domain Event Listener

```java
@Service
public class OrderEventListener {
    
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    
    @DomainEventHandler
    public void on(OrderCreatedEvent event) {
        User user = userRepository.findById(event.userId())
            .orElseThrow();
        
        NotificationRequest request = NotificationRequest.builder()
            .type(NotificationType.ORDER_CREATED)
            .title("Order Confirmed")
            .message("Your order has been confirmed!")
            .data(Map.of(
                "orderId", event.orderId().value(),
                "total", String.valueOf(event.total())
            ))
            .priority(Priority.HIGH)
            .build();
        
        notificationService.sendToUser(user.id(), request);
    }
    
    @DomainEventHandler
    public void on(OrderShippedEvent event) {
        User user = userRepository.findById(event.userId())
            .orElseThrow();
        
        NotificationRequest request = NotificationRequest.builder()
            .type(NotificationType.ORDER_SHIPPED)
            .title("Order Shipped!")
            .message("Your order is on the way!")
            .data(Map.of(
                "orderId", event.orderId().value(),
                "trackingNumber", event.trackingNumber()
            ))
            .priority(Priority.NORMAL)
            .build();
        
        notificationService.sendToUser(user.id(), request);
    }
}
```

---

## 📊 Notification Templates

### Template System

```java
@Service
public class NotificationTemplateService {
    
    private final TemplateEngine templateEngine;
    private final I18nService i18nService;
    
    public NotificationContent renderNotification(
        NotificationType type,
        Map<String, Object> variables,
        Locale locale
    ) {
        String titleKey = "notification." + type.name().toLowerCase() + ".title";
        String messageKey = "notification." + type.name().toLowerCase() + ".message";
        
        String title = i18nService.getMessage(titleKey, variables, locale);
        String message = i18nService.getMessage(messageKey, variables, locale);
        
        return new NotificationContent(title, message);
    }
}

// messages_en.properties
notification.order_created.title=Order Confirmed
notification.order_created.message=Your order #{orderId} has been confirmed!

notification.order_shipped.title=Order Shipped!
notification.order_shipped.message=Your order #{orderId} is on the way. Track: {trackingNumber}

// messages_pt.properties
notification.order_created.title=Pedido Confirmado
notification.order_created.message=Seu pedido #{orderId} foi confirmado!

notification.order_shipped.title=Pedido Enviado!
notification.order_shipped.message=Seu pedido #{orderId} está a caminho. Rastreio: {trackingNumber}
```

---

## 🧪 Testing

### Mock Notification Service

```java
public class MockNotificationService implements NotificationService {
    
    private final List<Notification> sentNotifications = new ArrayList<>();
    
    @Override
    public Result<List<NotificationId>> sendToUser(
        UserId userId,
        NotificationRequest request
    ) {
        NotificationId id = NotificationId.generate();
        
        Notification notification = Notification.builder()
            .id(id)
            .userId(userId)
            .type(request.type())
            .title(request.title())
            .message(request.message())
            .data(request.data())
            .priority(request.priority())
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();
        
        sentNotifications.add(notification);
        
        return Result.ok(List.of(id));
    }
    
    @Override
    public Result<Void> markAsRead(UserId userId, NotificationId notificationId) {
        sentNotifications.stream()
            .filter(n -> n.id().equals(notificationId))
            .findFirst()
            .ifPresent(n -> {
                int index = sentNotifications.indexOf(n);
                sentNotifications.set(index, n.markAsRead());
            });
        
        return Result.ok();
    }
    
    public List<Notification> getSentNotifications() {
        return Collections.unmodifiableList(sentNotifications);
    }
    
    public List<Notification> findByType(NotificationType type) {
        return sentNotifications.stream()
            .filter(n -> n.type() == type)
            .toList();
    }
}
```

### Test Example

```java
class OrderNotificationServiceTest {
    
    private MockNotificationService notificationService;
    private OrderNotificationService orderNotificationService;
    
    @BeforeEach
    void setUp() {
        notificationService = new MockNotificationService();
        orderNotificationService = new OrderNotificationService(notificationService);
    }
    
    @Test
    void shouldNotifyOrderCreated() {
        // Given
        Order order = Order.create(customer, items);
        User user = User.create("john@example.com", "John Doe");
        
        // When
        Result<Void> result = orderNotificationService.notifyOrderCreated(order, user);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        List<Notification> sent = notificationService.getSentNotifications();
        assertThat(sent).hasSize(1);
        
        Notification notification = sent.get(0);
        assertThat(notification.type()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(notification.title()).contains("Order Confirmed");
        assertThat(notification.data()).containsEntry("orderId", order.id().value());
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Respeite preferências do usuário
ChannelPreferences prefs = getPreferences(userId, type);

// ✅ Use priority adequadamente
.priority(Priority.HIGH)  // Para alertas críticos

// ✅ Adicione deep links
.data(Map.of("deepLink", "app://orders/" + orderId))

// ✅ Localize mensagens
i18nService.getMessage(key, variables, locale);

// ✅ Track delivery status
trackingService.track(notificationId, DeliveryStatus.SENT);
```

### ❌ DON'T

```java
// ❌ NÃO envie sem verificar preferências
notificationService.send(notification);  // ❌ User opted out!

// ❌ NÃO spam de notificações
for (order : orders) {
    notify(order);  // ❌ Envie batch digest!
}

// ❌ NÃO ignore prioridade
.priority(Priority.HIGH)  // ❌ Para promoção!

// ❌ NÃO exponha dados sensíveis
.message("Your password is: " + password);  // ❌ Security!

// ❌ NÃO bloqueie thread
notificationService.sendToUser(userId, request);  // ❌ Use async!
```

---

## Ver Também

- [Communication](./communication.md) - Email/SMS/Push channels
- [I18n](../app-i18n.md) - Localized messages
- [Messaging](./messaging.md) - Event-driven patterns
- [Domain Events](../domain-events.md) - Event listeners
