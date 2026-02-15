# gRPC Client Adapter

Production-ready gRPC client adapter with retry, circuit breaker, load balancing, and observability.

## Features

- **TLS/SSL Support**: Secure communication with transport security
- **Automatic Retry**: Exponential backoff with configurable max retries
- **Circuit Breaker**: Resilience4j integration for fault tolerance
- **Load Balancing**: Round-robin load balancing
- **Observability**: Built-in logging and metrics interceptors
- **Connection Management**: Configurable timeouts and message sizes
- **Type Safety**: Strong typing with builder pattern
- **Factory Methods**: Development and production profiles

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.marcusprado02</groupId>
    <artifactId>commons-adapters-grpc-client</artifactId>
    <version>${commons.version}</version>
</dependency>
```

## Quick Start

### Development Setup

```java
import com.marcusprado02.commons.adapters.grpc.client.GrpcClientConfiguration;
import com.marcusprado02.commons.adapters.grpc.client.GrpcClientFactory;

// Create development configuration
GrpcClientConfiguration config = GrpcClientConfiguration.forDevelopment("localhost", 9090);

// Create channel
ManagedChannel channel = GrpcClientFactory.createChannel(config);

// Create stub
GreeterGrpc.GreeterBlockingStub stub = GrpcClientFactory.createStub(
    channel,
    GreeterGrpc::newBlockingStub,
    config
);

// Use stub
HelloRequest request = HelloRequest.newBuilder()
    .setName("World")
    .build();

HelloReply response = stub.sayHello(request);
System.out.println("Response: " + response.getMessage());

// Shutdown
channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
```

### Production Setup

```java
// Create production configuration
GrpcClientConfiguration config = GrpcClientConfiguration.forProduction("grpc.example.com", 443);

// Create channel
ManagedChannel channel = GrpcClientFactory.createChannel(config);

// Create stub
GreeterGrpc.GreeterBlockingStub stub = GrpcClientFactory.createStub(
    channel,
    GreeterGrpc::newBlockingStub,
    config
);

// Use stub...

// Shutdown
GrpcClientFactory.shutdownChannel(channel, 10, TimeUnit.SECONDS);
```

## Configuration

### Development Profile

```java
GrpcClientConfiguration config = GrpcClientConfiguration.forDevelopment("localhost", 9090);
```

Default settings:
- TLS: Disabled (plaintext)
- Call timeout: 30 seconds
- Idle timeout: 5 minutes
- Max inbound message size: 16 MB
- Retry: Disabled
- Circuit breaker: Disabled

### Production Profile

```java
GrpcClientConfiguration config = GrpcClientConfiguration.forProduction("grpc.example.com", 443);
```

Default settings:
- TLS: Enabled
- Call timeout: 30 seconds
- Idle timeout: 10 minutes
- Max inbound message size: 4 MB
- Retry: Enabled (3 attempts, exponential backoff 100ms-5s)
- Circuit breaker: Enabled (5 failures, 60s wait)

### Custom Configuration

```java
GrpcClientConfiguration config = GrpcClientConfiguration.builder("api.example.com", 8443)
    .callTimeout(Duration.ofSeconds(60))
    .idleTimeout(Duration.ofMinutes(15))
    .maxInboundMessageSize(8 * 1024 * 1024) // 8 MB
    .maxInboundMetadataSize(16 * 1024) // 16 KB
    .enableTls(true)
    .enableRetry(true)
    .maxRetries(5)
    .retryDelay(Duration.ofMillis(200))
    .maxRetryDelay(Duration.ofSeconds(10))
    .enableCircuitBreaker(true)
    .circuitBreakerFailureThreshold(10)
    .circuitBreakerWaitDuration(Duration.ofSeconds(120))
    .userAgent("my-app/1.0.0")
    .build();
