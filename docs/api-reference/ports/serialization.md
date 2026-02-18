# Port: Serialization

## Visão Geral

`commons-ports-serialization` define contratos para serialização/desserialização de dados (JSON, XML, Protobuf, Avro), abstraindo bibliotecas como Jackson e Protocol Buffers.

**Quando usar:**
- API requests/responses
- Message queues
- Event serialization
- File formats
- Inter-service communication

**Implementações disponíveis:**
- `commons-adapters-serialization-jackson` - JSON/XML com Jackson
- `commons-adapters-serialization-protobuf` - Protocol Buffers

---

## 📦 Instalação

```xml
<!-- Port (interface) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-serialization</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapter (JSON) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-serialization-jackson</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔄 Serializer Interface

### Core Methods

```java
public interface Serializer {
    
    /**
     * Serializa objeto para bytes.
     */
    Result<byte[]> serialize(Object object);
    
    /**
     * Serializa para string.
     */
    Result<String> serializeToString(Object object);
    
    /**
     * Desserializa de bytes.
     */
    <T> Result<T> deserialize(byte[] data, Class<T> type);
    
    /**
     * Desserializa de string.
     */
    <T> Result<T> deserializeFromString(String data, Class<T> type);
    
    /**
     * Desserializa com TypeReference (para generics).
     */
    <T> Result<T> deserialize(byte[] data, TypeReference<T> typeRef);
}
```

### Type Reference

```java
public abstract class TypeReference<T> {
    private final Type type;
    
    protected TypeReference() {
        Type superclass = getClass().getGenericSuperclass();
        this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }
    
    public Type getType() {
        return type;
    }
}
```

---

## 📝 JSON Serialization

### Basic Usage

```java
@Service
public class UserService {
    
    private final Serializer jsonSerializer;
    private final CacheProvider cache;
    
    public Result<Void> cacheUser(User user) {
        return jsonSerializer.serializeToString(user)
            .andThen(json -> {
                cache.set(
                    "user:" + user.id().value(),
                    json,
                    Duration.ofHours(1)
                );
                return Result.ok();
            });
    }
    
    public Result<User> getUserFromCache(UserId userId) {
        return cache.get("user:" + userId.value(), String.class)
            .map(json -> jsonSerializer.deserializeFromString(json, User.class))
            .orElse(Result.error(Error.of("USER_NOT_FOUND", "User not in cache")));
    }
}
```

### Generic Types

```java
@Service
public class OrderService {
    
    private final Serializer jsonSerializer;
    
    public Result<List<Order>> deserializeOrders(String json) {
        // Use TypeReference for generics
        return jsonSerializer.deserializeFromString(
            json,
            new TypeReference<List<Order>>() {}
        );
    }
    
    public Result<Map<String, Order>> deserializeOrderMap(String json) {
        return jsonSerializer.deserializeFromString(
            json,
            new TypeReference<Map<String, Order>>() {}
        );
    }
}
```

---

## 🎯 Domain Event Serialization

### Event Serialization

```java
@Service
public class DomainEventSerializer {
    
    private final Serializer jsonSerializer;
    
    public Result<String> serializeEvent(DomainEvent event) {
        EventEnvelope envelope = new EventEnvelope(
            event.eventId().value(),
            event.getClass().getName(),
            event.occurredAt(),
            event
        );
        
        return jsonSerializer.serializeToString(envelope);
    }
    
    public Result<DomainEvent> deserializeEvent(String json) {
        Result<EventEnvelope> envelopeResult = jsonSerializer
            .deserializeFromString(json, EventEnvelope.class);
        
        if (envelopeResult.isError()) {
            return envelopeResult.mapError();
        }
        
        EventEnvelope envelope = envelopeResult.get();
        
        try {
            Class<?> eventClass = Class.forName(envelope.eventType());
            
            return jsonSerializer.deserializeFromString(
                jsonSerializer.serializeToString(envelope.payload()).get(),
                (Class<DomainEvent>) eventClass
            );
            
        } catch (ClassNotFoundException e) {
            return Result.error(Error.of(
                "EVENT_TYPE_NOT_FOUND",
                "Unknown event type: " + envelope.eventType()
            ));
        }
    }
}

public record EventEnvelope(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    Object payload
) {}
```

---

## 📬 Message Queue Serialization

### Queue Message Serializer

```java
@Service
public class QueueMessageSerializer {
    
