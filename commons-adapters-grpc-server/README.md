# gRPC Server Adapter

Enterprise-grade gRPC server adapter with built-in support for interceptors, health checks, metrics, authentication, and graceful shutdown.

## Features

- 🚀 **Easy Configuration**: Builder pattern with sensible defaults and factory methods
- 🔍 **Observability**: Built-in logging and metrics interceptors
- 🔒 **Security**: Token-based authentication with customizable validation
- 💚 **Health Checks**: gRPC Health Checking Protocol implementation
- 🔄 **Graceful Shutdown**: Proper cleanup with configurable timeout
- 📊 **Metrics**: Per-method statistics (calls, success rate, duration)
- 🛡️ **Error Mapping**: Automatic conversion from exceptions/Problems to gRPC Status codes
- 🔧 **Reflection**: Optional server reflection for debugging

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-grpc-server</artifactId>
    <version>${commons.version}</version>
</dependency>
```

## Quick Start

### Development Setup

```java
// Create server with development-friendly defaults
GrpcServerConfiguration config = GrpcServerConfiguration.forDevelopment()
    .port(9090)
    .build();

GrpcServer server = new GrpcServer(config);

// Add your service implementation
server.addService(new MyServiceImpl());

// Start the server
server.start();

// Keep running
server.awaitTermination();
```

### Production Setup

```java
// Create server with production-optimized defaults
GrpcServerConfiguration config = GrpcServerConfiguration.forProduction()
    .port(9090)
    .build();

GrpcServer server = new GrpcServer(config);
server.addService(new MyServiceImpl());
server.start();
server.awaitTermination();
```

## Configuration

### Factory Methods

**Development Configuration** (lenient settings):
- 16MB max message size
- Server reflection enabled
- Health checks enabled
- Metrics disabled

**Production Configuration** (strict settings):
- 4MB max message size
- Server reflection disabled
- Health checks enabled
- Metrics enabled
- Shorter keep-alive time (1 minute)

### Custom Configuration

```java
GrpcServerConfiguration config = GrpcServerConfiguration.builder()
    .port(8080)
    .maxInboundMessageSize(8 * 1024 * 1024) // 8MB
    .maxInboundMetadataSize(16 * 1024)      // 16KB
    .keepAliveTime(Duration.ofMinutes(2))
    .keepAliveTimeout(Duration.ofSeconds(20))
    .maxConnectionIdle(Duration.ofMinutes(5))
    .maxConnectionAge(Duration.ofHours(1))
    .handshakeTimeout(Duration.ofSeconds(20))
    .enableReflection(true)    // Enable for debugging
    .enableHealthCheck(true)   // Enable health checks
    .enableMetrics(true)       // Enable metrics collection
    .addInterceptor(new CustomInterceptor())
    .build();
```

### Configuration Options

| Option | Default (Dev) | Default (Prod) | Description |
|--------|--------------|----------------|-------------|
| `port` | 9090 | 9090 | Server port (1-65535) |
| `maxInboundMessageSize` | 16MB | 4MB | Maximum message size |
| `maxInboundMetadataSize` | 8KB | 8KB | Maximum metadata size |
| `keepAliveTime` | 2 min | 1 min | Keep-alive ping interval |
| `keepAliveTimeout` | 20s | 20s | Keep-alive timeout |
| `maxConnectionIdle` | 5 min | 10 min | Max connection idle time |
| `maxConnectionAge` | 1 hour | 1 hour | Max connection age |
| `handshakeTimeout` | 20s | 20s | TLS handshake timeout |
| `enableReflection` | true | false | Server reflection |
| `enableHealthCheck` | true | true | Health check service |
| `enableMetrics` | false | true | Metrics collection |

## Interceptors

### Logging Interceptor

Automatically logs all gRPC method calls:

```java
// Automatically enabled for all servers
// Logs include:
// - Method name
// - Start timestamp
// - Duration (ms)
// - Status code
// - Error details (if failed)
```

Example logs:
```
INFO: Starting gRPC call: test.Service/GetUser
INFO: Completed gRPC call: test.Service/GetUser in 125ms with status: OK
WARNING: Completed gRPC call: test.Service/GetUser in 45ms with status: NOT_FOUND - User not found
```

### Metrics Interceptor

Collects per-method statistics (when `enableMetrics(true)`):

```java
GrpcServer server = new GrpcServer(config);
server.start();

// Access metrics
MetricsInterceptor metrics = server.getMetricsInterceptor();

