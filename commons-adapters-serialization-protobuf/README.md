# Commons Adapters Serialization Protocol Buffers

This module provides a Protocol Buffers implementation of the serialization port interface, enabling efficient binary serialization with schema evolution support.

## Features

- **Protocol Buffers Integration**: Full support for Google Protocol Buffers
- **Multiple Formats**: Binary, JSON, and text serialization formats
- **Schema Evolution**: Backward and forward compatibility support
- **Type Safety**: Generic interfaces with compile-time type checking
- **Schema Registry**: In-memory registry for schema management
- **Validation**: Built-in schema validation capabilities
- **Error Handling**: Robust error handling with Result<T> pattern

## Dependencies

This adapter requires:
- Google Protocol Buffers Java library
- Protocol Buffers compiler (protoc) for code generation
- commons-ports-serialization for interface definitions

## Usage

### Basic Usage

```java
// Define your .proto file and generate Java classes
// Example: Person.proto -> PersonProtos.Person

import com.marcusprado02.commons.adapters.serialization.protobuf.ProtobufSerialization;
import com.marcusprado02.commons.ports.serialization.SerializationPort;

// Create a serialization adapter
SerializationPort<PersonProtos.Person> serializer = 
    ProtobufSerialization.forMessage(PersonProtos.Person.class);

// Create a person object
PersonProtos.Person person = PersonProtos.Person.newBuilder()
    .setId("123")
    .setName("John Doe")
    .setEmail("john@example.com")
    .setAge(30)
    .build();

// Serialize to bytes
Result<byte[]> result = serializer.serialize(person);
if (result.isSuccess()) {
    byte[] data = result.getValue();
    // Use serialized data
}

// Deserialize from bytes
Result<PersonProtos.Person> deserializeResult = 
    serializer.deserialize(data, PersonProtos.Person.class);
if (deserializeResult.isSuccess()) {
    PersonProtos.Person deserializedPerson = deserializeResult.getValue();
    // Use deserialized object
}
```

### With Schema Registry

```java
import com.marcusprado02.commons.adapters.serialization.protobuf.ProtobufSerialization;
import com.marcusprado02.commons.ports.serialization.*;

// Create schema registry
SchemaRegistry registry = ProtobufSerialization.createInMemoryRegistry();

// Register schema
String protoDefinition = """
    syntax = "proto3";
    message Person {
        string id = 1;
        string name = 2;
        string email = 3;
        int32 age = 4;
    }
    """;

Schema schema = new Schema("Person", "1.0", protoDefinition, SerializationFormat.PROTOBUF);
registry.register(schema);

// Create adapter with registry
SerializationPort<PersonProtos.Person> serializer = 
    ProtobufSerialization.forMessage(PersonProtos.Person.class, registry);
```

### Using Builder Pattern

```java
SerializationPort<PersonProtos.Person> serializer = 
    ProtobufSerialization.builder(PersonProtos.Person.class)
        .withInMemoryRegistry()
        .validateSchema(true)
        .useSchemaRegistry(true)
        .build();
```

### Different Serialization Formats

```java
// Binary format (default)
SerializationOptions binaryOptions = SerializationOptions.builder()
    .format(SerializationFormat.PROTOBUF)
    .build();

// JSON format
SerializationOptions jsonOptions = SerializationOptions.builder()  
    .format(SerializationFormat.PROTOBUF_JSON)
    .build();

// Text format
SerializationOptions textOptions = SerializationOptions.builder()
    .format(SerializationFormat.PROTOBUF_TEXT)
    .build();

// Serialize with specific format
Result<byte[]> jsonResult = serializer.serialize(person, jsonOptions);
```

### Stream-based Serialization

```java
// Serialize to stream
try (FileOutputStream fos = new FileOutputStream("person.pb")) {
    Result<Void> result = serializer.serialize(person, fos);
    if (!result.isSuccess()) {
        // Handle error
    }
}

// Deserialize from stream  
try (FileInputStream fis = new FileInputStream("person.pb")) {
    Result<PersonProtos.Person> result = 
        serializer.deserialize(fis, PersonProtos.Person.class);
    if (result.isSuccess()) {
        PersonProtos.Person person = result.getValue();
    }
}
```

### Schema Validation

```java
// Validate object against schema
Schema schema = registry.getLatestSchema("Person").orElse(null);
if (schema != null) {
    Result<ValidationResult> validation = serializer.validate(person, schema);
    if (validation.isSuccess()) {
        ValidationResult result = validation.getValue();
        if (!result.isValid()) {
            System.out.println("Validation errors: " + result.getErrors());
        }
    }
}
```