    private final Serializer jsonSerializer;
    
    public <T> Result<QueueMessage> serialize(T payload, Class<T> type) {
        return jsonSerializer.serializeToString(payload)
            .map(json -> QueueMessage.builder()
                .body(json)
                .attribute("payloadType", type.getName())
                .attribute("version", "1.0")
                .build()
            );
    }
    
    public <T> Result<T> deserialize(QueueMessage message, Class<T> type) {
        // Verify type
        String payloadType = message.attributes().get("payloadType");
        if (!type.getName().equals(payloadType)) {
            return Result.error(Error.of(
                "TYPE_MISMATCH",
                "Expected " + type.getName() + " but got " + payloadType
            ));
        }
        
        return jsonSerializer.deserializeFromString(message.body(), type);
    }
}

// Usage in consumer
@Service
public class OrderEventConsumer {
    
    private final QueueConsumer queueConsumer;
    private final QueueMessageSerializer messageSerializer;
    
    @Scheduled(fixedDelay = 1000)
    public void processEvents() {
        Result<List<ReceivedMessage>> result = queueConsumer.receive(
            "order-events",
            ReceiveOptions.defaults()
        );
        
        result.ifOk(messages -> 
            messages.forEach(this::processMessage)
        );
    }
    
    private void processMessage(ReceivedMessage message) {
        QueueMessage queueMessage = QueueMessage.of(message.body());
        
        Result<OrderCreatedEvent> eventResult = messageSerializer.deserialize(
            queueMessage,
            OrderCreatedEvent.class
        );
        
        if (eventResult.isError()) {
            log.error("Failed to deserialize event")
                .error(eventResult.getError())
                .log();
            return;
        }
        
        handleEvent(eventResult.get());
    }
}
```

---

## 🔐 Custom Serializers

### Custom Date Format

```java
public class CustomJsonSerializer implements Serializer {
    
    private final ObjectMapper objectMapper;
    
    public CustomJsonSerializer() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"))
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    
    @Override
    public Result<byte[]> serialize(Object object) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(object);
            return Result.ok(bytes);
        } catch (JsonProcessingException e) {
            return Result.error(Error.of(
                "SERIALIZATION_ERROR",
                "Failed to serialize: " + e.getMessage()
            ));
        }
    }
}
```

### Polymorphic Types

```java
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "ORDER_CREATED"),
    @JsonSubTypes.Type(value = OrderShippedEvent.class, name = "ORDER_SHIPPED"),
    @JsonSubTypes.Type(value = OrderDeliveredEvent.class, name = "ORDER_DELIVERED")
})
public interface OrderEvent extends DomainEvent {}

// Serialization handles polymorphism automatically
@Service
public class OrderEventService {
    
    private final Serializer jsonSerializer;
    
    public Result<String> serializeEvent(OrderEvent event) {
        return jsonSerializer.serializeToString(event);
    }
    
    public Result<OrderEvent> deserializeEvent(String json) {
        return jsonSerializer.deserializeFromString(json, OrderEvent.class);
    }
}

// JSON output includes type discriminator
{
    "type": "ORDER_CREATED",
    "eventId": "evt-123",
    "orderId": "ord-456",
    "occurredAt": "2024-01-15T10:30:00Z"
}
```

---

## 🗜️ Protocol Buffers

### Protobuf Serializer

```java
public interface ProtobufSerializer {
    
    /**
     * Serializa Protobuf message.
     */
    <T extends Message> Result<byte[]> serialize(T message);
    
    /**
     * Desserializa Protobuf message.
     */
    <T extends Message> Result<T> deserialize(
        byte[] data,
        Parser<T> parser
    );
}
```

### Usage Example

```protobuf
// order.proto
syntax = "proto3";

package com.mycompany.order;

message Order {
    string id = 1;
    string customer_id = 2;
    repeated OrderItem items = 3;
    double total = 4;
}

message OrderItem {
    string product_id = 1;
    string product_name = 2;
    int32 quantity = 3;
    double price = 4;
}
```

```java
@Service
public class OrderProtobufService {
    
    private final ProtobufSerializer protobufSerializer;
    