// Get metrics for specific method
MethodMetrics methodMetrics = metrics.getMethodMetrics("test.Service/GetUser");
System.out.println("Total calls: " + methodMetrics.totalCalls());
System.out.println("Success rate: " + methodMetrics.successRate() + "%");
System.out.println("Average duration: " + methodMetrics.averageDurationMs() + "ms");
System.out.println("Failures by status: " + methodMetrics.failuresByStatus());

// Get all metrics
Map<String, MethodMetrics> allMetrics = metrics.getAllMetrics();

// Reset metrics
metrics.reset();
```

### Authentication Interceptor

Token-based authentication with custom validation:

```java
// Define token validation function
Function<String, String> tokenValidator = token -> {
    // Validate token and return principal (user identifier)
    // Return null if token is invalid
    if (isValidToken(token)) {
        return extractUserFromToken(token);
    }
    return null;
};

// Add auth interceptor
AuthInterceptor authInterceptor = new AuthInterceptor(tokenValidator, true);

GrpcServerConfiguration config = GrpcServerConfiguration.builder()
    .port(9090)
    .addInterceptor(authInterceptor)
    .build();
```

**Token Format**: Supports both `Bearer <token>` and raw token.

**Access Authenticated User** in your service:

```java
public class MyServiceImpl extends MyServiceGrpc.MyServiceImplBase {
    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        // Get authenticated principal
        String principal = AuthInterceptor.getPrincipal();
        
        if (AuthInterceptor.isAuthenticated()) {
            // User is authenticated
            System.out.println("Authenticated user: " + principal);
        }
        
        // ... handle request
    }
}
```

**Optional Authentication**:

```java
// Allow unauthenticated requests (required=false)
AuthInterceptor authInterceptor = new AuthInterceptor(tokenValidator, false);
```

### Custom Interceptors

Add your own interceptors:

```java
ServerInterceptor customInterceptor = new ServerInterceptor() {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        // Custom logic
        return next.startCall(call, headers);
    }
};

GrpcServerConfiguration config = GrpcServerConfiguration.builder()
    .port(9090)
    .addInterceptor(customInterceptor)
    .build();
```

**Interceptor Chain Order**:
1. LoggingInterceptor (always first)
2. MetricsInterceptor (if enabled)
3. Custom interceptors (in order added)

## Error Mapping

Automatic conversion from exceptions and Problems to gRPC Status codes:

### Exception Mapping

```java
// In your service implementation
public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
    try {
        User user = userRepository.findById(request.getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        responseObserver.onNext(toResponse(user));
        responseObserver.onCompleted();
    } catch (Exception e) {
        // Automatically mapped to appropriate gRPC Status
        Status status = ErrorMapper.mapToStatus(e);
        responseObserver.onError(status.asRuntimeException());
    }
}
```

**Exception → Status Mapping**:
- `IllegalArgumentException`, `NullPointerException` → `INVALID_ARGUMENT`
- `IllegalStateException` → `FAILED_PRECONDITION`
- `SecurityException` → `PERMISSION_DENIED`
- `UnsupportedOperationException` → `UNIMPLEMENTED`
- Other exceptions → `INTERNAL`

### Problem Mapping

```java
// With Problem from commons-kernel-errors
public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
    Result<User> result = userService.findUser(request.getId());
    
    if (result.isFailure()) {
        Status status = ErrorMapper.mapProblemToStatus(result.problem());
        responseObserver.onError(status.asRuntimeException());
        return;
    }
    
    responseObserver.onNext(toResponse(result.get()));
    responseObserver.onCompleted();
}
```

**Problem → Status Mapping** (keyword-based):
- `NOT_FOUND`, `MISSING` → `NOT_FOUND`
- `UNAUTHORIZED`, `AUTH` → `UNAUTHENTICATED`
- `FORBIDDEN`, `PERMISSION` → `PERMISSION_DENIED`
- `INVALID`, `VALIDATION` → `INVALID_ARGUMENT`
- `CONFLICT`, `DUPLICATE` → `ALREADY_EXISTS`
- `TIMEOUT`, `DEADLINE` → `DEADLINE_EXCEEDED`
- `UNAVAILABLE`, `CONNECTION` → `UNAVAILABLE`
- And more (see ErrorMapper javadoc)

### Helper Methods

```java
// Common error helpers
throw ErrorMapper.notFound("User not found");
throw ErrorMapper.invalidArgument("Invalid email format");
throw ErrorMapper.permissionDenied("Insufficient permissions");
throw ErrorMapper.alreadyExists("User already exists");

