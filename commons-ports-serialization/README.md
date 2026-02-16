# Commons Ports Serialization

This module provides platform-agnostic abstractions for serialization operations, enabling support for various data formats like Protocol Buffers, Apache Avro, MessagePack, and others.

## Features

- **Format Agnostic**: Support for multiple serialization formats through a unified interface
- **Schema Management**: Built-in schema registry for version control and evolution
- **Type Safety**: Generic interfaces with compile-time type checking
- **Error Handling**: Uses Result<T> pattern for robust error management
- **Validation**: Schema validation and data integrity checking
- **HTTP Integration**: Content-type support for API serialization
- **Schema Evolution**: Support for backward/forward compatibility

## Core Components

### SerializationPort<T>

The main interface for serialization operations:

```java
public interface SerializationPort<T> {
    Result<byte[]> serialize(T object);
    Result<T> deserialize(byte[] data, Class<T> targetClass);
    Result<ValidationResult> validate(T object, Schema schema);
    // ... more methods
}
```

### Schema

Represents a serialization schema with version information:

```java
Schema schema = new Schema("UserEvent", "1.0", protoDefinition, SerializationFormat.PROTOBUF);
```

### SchemaRegistry

Manages schema lifecycle and evolution:

```java
public interface SchemaRegistry {
    Result<Void> register(Schema schema);
    Optional<Schema> getLatestSchema(String name);
    ValidationResult isCompatible(Schema source, Schema target);
    // ... more methods
}
```

### SerializationFormat

Enumeration of supported formats:

```java
public enum SerializationFormat {
    PROTOBUF("application/x-protobuf", "protobuf", true),
    PROTOBUF_JSON("application/json", "protobuf-json", false),
    AVRO("application/avro", "avro", true),
    JSON("application/json", "json", false);
    // ... more formats
}
```

### SerializationOptions

Configuration for serialization operations:

```java
SerializationOptions options = SerializationOptions.builder()
    .validateSchema(true)
    .useSchemaRegistry(true)
    .format(SerializationFormat.PROTOBUF)
    .build();
```

## Usage Examples

### Basic Serialization

```java
@Component
public class UserEventProcessor {
    
    private final SerializationPort<UserEvent> serializer;
    
    public void processEvent(UserEvent event) {
        Result<byte[]> result = serializer.serialize(event);
        
        if (result.isSuccess()) {
            byte[] data = result.getValue();
            // Send data to message queue, store in database, etc.
        } else {
            // Handle serialization error
            Problem problem = result.getProblem();
            log.error("Serialization failed: {}", problem.getDetail());
        }
    }
}
```

### Schema Validation

```java
@Service
public class EventValidator {
    
    private final SerializationPort<Event> serializer;
    private final SchemaRegistry registry;
    
    public boolean isValidEvent(Event event) {
        Schema schema = registry.getLatestSchema("Event").orElse(null);
        if (schema == null) {
            return false;
        }
        
        Result<ValidationResult> result = serializer.validate(event, schema);
        return result.isSuccess() && result.getValue().isValid();
    }
}
```

### HTTP API Integration

```java
@RestController
public class EventController {
    
    private final SerializationPort<Event> serializer;
    
    @PostMapping(value = "/events", consumes = "application/x-protobuf")
    public ResponseEntity<?> createEvent(@RequestBody byte[] data) {
        Result<Event> result = serializer.deserialize(data, Event.class);
        
        if (result.isSuccess()) {
            Event event = result.getValue();
            // Process event
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest()
                .body(result.getProblem().getDetail());
        }
    }
    
    @GetMapping(value = "/events/{id}", produces = "application/x-protobuf")
    public ResponseEntity<byte[]> getEvent(@PathVariable String id) {
        Event event = findEventById(id);
        Result<byte[]> result = serializer.serialize(event);
        
        return result.isSuccess() 
            ? ResponseEntity.ok(result.getValue())
            : ResponseEntity.internalServerError().build();
    }
}
```

### Schema Evolution

```java
@Service
public class SchemaEvolutionService {
    
    private final SchemaRegistry registry;
    
    public void upgradeSchema(String schemaName, String newDefinition) {
        Schema currentSchema = registry.getLatestSchema(schemaName)
            .orElseThrow(() -> new IllegalStateException("Schema not found"));
        
        String newVersion = incrementVersion(currentSchema.getVersion());
        Schema newSchema = new Schema(schemaName, newVersion, newDefinition, 
                                    currentSchema.getFormat());
        
        // Check compatibility
        ValidationResult compatibility = registry.isCompatible(currentSchema, newSchema);
        if (!compatibility.isValid()) {
            throw new IllegalArgumentException("Incompatible schema: " + 
                                             compatibility.getErrors());
        }
        
        // Register new version
        Result<Void> result = registry.register(newSchema);
        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to register schema: " + 
                                     result.getProblem().getDetail());
        }
    }
}
```

## Error Handling

All operations return `Result<T>` for type-safe error handling:

```java
Result<byte[]> serializationResult = serializer.serialize(event);

// Pattern matching style
switch (serializationResult) {
    case Success<byte[]> success -> {
        byte[] data = success.getValue();
        // Handle success
    }
    case Failure<byte[]> failure -> {
        Problem problem = failure.getProblem();
        // Handle specific error types
        if ("SCHEMA_VALIDATION_FAILED".equals(problem.getType())) {
            // Handle validation error
        }
    }
}
```

## Integration with Adapters

This ports module defines the contracts that concrete adapters must implement:

- **commons-adapters-serialization-protobuf**: Protocol Buffers implementation
- **commons-adapters-serialization-avro**: Apache Avro implementation  
- **commons-adapters-serialization-messagepack**: MessagePack implementation

## Dependencies

- **commons-kernel-result**: Result<T> pattern and error handling
- **commons-kernel-errors**: Problem API for standardized errors

## Thread Safety

All interfaces in this module are designed to be thread-safe when implemented correctly. Implementations should ensure:

- Immutable configuration objects
- Thread-safe schema registry operations
- Proper synchronization for mutable state

## Performance Considerations

- Schema validation can be expensive - consider caching validation results
- Binary formats (Protocol Buffers, Avro, MessagePack) are more efficient than text formats
- Schema registry lookups should be optimized with caching
- Consider using streaming APIs for large payloads