```

## Retry Configuration

Enable retry with exponential backoff:

```java
GrpcClientConfiguration config = GrpcClientConfiguration.builder("api.example.com", 443)
    .enableRetry(true)
    .maxRetries(3) // Maximum 3 retry attempts
    .retryDelay(Duration.ofMillis(100)) // Initial delay: 100ms
    .maxRetryDelay(Duration.ofSeconds(5)) // Maximum delay: 5s
    .build();
```

Retry behavior:
- Attempt 1: Immediate
- Attempt 2: Wait 100ms
- Attempt 3: Wait 200ms
- Attempt 4: Wait 400ms
- etc. (exponential backoff up to maxRetryDelay)

## Circuit Breaker Configuration

Enable circuit breaker for fault tolerance:

```java
GrpcClientConfiguration config = GrpcClientConfiguration.builder("api.example.com", 443)
    .enableCircuitBreaker(true)
    .circuitBreakerFailureThreshold(5) // Open after 5 consecutive failures
    .circuitBreakerWaitDuration(Duration.ofSeconds(60)) // Wait 60s before half-open
    .build();
```

Circuit breaker states:
1. **Closed**: Normal operation, calls pass through
2. **Open**: After threshold failures, all calls fail immediately
3. **Half-Open**: After wait duration, allows test calls

## Custom Interceptors

Add custom interceptors for cross-cutting concerns:

```java
ClientInterceptor authInterceptor = new ClientInterceptor() {
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {
            
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Add authorization header
                Metadata.Key<String> authKey = Metadata.Key.of(
                    "Authorization", 
                    Metadata.ASCII_STRING_MARSHALLER
                );
                headers.put(authKey, "Bearer " + getAccessToken());
                
                super.start(responseListener, headers);
            }
        };
    }
};

GrpcClientConfiguration config = GrpcClientConfiguration.builder("api.example.com", 443)
    .interceptors(List.of(authInterceptor))
    .build();
```

## Built-in Interceptors

### Logging Interceptor

Automatically logs all gRPC client calls:

```
INFO: gRPC client call started: myservice.v1.MyService/GetUser
INFO: gRPC client call completed: myservice.v1.MyService/GetUser - duration: 123ms
WARNING: gRPC client call failed: myservice.v1.MyService/GetUser - status: INTERNAL - duration: 456ms - description: Database connection failed
```

### Metrics Interceptor

Tracks comprehensive metrics for all method calls:

```java
ManagedChannel channel = GrpcClientFactory.createChannel(config);

// Get metrics interceptor (automatically added)
MetricsInterceptor metricsInterceptor = findMetricsInterceptor(channel);

// Get metrics for specific method
MethodMetrics metrics = metricsInterceptor.getMethodMetrics("myservice.v1.MyService/GetUser");

System.out.println("Total calls: " + metrics.totalCalls());
System.out.println("Success calls: " + metrics.successCalls());
System.out.println("Failed calls: " + metrics.failureCalls());
System.out.println("Average duration: " + metrics.averageDurationMs() + "ms");
System.out.println("Success rate: " + metrics.successRate() + "%");

// Get all metrics
Map<String, MethodMetrics> allMetrics = metricsInterceptor.getAllMetrics();
allMetrics.forEach((method, m) -> {
    System.out.println(method + ": " + m.totalCalls() + " calls");
});

// Reset metrics
metricsInterceptor.reset();
```

Available metrics:
- Total calls
- Success count
- Failure count
- Average duration
- Total duration
- Failures by status code
- Success rate

## Stub Types

### Blocking Stub (Synchronous)

```java
GreeterGrpc.GreeterBlockingStub stub = GrpcClientFactory.createStub(
    channel,
    GreeterGrpc::newBlockingStub,
    config
);

HelloReply response = stub.sayHello(request);
```

### Async Stub (Non-blocking)

```java
GreeterGrpc.GreeterStub stub = GrpcClientFactory.createStub(
    channel,
    GreeterGrpc::newStub,
    config
);