// Wrap with context
try {
    processRequest();
} catch (Exception e) {
    throw ErrorMapper.wrapWithContext(e, "Failed to process user request");
}
```

## Health Checks

Implements the [gRPC Health Checking Protocol](https://github.com/grpc/grpc/blob/master/doc/health-checking.md):

```java
GrpcServer server = new GrpcServer(config);
server.addService(new MyServiceImpl());
server.start();

// Update service health dynamically
server.setServiceHealth("test.Service", true);  // SERVING
server.setServiceHealth("test.Service", false); // NOT_SERVING
```

**Client Health Check**:

```bash
# Using grpc_health_probe
grpc_health_probe -addr=localhost:9090

# Using grpcurl with reflection
grpcurl -plaintext localhost:9090 grpc.health.v1.Health/Check
```

## Server Reflection

Enable server reflection for debugging and tooling:

```java
GrpcServerConfiguration config = GrpcServerConfiguration.builder()
    .port(9090)
    .enableReflection(true)  // Enable reflection
    .build();
```

**Use with grpcurl**:

```bash
# List services
grpcurl -plaintext localhost:9090 list

# List methods of a service
grpcurl -plaintext localhost:9090 list test.Service

# Describe a method
grpcurl -plaintext localhost:9090 describe test.Service.GetUser

# Call a method
grpcurl -plaintext -d '{"id": "123"}' localhost:9090 test.Service/GetUser
```

⚠️ **Security Note**: Disable reflection in production (`enableReflection(false)`) as it exposes service definitions.

## Lifecycle Management

### Graceful Shutdown

```java
GrpcServer server = new GrpcServer(config);
server.addService(new MyServiceImpl());
server.start();

// Add shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    System.out.println("Shutting down gRPC server...");
    server.shutdown();
    System.out.println("Server shut down.");
}));

server.awaitTermination();
```

**Shutdown Process**:
1. Health status set to `NOT_SERVING`
2. Server stops accepting new requests
3. Waits 30 seconds for in-flight requests to complete
4. Forces shutdown if needed (after 30s)
5. Waits additional 5 seconds for forced shutdown

### Server State

```java
if (server.isRunning()) {
    int port = server.getPort();
    System.out.println("Server running on port: " + port);
}
```

## Complete Example

```java
import com.marcusprado02.commons.adapters.grpc.server.*;
import io.grpc.stub.StreamObserver;

public class Application {
    public static void main(String[] args) throws Exception {
        // Configure server
        GrpcServerConfiguration config = GrpcServerConfiguration.forProduction()
            .port(9090)
            .enableMetrics(true)
            .build();

        // Create server
        GrpcServer server = new GrpcServer(config);

        // Add services
        server.addService(new UserServiceImpl());
        server.addService(new ProductServiceImpl());

        // Start server
        server.start();
        System.out.println("Server started on port: " + server.getPort());

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            server.shutdown();
        }));

        // Wait for termination
        server.awaitTermination();
    }

    static class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
        @Override
        public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
            try {
                // Business logic
                User user = findUser(request.getId());
                
                // Success response
                responseObserver.onNext(UserResponse.newBuilder()
                    .setUser(user)
                    .build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                // Error handling with ErrorMapper
                responseObserver.onError(
                    ErrorMapper.toStatusRuntimeException(e)
                );
            }
        }
    }
}
```

## Best Practices

### Development
- Use `forDevelopment()` for local testing
- Enable reflection for easier debugging
- Use larger message sizes for development flexibility

### Production
- Use `forProduction()` for production deployments
- Disable reflection for security
- Enable metrics for observability
- Configure appropriate message size limits
- Set up health checks for load balancer integration
- Implement proper shutdown hooks

### Security
- Always validate authentication tokens
- Use TLS in production (configure via `ServerBuilder`)
- Disable reflection in production
- Implement proper authorization in services
- Validate all input data

### Performance
- Adjust message size limits based on your needs
- Configure keep-alive settings for long-lived connections
- Monitor metrics to identify bottlenecks
- Use connection age/idle limits to prevent resource leaks

## Integration with Commons Library

This adapter integrates seamlessly with other commons modules:

- **commons-kernel-errors**: Automatic Problem → Status mapping
- **commons-kernel-result**: Easy Result to gRPC response conversion
- **commons-adapters-otel**: OpenTelemetry integration (future)

## License

This is part of the commons library. See the project LICENSE file for details.