    public Result<byte[]> serializeOrder(Order order) {
        // Convert domain Order to Protobuf OrderProto
        OrderProto proto = OrderProto.newBuilder()
            .setId(order.id().value())
            .setCustomerId(order.customerId().value())
            .addAllItems(order.items().stream()
                .map(this::toProtoItem)
                .toList()
            )
            .setTotal(order.total().amount())
            .build();
        
        return protobufSerializer.serialize(proto);
    }
    
    public Result<Order> deserializeOrder(byte[] data) {
        return protobufSerializer.deserialize(data, OrderProto.parser())
            .map(this::toDomainOrder);
    }
}
```

---

## 🧪 Testing

### Mock Serializer

```java
public class MockSerializer implements Serializer {
    
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    @Override
    public Result<byte[]> serialize(Object object) {
        try {
            return Result.ok(objectMapper.writeValueAsBytes(object));
        } catch (Exception e) {
            return Result.error(Error.of("MOCK_ERROR", e.getMessage()));
        }
    }
    
    @Override
    public Result<String> serializeToString(Object object) {
        try {
            return Result.ok(objectMapper.writeValueAsString(object));
        } catch (Exception e) {
            return Result.error(Error.of("MOCK_ERROR", e.getMessage()));
        }
    }
    
    @Override
    public <T> Result<T> deserialize(byte[] data, Class<T> type) {
        try {
            return Result.ok(objectMapper.readValue(data, type));
        } catch (Exception e) {
            return Result.error(Error.of("MOCK_ERROR", e.getMessage()));
        }
    }
    
    @Override
    public <T> Result<T> deserializeFromString(String data, Class<T> type) {
        try {
            return Result.ok(objectMapper.readValue(data, type));
        } catch (Exception e) {
            return Result.error(Error.of("MOCK_ERROR", e.getMessage()));
        }
    }
}
```

### Test Example

```java
class UserServiceTest {
    
    private MockSerializer serializer;
    private MockCacheProvider cache;
    private UserService userService;
    
    @BeforeEach
    void setUp() {
        serializer = new MockSerializer();
        cache = new MockCacheProvider();
        userService = new UserService(serializer, cache);
    }
    
    @Test
    void shouldCacheUser() {
        // Given
        User user = User.create("john@example.com", "John Doe");
        
        // When
        Result<Void> result = userService.cacheUser(user);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        // Verify cached
        Optional<String> cached = cache.get(
            "user:" + user.id().value(),
            String.class
        );
        assertThat(cached).isPresent();
        assertThat(cached.get()).contains("john@example.com");
    }
    
    @Test
    void shouldDeserializeUser() {
        // Given
        User user = User.create("john@example.com", "John Doe");
        userService.cacheUser(user);
        
        // When
        Result<User> result = userService.getUserFromCache(user.id());
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        User cached = result.get();
        assertThat(cached.email()).isEqualTo("john@example.com");
        assertThat(cached.name()).isEqualTo("John Doe");
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use TypeReference para generics
new TypeReference<List<Order>>() {}

// ✅ Handle serialization errors
Result<String> result = serializer.serializeToString(object);
if (result.isError()) {
    log.error("Serialization failed").error(result.getError()).log();
}

// ✅ Include version em messages
.attribute("version", "1.0")

// ✅ Use immutable records
public record User(String id, String name) {}

// ✅ Configure ObjectMapper adequadamente
objectMapper
    .registerModule(new JavaTimeModule())
    .disable(WRITE_DATES_AS_TIMESTAMPS);
```

### ❌ DON'T

```java
// ❌ NÃO ignore erros de serialização
String json = serializer.serializeToString(obj);  // ❌ Result not checked!

// ❌ NÃO serialize dados sensíveis
return serializer.serialize(user);  // ❌ Contains password!

// ❌ NÃO use tipos raw
Result<List> orders = serializer.deserialize(json, List.class);  // ❌ Type erasure!

// ❌ NÃO hardcode format
objectMapper.setDateFormat(...);  // ❌ Use ISO-8601

// ❌ NÃO serialize exceptions
return serializer.serialize(exception);  // ❌ Leak stack traces!
```

---

## Ver Também

- [Jackson Adapter](../../../commons-adapters-serialization-jackson/) - JSON implementation
- [Protobuf Adapter](../../../commons-adapters-serialization-protobuf/) - Protobuf implementation
- [Messaging](./messaging.md) - Event serialization
- [Queue](./queue.md) - Message serialization