stub.sayHello(request, new StreamObserver<HelloReply>() {
    @Override
    public void onNext(HelloReply reply) {
        System.out.println("Response: " + reply.getMessage());
    }

    @Override
    public void onError(Throwable t) {
        System.err.println("Error: " + t.getMessage());
    }

    @Override
    public void onCompleted() {
        System.out.println("Completed");
    }
});
```

### Future Stub (CompletableFuture)

```java
GreeterGrpc.GreeterFutureStub stub = GrpcClientFactory.createStub(
    channel,
    GreeterGrpc::newFutureStub,
    config
);

ListenableFuture<HelloReply> future = stub.sayHello(request);

Futures.addCallback(future, new FutureCallback<HelloReply>() {
    @Override
    public void onSuccess(HelloReply reply) {
        System.out.println("Response: " + reply.getMessage());
    }

    @Override
    public void onFailure(Throwable t) {
        System.err.println("Error: " + t.getMessage());
    }
}, MoreExecutors.directExecutor());
```

## Channel Management

### Graceful Shutdown

```java
boolean terminated = GrpcClientFactory.shutdownChannel(channel, 10, TimeUnit.SECONDS);

if (terminated) {
    System.out.println("Channel shutdown completed");
} else {
    System.err.println("Channel shutdown timed out");
    GrpcClientFactory.shutdownChannelNow(channel);
}
```

### Immediate Shutdown

```java
GrpcClientFactory.shutdownChannelNow(channel);
```

### Check Channel State

```java
if (channel.isShutdown()) {
    System.out.println("Channel is shutdown");
}

if (channel.isTerminated()) {
    System.out.println("Channel is fully terminated");
}
```

## Error Handling

### Retry Failed Calls

```java
GrpcClientConfiguration config = GrpcClientConfiguration.builder("api.example.com", 443)
    .enableRetry(true)
    .maxRetries(3)
    .retryDelay(Duration.ofMillis(100))
    .maxRetryDelay(Duration.ofSeconds(5))
    .build();

ManagedChannel channel = GrpcClientFactory.createChannel(config);
MyServiceGrpc.MyServiceBlockingStub stub = GrpcClientFactory.createStub(
    channel,
    MyServiceGrpc::newBlockingStub,
    config
);

try {
    MyResponse response = stub.myMethod(request);
} catch (StatusRuntimeException e) {
    if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        // Service unavailable after retries
        System.err.println("Service unavailable: " + e.getMessage());
    }
}
```

### Circuit Breaker Protection

```java
GrpcClientConfiguration config = GrpcClientConfiguration.builder("api.example.com", 443)
    .enableCircuitBreaker(true)
    .circuitBreakerFailureThreshold(5)
    .circuitBreakerWaitDuration(Duration.ofSeconds(60))
    .build();

// Circuit breaker will:
// 1. Track consecutive failures
// 2. Open after 5 failures (all calls fail fast)
// 3. Wait 60 seconds
// 4. Allow test calls (half-open)
// 5. Close if test calls succeed
```

### Handle Timeouts

```java
GrpcClientConfiguration config = GrpcClientConfiguration.builder("api.example.com", 443)
    .callTimeout(Duration.ofSeconds(10))
    .build();

try {
    MyResponse response = stub.myMethod(request);
} catch (StatusRuntimeException e) {
    if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
        System.err.println("Call timed out after 10 seconds");
    }
}
```

## Load Balancing

The client automatically uses round-robin load balancing:

```java
GrpcClientConfiguration config = GrpcClientConfiguration
    .forProduction("api.example.com", 443);

ManagedChannel channel = GrpcClientFactory.createChannel(config);

// Requests are automatically distributed across available backends
// using round-robin load balancing
```

For DNS-based load balancing, use DNS name with multiple A records:

```java
GrpcClientConfiguration config = GrpcClientConfiguration
    .forProduction("grpc-service.internal", 443);