### HTTP API Integration

```java
@RestController
public class PersonController {
    
    private final SerializationPort<PersonProtos.Person> serializer;
    
    @PostMapping(value = "/persons", 
                 consumes = "application/x-protobuf",
                 produces = "application/x-protobuf")
    public ResponseEntity<byte[]> createPerson(@RequestBody byte[] data) {
        // Deserialize incoming data
        Result<PersonProtos.Person> deserializeResult = 
            serializer.deserialize(data, PersonProtos.Person.class);
            
        if (!deserializeResult.isSuccess()) {
            return ResponseEntity.badRequest().build();
        }
        
        PersonProtos.Person person = deserializeResult.getValue();
        
        // Process person (save to database, etc.)
        PersonProtos.Person savedPerson = processPerson(person);
        
        // Serialize response
        Result<byte[]> serializeResult = serializer.serialize(savedPerson);
        
        return serializeResult.isSuccess() 
            ? ResponseEntity.ok(serializeResult.getValue())
            : ResponseEntity.internalServerError().build();
    }
}
```

## Protocol Buffers Schema Definition

Create `.proto` files in `src/main/proto/`:

```protobuf
syntax = "proto3";

package com.example.model;

option java_package = "com.example.model.proto";
option java_outer_classname = "PersonProtos";

message Person {
  string id = 1;
  string name = 2;
  string email = 3;
  int32 age = 4;
  repeated string phone_numbers = 5;
  Address address = 6;
  
  enum Status {
    UNKNOWN = 0;
    ACTIVE = 1;
    INACTIVE = 2;
  }
  
  Status status = 7;
}

message Address {
  string street = 1;
  string city = 2;
  string state = 3;
  string zip_code = 4;
  string country = 5;
}
```

## Maven Configuration

The module includes Protocol Buffers Maven plugin for code generation:

```xml
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.6.1</version>
    <configuration>
        <protocArtifact>com.google.protobuf:protoc:3.25.1:exe:${os.detected.classifier}</protocArtifact>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
                <goal>test-compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Schema Evolution Example

```java
// Version 1.0 schema
String v1Schema = """
    syntax = "proto3";
    message Person {
        string id = 1;
        string name = 2;
    }
    """;

// Version 2.0 schema (backward compatible)
String v2Schema = """
    syntax = "proto3";
    message Person {
        string id = 1;
        string name = 2;
        string email = 3;  // Added field
        int32 age = 4;     // Added field
    }
    """;

// Register both versions
Schema schema1 = new Schema("Person", "1.0", v1Schema, SerializationFormat.PROTOBUF);
Schema schema2 = new Schema("Person", "2.0", v2Schema, SerializationFormat.PROTOBUF);

registry.register(schema1);
registry.register(schema2);

// Check compatibility
ValidationResult compatibility = registry.isCompatible(schema1, schema2);
if (compatibility.isValid()) {
    System.out.println("Schemas are compatible");
}
```

## Error Handling

All operations return `Result<T>` for safe error handling:

```java
Result<byte[]> result = serializer.serialize(person);

switch (result) {
    case Success<byte[]> success -> {
        byte[] data = success.getValue();
        // Handle successful serialization
    }
    case Failure<byte[]> failure -> {
        Problem problem = failure.getProblem();
        switch (problem.getType()) {
            case "SCHEMA_VALIDATION_FAILED" -> {
                // Handle validation error
            }
            case "SERIALIZATION_ERROR" -> {
                // Handle serialization error
            }
            default -> {
                // Handle other errors
            }
        }
    }
}
```

## Performance Considerations

- **Binary Format**: Use `SerializationFormat.PROTOBUF` for best performance
- **Schema Validation**: Disable for high-throughput scenarios if not needed
- **Registry Caching**: Schema lookups are cached in memory
- **Code Generation**: Pre-compile `.proto` files for faster startup
- **Stream Operations**: Use streaming APIs for large payloads

## Thread Safety

- **ProtobufSerializationAdapter**: Thread-safe for read operations
- **InMemoryProtobufSchemaRegistry**: Thread-safe with concurrent access
- **Generated Classes**: Protocol Buffers generated classes are immutable and thread-safe

## Limitations

- Only supports Protocol Buffers message types that extend `com.google.protobuf.Message`
- Schema compatibility checking is simplified and may not catch all incompatibilities
- In-memory registry is not persistent - use external registry for production