// If grpc-service.internal resolves to multiple IPs,
// requests are distributed across all IPs
```

## Best Practices

### 1. Reuse Channels

Channels are expensive to create. Reuse them:

```java
public class MyServiceClient {
    private final ManagedChannel channel;
    private final MyServiceGrpc.MyServiceBlockingStub stub;

    public MyServiceClient(GrpcClientConfiguration config) {
        this.channel = GrpcClientFactory.createChannel(config);
        this.stub = GrpcClientFactory.createStub(
            channel,
            MyServiceGrpc::newBlockingStub,
            config
        );
    }

    public MyResponse callService(MyRequest request) {
        return stub.myMethod(request);
    }

    public void close() throws InterruptedException {
        GrpcClientFactory.shutdownChannel(channel, 10, TimeUnit.SECONDS);
    }
}
```

### 2. Use Connection Pooling

For high-traffic scenarios, create multiple channels:

```java
public class MyServiceClientPool {
    private final List<ManagedChannel> channels;
    private final AtomicInteger counter = new AtomicInteger();

    public MyServiceClientPool(GrpcClientConfiguration config, int poolSize) {
        this.channels = IntStream.range(0, poolSize)
            .mapToObj(i -> GrpcClientFactory.createChannel(config))
            .collect(Collectors.toList());
    }

    public MyServiceGrpc.MyServiceBlockingStub getStub() {
        int index = Math.abs(counter.getAndIncrement() % channels.size());
        return MyServiceGrpc.newBlockingStub(channels.get(index));
    }

    public void closeAll() throws InterruptedException {
        for (ManagedChannel channel : channels) {
            GrpcClientFactory.shutdownChannel(channel, 10, TimeUnit.SECONDS);
        }
    }
}
```

### 3. Configure Timeouts Appropriately

```java
GrpcClientConfiguration config = GrpcClientConfiguration.builder("api.example.com", 443)
    .callTimeout(Duration.ofSeconds(30)) // Per-call timeout
    .idleTimeout(Duration.ofMinutes(10)) // Idle connection timeout
    .build();
```

### 4. Enable TLS in Production

```java
GrpcClientConfiguration config = GrpcClientConfiguration.builder("api.example.com", 443)
    .enableTls(true)
    .build();
```

### 5. Monitor Metrics

```java
// Schedule metrics reporting
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(() -> {
    Map<String, MethodMetrics> metrics = metricsInterceptor.getAllMetrics();
    metrics.forEach((method, m) -> {
        System.out.println(String.format(
            "%s: %d calls, %.2f%% success, %.2fms avg",
            method, m.totalCalls(), m.successRate(), m.averageDurationMs()
        ));
    });
}, 0, 60, TimeUnit.SECONDS);
```

### 6. Handle Shutdown Gracefully

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    try {
        boolean terminated = GrpcClientFactory.shutdownChannel(channel, 10, TimeUnit.SECONDS);
        if (!terminated) {
            GrpcClientFactory.shutdownChannelNow(channel);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}));
```

## Spring Integration

### Bean Configuration

```java
@Configuration
public class GrpcClientConfig {

    @Bean
    public GrpcClientConfiguration grpcClientConfiguration(
            @Value("${grpc.server.host}") String host,
            @Value("${grpc.server.port}") int port) {
        return GrpcClientConfiguration.builder(host, port)
            .enableTls(true)
            .enableRetry(true)
            .maxRetries(3)
            .retryDelay(Duration.ofMillis(100))
            .maxRetryDelay(Duration.ofSeconds(5))
            .enableCircuitBreaker(true)
            .circuitBreakerFailureThreshold(5)
            .circuitBreakerWaitDuration(Duration.ofSeconds(60))
            .build();
    }

    @Bean
    public ManagedChannel grpcChannel(GrpcClientConfiguration config) {
        return GrpcClientFactory.createChannel(config);
    }

    @Bean
    public MyServiceGrpc.MyServiceBlockingStub myServiceStub(
            ManagedChannel channel,
            GrpcClientConfiguration config) {
        return GrpcClientFactory.createStub(
            channel,
            MyServiceGrpc::newBlockingStub,
            config
        );
    }

    @PreDestroy
    public void cleanup() throws InterruptedException {
        ManagedChannel channel = grpcChannel(grpcClientConfiguration(null, 0));
        GrpcClientFactory.shutdownChannel(channel, 10, TimeUnit.SECONDS);
    }
}
```

### Application Properties

```properties
grpc.server.host=api.example.com
grpc.server.port=443
```

## Troubleshooting

### Connection Refused

```
io.grpc.StatusRuntimeException: UNAVAILABLE: Connection refused
```

Solutions:
- Verify server is running
- Check host and port configuration
- Verify firewall rules
- Check network connectivity

### Certificate Verification Failed

```
io.grpc.StatusRuntimeException: UNAVAILABLE: SSL handshake failed
```

Solutions:
- Verify TLS is enabled
- Check certificate validity
- Verify server hostname matches certificate
- Add custom trust store if using self-signed certificates

### Deadline Exceeded

```
io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED: deadline exceeded after X seconds
```

Solutions:
- Increase call timeout: `.callTimeout(Duration.ofSeconds(60))`
- Optimize server-side processing
- Check network latency

### Message Too Large

```
io.grpc.StatusRuntimeException: RESOURCE_EXHAUSTED: message exceeds maximum size
```

Solutions:
- Increase max message size: `.maxInboundMessageSize(16 * 1024 * 1024)`
- Reduce request/response payload
- Use streaming for large data

### Too Many Failures

```
Circuit breaker is open, failing fast
```

Solutions:
- Check server health
- Increase failure threshold: `.circuitBreakerFailureThreshold(10)`
- Reduce wait duration: `.circuitBreakerWaitDuration(Duration.ofSeconds(30))`

## Example: Complete Client

```java
package com.example.grpc.client;

import com.marcusprado02.commons.adapters.grpc.client.GrpcClientConfiguration;
import com.marcusprado02.commons.adapters.grpc.client.GrpcClientFactory;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class GreeterClient {
    private static final Logger logger = Logger.getLogger(GreeterClient.class.getName());

    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub stub;

    public GreeterClient(String host, int port) {
        GrpcClientConfiguration config = GrpcClientConfiguration.builder(host, port)
            .enableTls(true)
            .callTimeout(Duration.ofSeconds(30))
            .idleTimeout(Duration.ofMinutes(10))
            .enableRetry(true)
            .maxRetries(3)
            .retryDelay(Duration.ofMillis(100))
            .maxRetryDelay(Duration.ofSeconds(5))
            .enableCircuitBreaker(true)
            .circuitBreakerFailureThreshold(5)
            .circuitBreakerWaitDuration(Duration.ofSeconds(60))
            .userAgent("greeter-client/1.0.0")
            .build();

        this.channel = GrpcClientFactory.createChannel(config);
        this.stub = GrpcClientFactory.createStub(
            channel,
            GreeterGrpc::newBlockingStub,
            config
        );
    }

    public String greet(String name) {
        HelloRequest request = HelloRequest.newBuilder()
            .setName(name)
            .build();

        try {
            HelloReply response = stub.sayHello(request);
            return response.getMessage();
        } catch (StatusRuntimeException e) {
            logger.warning("RPC failed: " + e.getStatus());
            throw e;
        }
    }

    public void shutdown() throws InterruptedException {
        GrpcClientFactory.shutdownChannel(channel, 10, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        GreeterClient client = new GreeterClient("localhost", 9090);

        try {
            String response = client.greet("World");
            logger.info("Response: " + response);
        } finally {
            client.shutdown();
        }
    }
}
```

## Dependencies

Core dependencies:
- gRPC Java 1.60.0
- Protocol Buffers 3.25.1

Optional dependencies:
- Resilience4j 2.1.0 (for circuit breaker and retry)

## License

MIT License
